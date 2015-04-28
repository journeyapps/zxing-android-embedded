package com.journeyapps.barcodescanner;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.google.zxing.LuminanceSource;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.R;
import com.journeyapps.barcodescanner.camera.CameraInstance;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 *
 */
public class DecoderThread {
    private static final String TAG = DecoderThread.class.getSimpleName();

    private CameraInstance cameraInstance;
    private HandlerThread thread;
    private Handler handler;
    private Decoder decoder;
    private Handler resultHandler;
    WeakReference<Handler> handlerWeakReference;
    private Rect cropRect;

    private final Handler.Callback callback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            if (message.what == R.id.zxing_decode) {
                decode((byte[]) message.obj, message.arg1, message.arg2);
            }
            return true;
        }
    };

    public DecoderThread(CameraInstance cameraInstance, Decoder decoder, Handler resultHandler) {
        Util.validateMainThread();

        this.cameraInstance = cameraInstance;
        this.decoder = decoder;
        this.resultHandler = resultHandler;
    }

    public Decoder getDecoder() {
        return decoder;
    }

    public void setDecoder(Decoder decoder) {
        this.decoder = decoder;
    }

    public Rect getCropRect() {
        return cropRect;
    }

    public void setCropRect(Rect cropRect) {
        this.cropRect = cropRect;
    }

    /**
     * Start decoding.
     *
     * This must be called from the UI thread.
     */
    public void start() {
        Util.validateMainThread();

        thread = new HandlerThread(TAG);
        thread.start();
        handler = new Handler(thread.getLooper(), callback);
        handlerWeakReference=new WeakReference<>(handler);
        requestNextPreview();
    }


    /**
     * Stop decoding.
     *
     * This must be called from the UI thread.
     */
    public void stop() {
        Util.validateMainThread();
        handler.removeCallbacksAndMessages(null);
        handler=null;
        handlerWeakReference.clear();
        thread.quit();
    }

    private void requestNextPreview() {
        if (cameraInstance.isOpen()) {
            cameraInstance.requestPreview(handlerWeakReference, R.id.zxing_decode);
        }
    }

    protected LuminanceSource createSource(byte[] data, int dataWidth, int dataHeight) {
        if (this.cropRect == null) {
            return null;
        } else {
            int rotation = cameraInstance.getCameraRotation();
            byte[] rotated = rotateCameraPreview(rotation, data, dataWidth, dataHeight);
            // TODO: handle mirrored (front) camera. Probably only the ResultPoints should be mirrored,
            // not the preview for decoding.
            if (cameraInstance.isCameraRotated()) {
                //noinspection SuspiciousNameCombination
                return new PlanarYUVLuminanceSource(rotated, dataHeight, dataWidth, cropRect.left, cropRect.top, cropRect.width(), cropRect.height(), false);
            } else {
                return new PlanarYUVLuminanceSource(rotated, dataWidth, dataHeight, cropRect.left, cropRect.top, cropRect.width(), cropRect.height(), false);
            }
        }
    }

    private void decode(byte[] data, int width, int height) {
        long start = System.currentTimeMillis();
        Result rawResult = null;
        LuminanceSource source = createSource(data, width, height);

        if (source != null) {
            rawResult = decoder.decode(source);
        }

        if (rawResult != null) {
            // Don't log the barcode contents for security.
            long end = System.currentTimeMillis();
            Log.d(TAG, "Found barcode in " + (end - start) + " ms");
            if (resultHandler != null) {
                Message message = Message.obtain(resultHandler, R.id.zxing_decode_succeeded, rawResult);
                Bundle bundle = new Bundle();
                message.setData(bundle);
                message.sendToTarget();
            }
        } else {
            if (resultHandler != null) {
                Message message = Message.obtain(resultHandler, R.id.zxing_decode_failed);
                message.sendToTarget();
            }
        }
        if (resultHandler != null) {
            List<ResultPoint> resultPoints = decoder.getPossibleResultPoints();
            Message message = Message.obtain(resultHandler, R.id.zxing_possible_result_points, resultPoints);
            message.sendToTarget();
        }
        requestNextPreview();
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
        //
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
