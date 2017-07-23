package com.journeyapps.barcodescanner.camera;

import android.hardware.Camera;

/**
 * Callback for {@link Camera.Parameters}.
 */
public interface CameraParametersCallback {

    /**
     * Changes the settings for Camera.
     *
     * @param parameters {@link Camera.Parameters}.
     * @return {@link Camera.Parameters} with arguments.
     */
    Camera.Parameters changeCameraParameters(Camera.Parameters parameters);
}