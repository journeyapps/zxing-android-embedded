package com.journeyapps.barcodescanner;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This contains the result of a barcode scan.
 *
 * This class delegate all read-only fields of {@link com.google.zxing.Result},
 * and adds a bitmap with scanned barcode.
 */
public class BarcodeResult {
    private static final float PREVIEW_LINE_WIDTH = 4.0f;
    private static final float PREVIEW_DOT_WIDTH = 10.0f;

    protected Result mResult;
    protected SourceData sourceData;

    private final int mScaleFactor = 2;

    public BarcodeResult(Result result, SourceData sourceData) {
        this.mResult = result;
        this.sourceData = sourceData;
    }

    private static void drawLine(Canvas canvas, Paint paint, ResultPoint a, ResultPoint b, int scaleFactor) {
        if (a != null && b != null) {
            canvas.drawLine(a.getX() / scaleFactor,
                    a.getY() / scaleFactor,
                    b.getX() / scaleFactor,
                    b.getY() / scaleFactor,
                    paint);
        }
    }

    /**
     * @return wrapped {@link com.google.zxing.Result}
     */
    public Result getResult() {
        return mResult;
    }

    /**
     * @return {@link Bitmap} with barcode preview
     * @see #getBitmapWithResultPoints(int)
     */
    public Bitmap getBitmap() {
        return sourceData.getBitmap(null, mScaleFactor);
    }

    public List<ResultPoint> getTransformedResultPoints() {
        if (this.mResult.getResultPoints() == null) {
            return Collections.emptyList();
        }
        return transformResultPoints(Arrays.asList(this.mResult.getResultPoints()), this.sourceData);
    }

    /**
     * @param color Color of result points
     * @return {@link Bitmap} with result points on it, or plain bitmap, if no result points
     */
    public Bitmap getBitmapWithResultPoints(int color) {
        Bitmap bitmap = getBitmap();
        Bitmap barcode = bitmap;
        List<ResultPoint> points = getTransformedResultPoints();

        if (!points.isEmpty() && bitmap != null) {
            barcode = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(barcode);
            canvas.drawBitmap(bitmap, 0, 0, null);
            Paint paint = new Paint();
            paint.setColor(color);
            if (points.size() == 2) {
                paint.setStrokeWidth(PREVIEW_LINE_WIDTH);
                drawLine(canvas, paint, points.get(0), points.get(1), mScaleFactor);
            } else if (points.size() == 4 &&
                    (mResult.getBarcodeFormat() == BarcodeFormat.UPC_A ||
                            mResult.getBarcodeFormat() == BarcodeFormat.EAN_13)) {
                // Hacky special case -- draw two lines, for the barcode and metadata
                drawLine(canvas, paint, points.get(0), points.get(1), mScaleFactor);
                drawLine(canvas, paint, points.get(2), points.get(3), mScaleFactor);
            } else {
                paint.setStrokeWidth(PREVIEW_DOT_WIDTH);
                for (ResultPoint point : points) {
                    if (point != null) {
                        canvas.drawPoint(point.getX() / mScaleFactor, point.getY() / mScaleFactor, paint);
                    }
                }
            }
        }
        return barcode;
    }

    /**
     *
     * @return Bitmap preview scale factor
     */
    public int getBitmapScaleFactor(){
        return mScaleFactor;
    }

    /**
     * @return raw text encoded by the barcode
     * @see Result#getText()
     */
    public String getText() {
        return mResult.getText();
    }

    /**
     * @return raw bytes encoded by the barcode, if applicable, otherwise {@code null}
     * @see Result#getRawBytes()
     */
    public byte[] getRawBytes() {
        return mResult.getRawBytes();
    }

    /**
     * @return points related to the barcode in the image. These are typically points
     * identifying finder patterns or the corners of the barcode. The exact meaning is
     * specific to the type of barcode that was decoded.
     * @see Result#getResultPoints()
     */
    public ResultPoint[] getResultPoints() {
        return mResult.getResultPoints();
    }

    /**
     * @return {@link BarcodeFormat} representing the format of the barcode that was decoded
     * @see Result#getBarcodeFormat()
     */
    public BarcodeFormat getBarcodeFormat() {
        return mResult.getBarcodeFormat();
    }

    /**
     * @return {@link Map} mapping {@link ResultMetadataType} keys to values. May be
     * {@code null}. This contains optional metadata about what was detected about the barcode,
     * like orientation.
     * @see Result#getResultMetadata()
     */
    public Map<ResultMetadataType, Object> getResultMetadata() {
        return mResult.getResultMetadata();
    }

    public long getTimestamp() {
        return mResult.getTimestamp();
    }

    @Override
    public String toString() {
        return mResult.getText();
    }


    public static List<ResultPoint> transformResultPoints(List<ResultPoint> resultPoints, SourceData sourceData) {
        List<ResultPoint> scaledPoints = new ArrayList<>(resultPoints.size());
        for (ResultPoint point : resultPoints) {
            scaledPoints.add(sourceData.translateResultPoint(point));
        }
        return scaledPoints;
    }
}
