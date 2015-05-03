package com.journeyapps.barcodescanner;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;
import com.google.zxing.common.HybridBinarizer;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for decoding images.
 *
 * A decoder contains all the configuration required for the binarization and decoding process.
 *
 * The actual decoding should happen on a dedicated thread.
 */
public class Decoder implements ResultPointCallback {
    private Reader reader;

    /**
     * Create a new Decoder with the specified Reader.
     *
     * It is recommended to use an instance of MultiFormatReader in most cases.
     *
     * @param reader the reader
     */
    public Decoder(Reader reader) {
        this.reader = reader;
    }

    protected Reader getReader() {
        return reader;
    }

    /**
     * Given an image source, attempt to decode the barcode.
     *
     * Must not raise an exception.
     *
     * @param source the image source
     * @return a Result or null
     */
    public Result decode(LuminanceSource source) {
        return decode(toBitmap(source));
    }

    /**
     * Given an image source, convert to a binary bitmap.
     *
     * Override this to use a custom binarizer.
     *
     * @param source the image source
     * @return a BinaryBitmap
     */
    protected BinaryBitmap toBitmap(LuminanceSource source) {
        return new BinaryBitmap(new HybridBinarizer(source));
    }

    /**
     * Decode a binary bitmap.
     *
     * @param bitmap the binary bitmap
     * @return a Result or null
     */
    protected Result decode(BinaryBitmap bitmap) {
        possibleResultPoints.clear();
        try {
            if (reader instanceof MultiFormatReader) {
                // Optimization - MultiFormatReader's normal decode() method is slow.
                return ((MultiFormatReader) reader).decodeWithState(bitmap);
            } else {
                return reader.decode(bitmap);
            }
        } catch (Exception e) {
            // Decode error, try again next frame
            return null;
        } finally {
            reader.reset();
        }
    }

    private List<ResultPoint> possibleResultPoints = new ArrayList<>();

    /**
     * Call immediately after decode(), from the same thread.
     *
     * The result is undefined while decode() is running.
     *
     * @return possible ResultPoints from the last decode.
     */
    public List<ResultPoint> getPossibleResultPoints() {
        return new ArrayList<>(possibleResultPoints);
    }

    @Override
    public void foundPossibleResultPoint(ResultPoint point) {
        possibleResultPoints.add(point);
    }
}
