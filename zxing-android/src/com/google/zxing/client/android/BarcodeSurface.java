package com.google.zxing.client.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.google.zxing.MultiFormatReader;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.client.android.camera.CameraThread;

/**
 *
 */
public class BarcodeSurface extends SurfaceView {
  private static final String TAG = BarcodeSurface.class.getSimpleName();

  private CameraThread.CameraInstance cameraInstance;
  private boolean hasSurface;
  private Activity activity;
  private Decoder decoder;

  private Handler resultHandler;

  private Reader barcodeReader;

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

  public Reader getBarcodeReader() {
    return barcodeReader;
  }

  public void setBarcodeReader(Reader barcodeReader) {
    this.barcodeReader = barcodeReader;
    if(this.decoder != null) {
      this.decoder.setReader(barcodeReader);
    }
  }

  private String lastText = null;


  private final Handler.Callback resultCallback = new Handler.Callback() {
    @Override
    public boolean handleMessage(Message message) {
      if(message.what == R.id.zxing_decode_succeeded) {
        Result result = (Result) message.obj;

        if(result != null && result.getText() != null) {
          if(!result.getText().equals(lastText)) {
            Toast.makeText(getContext(), "Scanned: " + result.getText(), Toast.LENGTH_SHORT).show();
            lastText = result.getText();
          }
        }
        Log.d(TAG, "Decode succeeded");
      } else if(message.what == R.id.zxing_decode_failed) {
        // Failed. Next preview is automatically tried.
      }
      return false;
    }
  };

  private void initialize() {
    activity = (Activity) getContext();

    resultHandler = new Handler(resultCallback);

    if(barcodeReader == null) {
      barcodeReader = new MultiFormatReader();
    }
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
    if(decoder != null) {
      decoder.stop();
      decoder = null;
    }
    if(cameraInstance != null) {
      cameraInstance.close();
      cameraInstance = null;
    }
    if (!hasSurface) {
      SurfaceHolder surfaceHolder = getHolder();
      surfaceHolder.removeCallback(surfaceCallback);
    }
  }

  private void initCamera(SurfaceHolder surfaceHolder) {
    if (surfaceHolder == null) {
      throw new IllegalStateException("No SurfaceHolder provided");
    }

    if(cameraInstance != null || decoder != null) {
      Log.w(TAG, "initCamera called twice");
      return;
    }

    cameraInstance = CameraThread.getInstance().open(getContext(), surfaceHolder);

    decoder = new Decoder(cameraInstance, barcodeReader, resultHandler);
    decoder.start();
  }

  public void destroy() {

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
