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

package com.journeyapps.barcodescanner.camera;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.google.zxing.client.android.camera.CameraConfigurationUtils;
import com.google.zxing.client.android.camera.open.OpenCameraInterface;

import java.io.IOException;

/**
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
public final class CameraManager {

  private static final String TAG = CameraManager.class.getSimpleName();


  private final Context context;

  private Camera camera;
  private Camera.CameraInfo cameraInfo;

  private AutoFocusManager autoFocusManager;
  private boolean previewing;
  private String defaultParameters;

  // User parameters
  private CameraSettings settings = new CameraSettings();

  private DisplayConfiguration displayConfiguration;

  // Actual chosen preview size
  private Point requestedPreviewSize;
  private Point previewSize;

  /**
   * Preview frames are delivered here, which we pass on to the registered handler. Make sure to
   * clear the handler so it will only receive one message.
   */
  private final PreviewCallback previewCallback;

  public CameraManager(Context context) {
    this.context = context;
    previewCallback = new PreviewCallback();
  }

  /**
   * Must be called from camera thread.
   */
  public void open() {
    camera = OpenCameraInterface.open(settings.getRequestedCameraId());
    if(camera == null) {
      throw new RuntimeException("Failed to open camera");
    }

    int cameraId = OpenCameraInterface.getCameraId(settings.getRequestedCameraId());
    cameraInfo = new Camera.CameraInfo();
    Camera.getCameraInfo(cameraId, cameraInfo);
  }

  /**
   * Configure the camera parameters, including preview size.
   *
   * The camera must be opened before calling this.
   *
   * Must be called from camera thread.
   */
  public void configure() {
    setParameters();
  }

  /**
   * Must be called from camera thread.
   */
  public void setPreviewDisplay(SurfaceHolder holder) throws IOException {
    camera.setPreviewDisplay(holder);
  }

  /**
   * Asks the camera hardware to begin drawing preview frames to the screen.
   *
   * Must be called from camera thread.
   */
  public void startPreview() {
    Camera theCamera = camera;
    if (theCamera != null && !previewing) {
      theCamera.startPreview();
      previewing = true;
      autoFocusManager = new AutoFocusManager(context, camera);
    }
  }

  /**
   * Tells the camera to stop drawing preview frames.
   *
   * Must be called from camera thread.
   */
  public void stopPreview() {
    if (autoFocusManager != null) {
      autoFocusManager.stop();
      autoFocusManager = null;
    }
    if (camera != null && previewing) {
      camera.stopPreview();
      previewCallback.setHandler(null, 0);
      previewing = false;
    }
  }


  /**
   * Closes the camera driver if still in use.
   *
   * Must be called from camera thread.
   */
  public void close() {
    if (camera != null) {
      camera.release();
      camera = null;
    }
  }


  private Camera.Parameters getDefaultCameraParameters() {
    Camera.Parameters parameters = camera.getParameters();
    if(defaultParameters == null) {
      defaultParameters = parameters.flatten();
    } else {
      parameters.unflatten(defaultParameters);
    }
    return parameters;
  }

  private void setDesiredParameters(boolean safeMode) {
    Camera.Parameters parameters = getDefaultCameraParameters();

    //noinspection ConstantConditions
    if (parameters == null) {
      Log.w(TAG, "Device error: no camera parameters are available. Proceeding without configuration.");
      return;
    }

    Log.i(TAG, "Initial camera parameters: " + parameters.flatten());

    if (safeMode) {
      Log.w(TAG, "In camera config safe mode -- most settings will not be honored");
    }


    CameraConfigurationUtils.setFocus(parameters, settings.isAutoFocus(), settings.isDisableContinuousFocus(), safeMode);

    if (!safeMode) {
      CameraConfigurationUtils.setTorch(parameters, false);

      if (settings.isInvertScan()) {
        CameraConfigurationUtils.setInvertColor(parameters);
      }

      if (!settings.isDisableBarcodeSceneMode()) {
        CameraConfigurationUtils.setBarcodeSceneMode(parameters);
      }

      if (!settings.isDisableMetering()) {
        CameraConfigurationUtils.setVideoStabilization(parameters);
        CameraConfigurationUtils.setFocusArea(parameters);
        CameraConfigurationUtils.setMetering(parameters);
      }

    }

    Point desiredSize = displayConfiguration.getDesiredLandscapePreviewSize();

    if(desiredSize != null) {
      requestedPreviewSize = CameraConfigurationUtils.findBestPreviewSizeValue(parameters, desiredSize);
      parameters.setPreviewSize(requestedPreviewSize.x, requestedPreviewSize.y);
    }

    Log.i(TAG, "Final camera parameters: " + parameters.flatten());

    camera.setParameters(parameters);
  }



  private void setCameraDisplayOrientation() {
    int rotation = displayConfiguration.getRotation();
    int degrees = 0;
    switch (rotation) {
      case Surface.ROTATION_0: degrees = 0; break;
      case Surface.ROTATION_90: degrees = 90; break;
      case Surface.ROTATION_180: degrees = 180; break;
      case Surface.ROTATION_270: degrees = 270; break;
    }

    int result;
    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
      result = (cameraInfo.orientation + degrees) % 360;
      result = (360 - result) % 360;  // compensate the mirror
    } else {  // back-facing
      result = (cameraInfo.orientation - degrees + 360) % 360;
    }
    camera.setDisplayOrientation(result);
  }


  private void setParameters() {
    try {
      setCameraDisplayOrientation();
    } catch(Exception e) {
      Log.w(TAG, "Failed to set rotation.");
    }
    try {
      setDesiredParameters(false);
    } catch (Exception e) {
      // Failed, use safe mode
      try {
        setDesiredParameters(false);
      } catch (Exception e2) {
        // Well, darn. Give up
        Log.w(TAG, "Camera rejected even safe-mode parameters! No configuration");
      }
    }

    Camera.Size realPreviewSize = camera.getParameters().getPreviewSize();
    if(realPreviewSize == null) {
      previewSize = requestedPreviewSize;
    } else {
      previewSize = new Point(realPreviewSize.width, realPreviewSize.height);
    }
    previewCallback.setResolution(previewSize);
  }


  public boolean isOpen() {
    return camera != null;
  }

  /**
   * Actual preview size in landscpae orientation. null if not determined yet.
   *
   * @return preview size
   */
  public Point getLandscapePreviewSize() {
    return previewSize;
  }

  /**
   * Actual preview size in current rotation. null if not determined yet.
   *
   * @return preview size
   */
  public Point getPreviewSize() {
    if(previewSize == null) {
      return null;
    } else if(displayConfiguration.isRotated()) {
      //noinspection SuspiciousNameCombination
      return new Point(previewSize.y, previewSize.x);
    } else {
      return previewSize;
    }
  }

  /**
   * A single preview frame will be returned to the handler supplied. The data will arrive as byte[]
   * in the message.obj field, with width and height encoded as message.arg1 and message.arg2,
   * respectively.
   *
   * @param handler The handler to send the message to.
   * @param message The what field of the message to be sent.
   */
  public void requestPreviewFrame(Handler handler, int message) {
    Camera theCamera = camera;
    if (theCamera != null && previewing) {
      previewCallback.setHandler(handler, message);
      theCamera.setOneShotPreviewCallback(previewCallback);
    }
  }

  public CameraSettings getCameraSettings() {
    return settings;
  }

  public void setCameraSettings(CameraSettings settings) {
    this.settings = settings;
  }

  public DisplayConfiguration getDisplayConfiguration() {
    return displayConfiguration;
  }

  public void setDisplayConfiguration(DisplayConfiguration displayConfiguration) {
    this.displayConfiguration = displayConfiguration;
  }
}
