/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraManager;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;

/**
 * This activity opens the camera and does the actual scanning on a background thread. It draws a
 * viewfinder to help the user place the barcode correctly, shows feedback as the image processing
 * is happening, and then overlays the results when a scan is successful.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class CaptureActivity extends Activity {
    //TODO this all definitely needs to be broken down into multiple classes
    private static final String TAG = CaptureActivity.class.getSimpleName();

    private static final long DEFAULT_INTENT_RESULT_DURATION_MS = 1500L;
    private static final long BULK_MODE_SCAN_DELAY_MS = 1000L;

    private static final String[] ZXING_URLS = { "http://zxing.appspot.com/scan", "zxing://scan/" };

    private static final Collection<ResultMetadataType> DISPLAYABLE_METADATA_TYPES =
            EnumSet.of(ResultMetadataType.ISSUE_NUMBER,
                    ResultMetadataType.SUGGESTED_PRICE,
                    ResultMetadataType.ERROR_CORRECTION_LEVEL,
                    ResultMetadataType.POSSIBLE_COUNTRY);
    public static final java.lang.String ZXING_CAPTURE_LAYOUT_ID_KEY = "ZXING_CAPTURE_LAYOUT_ID_KEY";
    public static final String ZXING_BEEP_ON_KEY = "BEEP_ON";
    public static final String ZXING_ORIENTATION_KEY = "ORIENTATION";

    public static final String LAYOUT_ORIENTATION_LANDSCAPE = "LANDSCAPE";
    public static final String LAYOUT_ORIENTATION_PORTRAIT = "PORTRAIT";

    private CameraManager cameraManager;
    private CaptureActivityHandler activityHandler;
    private Result savedResultToShow;
    private ViewfinderView viewfinderView;
    private TextView statusView;
    private View resultView;
    private Result lastResult;
    private IntentSource source;
    private String sourceUrl;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;
    private AmbientLightManager ambientLightManager;
    private Bundle intentOptions;
    private PreviewHandler previewHandler;

    ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getActivityHandler() {
        return activityHandler;
    }

    protected void setActivityHandler(CaptureActivityHandler handler) {
        this.activityHandler = handler;
    }

    CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // If the resource id with a layout was provided, set up this layout
        intentOptions = getIntent().getExtras();

        int zxingCaptureLayoutResourceId = R.layout.zxing_capture;
        if (intentOptions != null) {
            zxingCaptureLayoutResourceId = intentOptions.getInt(ZXING_CAPTURE_LAYOUT_ID_KEY, R.layout.zxing_capture);

            if(intentOptions.getBoolean(ZXING_BEEP_ON_KEY, true)) {
                beepManager = new BeepManager(this);
            }
        } else {
            //do optional things by default here if no parameters were given
            beepManager = new BeepManager(this);
        }
        setContentView (zxingCaptureLayoutResourceId);

        inactivityTimer = new InactivityTimer(this);
        ambientLightManager = new AmbientLightManager(this);

        PreferenceManager.setDefaultValues(this, R.xml.zxing_preferences, false);

        Button cancelButton = (Button) findViewById(R.id.zxing_back_button);

        // Since the layout can be dynamically set by the Intent, cancelButton may not be present
        if (cancelButton != null) {
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setResult(RESULT_CANCELED);
                    finish();
                }
            });
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        // CameraManager must be initialized here, not in onCreate(). This is necessary because we don't
        // want to open the camera driver and measure the screen size if we're going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the wrong size and partially
        // off screen.
        cameraManager = new CameraManager(getApplication(), intentOptions);

        previewHandler = new PreviewHandler(this, cameraManager);

        viewfinderView = (ViewfinderView) findViewById(R.id.zxing_viewfinder_view);
        viewfinderView.setCameraManager(cameraManager);

        resultView = findViewById(R.id.zxing_result_view);
        statusView = (TextView) findViewById(R.id.zxing_status_view);

        activityHandler = null;
        lastResult = null;

        setOrientation();

        resetStatusView();

        previewHandler.setPreviewCallbacks();

        if(beepManager != null) {
            beepManager.updatePrefs();
        }

        ambientLightManager.start(cameraManager);
        inactivityTimer.onResume();

        Intent intent = getIntent();

        source = IntentSource.NONE;

        parseIntentType(intent);
    }

    @Override
    protected void onPause() {
        if (activityHandler != null) {
            activityHandler.quitSynchronously();
            activityHandler = null;
        }
        inactivityTimer.onPause();
        ambientLightManager.stop();
        if(beepManager != null) beepManager.close();
        cameraManager.closeDriver();

        previewHandler.removeCallbacks();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (source == IntentSource.NATIVE_APP_INTENT) {
                    setResult(RESULT_CANCELED);
                    finish();
                    return true;
                }
                if ((source == IntentSource.NONE || source == IntentSource.ZXING_LINK) && lastResult != null) {
                    restartPreviewAfterDelay(0L);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
                // Handle these events so they don't launch the Camera app
                return true;
            // Use volume up/down to turn on light
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                cameraManager.setTorch(false);
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                cameraManager.setTorch(true);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.zxing_capture, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        int itemId = item.getItemId();
        if(itemId == R.id.zxing_menu_help) {
            intent.setClassName(this, HelpActivity.class.getName());
            startActivity(intent);
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private static boolean isZXingURL(String dataString) {
        if (dataString == null) {
            return false;
        }
        for (String url : ZXING_URLS) {
            if (dataString.startsWith(url)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A valid barcode has been found, so give an indication of success and show the results.
     *
     * @param rawResult The contents of the barcode.
     * @param barcode   A greyscale bitmap of the camera data which was decoded.
     */
    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        inactivityTimer.onActivity();
        lastResult = rawResult;

        boolean fromLiveScan = barcode != null;
        if (fromLiveScan) {
            // Then not from history, so beep/vibrate and we have an image to draw on
            if(beepManager != null) beepManager.playBeepSoundAndVibrate();
            //TODO wrap this in an option flag
            drawResultPoints(barcode, scaleFactor, rawResult);
        }

        handleDecodeExternally(rawResult, barcode);
    }

    protected void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
        // Bitmap isn't used yet -- will be used soon
        if (activityHandler == null) {
            savedResultToShow = result;
        } else {
            if (result != null) {
                savedResultToShow = result;
            }
            if (savedResultToShow != null) {
                Message message = Message.obtain(activityHandler, R.id.zxing_decode_succeeded, savedResultToShow);
                activityHandler.sendMessage(message);
            }
            savedResultToShow = null;
        }
    }

    /**
     * Superimpose a line for 1D or dots for 2D to highlight the key features of the barcode.
     *
     * @param barcode   A bitmap of the captured image.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param rawResult The decoded results which contains the points to draw.
     */
    private void drawResultPoints(Bitmap barcode, float scaleFactor, Result rawResult) {
        ResultPoint[] points = rawResult.getResultPoints();
        if (points != null && points.length > 0) {
            Canvas canvas = new Canvas(barcode);
            Paint paint = new Paint();
            paint.setColor(getResources().getColor(R.color.zxing_result_points));
            if (points.length == 2) {
                paint.setStrokeWidth(4.0f);
                drawLine(canvas, paint, points[0], points[1], scaleFactor,
                        cameraManager.getCurrentOrientation());
            } else if (points.length == 4 &&
                    (rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A ||
                            rawResult.getBarcodeFormat() == BarcodeFormat.EAN_13)) {
                // Hacky special case -- draw two lines, for the barcode and metadata
                drawLine(canvas, paint, points[0], points[1], scaleFactor,
                        cameraManager.getCurrentOrientation());
                drawLine(canvas, paint, points[2], points[3], scaleFactor,
                        cameraManager.getCurrentOrientation());
            } else {
                paint.setStrokeWidth(10.0f);
                for (ResultPoint point : points) {
                    if (point != null) {
                        if(cameraManager.getCurrentOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                            canvas.drawPoint(scaleFactor * point.getY(), scaleFactor * point.getX(), paint);
                        } else {
                            canvas.drawPoint(scaleFactor * point.getX(), scaleFactor * point.getY(), paint);
                        }
                    }
                }
            }
        }
    }

    private static void drawLine(Canvas canvas, Paint paint, ResultPoint a, ResultPoint b,
                                 float scaleFactor, int orientation) {
        if (a != null && b != null) {
            if(orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                canvas.drawLine(scaleFactor * a.getY(),
                        scaleFactor * a.getX(),
                        scaleFactor * b.getY(),
                        scaleFactor * b.getX(),
                        paint);
            } else {
                canvas.drawLine(scaleFactor * a.getX(),
                        scaleFactor * a.getY(),
                        scaleFactor * b.getX(),
                        scaleFactor * b.getY(),
                        paint);
            }
        }
    }

    // Briefly show the contents of the barcode, then handle the result outside Barcode Scanner.
    private void handleDecodeExternally(Result rawResult, Bitmap barcode) {

        if (barcode != null) {
            viewfinderView.drawResultBitmap(barcode);
        }

        long resultDurationMS;
        if (getIntent() == null) {
            resultDurationMS = DEFAULT_INTENT_RESULT_DURATION_MS;
        } else {
            resultDurationMS = getIntent().getLongExtra(Intents.Scan.RESULT_DISPLAY_DURATION_MS,
                    DEFAULT_INTENT_RESULT_DURATION_MS);
        }

        if (source == IntentSource.NATIVE_APP_INTENT) {

            // Hand back whatever action they requested - this can be changed to Intents.Scan.ACTION when
            // the deprecated intent is retired.
            Intent intent = new Intent(getIntent().getAction());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            intent.putExtra(Intents.Scan.RESULT, rawResult.toString());
            intent.putExtra(Intents.Scan.RESULT_FORMAT, rawResult.getBarcodeFormat().toString());
            byte[] rawBytes = rawResult.getRawBytes();
            if (rawBytes != null && rawBytes.length > 0) {
                intent.putExtra(Intents.Scan.RESULT_BYTES, rawBytes);
            }
            Map<ResultMetadataType,?> metadata = rawResult.getResultMetadata();
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
            sendReplyMessage(R.id.zxing_return_scan_result, intent, resultDurationMS);

        }
    }

    private void sendReplyMessage(int id, Object arg, long delayMS) {
        if (activityHandler != null) {
            Message message = Message.obtain(activityHandler, id, arg);
            if (delayMS > 0L) {
                activityHandler.sendMessageDelayed(message, delayMS);
            } else {
                activityHandler.sendMessage(message);
            }
        }
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (activityHandler != null) {
            activityHandler.sendEmptyMessageDelayed(R.id.zxing_restart_preview, delayMS);
        }
        resetStatusView();
    }

    private void resetStatusView() {
        resultView.setVisibility(View.GONE);

        if(statusView != null) {
            statusView.setText(R.string.zxing_msg_default_status);
            statusView.setVisibility(View.VISIBLE);
        }

        viewfinderView.setVisibility(View.VISIBLE);
        lastResult = null;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    private void setOrientation() {
        String orientation = (String) intentOptions.get(ZXING_ORIENTATION_KEY);

        //if we don't find the orientation option then just use the default
        if(orientation == null) {
            orientation = LAYOUT_ORIENTATION_LANDSCAPE;
        }

        if(orientation.equalsIgnoreCase(LAYOUT_ORIENTATION_LANDSCAPE)) {
            //requested orientation was landscape
            Log.i(TAG, "Setting layout orientation to landscape");

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            cameraManager.setCurrentOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else if(orientation.equalsIgnoreCase(LAYOUT_ORIENTATION_PORTRAIT)) {
            //requested orientation was portrait
            Log.i(TAG, "Setting layout orientation to portrait");

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            cameraManager.setCurrentOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            //if we didn't recognize it then default to landscape
            Log.i(TAG, "Setting layout orientation to default");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            cameraManager.setCurrentOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    private void parseIntentType(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            String dataString = intent.getDataString();

            if (Intents.Scan.ACTION.equals(action)) {

                // Scan the formats the intent requested, and return the result to the calling activity.
                source = IntentSource.NATIVE_APP_INTENT;
                previewHandler.setDecodeFormats(DecodeFormatManager.parseDecodeFormats(intent));
                previewHandler.setDecodeHints(DecodeHintManager.parseDecodeHints(intent));

                if (intent.hasExtra(Intents.Scan.WIDTH) && intent.hasExtra(Intents.Scan.HEIGHT)) {
                    int width = intent.getIntExtra(Intents.Scan.WIDTH, 0);
                    int height = intent.getIntExtra(Intents.Scan.HEIGHT, 0);
                    if (width > 0 && height > 0) {
                        cameraManager.setManualFramingRect(width, height);
                    }
                }

                if (intent.hasExtra(Intents.Scan.CAMERA_ID)) {
                    int cameraId = intent.getIntExtra(Intents.Scan.CAMERA_ID, -1);
                    if (cameraId >= 0) {
                        cameraManager.setManualCameraId(cameraId);
                    }
                }

                String customPromptMessage = intent.getStringExtra(Intents.Scan.PROMPT_MESSAGE);
                if (customPromptMessage != null) {
                    statusView.setText(customPromptMessage);
                }

            } else if (dataString != null &&
                    dataString.contains("http://www.google") &&
                    dataString.contains("/m/products/scan")) {

                // Scan only products and send the result to mobile Product Search.
                source = IntentSource.PRODUCT_SEARCH_LINK;
                sourceUrl = dataString;
                previewHandler.setDecodeFormats(DecodeFormatManager.PRODUCT_FORMATS);

            } else if (isZXingURL(dataString)) {
                // Scan formats requested in query string (all formats if none specified).
                // If a return URL is specified, send the results there. Otherwise, handle it ourselves.
                source = IntentSource.ZXING_LINK;
                sourceUrl = dataString;
                Uri inputUri = Uri.parse(dataString);

                previewHandler.setDecodeFormats(DecodeFormatManager.parseDecodeFormats(inputUri));
                previewHandler.setDecodeHints(DecodeHintManager.parseDecodeHints(inputUri));
            }

            previewHandler.setCharacterSet(intent.getStringExtra(Intents.Scan.CHARACTER_SET));
        }
    }
}
