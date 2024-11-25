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

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SCREEN_OFF
import android.content.IntentFilter
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.util.Log
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.apps.photos.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

const val TAG = "GCamPhotosPreview"

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding

    private var shutdownReceiverRegistered = false

    private val requestPermissionLauncher = registerForActivityResult(RequestPermission()) { ok ->
        if (ok) {
            viewModel.onNewIntent(intent)
        } else {
            Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // set-up view pager
        val viewPagerAdapter = ViewPagerAdapter(this)
        binding.viewPager.offscreenPageLimit = 1 // pre-load prev/next page automatically
        binding.viewPager.adapter = viewPagerAdapter

        // observe flow
        viewModel.items.onEach { items ->
            val wasEmpty = viewPagerAdapter.itemCount == 0
            Log.d(TAG, "new list: $items wasEmpty=$wasEmpty")
            viewPagerAdapter.submitList(items) {
                if (wasEmpty) binding.viewPager.setCurrentItem(1, false)
            }
        }.launchIn(lifecycleScope)

        if (savedInstanceState != null) return // not newly created, don't react

        // handle intent
        if (intent.action?.contains("REVIEW") == true) {
            if (viewModel.isSecure(intent)) onLaunchedWhileLocked()

            if (checkSelfPermission(READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {
                viewModel.onNewIntent(intent)
            } else {
                requestPermissionLauncher.launch(READ_EXTERNAL_STORAGE)
            }
        } else {
            @SuppressLint("SetTextI18n")
            binding.textView.text = "Unknown intent:\n\n$intent"
            binding.textView.visibility = VISIBLE
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.e(TAG, "onNewIntent: $intent")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (shutdownReceiverRegistered) unregisterReceiver(shutdownReceiver)
    }

    /**
     * Close activity when secure app passes lock screen or screen turns off.
     */
    private val shutdownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "shutdownReceiver: finish()")
            finish()
        }
    }

    private fun onLaunchedWhileLocked() {
        setShowWhenLocked(true)

        // Filter for screen off so that we can finish activity when screen is off.
        registerReceiver(shutdownReceiver, IntentFilter(ACTION_SCREEN_OFF))
        shutdownReceiverRegistered = true
    }

    private inner class ViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

        private val diffCallback = PagerItemCallback()
        private val differ: AsyncListDiffer<PagerItem> = AsyncListDiffer(this, diffCallback)

        override fun getItemId(position: Int): Long {
            return differ.currentList[position].id
        }

        override fun containsItem(itemId: Long): Boolean {
            return differ.currentList.any { it.id == itemId }
        }

        override fun getItemCount(): Int = differ.currentList.size

        fun submitList(list: List<PagerItem>?, commitCallback: Runnable? = null) {
            differ.submitList(list, commitCallback)
        }

        override fun createFragment(position: Int): Fragment {
            val item = differ.currentList[position]
            Log.d(TAG, "createFragment $position $item")
            return if (item is PagerItem.CamItem) CamFragment.newInstance(item.pendingIntent)
            else ImageFragment.newInstance(item.id)
        }
    }

}
