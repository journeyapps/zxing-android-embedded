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

package com.journeyapps.barcodescanner.camera;

import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;

/**
 * This should be created and used from the camera thread only. The thread message queue is used
 * to run all operations on the same thread.
 */
public final class AutoFocusManager {

    private static final String TAG = AutoFocusManager.class.getSimpleName();

    private static final long AUTO_FOCUS_INTERVAL_MS = 2000L;

    private boolean stopped;
    private boolean focusing;
    private final boolean useAutoFocus;
    private final Camera camera;
    private Handler handler;

    private int MESSAGE_FOCUS = 1;

    private static final Collection<String> FOCUS_MODES_CALLING_AF;

    static {
        FOCUS_MODES_CALLING_AF = new ArrayList<>(2);
        FOCUS_MODES_CALLING_AF.add(Camera.Parameters.FOCUS_MODE_AUTO);
        FOCUS_MODES_CALLING_AF.add(Camera.Parameters.FOCUS_MODE_MACRO);
    }

    private final Handler.Callback focusHandlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == MESSAGE_FOCUS) {
                focus();
                return true;
            }
            return false;
        }
    };

    private final Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera theCamera) {
            handler.post(() -> {
                focusing = false;
                autoFocusAgainLater();
            });
        }
    };

    public AutoFocusManager(Camera camera, CameraSettings settings) {
        this.handler = new Handler(focusHandlerCallback);
        this.camera = camera;
        String currentFocusMode = camera.getParameters().getFocusMode();
        useAutoFocus = settings.isAutoFocusEnabled() && FOCUS_MODES_CALLING_AF.contains(currentFocusMode);
        Log.i(TAG, "Current focus mode '" + currentFocusMode + "'; use auto focus? " + useAutoFocus);
        start();
    }

    private synchronized void autoFocusAgainLater() {
        if (!stopped && !handler.hasMessages(MESSAGE_FOCUS)) {
            handler.sendMessageDelayed(handler.obtainMessage(MESSAGE_FOCUS), AUTO_FOCUS_INTERVAL_MS);
        }
    }

    /**
     * Start auto-focus. The first focus will happen now, then repeated every two seconds.
     */
    public void start() {
        stopped = false;
        focus();
    }

    private void focus() {
        if (useAutoFocus) {
            if (!stopped && !focusing) {
                try {
                    camera.autoFocus(autoFocusCallback);
                    focusing = true;
                } catch (RuntimeException re) {
                    // Have heard RuntimeException reported in Android 4.0.x+; continue?
                    Log.w(TAG, "Unexpected exception while focusing", re);
                    // Try again later to keep cycle going
                    autoFocusAgainLater();
                }
            }
        }
    }

    private void cancelOutstandingTask() {
        handler.removeMessages(MESSAGE_FOCUS);
    }

    /**
     * Stop auto-focus.
     */
    public void stop() {
        stopped = true;
        focusing = false;
        cancelOutstandingTask();
        if (useAutoFocus) {
            // Doesn't hurt to call this even if not focusing
            try {
                camera.cancelAutoFocus();
            } catch (RuntimeException re) {
                // Have heard RuntimeException reported in Android 4.0.x+; continue?
                Log.w(TAG, "Unexpected exception while cancelling focusing", re);
            }
        }
    }
}
