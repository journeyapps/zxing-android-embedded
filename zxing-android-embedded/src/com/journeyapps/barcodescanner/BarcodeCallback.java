package com.journeyapps.barcodescanner;

import com.google.zxing.Result;
import com.google.zxing.ResultPoint;

import java.util.List;

/**
 *
 */
public interface BarcodeCallback {
    public void barcodeResult(Result result);

    public void possibleResultPoints(List<ResultPoint> resultPoints);
}
