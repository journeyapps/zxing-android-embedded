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

package com.google.zxing.client.android.camera.open;

import android.hardware.Camera;
import android.util.Log;

public final class OpenCameraInterface {

    private static final String TAG = OpenCameraInterface.class.getName();

    private OpenCameraInterface() {
    }

    /**
     * For {@link #open(int)}, means no preference for which camera to open.
     */
    public static final int NO_REQUESTED_CAMERA = -1;

    public static int getCameraId(int requestedId) {
        int numCameras = Camera.getNumberOfCameras();
        if (numCameras == 0) {
            Log.w(TAG, "No cameras!");
            return -1;
        }

        int cameraId = requestedId;

        boolean explicitRequest = cameraId >= 0;

        if (!explicitRequest) {
            // Select a camera if no explicit camera requested
            int index = 0;
            while (index < numCameras) {
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                Camera.getCameraInfo(index, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    break;
                }
                index++;
            }

            cameraId = index;
        }

        if (cameraId < numCameras) {
            return cameraId;
        } else {
            if (explicitRequest) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    /**
     * Opens the requested camera with {@link Camera#open(int)}, if one exists.
     *
     * @param requestedId camera ID of the camera to use. A negative value
     *                    or {@link #NO_REQUESTED_CAMERA} means "no preference"
     * @return handle to {@link Camera} that was opened
     */
    public static Camera open(int requestedId) {
        int cameraId = getCameraId(requestedId);
        if (cameraId == -1) {
            return null;
        } else {
            return Camera.open(cameraId);
        }
    }

}
