package com.google.zxing.client.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.client.android.camera.CameraManager;
import com.google.zxing.client.android.camera.CameraThread;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 *
 */
public class BarcodeSurface extends SurfaceView {
  private static final String TAG = BarcodeSurface.class.getSimpleName();

  private CameraThread cameraThread;
  private boolean hasSurface;
  private Collection<BarcodeFormat> decodeFormats;
  private Map<DecodeHintType,?> decodeHints;
  private String characterSet;
  private Activity activity;

  private final SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
      if (holder == null) {
        Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
      }
      if (!hasSurface) {
        hasSurface = true;
        initCamera(holder);
      }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
      hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }
  };

  public BarcodeSurface(Context context) {
    super(context);
    initialize();
  }

  public BarcodeSurface(Context context, AttributeSet attrs) {
    super(context, attrs);
    initialize();
  }

  public BarcodeSurface(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize();
  }

  public BarcodeSurface(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    initialize();
  }

  private void initialize() {
    activity = (Activity) getContext();

    cameraThread = CameraThread.getInstance(getContext());
  }

  public void resume() {
    SurfaceHolder surfaceHolder = getHolder();
    if (hasSurface) {
      // The activity was paused but not stopped, so the surface still exists. Therefore
      // surfaceCreated() won't be called, so init the camera here.
      initCamera(surfaceHolder);
    } else {
      // Install the callback and wait for surfaceCreated() to init the camera.
      surfaceHolder.addCallback(surfaceCallback);
    }
  }


  protected void pause() {
//    if (handler != null) {
//      handler.quitSynchronously();
//      handler = null;
//    }
    cameraThread.closeAsync();
    if (!hasSurface) {
      SurfaceHolder surfaceHolder = getHolder();
      surfaceHolder.removeCallback(surfaceCallback);
    }
  }

  private void initCamera(SurfaceHolder surfaceHolder) {
    if (surfaceHolder == null) {
      throw new IllegalStateException("No SurfaceHolder provided");
    }

    cameraThread.openAsync(surfaceHolder);
  }

  public void destroy() {
//    cameraThread.destroyAsync();
  }

  private void displayFrameworkBugMessageAndExit() {
    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
    builder.setTitle(getContext().getString(R.string.zxing_app_name));
    builder.setMessage(getContext().getString(R.string.zxing_msg_camera_framework_bug));
    builder.setPositiveButton(R.string.zxing_button_ok, new FinishListener(activity));
    builder.setOnCancelListener(new FinishListener(activity));
    builder.show();
  }
}
