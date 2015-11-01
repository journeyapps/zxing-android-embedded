package com.journeyapps.barcodescanner.camera;

import com.google.zxing.client.android.camera.open.OpenCameraInterface;
import com.journeyapps.barcodescanner.Size;

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
    private Size maximumPreviewSize;


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

    public Size getMaximumPreviewSize() {
        return maximumPreviewSize;
    }

    /**
     * Set the maximum preview size to use. A preview will be excluded if it's it does not fit into
     * a rectangle of this size in any orientation. For example, a 800x500 preview will be
     * considered to fit into a 600x800 maximum size.
     *
     * @param maximumPreviewSize the maximum size. null means no maximum.
     */
    public void setMaximumPreviewSize(Size maximumPreviewSize) {
        this.maximumPreviewSize = maximumPreviewSize;
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
