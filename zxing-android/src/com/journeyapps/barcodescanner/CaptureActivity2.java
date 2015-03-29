package com.journeyapps.barcodescanner;

import android.app.Activity;
import android.os.Bundle;

import com.google.zxing.client.android.R;

/**
 *
 */
public class CaptureActivity2 extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.zxing_capture2);
  }

  @Override
  protected void onResume() {
    super.onResume();

    BarcodeSurface surface = (BarcodeSurface) findViewById(R.id.zxing_barcode_surface);
    surface.resume();
  }

  @Override
  protected void onPause() {
    super.onPause();


    BarcodeSurface surface = (BarcodeSurface) findViewById(R.id.zxing_barcode_surface);
    surface.pause();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    BarcodeSurface surface = (BarcodeSurface) findViewById(R.id.zxing_barcode_surface);
    surface.destroy();
  }
}
