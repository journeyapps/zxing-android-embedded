package com.journeyapps.barcodescanner;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.zxing.Result;
import com.google.zxing.client.android.R;

/**
 *
 */
public class CaptureActivity2 extends Activity {
  private BarcodeSurface surface;

  private BarcodeCallback callback = new BarcodeCallback() {
    @Override
    public void barcodeResult(Result result) {
      Toast.makeText(CaptureActivity2.this, "Scanned: " + result.getText(), Toast.LENGTH_SHORT).show();
      finish();
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.zxing_capture2);
    surface = (BarcodeSurface) findViewById(R.id.zxing_barcode_surface);
    surface.decodeSingle(callback);
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
}
