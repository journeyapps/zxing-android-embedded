package com.journeyapps.barcodescanner;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;

import com.google.zxing.PlanarYUVLuminanceSource;

import java.io.ByteArrayOutputStream;

/**
 * Raw preview data from a camera.
 */
public class SourceData {
    /** Raw YUV data */
    private byte[] data;

    /** Source data width */
    private int dataWidth;

    /** Source data height */
    private int dataHeight;

    /** The format of the image data. ImageFormat.NV21 and ImageFormat.YUY2 are supported. */
    private int imageFormat;

    /** Rotation in degrees (0, 90, 180 or 270). This is camera rotation relative to display rotation. */
    private int rotation;

    /** Crop rectangle, in display orientation. */
    private Rect cropRect;

    /**
     *
     * @param data the image data
     * @param dataWidth width of the data
     * @param dataHeight height of the data
     * @param imageFormat ImageFormat.NV21 or ImageFormat.YUY2
     * @param rotation camera rotation relative to display rotation, in degrees (0, 90, 180 or 270).
     */
    public SourceData(byte[] data, int dataWidth, int dataHeight, int imageFormat, int rotation) {
        this.data = data;
        this.dataWidth = dataWidth;
        this.dataHeight = dataHeight;
        this.rotation = rotation;
        this.imageFormat = imageFormat;
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

    public byte[] getData() {
        return data;
    }

    /**
     *
     * @return width of the data
     */
    public int getDataWidth() {
        return dataWidth;
    }

    /**
     *
     * @return height of the data
     */
    public int getDataHeight() {
        return dataHeight;
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
        byte[] rotated = rotateCameraPreview(rotation, data, dataWidth, dataHeight);
        // TODO: handle mirrored (front) camera. Probably only the ResultPoints should be mirrored,
        // not the preview for decoding.
        if (isRotated()) {
            //noinspection SuspiciousNameCombination
            return new PlanarYUVLuminanceSource(rotated, dataHeight, dataWidth, cropRect.left, cropRect.top, cropRect.width(), cropRect.height(), false);
        } else {
            return new PlanarYUVLuminanceSource(rotated, dataWidth, dataHeight, cropRect.left, cropRect.top, cropRect.width(), cropRect.height(), false);
        }
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

    private Bitmap getBitmap(Rect cropRect, int scaleFactor) {
        if(isRotated()) {
            //noinspection SuspiciousNameCombination
            cropRect = new Rect(cropRect.top, cropRect.left, cropRect.bottom, cropRect.right);
        }

        // TODO: there should be a way to do this without JPEG compression / decompression cycle.
        YuvImage img = new YuvImage(data, imageFormat, dataWidth, dataHeight, null);
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

    public static byte[] rotateCameraPreview(int cameraRotation, byte[] data, int imageWidth, int imageHeight) {
        switch (cameraRotation) {
            case 0:
                return data;
            case 90:
                return rotateCW(data, imageWidth, imageHeight);
            case 180:
                return rotate180(data, imageWidth, imageHeight);
            case 270:
                return rotateCCW(data, imageWidth, imageHeight);
            default:
                // Should not happen
                return data;
        }
    }

    /**
     * Rotate an image by 90 degrees CW.
     *
     * @param data        the image data, in with the first width * height bytes being the luminance data.
     * @param imageWidth  the width of the image
     * @param imageHeight the height of the image
     * @return the rotated bytes
     */
    public static byte[] rotateCW(byte[] data, int imageWidth, int imageHeight) {
        // Adapted from http://stackoverflow.com/a/15775173
        // data may contain more than just y (u and v), but we are only interested in the y section.

        byte[] yuv = new byte[imageWidth * imageHeight];
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }
        return yuv;
    }

    /**
     * Rotate an image by 180 degrees.
     *
     * @param data        the image data, in with the first width * height bytes being the luminance data.
     * @param imageWidth  the width of the image
     * @param imageHeight the height of the image
     * @return the rotated bytes
     */
    public static byte[] rotate180(byte[] data, int imageWidth, int imageHeight) {
        int n = imageWidth * imageHeight;
        byte[] yuv = new byte[n];

        int i = n - 1;
        for (int j = 0; j < n; j++) {
            yuv[i] = data[j];
            i--;
        }
        return yuv;
    }

    /**
     * Rotate an image by 90 degrees CCW.
     *
     * @param data        the image data, in with the first width * height bytes being the luminance data.
     * @param imageWidth  the width of the image
     * @param imageHeight the height of the image
     * @return the rotated bytes
     */
    public static byte[] rotateCCW(byte[] data, int imageWidth, int imageHeight) {
        int n = imageWidth * imageHeight;
        byte[] yuv = new byte[n];
        int i = n - 1;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                yuv[i] = data[y * imageWidth + x];
                i--;
            }
        }
        return yuv;
    }

}
