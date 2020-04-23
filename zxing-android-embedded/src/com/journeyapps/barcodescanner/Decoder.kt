package com.journeyapps.barcodescanner

import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.util.*

/**
 * A class for decoding images.
 *
 * A decoder contains all the configuration required for the binarization and decoding process.
 *
 * The actual decoding should happen on a dedicated thread.
 */
open class Decoder
/**
 * Create a new Decoder with the specified Reader.
 *
 * It is recommended to use an instance of MultiFormatReader in most cases.
 *
 * @param reader the reader
 */(private val reader: Reader) : ResultPointCallback {

    /**
     * Given an image source, attempt to decode the barcode.
     *
     * Must not raise an exception.
     *
     * @param source the image source
     * @return a Result or null
     */
    fun decode(source: LuminanceSource): Result? {
        return decode(toBitmap(source))
    }

    /**
     * Given an image source, convert to a binary bitmap.
     *
     * Override this to use a custom binarizer.
     *
     * @param source the image source
     * @return a BinaryBitmap
     */
    protected open fun toBitmap(source: LuminanceSource): BinaryBitmap {
        return BinaryBitmap(HybridBinarizer(source))
    }

    /**
     * Decode a binary bitmap.
     *
     * @param bitmap the binary bitmap
     * @return a Result or null
     */
    protected fun decode(bitmap: BinaryBitmap): Result? {
        possibleResultPoints.clear()
        return try {
            if (reader is MultiFormatReader) {
                // Optimization - MultiFormatReader's normal decode() method is slow.
                reader.decodeWithState(bitmap)
            } else {
                reader.decode(bitmap)
            }
        } catch (e: Exception) {
            // Decode error, try again next frame
            null
        } finally {
            reader.reset()
        }
    }

    private val possibleResultPoints: MutableList<ResultPoint> = ArrayList()

    /**
     * Call immediately after decode(), from the same thread.
     *
     * The result is undefined while decode() is running.
     *
     * @return possible ResultPoints from the last decode.
     */
    fun getPossibleResultPoints(): List<ResultPoint> {
        return ArrayList(possibleResultPoints)
    }

    override fun foundPossibleResultPoint(point: ResultPoint) {
        possibleResultPoints.add(point)
    }

}