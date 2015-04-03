package com.journeyapps.barcodescanner;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.R;

import java.util.List;

/**
 *
 */
public class CaptureActivity2 extends Activity {
  private static final String TAG = CaptureActivity2.class.getSimpleName();
  private BarcodeView barcodeView;
  private ViewfinderView viewFinder;

  private BarcodeCallback callback = new BarcodeCallback() {
    @Override
    public void barcodeResult(Result result) {
      TextView text = (TextView)findViewById(R.id.zxing_barcode_status);
      if(result.getText() != null) {
        text.setText(result.getText());
      }
    }

    @Override
    public void possibleResultPoints(List<ResultPoint> resultPoints) {
      for (ResultPoint point : resultPoints) {
        viewFinder.addPossibleResultPoint(point);
      }
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.zxing_capture2);
    barcodeView = (BarcodeView) findViewById(R.id.zxing_barcode_surface);
    barcodeView.decodeContinuous(callback);

    viewFinder = (ViewfinderView)findViewById(R.id.zxing_viewfinder_view);
    viewFinder.setCameraPreview(barcodeView);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);

    Log.d(TAG, "Configuration changed");

  }

  private void orientationChanged() {
    barcodeView.pause();
    barcodeView.resume();
  }

  @Override
  protected void onResume() {
    super.onResume();

    barcodeView.resume();
  }

  @Override
  protected void onPause() {
    super.onPause();

    barcodeView.pause();
  }

  public void pause(View view) {
    barcodeView.pause();
  }

  public void resume(View view) {
    barcodeView.resume();
  }

  public void triggerScan(View view) {
    barcodeView.decodeSingle(callback);
  }


  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    switch (keyCode) {
      case KeyEvent.KEYCODE_BACK:
        setResult(RESULT_CANCELED);
        finish();
        return true;
      case KeyEvent.KEYCODE_FOCUS:
      case KeyEvent.KEYCODE_CAMERA:
        // Handle these events so they don't launch the Camera app
        return true;
      // Use volume up/down to turn on light
      case KeyEvent.KEYCODE_VOLUME_DOWN:
        barcodeView.setTorch(false);
        return true;
      case KeyEvent.KEYCODE_VOLUME_UP:
        barcodeView.setTorch(true);
        return true;
    }
    return super.onKeyDown(keyCode, event);
  }
}
