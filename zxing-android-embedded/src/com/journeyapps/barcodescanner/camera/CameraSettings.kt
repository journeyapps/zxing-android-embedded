package com.journeyapps.barcodescanner.camera

import com.google.zxing.client.android.camera.open.OpenCameraInterface

/**
 *
 */
class CameraSettings {
    /**
     * Allows third party apps to specify the camera ID, rather than determine
     * it automatically based on available cameras and their orientation.
     *
     * @param requestedCameraId camera ID of the camera to use. A negative value means "no preference".
     */
    var requestedCameraId = OpenCameraInterface.NO_REQUESTED_CAMERA

    /**
     * Default to false.
     *
     * Inverted means dark & light colors are inverted.
     *
     * @return true if scan is inverted
     */
    var isScanInverted = false

    /**
     * Default to false.
     *
     * @return true if barcode scene mode is enabled
     */
    var isBarcodeSceneModeEnabled = false

    /**
     * Default to false.
     *
     * If enabled, metering is performed to determine focus area.
     *
     * @return true if metering is enabled
     */
    var isMeteringEnabled = false
    var autoFocusEnabled = true
        set(value) {
            field = value
            focusMode = if (field && continuousFocusEnabled) {
                FocusMode.CONTINUOUS
            } else if (field) {
                FocusMode.AUTO
            } else {
                null
            }
        }

    var continuousFocusEnabled = false
        set(value) {
            field = value
            focusMode = when {
                continuousFocusEnabled -> {
                    FocusMode.CONTINUOUS
                }
                autoFocusEnabled -> {
                    FocusMode.AUTO
                }
                else -> {
                    null
                }
            }
        }

    /**
     * Default to false.
     *
     * @return true if exposure is enabled.
     */
    var isExposureEnabled = false

    /**
     * Default to false.
     *
     * @return true if the torch is automatically controlled based on ambient light.
     */
    var isAutoTorchEnabled = false

    /**
     * Default to FocusMode.AUTO.
     *
     * @return value of selected focus mode
     */
    var focusMode: FocusMode? = FocusMode.AUTO

    enum class FocusMode {
        AUTO, CONTINUOUS, INFINITY, MACRO
    }

}