package com.journeyapps.barcodescanner.camera;

import com.google.zxing.client.android.camera.open.OpenCameraInterface;

/**
 *
 */
public class CameraSettings {
    private int requestedCameraId = OpenCameraInterface.NO_REQUESTED_CAMERA;
    private boolean scanInverted = false;
    private boolean barcodeSceneModeEnabled = false;
    private boolean meteringEnabled = false;
    private boolean autoFocusEnabled = true;
    private boolean continuousFocusEnabled = false;
    private boolean exposureEnabled = false;
    private boolean autoTorchEnabled = false;
    private FocusMode focusMode = FocusMode.AUTO;

    public enum FocusMode {
        AUTO,
        CONTINUOUS,
        INFINITY,
        MACRO
    }

    public int getRequestedCameraId() {
        return requestedCameraId;
    }

    /**
     * Allows third party apps to specify the camera ID, rather than determine
     * it automatically based on available cameras and their orientation.
     *
     * @param requestedCameraId camera ID of the camera to use. A negative value means "no preference".
     */
    public void setRequestedCameraId(int requestedCameraId) {
        this.requestedCameraId = requestedCameraId;
    }

    /**
     * Default to false.
     *
     * Inverted means dark & light colors are inverted.
     *
     * @return true if scan is inverted
     */
    public boolean isScanInverted() {
        return scanInverted;
    }

    public void setScanInverted(boolean scanInverted) {
        this.scanInverted = scanInverted;
    }

    /**
     * Default to false.
     *
     * @return true if barcode scene mode is enabled
     */
    public boolean isBarcodeSceneModeEnabled() {
        return barcodeSceneModeEnabled;
    }

    public void setBarcodeSceneModeEnabled(boolean barcodeSceneModeEnabled) {
        this.barcodeSceneModeEnabled = barcodeSceneModeEnabled;
    }

    /**
     * Default to false.
     *
     * @return true if exposure is enabled.
     */
    public boolean isExposureEnabled() {
        return exposureEnabled;
    }

    public void setExposureEnabled(boolean exposureEnabled) {
        this.exposureEnabled = exposureEnabled;
    }

    /**
     * Default to false.
     *
     * If enabled, metering is performed to determine focus area.
     *
     * @return true if metering is enabled
     */
    public boolean isMeteringEnabled() {
        return meteringEnabled;
    }

    public void setMeteringEnabled(boolean meteringEnabled) {
        this.meteringEnabled = meteringEnabled;
    }

    /**
     * Default to true.
     *
     * @return true if auto-focus is enabled
     */
    public boolean isAutoFocusEnabled() {
        return autoFocusEnabled;
    }

    public void setAutoFocusEnabled(boolean autoFocusEnabled) {
        this.autoFocusEnabled = autoFocusEnabled;

        if (autoFocusEnabled && continuousFocusEnabled) {
            focusMode = FocusMode.CONTINUOUS;
        } else if (autoFocusEnabled) {
            focusMode = FocusMode.AUTO;
        } else {
            focusMode = null;
        }
    }

    /**
     * Default to false.
     *
     * @return true if continuous focus is enabled
     */
    public boolean isContinuousFocusEnabled() {
        return continuousFocusEnabled;
    }

    public void setContinuousFocusEnabled(boolean continuousFocusEnabled) {
        this.continuousFocusEnabled = continuousFocusEnabled;

        if (continuousFocusEnabled) {
            focusMode = FocusMode.CONTINUOUS;
        } else if (autoFocusEnabled) {
            focusMode = FocusMode.AUTO;
        } else {
            focusMode = null;
        }
    }

    /**
     * Default to FocusMode.AUTO.
     *
     * @return value of selected focus mode
     */
    public FocusMode getFocusMode() {
        return focusMode;
    }

    public void setFocusMode(FocusMode focusMode) {
        this.focusMode = focusMode;
    }

    /**
     * Default to false.
     *
     * @return true if the torch is automatically controlled based on ambient light.
     */
    public boolean isAutoTorchEnabled() {
        return autoTorchEnabled;
    }

    public void setAutoTorchEnabled(boolean autoTorchEnabled) {
        this.autoTorchEnabled = autoTorchEnabled;
    }
}
