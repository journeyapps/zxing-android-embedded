package com.journeyapps.barcodescanner;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.zxing.Result;
import com.google.zxing.client.android.R;

/**
 *
 */
public class CaptureActivity2 extends Activity {
  private BarcodeView surface;

  private BarcodeCallback callback = new BarcodeCallback() {
    @Override
    public void barcodeResult(Result result) {
      TextView text = (TextView)findViewById(R.id.zxing_barcode_status);
      if(result.getText() != null) {
        text.setText(result.getText());
      }
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.zxing_capture2);
    surface = (BarcodeView) findViewById(R.id.zxing_barcode_surface);
    surface.decodeContinuous(callback);

    ViewfinderView viewFinder = (ViewfinderView)findViewById(R.id.zxing_viewfinder_view);
    viewFinder.setBarcodeView(surface);
  }

  @Override
  protected void onResume() {
    super.onResume();

    surface.resume();
  }

  @Override
  protected void onPause() {
    super.onPause();

    surface.pause();
  }

  public void triggerScan(View view) {
    surface.decodeSingle(callback);
  }
}
