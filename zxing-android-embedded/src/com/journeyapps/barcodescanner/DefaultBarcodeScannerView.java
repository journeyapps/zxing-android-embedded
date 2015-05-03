package com.journeyapps.barcodescanner;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.DecodeFormatManager;
import com.google.zxing.client.android.DecodeHintManager;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.R;
import com.journeyapps.barcodescanner.camera.CameraSettings;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates BarcodeView, ViewfinderView and status text.
 */
public class DefaultBarcodeScannerView extends FrameLayout {
    private static final String TAG = DefaultBarcodeScannerView.class.getSimpleName();

    private BarcodeView barcodeView;
    private ViewfinderView viewFinder;
    private TextView statusView;

    private class WrappedCallback implements BarcodeCallback {
        private BarcodeCallback delegate;

        public WrappedCallback(BarcodeCallback delegate) {
            this.delegate = delegate;
        }

        @Override
        public void barcodeResult(BarcodeResult result) {
            delegate.barcodeResult(result);
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
            for (ResultPoint point : resultPoints) {
                viewFinder.addPossibleResultPoint(point);
            }
        }
    }

    public DefaultBarcodeScannerView(Context context) {
        super(context);
        initialize();
    }

    public DefaultBarcodeScannerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public DefaultBarcodeScannerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        inflate(getContext(), R.layout.zxing_barcode_scanner, this);

        barcodeView = (BarcodeView) findViewById(R.id.zxing_barcode_surface);

        viewFinder = (ViewfinderView) findViewById(R.id.zxing_viewfinder_view);
        viewFinder.setCameraPreview(barcodeView);

        statusView = (TextView) findViewById(R.id.zxing_status_view);
    }


    public void initializeFromIntent(Intent intent) {
        // Scan the formats the intent requested, and return the result to the calling activity.
        Set<BarcodeFormat> decodeFormats = DecodeFormatManager.parseDecodeFormats(intent);
        Map<DecodeHintType, Object> decodeHints = DecodeHintManager.parseDecodeHints(intent);

        CameraSettings settings = new CameraSettings();

        if (intent.hasExtra(Intents.Scan.CAMERA_ID)) {
            int cameraId = intent.getIntExtra(Intents.Scan.CAMERA_ID, -1);
            if (cameraId >= 0) {
                settings.setRequestedCameraId(cameraId);
            }
        }

        String customPromptMessage = intent.getStringExtra(Intents.Scan.PROMPT_MESSAGE);
        if (customPromptMessage != null) {
            setStatusText(customPromptMessage);
        }

        String characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET);

        MultiFormatReader reader = new MultiFormatReader();
        reader.setHints(decodeHints);

        barcodeView.setCameraSettings(settings);
        barcodeView.setDecoderFactory(new DefaultDecoderFactory(decodeFormats, decodeHints, characterSet));
    }

    public void setStatusText(String text) {
        statusView.setText(text);
    }

    public void pause() {
        barcodeView.pause();
    }

    public void resume() {
        barcodeView.resume();
    }

    public BarcodeView getBarcodeView() {
        return (BarcodeView) findViewById(R.id.zxing_barcode_surface);
    }

    public ViewfinderView getViewFinder() {
        return viewFinder;
    }

    public TextView getStatusView() {
        return statusView;
    }

    public void decodeSingle(BarcodeCallback callback) {
        barcodeView.decodeSingle(new WrappedCallback(callback));

    }

    public void decodeContinuous(BarcodeCallback callback) {
        barcodeView.decodeContinuous(new WrappedCallback(callback));
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
                // Handle these events so they don't launch the Camera app
                return true;
            // Use volume up/down to turn on light
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                barcodeView.setTorch(false);
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                barcodeView.setTorch(true);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
