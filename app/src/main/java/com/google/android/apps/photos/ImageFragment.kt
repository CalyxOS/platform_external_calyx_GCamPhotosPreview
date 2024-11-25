/*
 * Copyright 2021 The Calyx Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.photos

import android.app.Activity.RESULT_OK
import android.app.KeyguardManager
import android.app.KeyguardManager.KeyguardDismissCallback
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_EDIT
import android.content.Intent.ACTION_SEND
import android.content.Intent.ACTION_VIEW
import android.content.Intent.EXTRA_STREAM
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.transition.TransitionManager
import android.util.Log
import android.util.Size
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.ORIENTATION_USE_EXIF
import com.google.android.apps.photos.databinding.FragmentImageBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

class ImageFragment : Fragment(R.layout.fragment_image) {

    companion object {
        fun newInstance(id: Long) = ImageFragment().apply {
            arguments = Bundle().apply {
                putLong("id", id)
            }
        }
    }

    private val viewModel: MainViewModel by activityViewModels()

    private var _binding: FragmentImageBinding? = null
    private val binding get() = _binding!!

    private var currentItem: PagerItem.UriItem? = null

    private val deletionLauncher = registerForActivityResult(StartIntentSenderForResult()) {
        if (it.resultCode == RESULT_OK) viewModel.onItemDeleted(currentItem!!)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentImageBinding.bind(view)

        // our fragments don't get recreated for changes, so listen to changes here
        val id = requireArguments().getLong("id")
        viewModel.items.onEach { items ->
            val item = items.find { it.id == id } as PagerItem.UriItem?
            if (item != null) setItem(item)
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        viewModel.showBottomBar.onEach { show ->
            TransitionManager.beginDelayedTransition(view as ViewGroup)
            binding.actionBar.isVisible = show
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setItem(item: PagerItem.UriItem) {
        if (currentItem == item) return
        currentItem = item
        if (item.ready) {
            binding.progressBar.visibility = INVISIBLE
            if (item.mimeType?.startsWith("video") == true) {
                setVideo(item)
            } else {
                try {
                    binding.imageView.orientation = ORIENTATION_USE_EXIF
                    binding.imageView.setImage(ImageSource.uri(item.uri))
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting image", e)
                }
            }
            binding.imageView.setOnClickListener {
                viewModel.toggleBottomBar()
            }
        } else {
            binding.progressBar.visibility = VISIBLE
            binding.actionBar.visibility = GONE
        }
        binding.editButton.setOnClickListener { onEditButtonClicked(item) }
        binding.shareButton.setOnClickListener { onShareButtonClicked(item) }
        binding.deleteButton.setOnClickListener { onDeleteButtonClicked(item) }
    }

    private fun setVideo(item: PagerItem.UriItem) {
        binding.playButton.visibility = VISIBLE
        binding.playButton.setOnClickListener {
            doOrUnlockFirst {
                val intent = Intent(ACTION_VIEW).apply {
                    setDataAndType(item.uri, item.mimeType)
                    addFlags(FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivityOrToast(intent)
            }
        }
        val contentResolver = requireContext().contentResolver
        val metrics = requireActivity().windowManager.currentWindowMetrics
        val size = Size(metrics.bounds.width(), metrics.bounds.height())
        lifecycleScope.launchWhenStarted {
            withContext(Dispatchers.IO) {
                @Suppress("BlockingMethodInNonBlockingContext")
                val b = contentResolver.loadThumbnail(item.uri, size, null)
                withContext(Dispatchers.Main) {
                    binding.imageView.setImage(ImageSource.bitmap(b))
                }
            }
        }
    }

    private fun onEditButtonClicked(item: PagerItem.UriItem) = doOrUnlockFirst {
        val intent = Intent(ACTION_EDIT).apply {
            setDataAndType(item.uri, item.mimeType)
            addFlags(FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (item.mimeType?.startsWith("video") == true) {
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                intent.action = "com.android.camera.action.TRIM"
                startActivityOrToast(intent)
            }
        } else {
            startActivityOrToast(intent)
        }
    }

    private fun onShareButtonClicked(item: PagerItem.UriItem) = doOrUnlockFirst {
        val intent = Intent(ACTION_SEND).apply {
            type = item.mimeType
            putExtra(EXTRA_STREAM, item.uri)
            addFlags(FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivityOrToast(Intent.createChooser(intent, null))
    }

    private fun onDeleteButtonClicked(item: PagerItem.UriItem) = doOrUnlockFirst {
        requestDeletion(requireContext(), item.uri)
    }

    private fun requestDeletion(context: Context, uri: Uri) {
        val contentResolver = context.contentResolver
        val intent = MediaStore.createDeleteRequest(contentResolver, listOf(uri))
        val request = IntentSenderRequest.Builder(intent).build()
        deletionLauncher.launch(request)
    }

    private fun doOrUnlockFirst(block: () -> Unit) {
        val context = requireActivity()
        val km = getSystemService(context, KeyguardManager::class.java)!!
        if (km.isDeviceLocked) {
            // If the device is locked, the deletion request won't work.
            // We are not allowed to delete the file ourselves,
            // so the only other option would be getting MANAGE_EXTERNAL_STORAGE permission.
            // Here we just ask for unlocking instead for now.
            km.requestDismissKeyguard(context, object : KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    block()
                }
            })
        } else {
            block()
        }
    }

    private fun startActivityOrToast(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.activity_not_found, LENGTH_LONG).show()
        }
    }

}
