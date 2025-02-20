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

import android.content.ClipData
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log

private const val SECURE_MODE = "com.google.android.apps.photos.api.secure_mode"
private const val EXTRA_SECURE_IDS = "com.google.android.apps.photos.api.secure_mode_ids"
private const val EXTRA_PROCESSING = "processing_uri_intent_extra"

private val FILES_EXTERNAL_CONTENT_URI = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

object IntentHandler {

    fun isSecure(intent: Intent) = intent.getBooleanExtra(SECURE_MODE, false)

    fun rewriteIntent(intent: Intent): Intent {
        if (BuildConfig.DEBUG) log(intent)

        // TODO accessing the processing preview throws SecurityException, because uses signature allow-list
        val processingUri = intent.getParcelableExtra(EXTRA_PROCESSING, Uri::class.java)

        val clipData = if (isSecure(intent)) {
            val mainId = intent.data?.lastPathSegment?.toLongOrNull()
            val secureIds = intent.extras?.getSerializable(EXTRA_SECURE_IDS, LongArray::class.java)
                ?.filter { id -> id != mainId } // don't include main Uri again
            if (secureIds.isNullOrEmpty()) {
                null
            } else {
                // clip data needs to be constructed with first Uri
                val clipDataUri =
                    ContentUris.withAppendedId(FILES_EXTERNAL_CONTENT_URI, secureIds[0])
                ClipData.newRawUri("", clipDataUri).apply {
                    // now take rest of secureIds to add ClipData Items
                    secureIds.subList(1, secureIds.size).forEach { secureId ->
                        val uri = ContentUris.withAppendedId(FILES_EXTERNAL_CONTENT_URI, secureId)
                        val item = ClipData.Item.Builder().setUri(uri).build()
                        addItem(item)
                    }
                }
            }
        } else {
            null // only needed to pass in media captured while phone was locked
        }

        // let's create a new intent that has only what we need
        val newIntent = Intent().apply {
            action = intent.action // use original action, should be safe
            data = intent.data // use original URI, should be safe
            this.clipData = clipData
            setPackage("org.calyxos.glimpse") // TODO don't hardcode
        }

        return newIntent
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
