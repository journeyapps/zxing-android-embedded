package com.journeyapps.barcodescanner.camera;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;

import com.google.zxing.client.android.R;
import com.journeyapps.barcodescanner.Util;

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
    private Handler readyHandler;

    public CameraInstance(Context context, final SurfaceHolder surfaceHolder) {
      this.surfaceHolder = surfaceHolder;
      this.cameraManager = new CameraManager(context);
    }

    public void setDisplayConfiguration(DisplayConfiguration configuration) {
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

          if(readyHandler != null) {
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

        synchronized (LOCK) {
          openCount -= 1;
          if(openCount == 0) {
            quit();
          }
        }
      }
    };

    public void setReadyHandler(Handler readyHandler) {
      this.readyHandler = readyHandler;
    }

    public CameraManager getCameraManager() {
      return cameraManager;
    }

    public void open() {
      Util.validateMainThread();

      synchronized (LOCK) {
        openCount += 1;
        enqueue(opener);
      }
    }

    public void close() {
      Util.validateMainThread();

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

  public CameraInstance create(Context context, final SurfaceHolder holder) {
    Util.validateMainThread();
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
