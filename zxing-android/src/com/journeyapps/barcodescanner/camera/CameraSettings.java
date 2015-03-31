package com.journeyapps.barcodescanner.camera;

import com.google.zxing.client.android.camera.open.OpenCameraInterface;

/**
 *
 */
public class CameraSettings {
  private int requestedCameraId = OpenCameraInterface.NO_REQUESTED_CAMERA;
  private boolean invertScan = false;
  private boolean disableBarcodeSceneMode = true;
  private boolean disableMetering = true;
  private boolean autoFocus = true;
  private boolean disableContinuousFocus = true;

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

  public boolean isInvertScan() {
    return invertScan;
  }

  public void setInvertScan(boolean invertScan) {
    this.invertScan = invertScan;
  }

  public boolean isDisableBarcodeSceneMode() {
    return disableBarcodeSceneMode;
  }

  public void setDisableBarcodeSceneMode(boolean disableBarcodeSceneMode) {
    this.disableBarcodeSceneMode = disableBarcodeSceneMode;
  }

  public boolean isDisableMetering() {
    return disableMetering;
  }

  public void setDisableMetering(boolean disableMetering) {
    this.disableMetering = disableMetering;
  }

  public boolean isAutoFocus() {
    return autoFocus;
  }

  public void setAutoFocus(boolean autoFocus) {
    this.autoFocus = autoFocus;
  }

  public boolean isDisableContinuousFocus() {
    return disableContinuousFocus;
  }

  public void setDisableContinuousFocus(boolean disableContinuousFocus) {
    this.disableContinuousFocus = disableContinuousFocus;
  }
}
