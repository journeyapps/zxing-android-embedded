package com.journeyapps.barcodescanner

import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.Reader
import com.google.zxing.common.HybridBinarizer

/**
 * Created by leighmd on 11/2/16.
 */
class InvertedDecoder
/**
 * Create a new Decoder with the specified Reader.
 *
 *
 * It is recommended to use an instance of MultiFormatReader in most cases.
 *
 * @param reader the reader
 */
(reader: Reader) : Decoder(reader) {
    /**
     * Given an image source, convert to a binary bitmap.
     *
     * Override this to use a custom binarizer.
     *
     * @param source the image source
     * @return a BinaryBitmap
     */
    override fun toBitmap(source: LuminanceSource): BinaryBitmap {
        return BinaryBitmap(HybridBinarizer(source.invert()))
    }
}