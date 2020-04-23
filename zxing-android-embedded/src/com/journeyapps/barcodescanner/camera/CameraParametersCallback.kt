package com.journeyapps.barcodescanner.camera

import android.hardware.Camera

/**
 * Callback for [Camera.Parameters].
 */
interface CameraParametersCallback {
    /**
     * Changes the settings for Camera.
     *
     * @param parameters [Camera.Parameters].
     * @return [Camera.Parameters] with arguments.
     */
    fun changeCameraParameters(parameters: Camera.Parameters): Camera.Parameters
}