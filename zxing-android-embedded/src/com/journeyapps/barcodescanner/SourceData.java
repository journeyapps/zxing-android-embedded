package com.journeyapps.barcodescanner;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;

import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ResultPoint;

import java.io.ByteArrayOutputStream;

/**
 * Raw preview data from a camera.
 */
public class SourceData {
    private RawImageData data;

    /** The format of the image data. ImageFormat.NV21 and ImageFormat.YUY2 are supported. */
    private int imageFormat;

    /** Rotation in degrees (0, 90, 180 or 270). This is camera rotation relative to display rotation. */
    private int rotation;

    /** Crop rectangle, in display orientation. */
    private Rect cropRect;

    /**
     * Factor by which to scale down before decoding.
     */
    private int scalingFactor = 1;

    private boolean previewMirrored;

    /**
     *
     * @param data the image data
     * @param dataWidth width of the data
     * @param dataHeight height of the data
     * @param imageFormat ImageFormat.NV21 or ImageFormat.YUY2
     * @param rotation camera rotation relative to display rotation, in degrees (0, 90, 180 or 270).
     */
    public SourceData(byte[] data, int dataWidth, int dataHeight, int imageFormat, int rotation) {
        this.data = new RawImageData(data, dataWidth, dataHeight);
        this.rotation = rotation;
        this.imageFormat = imageFormat;
        if(dataWidth * dataHeight > data.length) {
            throw new IllegalArgumentException("Image data does not match the resolution. " + dataWidth + "x" + dataHeight + " > " + data.length);
        }
    }

    public Rect getCropRect() {
        return cropRect;
    }

    /**
     * Set the crop rectangle.
     *
     * @param cropRect the new crop rectangle.
     */
    public void setCropRect(Rect cropRect) {
        this.cropRect = cropRect;
    }

    public boolean isPreviewMirrored() {
        return previewMirrored;
    }

    public void setPreviewMirrored(boolean previewMirrored) {
        this.previewMirrored = previewMirrored;
    }

    public int getScalingFactor() {
        return scalingFactor;
    }

    public void setScalingFactor(int scalingFactor) {
        this.scalingFactor = scalingFactor;
    }

    public byte[] getData() {
        return data.getData();
    }

    /**
     *
     * @return width of the data
     */
    public int getDataWidth() {
        return data.getWidth();
    }

    /**
     *
     * @return height of the data
     */
    public int getDataHeight() {
        return data.getHeight();
    }

    public ResultPoint translateResultPoint(ResultPoint point) {
        float x = point.getX() * this.scalingFactor + this.cropRect.left;
        float y = point.getY() * this.scalingFactor + this.cropRect.top;
        if (previewMirrored) {
            x = data.getWidth() - x;
        }
        return new ResultPoint(x, y);
    }

    /**
     *
     * @return true if the preview image is rotated orthogonal to the display
     */
    public boolean isRotated() {
        return rotation % 180 != 0;
    }

    public int getImageFormat() {
        return imageFormat;
    }

    public PlanarYUVLuminanceSource createSource() {
        RawImageData rotated = this.data.rotateCameraPreview(rotation);
        RawImageData scaled = rotated.cropAndScale(this.cropRect, this.scalingFactor);

        // not the preview for decoding.
        return new PlanarYUVLuminanceSource(scaled.getData(), scaled.getWidth(), scaled.getHeight(), 0, 0, scaled.getWidth(), scaled.getHeight(), false);
    }

    /**
     * Return the source bitmap (cropped; in display orientation).
     *
     * @return the bitmap
     */
    public Bitmap getBitmap() {
        return getBitmap(1);
    }

    /**
     * Return the source bitmap (cropped; in display orientation).
     *
     * @param scaleFactor factor to scale down by. Must be a power of 2.
     * @return the bitmap
     */
    public Bitmap getBitmap(int scaleFactor) {
        return getBitmap(cropRect, scaleFactor);
    }

    public Bitmap getBitmap(Rect cropRect, int scaleFactor) {
        if (cropRect == null) {
            cropRect = new Rect(0, 0, data.getWidth(), data.getHeight());
        } else if(isRotated()) {
            //noinspection SuspiciousNameCombination
            cropRect = new Rect(cropRect.top, cropRect.left, cropRect.bottom, cropRect.right);
        }

        // TODO: there should be a way to do this without JPEG compression / decompression cycle.
        YuvImage img = new YuvImage(data.getData(), imageFormat, data.getWidth(), data.getHeight(), null);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        img.compressToJpeg(cropRect, 90, buffer);
        byte[] jpegData = buffer.toByteArray();

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = scaleFactor;
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, options);

        // Rotate if required
        if (rotation != 0) {
            Matrix imageMatrix = new Matrix();
            imageMatrix.postRotate(rotation);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), imageMatrix, false);
        }

        return bitmap;
    }

}
