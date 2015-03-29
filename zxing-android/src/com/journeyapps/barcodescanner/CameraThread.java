package com.journeyapps.barcodescanner;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.zxing.client.android.camera.CameraManager;

/**
 *
 */
public class CameraThread {
  private static final String TAG = CameraThread.class.getSimpleName();

  private static CameraThread instance;

  public static CameraThread getInstance() {
    if(instance == null) {
      instance = new CameraThread();
    }
    return instance;
  }


  private Handler handler;
  private HandlerThread thread;

  private int openCount = 0;

  private final Object LOCK = new Object();


  public class CameraInstance {
    private SurfaceHolder surfaceHolder;
    private CameraManager cameraManager;

    public CameraInstance(Context context, final SurfaceHolder surfaceHolder) {
      this.surfaceHolder = surfaceHolder;
      this.cameraManager = new CameraManager(context);

      open();
    }

    private Runnable opener = new Runnable() {
      @Override
      public void run() {
        try {
          Log.d(TAG, "Opening camera");
          cameraManager.openDriver(surfaceHolder);
          cameraManager.startPreview();
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
          cameraManager.closeDriver();
        } catch (Exception e) {
          Log.e(TAG, "Failed to close camera", e);
        }

        synchronized (LOCK) {
          openCount -= 1;
          if(openCount == 0) {
            quit();
          }
        }
      }
    };

    public CameraManager getCameraManager() {
      return cameraManager;
    }

    public void open() {
      synchronized (LOCK) {
        openCount += 1;
        enqueue(opener);
      }
    }

    public void close() {
      synchronized (LOCK) {
        enqueue(closer);
      }
    }

    public void requestPreview(final Handler handler, final int message) {
      synchronized (LOCK) {
        enqueue(new Runnable() {
          @Override
          public void run() {
            cameraManager.requestPreviewFrame(handler, message);
          }
        });
      }
    }
  }



  private CameraThread() {
  }

  private void enqueue(Runnable runnable) {
    synchronized (LOCK) {
      if (this.handler == null) {
        Log.d(TAG, "Opening thread");
        this.thread = new HandlerThread("CameraThread");
        this.thread.start();
        this.handler = new Handler(thread.getLooper());
      }
      this.handler.post(runnable);
    }
  }

  public CameraInstance open(Context context, final SurfaceHolder holder) {
    return new CameraInstance(context, holder);
  }


  private void quit() {
    Log.d(TAG, "Closing thread");
    synchronized (LOCK) {
      this.thread.quit();
      this.thread = null;
      this.handler = null;
    }
  }
}
