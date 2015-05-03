package com.journeyapps.barcodescanner;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.google.zxing.LuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.R;
import com.journeyapps.barcodescanner.camera.CameraInstance;
import com.journeyapps.barcodescanner.camera.PreviewCallback;

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
    private Rect cropRect;
    private boolean running = false;
    private final Object LOCK = new Object();

    private final Handler.Callback callback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            if (message.what == R.id.zxing_decode) {
                decode((SourceData) message.obj);
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
        running = true;
        requestNextPreview();
    }


    /**
     * Stop decoding.
     *
     * This must be called from the UI thread.
     */
    public void stop() {
        Util.validateMainThread();

        synchronized (LOCK) {
            running = false;
            handler.removeCallbacksAndMessages(null);
            thread.quit();
        }
    }


    private final PreviewCallback previewCallback = new PreviewCallback() {
        @Override
        public void onPreview(SourceData sourceData) {
            // Only post if running, to prevent a warning like this:
            //   java.lang.RuntimeException: Handler (android.os.Handler) sending message to a Handler on a dead thread

            // synchronize to handle cases where this is called concurrently with stop()
            synchronized (LOCK) {
                if (running) {
                    // Post to our thread.
                    handler.obtainMessage(R.id.zxing_decode, sourceData).sendToTarget();
                }
            }
        }
    };

    private void requestNextPreview() {
        if (cameraInstance.isOpen()) {
            cameraInstance.requestPreview(previewCallback);
        }
    }

    protected LuminanceSource createSource(SourceData sourceData) {
        if (this.cropRect == null) {
            return null;
        } else {
            return sourceData.createSource();
        }
    }

    private void decode(SourceData sourceData) {
        long start = System.currentTimeMillis();
        Result rawResult = null;
        sourceData.setCropRect(cropRect);
        LuminanceSource source = createSource(sourceData);

        if(source != null) {
            rawResult = decoder.decode(source);
        }

        if (rawResult != null) {
            // Don't log the barcode contents for security.
            long end = System.currentTimeMillis();
            Log.d(TAG, "Found barcode in " + (end - start) + " ms");
            if (resultHandler != null) {
                BarcodeResult barcodeResult = new BarcodeResult(rawResult, sourceData);
                Message message = Message.obtain(resultHandler, R.id.zxing_decode_succeeded, barcodeResult);
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

}
