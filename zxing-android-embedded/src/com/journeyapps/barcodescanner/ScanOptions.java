/*
 * Based on IntentIntegrator, Copyright 2009 ZXing authors.
 *
 */
package com.journeyapps.barcodescanner;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.zxing.client.android.Intents;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScanOptions {
    // supported barcode formats

    // Product Codes
    public static final String UPC_A = "UPC_A";
    public static final String UPC_E = "UPC_E";
    public static final String EAN_8 = "EAN_8";
    public static final String EAN_13 = "EAN_13";
    public static final String RSS_14 = "RSS_14";

    // Other 1D
    public static final String CODE_39 = "CODE_39";
    public static final String CODE_93 = "CODE_93";
    public static final String CODE_128 = "CODE_128";
    public static final String ITF = "ITF";

    public static final String RSS_EXPANDED = "RSS_EXPANDED";

    // 2D
    public static final String QR_CODE = "QR_CODE";
    public static final String DATA_MATRIX = "DATA_MATRIX";
    public static final String PDF_417 = "PDF_417";


    public static final Collection<String> PRODUCT_CODE_TYPES = list(UPC_A, UPC_E, EAN_8, EAN_13, RSS_14);
    public static final Collection<String> ONE_D_CODE_TYPES =
            list(UPC_A, UPC_E, EAN_8, EAN_13, RSS_14, CODE_39, CODE_93, CODE_128,
                    ITF, RSS_14, RSS_EXPANDED);

    public static final Collection<String> ALL_CODE_TYPES = null;

    private final Map<String, Object> moreExtras = new HashMap<>(3);

    private Collection<String> desiredBarcodeFormats;

    private Class<?> captureActivity;


    protected Class<?> getDefaultCaptureActivity() {
        return CaptureActivity.class;
    }

    public ScanOptions() {

    }


    public Class<?> getCaptureActivity() {
        if (captureActivity == null) {
            captureActivity = getDefaultCaptureActivity();
        }
        return captureActivity;
    }

    /**
     * Set the Activity class to use. It can be any activity, but should handle the intent extras
     * as used here.
     *
     * @param captureActivity the class
     */
    public ScanOptions setCaptureActivity(Class<?> captureActivity) {
        this.captureActivity = captureActivity;
        return this;
    }

    public Map<String, ?> getMoreExtras() {
        return moreExtras;
    }

    public final ScanOptions addExtra(String key, Object value) {
        moreExtras.put(key, value);
        return this;
    }

    /**
     * Set a prompt to display on the capture screen, instead of using the default.
     *
     * @param prompt the prompt to display
     */
    public final ScanOptions setPrompt(String prompt) {
        if (prompt != null) {
            addExtra(Intents.Scan.PROMPT_MESSAGE, prompt);
        }
        return this;
    }

    /**
     * By default, the orientation is locked. Set to false to not lock.
     *
     * @param locked true to lock orientation
     */
    public ScanOptions setOrientationLocked(boolean locked) {
        addExtra(Intents.Scan.ORIENTATION_LOCKED, locked);
        return this;
    }

    /**
     * Use the specified camera ID.
     *
     * @param cameraId camera ID of the camera to use. A negative value means "no preference".
     * @return this
     */
    public ScanOptions setCameraId(int cameraId) {
        if (cameraId >= 0) {
            addExtra(Intents.Scan.CAMERA_ID, cameraId);
        }
        return this;
    }

    /**
     * Set to true to enable initial torch
     *
     * @param enabled true to enable initial torch
     * @return this
     */
    public ScanOptions setTorchEnabled(boolean enabled) {
        addExtra(Intents.Scan.TORCH_ENABLED, enabled);
        return this;
    }


    /**
     * Set to false to disable beep on scan.
     *
     * @param enabled false to disable beep
     * @return this
     */
    public ScanOptions setBeepEnabled(boolean enabled) {
        addExtra(Intents.Scan.BEEP_ENABLED, enabled);
        return this;
    }

    /**
     * Set to true to enable saving the barcode image and sending its path in the result Intent.
     *
     * @param enabled true to enable barcode image
     * @return this
     */
    public ScanOptions setBarcodeImageEnabled(boolean enabled) {
        addExtra(Intents.Scan.BARCODE_IMAGE_ENABLED, enabled);
        return this;
    }

    /**
     * Set the desired barcode formats to scan.
     *
     * @param desiredBarcodeFormats names of {@code BarcodeFormat}s to scan for
     * @return this
     */
    public ScanOptions setDesiredBarcodeFormats(Collection<String> desiredBarcodeFormats) {
        this.desiredBarcodeFormats = desiredBarcodeFormats;
        return this;
    }

    /**
     * Set the desired barcode formats to scan.
     *
     * @param desiredBarcodeFormats names of {@code BarcodeFormat}s to scan for
     * @return this
     */
    public ScanOptions setDesiredBarcodeFormats(String... desiredBarcodeFormats) {
        this.desiredBarcodeFormats = Arrays.asList(desiredBarcodeFormats);
        return this;
    }

    /**
     * Initiates a scan for all known barcode types with the default camera.
     * And starts a timer to finish on timeout
     *
     * @return Activity.RESULT_CANCELED and true on parameter TIMEOUT.
     */
    public ScanOptions setTimeout(long timeout) {
        addExtra(Intents.Scan.TIMEOUT, timeout);
        return this;
    }

    /**
     * Create an scan intent with the specified options.
     *
     * @return the intent
     */
    public Intent createScanIntent(Context context) {
        Intent intentScan = new Intent(context, getCaptureActivity());
        intentScan.setAction(Intents.Scan.ACTION);

        // check which types of codes to scan for
        if (desiredBarcodeFormats != null) {
            // set the desired barcode types
            StringBuilder joinedByComma = new StringBuilder();
            for (String format : desiredBarcodeFormats) {
                if (joinedByComma.length() > 0) {
                    joinedByComma.append(',');
                }
                joinedByComma.append(format);
            }
            intentScan.putExtra(Intents.Scan.FORMATS, joinedByComma.toString());
        }

        intentScan.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intentScan.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        attachMoreExtras(intentScan);
        return intentScan;
    }

    private static List<String> list(String... values) {
        return Collections.unmodifiableList(Arrays.asList(values));
    }

    private void attachMoreExtras(Intent intent) {
        for (Map.Entry<String, Object> entry : moreExtras.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            // Kind of hacky
            if (value instanceof Integer) {
                intent.putExtra(key, (Integer) value);
            } else if (value instanceof Long) {
                intent.putExtra(key, (Long) value);
            } else if (value instanceof Boolean) {
                intent.putExtra(key, (Boolean) value);
            } else if (value instanceof Double) {
                intent.putExtra(key, (Double) value);
            } else if (value instanceof Float) {
                intent.putExtra(key, (Float) value);
            } else if (value instanceof Bundle) {
                intent.putExtra(key, (Bundle) value);
            } else if (value instanceof int[]) {
                intent.putExtra(key, (int[]) value);
            } else if (value instanceof long[]) {
                intent.putExtra(key, (long[]) value);
            } else if (value instanceof boolean[]) {
                intent.putExtra(key, (boolean[]) value);
            } else if (value instanceof double[]) {
                intent.putExtra(key, (double[]) value);
            } else if (value instanceof float[]) {
                intent.putExtra(key, (float[]) value);
            } else if (value instanceof String[]) {
                intent.putExtra(key, (String[]) value);
            } else {
                intent.putExtra(key, value.toString());
            }
        }
    }
}
