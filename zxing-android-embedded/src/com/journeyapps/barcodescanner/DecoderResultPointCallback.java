package com.journeyapps.barcodescanner;

import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;

/**
 * ResultPointCallback delegating the ResultPoints to a decoder.
 */
public class DecoderResultPointCallback implements ResultPointCallback {
    private Decoder decoder;

    public DecoderResultPointCallback(Decoder decoder) {
        this.decoder = decoder;
    }

    public DecoderResultPointCallback() {
    }

    public Decoder getDecoder() {
        return decoder;
    }

    public void setDecoder(Decoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public void foundPossibleResultPoint(ResultPoint point) {
        if(decoder != null) {
            decoder.foundPossibleResultPoint(point);
        }
    }
}
