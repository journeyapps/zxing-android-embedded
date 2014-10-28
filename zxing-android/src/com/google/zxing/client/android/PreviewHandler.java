package com.google.zxing.client.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.client.android.camera.CameraManager;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Created by hans.reichenbach on 10/2/14.
 */
public class PreviewHandler implements SurfaceHolder.Callback {
  private static final String TAG = PreviewHandler.class.getSimpleName();

  private CameraManager cameraManager;
  private CaptureActivity context;
  private boolean hasSurface = false;
  private Collection<BarcodeFormat> decodeFormats;
  private Map<DecodeHintType,?> decodeHints;
  private String characterSet;

  public PreviewHandler(CaptureActivity context, CameraManager cameraManager) {
    this.cameraManager = cameraManager;
    this.context = context;
    decodeFormats = null;
    decodeHints = null;
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    if (holder == null) {
      Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
    }
    if (!hasSurface) {
      hasSurface = true;
      Log.d(TAG, "initializing camera in surfaceCreated");
      initCamera(holder);
    }
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    hasSurface = false;
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    Log.d(TAG, "surface changed with width=" + width + " and height=" + height);

    if(cameraManager.isPreviewRunning()) {
      cameraManager.stopPreview();
    }

    Display display = ((WindowManager)context.getSystemService(Activity.WINDOW_SERVICE)).getDefaultDisplay();

    if(display.getRotation() == Surface.ROTATION_0) {
      //source code has (height, width) when changing previewSize
      cameraManager.setCameraOrientation(90);
    } else if(display.getRotation() == Surface.ROTATION_270) {
      cameraManager.setCameraOrientation(180);
    }

    cameraManager.setPreviewFrameSize(width, height);

    //double check the preview frame size
    Intent intent = context.getIntent();
    if (intent.hasExtra(Intents.Scan.WIDTH) && intent.hasExtra(Intents.Scan.HEIGHT)) {
      int frameWidth = intent.getIntExtra(Intents.Scan.WIDTH, 0);
      int frameHeight = intent.getIntExtra(Intents.Scan.HEIGHT, 0);
      if (width > 0 && height > 0) {
        cameraManager.setManualFramingRect(frameWidth, frameHeight);
      }
    }

    CaptureActivityHandler handler = (CaptureActivityHandler) context.getActivityHandler();
    handler.startPreview();
  }

  public void setPreviewCallbacks() {
    try {
      QRScannerView qrPreview = (QRScannerView) context.findViewById(R.id.zxing_preview_view);
      SurfaceHolder surfaceHolder = qrPreview.getHolder();
      if (hasSurface) {
        // The activity was paused but not stopped, so the surface still exists. Therefore
        // surfaceCreated() won't be called, so init the camera here.
        initCamera(surfaceHolder);
      } else {
        // Install the callback and wait for surfaceCreated() to init the camera.
        surfaceHolder.addCallback(this);
      }

      qrPreview.setCameraManager(cameraManager);
    } catch(Exception e) {
      //must be a normal surface view so just carry on carrying on
      Log.i(TAG, "found a surface view instead of a QRScannerView");

      SurfaceView qrPreview = (SurfaceView) context.findViewById(R.id.zxing_preview_view);
      SurfaceHolder surfaceHolder = qrPreview.getHolder();
      if (hasSurface) {
        // The activity was paused but not stopped, so the surface still exists. Therefore
        // surfaceCreated() won't be called, so init the camera here.
        initCamera(surfaceHolder);
      } else {
        // Install the callback and wait for surfaceCreated() to init the camera.
        surfaceHolder.addCallback(this);
      }
    }
  }

  public void removeCallbacks() {
    if (!hasSurface) {
      try {
        SurfaceView surfaceView = (SurfaceView) context.findViewById(R.id.zxing_preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.removeCallback(this);
      } catch(ClassCastException e) {
        //probably just a QRScannerView instead of a surface view so try that instead
        QRScannerView surfaceView =
            (QRScannerView) context.findViewById(R.id.zxing_preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.removeCallback(this);
      }
    }
  }

  private void initCamera(SurfaceHolder surfaceHolder) {
    if (surfaceHolder == null) {
      throw new IllegalStateException("No SurfaceHolder provided");
    }
    if (cameraManager.isOpen()) {
      Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
      return;
    }
    try {
      cameraManager.openDriver(surfaceHolder);
      // Creating the handler starts the preview, which can also throw a RuntimeException.
      if (context.getActivityHandler() == null) {
        context.setActivityHandler(
            new CaptureActivityHandler(context, decodeFormats, decodeHints,
                characterSet, cameraManager));
      }
      context.decodeOrStoreSavedBitmap(null, null);
    } catch (IOException ioe) {
      Log.w(TAG, ioe);
      displayFrameworkBugMessageAndExit();
    } catch (RuntimeException e) {
      // Barcode Scanner has seen crashes in the wild of this variety:
      // java.?lang.?RuntimeException: Fail to connect to camera service
      Log.w(TAG, "Unexpected error initializing camera", e);
      displayFrameworkBugMessageAndExit();
    }
  }

  private void displayFrameworkBugMessageAndExit() {
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(context.getString(R.string.zxing_app_name));
    builder.setMessage(context.getString(R.string.zxing_msg_camera_framework_bug));
    builder.setPositiveButton(R.string.zxing_button_ok, new FinishListener(context));
    builder.setOnCancelListener(new FinishListener(context));
    builder.show();
  }

  public void setDecodeHints(Map<DecodeHintType,?> hints) {
    decodeHints = hints;
  }

  public void setDecodeFormats(Collection<BarcodeFormat> formats) {
    decodeFormats = formats;
  }

  public void setCharacterSet(String characterSet) {
    this.characterSet = characterSet;
  }
}
