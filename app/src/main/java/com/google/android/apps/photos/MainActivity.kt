/*
 * SPDX-FileCopyrightText: 2021-2026 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.apps.photos

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager.MATCH_ALL
import android.os.Bundle
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Toast
import com.google.android.apps.photos.databinding.ActivityMainBinding

const val TAG = "GCamPhotosPreview"
private const val CALYX_PACKAGE_NAME = "org.calyxos.glimpse"
private const val LOS_PACKAGE_NAME = "org.lineageos.glimpse"

class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding

    @SuppressLint("UnsafeIntentLaunch") // we re-construct the intent in [IntentHandler]
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) return // not newly created, don't react

        // handle intent
        val uri = intent.data
        if (intent.action?.contains("REVIEW") == true && uri != null) {
            if (IntentHandler.isSecure(intent)) setShowWhenLocked(true)
            val mediaManager = MediaManager(applicationContext)
            if (mediaManager.isUriReady(uri)) {
                onUriReady(intent)
            } else {
                // not ready, show progress bar until ready
                binding.progressBar.visibility = VISIBLE
                mediaManager.whenReady(uri) {
                    binding.progressBar.visibility = INVISIBLE
                    onUriReady(intent)
                }
            }
        } else {
            @SuppressLint("SetTextI18n")
            binding.textView.text = "Unknown intent:\n\n$intent"
            binding.textView.visibility = VISIBLE
        }
    }

    private fun onUriReady(intent: Intent) {
        val i = IntentHandler.rewriteIntent(intent)
        // see what activities would handle the intent
        val resolveInfos = packageManager.queryIntentActivities(i, MATCH_ALL)
        if (resolveInfos.isEmpty()) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show()
        } else {
            val packageNames = listOf(
                CALYX_PACKAGE_NAME,
                LOS_PACKAGE_NAME,
            )
            IntentHandler.setPackageName(resolveInfos, i, packageNames)
            startActivity(i)
        }
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.e(TAG, "onNewIntent: $intent")
    }
}
