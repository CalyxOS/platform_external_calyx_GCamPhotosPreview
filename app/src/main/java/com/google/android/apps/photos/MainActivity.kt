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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import com.google.android.apps.photos.databinding.ActivityMainBinding

const val TAG = "GCamPhotosPreview"

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
        startActivity(i)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.e(TAG, "onNewIntent: $intent")
    }
}
