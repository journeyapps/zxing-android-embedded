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

package com.google.zxing.client.android.camera;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.client.android.ScannerOptions;
import com.google.zxing.client.android.camera.open.OpenCameraInterface;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CameraManager {
  //todo check into pulling some methods out into their own classes? this class is bulky

  private static final String TAG = CameraManager.class.getSimpleName();

  private static final int MIN_FRAME_WIDTH = 240;
  private static final int MIN_FRAME_HEIGHT = 240;
  private static final int MAX_FRAME_WIDTH = 1200; // = 5/8 * 1920
  private static final int MAX_FRAME_HEIGHT = 675; // = 5/8 * 1080

  private final Context context;
  private final CameraConfigurationManager configManager;
  private Camera camera;
  private AutoFocusManager autoFocusManager;
  private Rect framingRect;
  private Rect framingRectInPreview;
  private boolean initialized;
  private boolean previewing;
  private int requestedCameraId = -1;
  private int requestedFramingRectWidth;
  private int requestedFramingRectHeight;
  private ScannerOptions scannerOptions;
  private Point mPreviewFrameSize;
  private int mCurrentOrientation;

  private static final Set<Integer> ORIENTATION_SUPPORT_LIST;
  static {
    Set<Integer> tempSet = new HashSet<>();
    tempSet.add(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    tempSet.add(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

    ORIENTATION_SUPPORT_LIST = Collections.unmodifiableSet(tempSet);
  }

  /**
   * Preview frames are delivered here, which we pass on to the registered handler. Make sure to
   * clear the handler so it will only receive one message.
   */
  private final PreviewCallback previewCallback;

  public CameraManager(Context context, ScannerOptions options) {
    this.context = context;
    this.configManager = new CameraConfigurationManager(context);
    previewCallback = new PreviewCallback(configManager);

    //keep any custom ui options
    scannerOptions = options;
  }

  /**
   * Opens the camera driver and initializes the hardware parameters.
   *
   * @param holder The surface object which the camera will draw preview frames into.
   * @throws IOException Indicates the camera driver failed to open.
   */
  public synchronized void openDriver(SurfaceHolder holder) throws IOException {
    Camera theCamera = camera;
    if (theCamera == null) {

      if (requestedCameraId >= 0) {
        theCamera = OpenCameraInterface.open(requestedCameraId);
      } else {
        theCamera = OpenCameraInterface.open();
      }

      if (theCamera == null) {
        throw new IOException();
      }
      camera = theCamera;
    }
    theCamera.setPreviewDisplay(holder);

    if (!initialized) {
      initialized = true;
      configManager.initFromCameraParameters(theCamera);
      if (requestedFramingRectWidth > 0 && requestedFramingRectHeight > 0) {
        setManualFramingRect(requestedFramingRectWidth, requestedFramingRectHeight);
        requestedFramingRectWidth = 0;
        requestedFramingRectHeight = 0;
      }
    }

    Camera.Parameters parameters = theCamera.getParameters();
    String parametersFlattened = parameters == null ? null : parameters.flatten(); // Save these, temporarily
    try {
      configManager.setDesiredCameraParameters(theCamera, false);
    } catch (RuntimeException re) {
      // Driver failed
      Log.w(TAG, "Camera rejected parameters. Setting only minimal safe-mode parameters");
      Log.i(TAG, "Resetting to saved camera params: " + parametersFlattened);
      // Reset:
      if (parametersFlattened != null) {
        parameters = theCamera.getParameters();
        parameters.unflatten(parametersFlattened);
        try {
          theCamera.setParameters(parameters);
          configManager.setDesiredCameraParameters(theCamera, true);
        } catch (RuntimeException re2) {
          // Well, darn. Give up
          Log.w(TAG, "Camera rejected even safe-mode parameters! No configuration");
        }
      }
    }

  }

  public synchronized boolean isOpen() {
    return camera != null;
  }

  /**
   * Closes the camera driver if still in use.
   */
  public synchronized void closeDriver() {
    if (camera != null) {
      camera.release();
      camera = null;
      // Make sure to clear these each time we close the camera, so that any scanning rect
      // requested by intent is forgotten.
      framingRect = null;
      framingRectInPreview = null;
    }
  }

  public synchronized void startPreview(SurfaceHolder holder) {
    try {
      if (camera != null && !isPreviewRunning()) {
        camera.setPreviewDisplay(holder);
        startPreview();
      }
    } catch(IOException e) {
      Log.e(TAG, e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Asks the camera hardware to begin drawing preview frames to the screen.
   */
  public synchronized void startPreview() {
    Log.d(TAG, "starting camera preview");

    Camera theCamera = camera;
    if (theCamera != null && !isPreviewRunning()) {
      theCamera.startPreview();
      previewing = true;
      autoFocusManager = new AutoFocusManager(context, camera);
    }
  }

  /**
   * Tells the camera to stop drawing preview frames.
   */
  public synchronized void stopPreview() {
    Log.d(TAG, "stopping camera preview");
    if (autoFocusManager != null) {
      autoFocusManager.stop();
      autoFocusManager = null;
    }
    if (camera != null && isPreviewRunning()) {
      camera.stopPreview();
      previewCallback.setHandler(null, 0);
      previewing = false;
    }
  }

  /**
   * Informs whether the camera is currently in preview mode or not
   */
  public synchronized boolean isPreviewRunning() {
    return previewing;
  }

  /**
   * Get the camera parameters
   *
   * @return the current camera parameters or null if no camera found
   */
  public synchronized Camera.Parameters getCameraParameters() {
    if(camera != null) {
      return camera.getParameters();
    } else {
      return null;
    }
  }

  /**
   * Set the camera parameters if possible
   *
   * @param params the new camera parameters to set
   */
  public synchronized void setCameraParameters(Camera.Parameters params) {
    if(camera != null && !isPreviewRunning()) {
      camera.setParameters(params);
    } else {
      Log.w(TAG, "tried to set parameters when the camera was unavailable");
    }
  }

  /**
   * Rotate the camera orientation to whatever angle you desire
   * @param orientation the orientation in degrees ot rotate the camera display
   */
  public synchronized  void setCameraOrientation(int orientation) {
    if(camera != null) {
      camera.setDisplayOrientation(orientation);
    }
  }

  /**
   * Convenience method for {@link com.google.zxing.client.android.CaptureActivity}
   *
   * @param newSetting if {@code true}, light should be turned on if currently off. And vice versa.
   */
  public synchronized void setTorch(boolean newSetting) {
    if (newSetting != configManager.getTorchState(camera)) {
      if (camera != null) {
        if (autoFocusManager != null) {
          autoFocusManager.stop();
        }
        configManager.setTorch(camera, newSetting);
        if (autoFocusManager != null) {
          autoFocusManager.start();
        }
      }
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
  public synchronized void requestPreviewFrame(Handler handler, int message) {
    Camera theCamera = camera;
    if (theCamera != null && previewing) {
      previewCallback.setHandler(handler, message);
      theCamera.setOneShotPreviewCallback(previewCallback);
    }
  }

  /**
   * Calculates the framing rect which the UI should draw to show the user where to place the
   * barcode. This target helps with alignment as well as forces the user to hold the device
   * far enough away to ensure the image will be in focus.
   *
   * @return The rectangle to draw on screen in window coordinates.
   */
  public synchronized Rect getFramingRect() {
    if (framingRect == null) {
      if (camera == null) {
        return null;
      }
      Camera.Size previewResolution = getPreviewSize();
      if (previewResolution == null) {
        // Called early, before init even finished
        return null;
      }

      int width = findDesiredDimensionInRange(previewResolution.width, MIN_FRAME_WIDTH, MAX_FRAME_WIDTH);
      int height = findDesiredDimensionInRange(previewResolution.height, MIN_FRAME_HEIGHT, MAX_FRAME_HEIGHT);

      int leftOffset = (previewResolution.width - width) / 2;
      int topOffset = (previewResolution.height - height) / 2;
      framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
      Log.d(TAG, "Calculated framing rect: " + framingRect);
    }
    return framingRect;
  }

  private static int findDesiredDimensionInRange(int resolution, int hardMin, int hardMax) {
    int dim = 5 * resolution / 8; // Target 5/8 of each dimension
    if (dim < hardMin) {
      return hardMin;
    }
    if (dim > hardMax) {
      return hardMax;
    }
    return dim;
  }

  /**
   * Like {@link #getFramingRect} but coordinates are in terms of the preview frame,
   * not UI / screen.
   *
   * @return {@link Rect} expressing barcode scan area in terms of the preview size
   */
  public synchronized Rect getFramingRectInPreview() {
    if (framingRectInPreview == null) {
      Rect framingRect = getFramingRect();
      if (framingRect == null) {
        return null;
      }
      Rect rect = new Rect(framingRect);
      Point cameraResolution = configManager.getCameraResolution();
      Camera.Size previewResolution = getPreviewSize();
      if (cameraResolution == null || previewResolution == null) {
        // Called early, before init even finished
        return null;
      } else if(mPreviewFrameSize != null) {
        cameraResolution = mPreviewFrameSize;
      }

      rect.left = rect.left * cameraResolution.x / previewResolution.width;
      rect.right = rect.right * cameraResolution.x / previewResolution.width;
      rect.top = rect.top * cameraResolution.y / previewResolution.height;
      rect.bottom = rect.bottom * cameraResolution.y / previewResolution.height;
      framingRectInPreview = rect;

      Log.d(TAG, "Calculated framing rect in preview: " + framingRectInPreview);
    }
    return framingRectInPreview;
  }


  /**
   * Allows third party apps to specify the camera ID, rather than determine
   * it automatically based on available cameras and their orientation.
   *
   * @param cameraId camera ID of the camera to use. A negative value means "no preference".
   */
  public synchronized void setManualCameraId(int cameraId) {
    if (initialized) {
      throw new IllegalStateException();
    } else {
      requestedCameraId = cameraId;
    }
  }

  /**
   * Allows third party apps to specify the scanning rectangle dimensions, rather than determine
   * them automatically based on screen resolution.
   *
   * @param width The width in pixels to scan.
   * @param height The height in pixels to scan.
   */
  public synchronized void setManualFramingRect(int width, int height) {
    if (initialized) {
      Camera.Size screenSize = getPreviewSize();

      //check if the frame box is different than the preview size
      if(mPreviewFrameSize != null) {
        screenSize.width = mPreviewFrameSize.x;
        screenSize.height = mPreviewFrameSize.y;
      }

      if (width > screenSize.width) {
        width = screenSize.width;
      }
      if (height > screenSize.height) {
        height = screenSize.height;
      }

      int leftOffset = (screenSize.width - width) / 2;
      int topOffset = (screenSize.height - height) / 2;
      framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
      Log.d(TAG, "Calculated manual framing rect: " + framingRect);
      framingRectInPreview = null;
    } else {
      requestedFramingRectWidth = width;
      requestedFramingRectHeight = height;
    }
  }

  /**
   * Sets the size of the preview frame for use in building the framing rectangle. DOES NOT
   * ACTUALLY CHANGE THE SIZE OF THE PREVIEW FRAME, JUST RECORDS IT FOR INTERNAL USE.
   *
   * @param width how wide the preview frame is
   * @param height how tall the preview frame is
   */
  public synchronized void setPreviewFrameSize(int width, int height) {
    if(width > MIN_FRAME_WIDTH && height > MIN_FRAME_HEIGHT) {
      mPreviewFrameSize = new Point(width, height);
    } else {
      throw new IllegalArgumentException("Provided preview frame size was too small");
    }
  }

  /**
   * A factory method to build the appropriate LuminanceSource object based on the format
   * of the preview buffers, as described by Camera.Parameters.
   *
   * @param data A preview frame.
   * @param width The width of the image.
   * @param height The height of the image.
   * @return A PlanarYUVLuminanceSource instance.
   */
  public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
    Rect rect = getFramingRect();
    if (rect == null) {
      return null;
    }
    // Go ahead and assume it's YUV rather than die.
    return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
        rect.width(), rect.height(), false);
  }

  public Camera.Size getPreviewSize() {
    Camera.Parameters params = getCameraParameters();
    if(params != null) {
      return params.getPreviewSize();
    }

    return null;
  }

  public boolean isScannerLineEnabled() {
    return scannerOptions.isScannerLineEnabled();
  }

  public boolean isFrameEnabled() {
    return scannerOptions.isFrameEnabled();
  }

  public boolean isOverlayEnabled() {
    return scannerOptions.isOverlayEnabled();
  }

  public boolean isPotentialIndicatorsEnabled() {
    return scannerOptions.isPotentialIndicatorsEnabled();
  }

  public boolean isResultIndicatorsEnabled() {
    return scannerOptions.isResultIndicatorsEnabled();
  }

  /**
   * Returns the current orientation of the layout as a value of ActivityInfo constants
   *
   * @return an int corresponding to a constant from ActivityInfo
   */
  public int getCurrentOrientation() {
    return mCurrentOrientation;
  }

  public void setCurrentOrientation(int orientation) {
    if(!ORIENTATION_SUPPORT_LIST.contains(orientation)) {
      throw new IllegalArgumentException("Unsupported orientation value given");
    }

    Log.d(TAG, "setting current orientation to " + orientation);

    mCurrentOrientation = orientation;
  }

  public int getOverlayOpacity() {
    return scannerOptions.getOverlayOpacity();
  }

}
