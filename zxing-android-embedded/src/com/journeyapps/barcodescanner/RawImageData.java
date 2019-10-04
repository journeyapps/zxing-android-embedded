package com.journeyapps.barcodescanner;

import android.graphics.Rect;

public class RawImageData {
    private byte[] data;
    private int width;
    private int height;

    public RawImageData(byte[] data, int width, int height) {
        this.data = data;
        this.width = width;
        this.height = height;
    }

    public byte[] getData() {
        return data;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public RawImageData cropAndScale(Rect cropRect, int scale) {
        int width = cropRect.width() / scale;
        int height = cropRect.height() / scale;

        int top = cropRect.top;

        int area = width * height;
        byte[] matrix = new byte[area];

        if (scale == 1) {
            int inputOffset = top * this.width + cropRect.left;

            // Copy one cropped row at a time.
            for (int y = 0; y < height; y++) {
                int outputOffset = y * width;
                System.arraycopy(this.data, inputOffset, matrix, outputOffset, width);
                inputOffset += this.width;
            }
        } else {
            int inputOffset = top * this.width + cropRect.left;

            // Copy one cropped row at a time.
            for (int y = 0; y < height; y++) {
                int outputOffset = y * width;
                int xOffset = inputOffset;
                for (int x = 0; x < width; x++) {
                    matrix[outputOffset] = this.data[xOffset];
                    xOffset += scale;
                    outputOffset += 1;
                }
                inputOffset += this.width * scale;
            }
        }
        return new RawImageData(matrix, width, height);
    }


    public RawImageData rotateCameraPreview(int cameraRotation) {
        switch (cameraRotation) {
            case 90:
                return new RawImageData(rotateCW(data, this.width, this.height), this.height, this.width);
            case 180:
                return new RawImageData(rotate180(data, this.width, this.height), this.width, this.height);
            case 270:
                return new RawImageData(rotateCCW(data, this.width, this.height), this.height, this.width);
            case 0:
            default:
                return this;
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
