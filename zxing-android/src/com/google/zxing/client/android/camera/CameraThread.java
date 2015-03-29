package com.google.zxing.client.android.camera;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;

/**
 *
 */
public class CameraThread {
  private static CameraThread instance;

  public static CameraThread getInstance(Context context) {
    if(instance == null) {
      instance = new CameraThread(context.getApplicationContext());
    }
    return instance;
  }

  private static final String TAG = CameraThread.class.getSimpleName();

  private Context context;
  private CameraManager cameraManager;

  private Handler handler;
  private HandlerThread thread;

  private CameraThread(Context context) {
    this.context = context;
    this.cameraManager = new CameraManager(context);

    this.thread = new HandlerThread("CameraThread");
    this.thread.start();
    this.handler = new Handler(thread.getLooper());

  }

  public void openAsync(final SurfaceHolder holder) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        try {
          Log.d(TAG, "Opening camera");
          cameraManager.openDriver(holder);
          cameraManager.startPreview();
        } catch (Exception e) {
          Log.e(TAG, "Failed to open camera", e);
        }
      }
    });
  }

  public void closeAsync() {
    handler.post(new Runnable() {
      @Override
      public void run() {
        try {
          Log.d(TAG, "Closing camera");
          cameraManager.stopPreview();
          cameraManager.closeDriver();
        } catch (Exception e) {
          Log.e(TAG, "Failed to close camera", e);
        }
      }
    });
  }

  public void destroyAsync() {
    handler.post(new Runnable() {
      @Override
      public void run() {
        try {
          thread.quit();
        } catch (Exception e) {
          Log.e(TAG, "Failed to quit thread", e);
        }
      }
    });
  }
}
