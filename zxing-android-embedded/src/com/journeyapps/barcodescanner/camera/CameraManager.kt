/*
 * Copyright (C) 2008 ZXing authors
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

import android.content.Context
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import com.google.zxing.client.android.AmbientLightManager
import com.google.zxing.client.android.camera.CameraConfigurationUtils
import com.google.zxing.client.android.camera.open.OpenCameraInterface
import com.journeyapps.barcodescanner.Size
import com.journeyapps.barcodescanner.SourceData
import java.io.IOException
import java.util.*

/**
 * Wrapper to manage the Camera. This is not thread-safe, and the methods must always be called
 * from the same thread.
 *
 *
 * Call order:
 *
 * 1. setCameraSettings()
 * 2. open(), set desired preview size (any order)
 * 3. configure(), setPreviewDisplay(holder) (any order)
 * 4. startPreview()
 * 5. requestPreviewFrame (repeat)
 * 6. stopPreview()
 * 7. close()
 */
class CameraManager(private val context: Context) {
    /**
     * Returns the Camera. This returns null if the camera is not opened yet, failed to open, or has
     * been closed.
     *
     * @return the Camera
     */
    var camera: Camera? = null
        private set
    private var cameraInfo: CameraInfo? = null
    private var autoFocusManager: AutoFocusManager? = null
    private var ambientLightManager: AmbientLightManager? = null
    private var previewing = false
    private var defaultParameters: String? = null

    // User parameters
    var cameraSettings = CameraSettings()
    var displayConfiguration: DisplayConfiguration? = null

    // Actual chosen preview size
    private var requestedPreviewSize: Size? = null

    /**
     * Actual preview size in *natural camera* orientation. null if not determined yet.
     *
     * @return preview size
     */
    var naturalPreviewSize: Size? = null
        private set

    /**
     *
     * @return the camera rotation relative to display rotation, in degrees. Typically 0 if the
     * display is in landscape orientation.
     */
    var cameraRotation = -1 // camera rotation vs display rotation
        private set

    private inner class CameraPreviewCallback : Camera.PreviewCallback {
        private var callback: PreviewCallback? = null
        private var resolution: Size? = null
        fun setResolution(resolution: Size?) {
            this.resolution = resolution
        }

        fun setCallback(callback: PreviewCallback?) {
            this.callback = callback
        }

        override fun onPreviewFrame(data: ByteArray, camera: Camera) {
            val cameraResolution = resolution
            val callback = callback
            if (cameraResolution != null && callback != null) {
                try {
                    val format = camera.parameters.previewFormat
                    val source = SourceData(data, cameraResolution.width, cameraResolution.height, format, cameraRotation)
                    if (cameraInfo!!.facing == CameraInfo.CAMERA_FACING_FRONT) {
                        source.isPreviewMirrored = true
                    }
                    callback.onPreview(source)
                } catch (e: RuntimeException) {
                    // Could be:
                    // java.lang.RuntimeException: getParameters failed (empty parameters)
                    // IllegalArgumentException: Image data does not match the resolution
                    Log.e(TAG, "Camera preview failed", e)
                    callback.onPreviewError(e)
                }
            } else {
                Log.d(TAG, "Got preview callback, but no handler or resolution available")
                callback?.onPreviewError(Exception("No resolution available"))
            }
        }
    }

    /**
     * Preview frames are delivered here, which we pass on to the registered handler. Make sure to
     * clear the handler so it will only receive one message.
     */
    private val cameraPreviewCallback: CameraPreviewCallback

    /**
     * Must be called from camera thread.
     */
    fun open() {
        camera = OpenCameraInterface.open(cameraSettings.requestedCameraId)
        if (camera == null) {
            throw RuntimeException("Failed to open camera")
        }
        val cameraId = OpenCameraInterface.getCameraId(cameraSettings.requestedCameraId)
        cameraInfo = CameraInfo()
        Camera.getCameraInfo(cameraId, cameraInfo)
    }

    /**
     * Configure the camera parameters, including preview size.
     *
     * The camera must be opened before calling this.
     *
     * Must be called from camera thread.
     */
    fun configure() {
        if (camera == null) {
            throw RuntimeException("Camera not open")
        }
        setParameters()
    }

    /**
     * Must be called from camera thread.
     */
    @Throws(IOException::class)
    fun setPreviewDisplay(holder: SurfaceHolder) {
        setPreviewDisplay(CameraSurface(holder))
    }

    @Throws(IOException::class)
    fun setPreviewDisplay(surface: CameraSurface) {
        camera?.let { surface.setPreview(it) }
    }

    /**
     * Asks the camera hardware to begin drawing preview frames to the screen.
     *
     * Must be called from camera thread.
     */
    fun startPreview() {
        val theCamera = camera
        if (theCamera != null && !previewing) {
            theCamera.startPreview()
            previewing = true
            autoFocusManager = AutoFocusManager(camera, cameraSettings)
            ambientLightManager = AmbientLightManager(context, this, cameraSettings)
            ambientLightManager!!.start()
        }
    }

    /**
     * Tells the camera to stop drawing preview frames.
     *
     * Must be called from camera thread.
     */
    fun stopPreview() {
        if (autoFocusManager != null) {
            autoFocusManager!!.stop()
            autoFocusManager = null
        }
        if (ambientLightManager != null) {
            ambientLightManager!!.stop()
            ambientLightManager = null
        }
        if (camera != null && previewing) {
            camera!!.stopPreview()
            cameraPreviewCallback.setCallback(null)
            previewing = false
        }
    }

    /**
     * Closes the camera driver if still in use.
     *
     * Must be called from camera thread.
     */
    fun close() {
        if (camera != null) {
            camera!!.release()
            camera = null
        }
    }

    /**
     * @return true if the camera rotation is perpendicular to the current display rotation.
     */
    val isCameraRotated: Boolean
        get() {
            check(cameraRotation != -1) { "Rotation not calculated yet. Call configure() first." }
            return cameraRotation % 180 != 0
        }

    private val defaultCameraParameters: Camera.Parameters
        private get() {
            val parameters = camera!!.parameters
            if (defaultParameters == null) {
                defaultParameters = parameters.flatten()
            } else {
                parameters.unflatten(defaultParameters)
            }
            return parameters
        }

    private fun setDesiredParameters(safeMode: Boolean) {
        val parameters = defaultCameraParameters
        Log.i(TAG, "Initial camera parameters: " + parameters.flatten())
        if (safeMode) {
            Log.w(TAG, "In camera config safe mode -- most settings will not be honored")
        }
        CameraConfigurationUtils.setFocus(parameters, cameraSettings.focusMode, safeMode)
        if (!safeMode) {
            CameraConfigurationUtils.setTorch(parameters, false)
            if (cameraSettings.isScanInverted) {
                CameraConfigurationUtils.setInvertColor(parameters)
            }
            if (cameraSettings.isBarcodeSceneModeEnabled) {
                CameraConfigurationUtils.setBarcodeSceneMode(parameters)
            }
            if (cameraSettings.isMeteringEnabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    CameraConfigurationUtils.setVideoStabilization(parameters)
                    CameraConfigurationUtils.setFocusArea(parameters)
                    CameraConfigurationUtils.setMetering(parameters)
                }
            }
        }
        val previewSizes = getPreviewSizes(parameters)
        if (previewSizes.isEmpty()) {
            requestedPreviewSize = null
        } else {
            requestedPreviewSize = displayConfiguration!!.getBestPreviewSize(previewSizes, isCameraRotated)
            parameters.setPreviewSize(requestedPreviewSize!!.width, requestedPreviewSize!!.height)
        }
        if (Build.DEVICE == "glass-1") {
            // We need to set the FPS on Google Glass devices, otherwise the preview is scrambled.
            // FIXME - can/should we do this for other devices as well?
            CameraConfigurationUtils.setBestPreviewFPS(parameters)
        }
        Log.i(TAG, "Final camera parameters: " + parameters.flatten())
        camera!!.parameters = parameters
    }

    private fun calculateDisplayRotation(): Int {
        // http://developer.android.com/reference/android/hardware/Camera.html#setDisplayOrientation(int)
        val rotation = displayConfiguration!!.rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        var result: Int
        if (cameraInfo!!.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo!!.orientation + degrees) % 360
            result = (360 - result) % 360 // compensate the mirror
        } else {  // back-facing
            result = (cameraInfo!!.orientation - degrees + 360) % 360
        }
        Log.i(TAG, "Camera Display Orientation: $result")
        return result
    }

    private fun setCameraDisplayOrientation(rotation: Int) {
        camera!!.setDisplayOrientation(rotation)
    }

    private fun setParameters() {
        try {
            cameraRotation = calculateDisplayRotation()
            setCameraDisplayOrientation(cameraRotation)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set rotation.")
        }
        try {
            setDesiredParameters(false)
        } catch (e: Exception) {
            // Failed, use safe mode
            try {
                setDesiredParameters(true)
            } catch (e2: Exception) {
                // Well, darn. Give up
                Log.w(TAG, "Camera rejected even safe-mode parameters! No configuration")
            }
        }
        val realPreviewSize = camera!!.parameters.previewSize
        naturalPreviewSize = if (realPreviewSize == null) {
            requestedPreviewSize
        } else {
            Size(realPreviewSize.width, realPreviewSize.height)
        }
        cameraPreviewCallback.setResolution(naturalPreviewSize)
    }

    /**
     * This returns false if the camera is not opened yet, failed to open, or has
     * been closed.
     */
    val isOpen: Boolean
        get() = camera != null

    /**
     * Actual preview size in *current display* rotation. null if not determined yet.
     *
     * @return preview size
     */
    fun getPreviewSize(): Size? {
        return when {
            naturalPreviewSize == null -> {
                null
            }
            isCameraRotated -> {
                naturalPreviewSize!!.rotate()
            }
            else -> {
                naturalPreviewSize
            }
        }
    }

    /**
     * A single preview frame will be returned to the supplied callback.
     *
     * The thread on which this called is undefined, so a Handler should be used to post the result
     * to the correct thread.
     *
     * @param callback The callback to receive the preview.
     */
    fun requestPreviewFrame(callback: PreviewCallback?) {
        val theCamera = camera
        if (theCamera != null && previewing) {
            cameraPreviewCallback.setCallback(callback)
            theCamera.setOneShotPreviewCallback(cameraPreviewCallback)
        }
    }

    fun setTorch(on: Boolean) {
        if (camera != null) {
            try {
                val isOn = isTorchOn
                if (on != isOn) {
                    if (autoFocusManager != null) {
                        autoFocusManager!!.stop()
                    }
                    val parameters = camera!!.parameters
                    CameraConfigurationUtils.setTorch(parameters, on)
                    if (cameraSettings.isExposureEnabled) {
                        CameraConfigurationUtils.setBestExposure(parameters, on)
                    }
                    camera!!.parameters = parameters
                    if (autoFocusManager != null) {
                        autoFocusManager!!.start()
                    }
                }
            } catch (e: RuntimeException) {
                // Camera error. Could happen if the camera is being closed.
                Log.e(TAG, "Failed to set torch", e)
            }
        }
    }

    /**
     * Changes the settings for Camera.
     *
     * @param callback [CameraParametersCallback]
     */
    fun changeCameraParameters(callback: CameraParametersCallback?) {
        if (camera != null) {
            try {
                camera!!.parameters = callback?.changeCameraParameters(camera!!.parameters)
            } catch (e: RuntimeException) {
                // Camera error. Could happen if the camera is being closed.
                Log.e(TAG, "Failed to change camera parameters", e)
            }
        }
    }

    /**
     *
     * @return true if the torch is on
     * @throws RuntimeException if there is a camera error
     */
    val isTorchOn: Boolean
        get() {
            val parameters = camera?.parameters
            return if (parameters != null) {
                val flashMode = parameters.flashMode
                flashMode != null &&
                        (Camera.Parameters.FLASH_MODE_ON == flashMode || Camera.Parameters.FLASH_MODE_TORCH == flashMode)
            } else {
                false
            }
        }

    companion object {
        private val TAG = CameraManager::class.java.simpleName
        private fun getPreviewSizes(parameters: Camera.Parameters): List<Size> {
            val rawSupportedSizes = parameters.supportedPreviewSizes
            val previewSizes: MutableList<Size> = ArrayList()
            if (rawSupportedSizes == null) {
                val defaultSize = parameters.previewSize
                if (defaultSize != null) {
                    val previewSize = Size(defaultSize.width, defaultSize.height)
                    previewSizes.add(Size(defaultSize.width, defaultSize.height))
                }
                return previewSizes
            }
            for (size in rawSupportedSizes) {
                previewSizes.add(Size(size.width, size.height))
            }
            return previewSizes
        }
    }

    init {
        cameraPreviewCallback = CameraPreviewCallback()
        configure()
    }
}