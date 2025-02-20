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

import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.provider.MediaStore.MediaColumns
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

class MediaManager(context: Context) {

    private val contentResolver = context.contentResolver
    private val handler = Handler(context.mainLooper)

    internal fun isUriReady(uri: Uri): Boolean {
        return getReadyCursor(uri)?.use { c ->
            isCursorReady(c)
        } ?: return false
    }

    private fun isCursorReady(c: Cursor): Boolean {
        c.moveToNext()
        return c.getInt(0) == 0
    }

    private fun getReadyCursor(uri: Uri): Cursor? {
        return contentResolver.query(uri, arrayOf(MediaColumns.IS_PENDING), null, null, null)
    }

    internal fun whenReady(uri: Uri, block: () -> Unit) {
        Log.e(TAG, "waitForUriToBecomeReady $uri")
        val cursor = getReadyCursor(uri) ?: error("ready cursor null for $uri")
        if (isCursorReady(cursor)) {
            Log.d(TAG, "cursor was ready early for $uri")
            block()
            return
        }
        val executedBlock = AtomicBoolean(false)
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                Log.e(TAG, "uri changed while waiting: $uri")
                if (!executedBlock.get() && isUriReady(uri)) {
                    // need to check if still active, because fires several times
                    if (executedBlock.compareAndSet(false, true)) block()
                    cursor.unregisterContentObserver(this)
                }
            }
        }
        cursor.registerContentObserver(observer)
    }
}
