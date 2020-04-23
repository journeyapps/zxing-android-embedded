package com.journeyapps.barcodescanner

import com.google.zxing.ResultPoint
import com.google.zxing.ResultPointCallback

/**
 * ResultPointCallback delegating the ResultPoints to a decoder.
 */
class DecoderResultPointCallback(var decoder: Decoder?) : ResultPointCallback {
    override fun foundPossibleResultPoint(point: ResultPoint) {
        decoder?.foundPossibleResultPoint(point)
    }
}