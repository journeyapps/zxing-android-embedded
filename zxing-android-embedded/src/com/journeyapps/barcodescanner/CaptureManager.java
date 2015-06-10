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
import android.view.Surface;
import android.view.Window;
import android.view.WindowManager;

import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.BeepManager;
import com.google.zxing.client.android.InactivityTimer;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.R;

import java.util.List;
import java.util.Map;

/**
 * Manages barcode scanning for a CaptureActivity. This class may be used to have a custom Activity
 * (e.g. with a customized look and feel, or a different superclass), but not the barcode scanning
 * process itself.
 *
 * This is intended for an Activity that is dedicated to capturing a single barcode and returning
 * it via setResult(). For other use cases, use DefaultBarcodeScannerView or BarcodeView directly.
 *
 * The following is managed by this class:
 * - Orientation lock
 * - InactivityTimer
 * - BeepManager
 * - Initializing from an Intent (via IntentIntegrator)
 * - Setting the result and finishing the Activity when a barcode is scanned
 * - Displaying camera errors
 */
public class CaptureManager {
    private static final String TAG = CaptureManager.class.getSimpleName();

    private Activity activity;
    private CompoundBarcodeView barcodeView;
    private int orientationLock = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    private static final String SAVED_ORIENTATION_LOCK = "SAVED_ORIENTATION_LOCK";

    private boolean destroyed = false;

    // Delay long enough that the beep can be played.
    // TODO: play beep in background
    private static final long DELAY_BEEP = 150;

    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;

    private Handler handler;

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

    public CaptureManager(Activity activity, CompoundBarcodeView barcodeView) {
        this.activity = activity;
        this.barcodeView = barcodeView;
        barcodeView.getBarcodeView().addStateListener(stateListener);

        handler = new Handler();

        inactivityTimer = new InactivityTimer(activity, new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Finishing due to inactivity");
                finish();
            }
        });

        beepManager = new BeepManager(activity);
    }

    /**
     * Perform initialization, according to preferences set in the intent.
     *
     * @param intent the intent containing the scanning preferences
     * @param savedInstanceState saved state, containing orientation lock
     */
    public void initializeFromIntent(Intent intent, Bundle savedInstanceState) {
        Window window = activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (savedInstanceState != null) {
            // If the screen was locked and unlocked again, we may start in a different orientation
            // (even one not allowed by the manifest). In this case we restore the orientation we were
            // previously locked to.
            this.orientationLock = savedInstanceState.getInt(SAVED_ORIENTATION_LOCK, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }

        if(intent != null) {
            if (orientationLock == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                // Only lock the orientation if it's not locked to something else yet
                boolean orientationLocked = intent.getBooleanExtra(Intents.Scan.ORIENTATION_LOCKED, true);

                if (orientationLocked) {
                    lockOrientation();
                }
            }

            if (Intents.Scan.ACTION.equals(intent.getAction())) {
                barcodeView.initializeFromIntent(intent);
            }

            if (!intent.getBooleanExtra(Intents.Scan.BEEP_ENABLED, true)) {
                beepManager.setBeepEnabled(false);
                beepManager.updatePrefs();
            }
        }
    }

    /**
     * Lock display to current orientation.
     */
    protected void lockOrientation() {
        // Only get the orientation if it's not locked to one yet.
        if (this.orientationLock == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            // Adapted from http://stackoverflow.com/a/14565436
            Display display = activity.getWindowManager().getDefaultDisplay();
            int rotation = display.getRotation();
            int baseOrientation = activity.getResources().getConfiguration().orientation;
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
        activity.setRequestedOrientation(this.orientationLock);
    }

    /**
     * Start decoding.
     */
    public void decode() {
        barcodeView.decodeSingle(callback);
    }

    /**
     * Call from Activity#onResume().
     */
    public void onResume() {
        barcodeView.resume();
        beepManager.updatePrefs();
        inactivityTimer.start();
    }

    /**
     * Call from Activity#onPause().
     */
    public void onPause() {
        barcodeView.pause();

        inactivityTimer.cancel();
        beepManager.close();
    }

    /**
     * Call from Activity#onDestroy().
     */
    public void onDestroy() {
        destroyed = true;
        inactivityTimer.cancel();
    }

    /**
     * Call from Activity#onSaveInstanceState().
     */
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SAVED_ORIENTATION_LOCK, this.orientationLock);
    }


    /**
     * Create a intent to return as the Activity result.
     *
     * @param rawResult the BarcodeResult, must not be null.
     * @return the Intent
     */
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

    private void finish() {
        activity.finish();
    }


    protected void returnResult(BarcodeResult rawResult) {
        Intent intent = resultIntent(rawResult);
        activity.setResult(Activity.RESULT_OK, intent);
        finish();
    }

    protected void displayFrameworkBugMessageAndExit() {
        if (activity.isFinishing() || this.destroyed) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getString(R.string.zxing_app_name));
        builder.setMessage(activity.getString(R.string.zxing_msg_camera_framework_bug));
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
