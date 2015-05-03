package com.journeyapps.barcodescanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.BeepManager;
import com.google.zxing.client.android.DecodeFormatManager;
import com.google.zxing.client.android.DecodeHintManager;
import com.google.zxing.client.android.InactivityTimer;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.R;
import com.journeyapps.barcodescanner.camera.CameraSettings;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class CaptureActivity extends Activity {
    private static final String TAG = CaptureActivity.class.getSimpleName();

    private BarcodeView barcodeView;
    private ViewfinderView viewFinder;
    private TextView statusView;

    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;

    private Handler handler;

    private boolean destroyed = false;

    private static final String SAVED_ORIENTATION_LOCK = "SAVED_ORIENTATION_LOCK";

    private int orientationLock = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

    // Delay long enough that the beep can be played.
    // TODO: play beep in background
    private static final long DELAY_BEEP = 150;

    private BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(final BarcodeResult result) {
            barcodeView.pause();
            beepManager.playBeepSoundAndVibrate();

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    returnResult(result);
                }
            }, DELAY_BEEP);

        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
            for (ResultPoint point : resultPoints) {
                viewFinder.addPossibleResultPoint(point);
            }
        }
    };

    private final CameraPreview.StateListener stateListener = new CameraPreview.StateListener() {
        @Override
        public void previewSized() {

        }

        @Override
        public void previewStarted() {

        }

        @Override
        public void previewStopped() {

        }

        @Override
        public void cameraError(Exception error) {
            displayFrameworkBugMessageAndExit();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.zxing_capture);

        handler = new Handler();

        barcodeView = (BarcodeView) findViewById(R.id.zxing_barcode_surface);
        barcodeView.addStateListener(stateListener);
        barcodeView.decodeSingle(callback);

        viewFinder = (ViewfinderView) findViewById(R.id.zxing_viewfinder_view);
        viewFinder.setCameraPreview(barcodeView);

        statusView = (TextView) findViewById(R.id.zxing_status_view);

        inactivityTimer = new InactivityTimer(this, new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Finishing due to inactivity");
                finish();
            }
        });

        beepManager = new BeepManager(this);

        if (savedInstanceState != null) {
            // If the screen was locked and unlocked again, we may start in a different orientation
            // (even one not allowed by the manifest). In this case we restore the orientation we were
            // previously locked to.
            this.orientationLock = savedInstanceState.getInt(SAVED_ORIENTATION_LOCK, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }

        if (getIntent() != null && Intents.Scan.ACTION.equals(getIntent().getAction())) {
            initializeFromIntent(getIntent());
        }
    }

    private void initializeFromIntent(Intent intent) {
        // Scan the formats the intent requested, and return the result to the calling activity.
        Set<BarcodeFormat> decodeFormats = DecodeFormatManager.parseDecodeFormats(intent);
        Map<DecodeHintType, Object> decodeHints = DecodeHintManager.parseDecodeHints(intent);

        CameraSettings settings = new CameraSettings();

        if (orientationLock == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            // Only lock the orientation if it's not locked to something else yet
            boolean orientationLocked = intent.getBooleanExtra(Intents.Scan.ORIENTATION_LOCKED, true);

            if (orientationLocked) {
                lockOrientation();
            }
        }

        if (intent.hasExtra(Intents.Scan.CAMERA_ID)) {
            int cameraId = intent.getIntExtra(Intents.Scan.CAMERA_ID, -1);
            if (cameraId >= 0) {
                settings.setRequestedCameraId(cameraId);
            }
        }

        String customPromptMessage = intent.getStringExtra(Intents.Scan.PROMPT_MESSAGE);
        if (customPromptMessage != null) {
            statusView.setText(customPromptMessage);
        }

        String characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET);

        MultiFormatReader reader = new MultiFormatReader();
        reader.setHints(decodeHints);

        barcodeView.setCameraSettings(settings);
        barcodeView.setDecoderFactory(new DefaultDecoderFactory(decodeFormats, decodeHints, characterSet));
    }

    /**
     * Lock display to current orientation.
     */
    protected void lockOrientation() {
        // Only get the orientation if it's not locked to one yet.
        if (this.orientationLock == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            // Adapted from http://stackoverflow.com/a/14565436
            Display display = getWindowManager().getDefaultDisplay();
            int rotation = display.getRotation();
            int baseOrientation = getResources().getConfiguration().orientation;
            int orientation = 0;
            if (baseOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) {
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                } else {
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                }
            } else if (baseOrientation == Configuration.ORIENTATION_PORTRAIT) {
                if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_270) {
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                } else {
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                }
            }

            this.orientationLock = orientation;
        }
        //noinspection ResourceType
        setRequestedOrientation(this.orientationLock);
    }

    @Override
    protected void onResume() {
        super.onResume();

        barcodeView.resume();
        beepManager.updatePrefs();
        inactivityTimer.start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        barcodeView.pause();

        inactivityTimer.cancel();
        beepManager.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyed = true;
        inactivityTimer.cancel();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVED_ORIENTATION_LOCK, this.orientationLock);
    }

    @Override
    protected void onStop() {
        super.onStop();
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

    public static Intent resultIntent(BarcodeResult rawResult) {
        Intent intent = new Intent(Intents.Scan.ACTION);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.putExtra(Intents.Scan.RESULT, rawResult.toString());
        intent.putExtra(Intents.Scan.RESULT_FORMAT, rawResult.getBarcodeFormat().toString());
        byte[] rawBytes = rawResult.getRawBytes();
        if (rawBytes != null && rawBytes.length > 0) {
            intent.putExtra(Intents.Scan.RESULT_BYTES, rawBytes);
        }
        Map<ResultMetadataType, ?> metadata = rawResult.getResultMetadata();
        if (metadata != null) {
            if (metadata.containsKey(ResultMetadataType.UPC_EAN_EXTENSION)) {
                intent.putExtra(Intents.Scan.RESULT_UPC_EAN_EXTENSION,
                        metadata.get(ResultMetadataType.UPC_EAN_EXTENSION).toString());
            }
            Number orientation = (Number) metadata.get(ResultMetadataType.ORIENTATION);
            if (orientation != null) {
                intent.putExtra(Intents.Scan.RESULT_ORIENTATION, orientation.intValue());
            }
            String ecLevel = (String) metadata.get(ResultMetadataType.ERROR_CORRECTION_LEVEL);
            if (ecLevel != null) {
                intent.putExtra(Intents.Scan.RESULT_ERROR_CORRECTION_LEVEL, ecLevel);
            }
            @SuppressWarnings("unchecked")
            Iterable<byte[]> byteSegments = (Iterable<byte[]>) metadata.get(ResultMetadataType.BYTE_SEGMENTS);
            if (byteSegments != null) {
                int i = 0;
                for (byte[] byteSegment : byteSegments) {
                    intent.putExtra(Intents.Scan.RESULT_BYTE_SEGMENTS_PREFIX + i, byteSegment);
                    i++;
                }
            }
        }
        return intent;
    }

    protected void returnResult(BarcodeResult rawResult) {
        Intent intent = resultIntent(rawResult);
        setResult(RESULT_OK, intent);
        finish();
    }


    protected void displayFrameworkBugMessageAndExit() {
        if (this.isFinishing() || this.destroyed) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.zxing_app_name));
        builder.setMessage(getString(R.string.zxing_msg_camera_framework_bug));
        builder.setPositiveButton(R.string.zxing_button_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        builder.show();
    }

}
