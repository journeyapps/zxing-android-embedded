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

  public CameraInstance(Context context) {
    Util.validateMainThread();

    this.cameraThread = CameraThread.getInstance();
    this.cameraManager = new CameraManager(context);
  }

  public void setDisplayConfiguration(DisplayConfiguration configuration) {
    this.displayConfiguration = configuration;
    cameraManager.setDisplayConfiguration(configuration);
  }

  public DisplayConfiguration getDisplayConfiguration() {
    return displayConfiguration;
  }

  public void setReadyHandler(Handler readyHandler) {
    this.readyHandler = readyHandler;
  }

  public void setSurfaceHolder(SurfaceHolder surfaceHolder) {
    this.surfaceHolder = surfaceHolder;
  }

  /**
   * Actual preview size in current rotation. null if not determined yet.
   *
   * @return preview size
   */
  private Point getPreviewSize() {
    return cameraManager.getPreviewSize();
  }

  public void open() {
    Util.validateMainThread();

    open = true;

    cameraThread.incrementAndEnqueue(opener);
  }

  public void configureCamera() {
    Util.validateMainThread();
    validateOpen();

    cameraThread.enqueue(configure);
  }

  public void startPreview() {
    Util.validateMainThread();
    validateOpen();

    cameraThread.enqueue(previewStarter);
  }

  public void setTorch(final boolean on) {
    Util.validateMainThread();

    if(open) {
      cameraThread.enqueue(new Runnable() {
        @Override
        public void run() {
          cameraManager.setTorch(on);
        }
      });
    }
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


  private Runnable opener = new Runnable() {
    @Override
    public void run() {
      try {
        Log.d(TAG, "Opening camera");
        cameraManager.open();
      } catch (Exception e) {
        Log.e(TAG, "Failed to open camera", e);
      }
    }
  };

  private Runnable configure = new Runnable() {
    @Override
    public void run() {
      try {
        Log.d(TAG, "Configuring camera");
        cameraManager.configure();
        if (readyHandler != null) {
          readyHandler.obtainMessage(R.id.zxing_prewiew_size_ready, getPreviewSize()).sendToTarget();
        }
      } catch (Exception e) {
        Log.e(TAG, "Failed to configure camera", e);
      }
    }
  };

  private Runnable previewStarter = new Runnable() {
    @Override
    public void run() {
      try {
        Log.d(TAG, "Starting preview");
        cameraManager.setPreviewDisplay(surfaceHolder);
        cameraManager.startPreview();
      } catch (Exception e) {
        Log.e(TAG, "Failed to start preview", e);
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
}
