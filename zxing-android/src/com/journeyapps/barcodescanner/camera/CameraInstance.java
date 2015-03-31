package com.journeyapps.barcodescanner.camera;

import android.content.Context;
import android.graphics.Point;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.zxing.client.android.R;
import com.journeyapps.barcodescanner.Util;

/**
 *
 */
public class CameraInstance {
  private static final String TAG = CameraInstance.class.getSimpleName();

  private CameraThread cameraThread;
  private SurfaceHolder surfaceHolder;
  private CameraManager cameraManager;
  private Handler readyHandler;
  private DisplayConfiguration displayConfiguration;
  private boolean open = false;

  public CameraInstance(Context context, final SurfaceHolder surfaceHolder) {
    Util.validateMainThread();

    this.cameraThread = CameraThread.getInstance();
    this.surfaceHolder = surfaceHolder;
    this.cameraManager = new CameraManager(context);
  }

  public void setDisplayConfiguration(DisplayConfiguration configuration) {
    this.displayConfiguration = configuration;
    cameraManager.setDisplayConfiguration(configuration);
  }

  private Runnable opener = new Runnable() {
    @Override
    public void run() {
      try {
        Log.d(TAG, "Opening camera");
        cameraManager.open();
        cameraManager.configure();
        cameraManager.setPreviewDisplay(surfaceHolder);
        cameraManager.startPreview();

        if (readyHandler != null) {
          readyHandler.obtainMessage(R.id.zxing_prewiew_ready).sendToTarget();
        }
      } catch (Exception e) {
        Log.e(TAG, "Failed to open camera", e);
      }
    }
  };

  private Runnable closer = new Runnable() {
    @Override
    public void run() {
      try {
        Log.d(TAG, "Closing camera");
        cameraManager.stopPreview();
        cameraManager.close();
      } catch (Exception e) {
        Log.e(TAG, "Failed to close camera", e);
      }

      cameraThread.decrementInstances();
    }
  };

  public void setReadyHandler(Handler readyHandler) {
    this.readyHandler = readyHandler;
  }

  /**
   * Actual preview size in current rotation. null if not determined yet.
   *
   * @return preview size
   */
  public Point getPreviewSize() {
    return cameraManager.getPreviewSize();
  }


  /**
   * @return true if the preview image is rotated
   */
  public boolean isPreviewRotated() {
    return displayConfiguration.isRotated();
  }

  public void open() {
    Util.validateMainThread();

    open = true;

    cameraThread.incrementAndEnqueue(opener);
  }

  public void close() {
    Util.validateMainThread();

    if(open) {
      cameraThread.enqueue(closer);
    }

    open = false;
  }

  public boolean isOpen() {
    return open;
  }

  public void requestPreview(final Handler handler, final int message) {
    validateOpen();

    cameraThread.enqueue(new Runnable() {
      @Override
      public void run() {
        cameraManager.requestPreviewFrame(handler, message);
      }
    });
  }

  private void validateOpen() {
    if(!open) {
      throw new IllegalStateException("CameraInstance is not open");
    }
  }
}
