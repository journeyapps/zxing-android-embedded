/*
 * Copyright (C) 2012 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.journeyapps.barcodescanner.camera

import android.hardware.Camera
import android.os.Handler
import android.util.Log
import java.util.*

/**
 * This should be created and used from the camera thread only. The thread message queue is used
 * to run all operations on the same thread.
 */
class AutoFocusManager(var camera: Camera?, settings: CameraSettings) {
    private var stopped = false
    private var focusing = false
    private val useAutoFocus: Boolean
    private val handler: Handler
    private val MESSAGE_FOCUS = 1

    companion object {
        private val TAG = AutoFocusManager::class.java.simpleName
        private const val AUTO_FOCUS_INTERVAL_MS = 2000L
        private var FOCUS_MODES_CALLING_AF: MutableCollection<String>? = null

        init {
            FOCUS_MODES_CALLING_AF = ArrayList(2)
            FOCUS_MODES_CALLING_AF!!.add(Camera.Parameters.FOCUS_MODE_AUTO)
            FOCUS_MODES_CALLING_AF!!.add(Camera.Parameters.FOCUS_MODE_MACRO)
        }
    }

    private val focusHandlerCallback = Handler.Callback { msg ->
        if (msg.what == MESSAGE_FOCUS) {
            focus()
            return@Callback true
        }
        false
    }


    init {
        handler = Handler(focusHandlerCallback)
        val currentFocusMode = camera?.parameters?.focusMode
        useAutoFocus = settings.autoFocusEnabled && FOCUS_MODES_CALLING_AF!!.contains(currentFocusMode)
        Log.i(TAG, "Current focus mode '$currentFocusMode'; use auto focus? $useAutoFocus")
        start()
    }

    private val autoFocusCallback = Camera.AutoFocusCallback { _, _ ->
        handler.post {
            focusing = false
            autoFocusAgainLater()
        }
    }

    @Synchronized
    private fun autoFocusAgainLater() {
        if (!stopped && !handler.hasMessages(MESSAGE_FOCUS)) {
            handler.sendMessageDelayed(handler.obtainMessage(MESSAGE_FOCUS), AUTO_FOCUS_INTERVAL_MS)
        }
    }

    /**
     * Start auto-focus. The first focus will happen now, then repeated every two seconds.
     */
    fun start() {
        stopped = false
        focus()
    }

    private fun focus() {
        if (useAutoFocus) {
            if (!stopped && !focusing) {
                try {
                    camera?.autoFocus(autoFocusCallback)
                    focusing = true
                } catch (re: RuntimeException) {
                    // Have heard RuntimeException reported in Android 4.0.x+; continue?
                    Log.w(TAG, "Unexpected exception while focusing", re)
                    // Try again later to keep cycle going
                    autoFocusAgainLater()
                }
            }
        }
    }

    private fun cancelOutstandingTask() {
        handler.removeMessages(MESSAGE_FOCUS)
    }

    /**
     * Stop auto-focus.
     */
    fun stop() {
        stopped = true
        focusing = false
        cancelOutstandingTask()
        if (useAutoFocus) {
            // Doesn't hurt to call this even if not focusing
            try {
                camera?.cancelAutoFocus()
            } catch (re: RuntimeException) {
                // Have heard RuntimeException reported in Android 4.0.x+; continue?
                Log.w(TAG, "Unexpected exception while cancelling focusing", re)
            }
        }
    }
}