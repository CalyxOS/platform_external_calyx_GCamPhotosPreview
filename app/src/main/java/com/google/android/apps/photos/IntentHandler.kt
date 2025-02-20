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

import android.content.Intent
import android.net.Uri
import android.util.Log

private const val SECURE_MODE = "com.google.android.apps.photos.api.secure_mode"
private const val EXTRA_PROCESSING = "processing_uri_intent_extra"

object IntentHandler {

    fun isSecure(intent: Intent) = intent.getBooleanExtra(SECURE_MODE, false)

    fun rewriteIntent(intent: Intent): Intent {
        if (BuildConfig.DEBUG) log(intent)

        // TODO accessing the processing preview throws SecurityException, because uses signature allow-list
        val processingUri = intent.getParcelableExtra(EXTRA_PROCESSING, Uri::class.java)

        intent.component = null // remove hard-coded component which includes our own package name
        intent.setPackage("org.calyxos.glimpse.dev") // TODO don't hardcode

        return intent
    }

    private fun log(intent: Intent?) {
        Log.e(TAG, "intent: $intent")
        val extras = intent?.extras ?: return
        extras.keySet().forEach { key ->
            @Suppress("DEPRECATION")
            val value = extras.get(key)
            Log.e(TAG, "$key: $value")
        }
    }

}
