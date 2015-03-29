package com.journeyapps.barcodescanner;

import com.google.zxing.Result;

/**
 *
 */
public interface BarcodeCallback {
  public void barcodeResult(Result result);
}
