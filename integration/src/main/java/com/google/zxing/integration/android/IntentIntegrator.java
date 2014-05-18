/*
 * Copyright 2009 ZXing authors
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

package com.google.zxing.integration.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>A utility class which helps ease integration with Barcode Scanner via {@link Intent}s. This is a simple
 * way to invoke barcode scanning and receive the result, without any need to integrate, modify, or learn the
 * project's source code.</p>
 *
 * <h2>Initiating a barcode scan</h2>
 *
 * <p>To integrate, create an instance of {@code IntentIntegrator} and call {@link #initiateScan()} and wait
 * for the result in your app.</p>
 *
 * <p>It does require that the Barcode Scanner (or work-alike) application is installed. The
 * {@link #initiateScan()} method will prompt the user to download the application, if needed.</p>
 *
 * <p>There are a few steps to using this integration. First, your {@link Activity} must implement
 * the method {@link Activity#onActivityResult(int, int, Intent)} and include a line of code like this:</p>
 *
 * <pre>{@code
 * public void onActivityResult(int requestCode, int resultCode, Intent intent) {
 *   IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
 *   if (scanResult != null) {
 *     // handle scan result
 *   }
 *   // else continue with any other code you need in the method
 *   ...
 * }
 * }</pre>
 *
 * <p>This is where you will handle a scan result.</p>
 *
 * <p>Second, just call this in response to a user action somewhere to begin the scan process:</p>
 *
 * <pre>{@code
 * IntentIntegrator integrator = new IntentIntegrator(yourActivity);
 * integrator.initiateScan();
 * }</pre>
 *
 * <p>Finally, you can use {@link #addExtra(String, Object)} to add more parameters to the Intent used
 * to invoke the scanner. This can be used to set additional options not directly exposed by this
 * simplified API.</p>
 *
 * <h2>Sharing text via barcode</h2>
 *
 * <p>To share text, encoded as a QR Code on-screen, similarly, see {@link #shareText(CharSequence)}.</p>
 *
 * <p>Some code, particularly download integration, was contributed from the Anobiit application.</p>
 *
 * <h2>Enabling experimental barcode formats</h2>
 *
 * <p>Some formats are not enabled by default even when scanning with {@link #ALL_CODE_TYPES}, such as
 * PDF417. Use {@link #initiateScan(java.util.Collection)} with
 * a collection containing the names of formats to scan for explicitly, like "PDF_417", to use such
 * formats.</p>
 *
 * @author Sean Owen
 * @author Fred Lin
 * @author Isaac Potoczny-Jones
 * @author Brad Drehmer
 * @author gcstang
 */
public class IntentIntegrator {

    public static final int REQUEST_CODE = 0x0000c0de; // Only use bottom 16 bits
    private static final String TAG = IntentIntegrator.class.getSimpleName();

    // supported barcode formats
    public static final Collection<String> PRODUCT_CODE_TYPES = list("UPC_A", "UPC_E", "EAN_8", "EAN_13", "RSS_14");
    public static final Collection<String> ONE_D_CODE_TYPES =
            list("UPC_A", "UPC_E", "EAN_8", "EAN_13", "CODE_39", "CODE_93", "CODE_128",
                    "ITF", "RSS_14", "RSS_EXPANDED");
    public static final Collection<String> QR_CODE_TYPES = Collections.singleton("QR_CODE");
    public static final Collection<String> DATA_MATRIX_TYPES = Collections.singleton("DATA_MATRIX");

    public static final Collection<String> ALL_CODE_TYPES = null;

    private final Activity activity;
    private android.app.Fragment fragment;
    private android.support.v4.app.Fragment supportFragment;

    private final Map<String,Object> moreExtras = new HashMap<String,Object>(3);

    private Collection<String> desiredBarcodeFormats;

    private static final boolean HAVE_STANDARD_SCANNER;
    private static final boolean HAVE_LEGACY_SCANNER;

    private static final String STANDARD_PACKAGE_NAME = "com.google.zxing.client.android";
    private static final String LEGACY_PACKAGE_NAME = "com.google.zxing.client.androidlegacy";

    protected Class<?> getCaptureActivity() {
        try {
            return Class.forName(getScannerPackage() + ".CaptureActivity");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not find CaptureActivity. Make sure one of the zxing-android libraries are loaded.", e);
        }
    }

    protected Class<?> getEncodeActivity() {
        try {
            return Class.forName(getScannerPackage() + ".encode.EncodeActivity");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not find EncodeActivity. Make sure one of the zxing-android libraries are loaded.", e);
        }
    }

    private static String getScannerPackage() {
        if(HAVE_STANDARD_SCANNER && Build.VERSION.SDK_INT >= 15) {
            return STANDARD_PACKAGE_NAME;
        } else if(HAVE_LEGACY_SCANNER) {
            return LEGACY_PACKAGE_NAME;
        } else {
            return STANDARD_PACKAGE_NAME;
        }
    }

    static {
        boolean test1 = false;
        try {
            Class.forName(STANDARD_PACKAGE_NAME + ".CaptureActivity");
            test1 = true;
        } catch (ClassNotFoundException e) {
            // Ignore
        }
        HAVE_STANDARD_SCANNER = test1;

        boolean test2 = false;
        try {
            Class.forName(LEGACY_PACKAGE_NAME + ".CaptureActivity");
            test2 = true;
        } catch (ClassNotFoundException e) {
            // Ignore
        }
        HAVE_LEGACY_SCANNER = test2;
    }

    /**
     * @param activity {@link Activity} invoking the integration
     */
    public IntentIntegrator(Activity activity) {
        this.activity = activity;
    }

    /**
     * @param fragment {@link Fragment} invoking the integration.
     *  {@link #startActivityForResult(Intent, int)} will be called on the {@link Fragment} instead
     *  of an {@link Activity}
     */
    public static IntentIntegrator forSupportFragment(android.support.v4.app.Fragment fragment) {
        IntentIntegrator integrator = new IntentIntegrator(fragment.getActivity());
        integrator.supportFragment = fragment;
        return integrator;
    }

    /**
     * @param fragment {@link Fragment} invoking the integration.
     *  {@link #startActivityForResult(Intent, int)} will be called on the {@link Fragment} instead
     *  of an {@link Activity}
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static IntentIntegrator forFragment(Fragment fragment) {
        IntentIntegrator integrator = new IntentIntegrator(fragment.getActivity());
        integrator.fragment = fragment;
        return integrator;
    }

    public Map<String,?> getMoreExtras() {
        return moreExtras;
    }

    public final IntentIntegrator addExtra(String key, Object value) {
        moreExtras.put(key, value);
        return this;
    }

    /**
     * Change the layout used for scanning in zxing-android.
     *
     * @param resourceId the layout resource id to use.
     */
    public final IntentIntegrator setCaptureLayout(int resourceId) {
        addExtra("ZXING_CAPTURE_LAYOUT_ID_KEY", resourceId);
        return this;
    }

    /**
     * Change the layout used for scanning in zxing-android-legacy.
     *
     * @param resourceId the layout resource id to use.
     */
    public final IntentIntegrator setLegacyCaptureLayout(int resourceId) {
        addExtra("ZXINGLEGACY_CAPTURE_LAYOUT_ID_KEY", resourceId);
        return this;
    }

    /**
     * Set a prompt to display on the capture screen, instead of using the default.
     *
     * @param prompt the prompt to display
     */
    public final IntentIntegrator setPrompt(String prompt) {
        if (prompt != null) {
            addExtra("PROMPT_MESSAGE", prompt);
        }
        return this;
    }

    /**
     * Set the duration that the result should be displayed after scanning.
     *
     * @param ms time to display the result in ms
     */
    public final IntentIntegrator setResultDisplayDuration(long ms) {
        addExtra("RESULT_DISPLAY_DURATION_MS", ms);
        return this;
    }

    /**
     * Set the size of the scanning rectangle.
     *
     * @param desiredWidth the desired width in pixels
     * @param desiredHeight the desired height in pixels
     */
    public final IntentIntegrator setScanningRectangle(int desiredWidth, int desiredHeight) {
        addExtra("SCAN_WIDTH", desiredWidth);
        addExtra("SCAN_HEIGHT", desiredHeight);
        return this;
    }

    /**
     * Use a wide scanning rectangle.
     *
     * May work better for 1D barcodes.
     */
    public void setWide() {
        WindowManager window = activity.getWindowManager();
        Display display = window.getDefaultDisplay();
        int displayWidth = display.getWidth();
        int displayHeight = display.getHeight();
        if (displayHeight > displayWidth) {
            // This is portrait dimensions, but the barcode scanner is always in landscape mode.
            int temp = displayWidth;
            //noinspection SuspiciousNameCombination
            displayWidth = displayHeight;
            displayHeight = temp;
        }

        int desiredWidth = displayWidth * 9 / 10;
        int desiredHeight = Math.min(displayHeight * 3 / 4, 400);    // Limit to 400px
        setScanningRectangle(desiredWidth, desiredHeight);
    }

    /**
     * Heuristics for whether or not the barcode scanning rectangle should be wide or not.
     *
     * Current heuristics make it wide if 1D barcode formats are scanned, and no QR codes.
     *
     * @param desiredBarcodeFormats the formats that will be scanned
     * @return true if it should be wide
     */
    public static boolean shouldBeWide(Collection<String> desiredBarcodeFormats) {
        boolean scan1d = false;
        boolean scan2d = false;
        for (String format : desiredBarcodeFormats) {
            if(ONE_D_CODE_TYPES.contains(format)) {
                scan1d = true;
            }
            if(QR_CODE_TYPES.contains(format) || DATA_MATRIX_TYPES.contains(format)) {
                scan2d = true;
            }
        }

        return scan1d && !scan2d;
    }

    /**
     * Make the scanning rectangle wide if only 1D barcodes are scanned.
     *
     * This must be called *after* setting the desired barcode formats.
     *
     * @return this
     */
    public IntentIntegrator autoWide() {
        if(desiredBarcodeFormats != null && shouldBeWide(desiredBarcodeFormats)) {
            setWide();
        }
        return this;
    }

    /**
     * Use the specified camera ID to scan barcodes.
     *
     * @param cameraId camera ID of the camera to use. A negative value means "no preference".
     * @return this
     */
    public IntentIntegrator setCameraId(int cameraId) {
        if (cameraId >= 0) {
            addExtra("SCAN_CAMERA_ID", cameraId);
        }
        return this;
    }

    /**
     * Set the desired barcode formats to scan.
     *
     * @param desiredBarcodeFormats names of {@code BarcodeFormat}s to scan for
     * @return this
     */
    public IntentIntegrator setDesiredBarcodeFormats(Collection<String> desiredBarcodeFormats) {
        this.desiredBarcodeFormats = desiredBarcodeFormats;
        return this;
    }

    /**
     * Initiates a scan for all known barcode types with the default camera.
     */
    public final void initiateScan() {
        startActivityForResult(createScanIntent(), REQUEST_CODE);
    }

    /**
     * Create an scan intent with the specified options.
     *
     * @return the intent
     */
    public Intent createScanIntent() {
        Intent intentScan = new Intent(activity, getCaptureActivity());
        intentScan.setAction("com.google.zxing.client.android.SCAN");

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
            intentScan.putExtra("SCAN_FORMATS", joinedByComma.toString());
        }

        intentScan.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intentScan.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        attachMoreExtras(intentScan);
        return intentScan;
    }

    /**
     * Initiates a scan, only for a certain set of barcode types, given as strings corresponding
     * to their names in ZXing's {@code BarcodeFormat} class like "UPC_A". You can supply constants
     * like {@link #PRODUCT_CODE_TYPES} for example.
     *
     * @param desiredBarcodeFormats names of {@code BarcodeFormat}s to scan for
     */
    public final void initiateScan(Collection<String> desiredBarcodeFormats) {
        setDesiredBarcodeFormats(desiredBarcodeFormats);
        initiateScan();
    }

    /**
     * Start an activity. This method is defined to allow different methods of activity starting for
     * newer versions of Android and for compatibility library.
     *
     * @param intent Intent to start.
     * @param code Request code for the activity
     * @see android.app.Activity#startActivityForResult(Intent, int)
     * @see android.app.Fragment#startActivityForResult(Intent, int)
     */
    protected void startActivityForResult(Intent intent, int code) {
        if (fragment != null) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                fragment.startActivityForResult(intent, code);
            }
        } else if(supportFragment != null) {
            supportFragment.startActivityForResult(intent, code);
        } else {
            activity.startActivityForResult(intent, code);
        }
    }


    protected void startActivity(Intent intent) {
        if (fragment != null) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                fragment.startActivity(intent);
            }
        } else if(supportFragment != null) {
            supportFragment.startActivity(intent);
        } else {
            activity.startActivity(intent);
        }
    }

    /**
     * <p>Call this from your {@link Activity}'s
     * {@link Activity#onActivityResult(int, int, Intent)} method.</p>
     *
     * @param requestCode request code from {@code onActivityResult()}
     * @param resultCode result code from {@code onActivityResult()}
     * @param intent {@link Intent} from {@code onActivityResult()}
     * @return null if the event handled here was not related to this class, or
     *  else an {@link IntentResult} containing the result of the scan. If the user cancelled scanning,
     *  the fields will be null.
     */
    public static IntentResult parseActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                String contents = intent.getStringExtra("SCAN_RESULT");
                String formatName = intent.getStringExtra("SCAN_RESULT_FORMAT");
                byte[] rawBytes = intent.getByteArrayExtra("SCAN_RESULT_BYTES");
                int intentOrientation = intent.getIntExtra("SCAN_RESULT_ORIENTATION", Integer.MIN_VALUE);
                Integer orientation = intentOrientation == Integer.MIN_VALUE ? null : intentOrientation;
                String errorCorrectionLevel = intent.getStringExtra("SCAN_RESULT_ERROR_CORRECTION_LEVEL");
                return new IntentResult(contents,
                        formatName,
                        rawBytes,
                        orientation,
                        errorCorrectionLevel);
            }
            return new IntentResult();
        }
        return null;
    }


    /**
     * Defaults to type "TEXT_TYPE".
     *
     * @param text the text string to encode as a barcode
     * @see #shareText(CharSequence, CharSequence)
     */
    public final void shareText(CharSequence text) {
        shareText(text, "TEXT_TYPE");
    }

    /**
     * Shares the given text by encoding it as a barcode, such that another user can
     * scan the text off the screen of the device.
     *
     * @param text the text string to encode as a barcode
     * @param type type of data to encode. See {@code com.google.zxing.client.android.Contents.Type} constants.
     */
    public final void shareText(CharSequence text, CharSequence type) {
        Intent intent = new Intent(activity, getEncodeActivity());
        intent.setAction("com.google.zxing.client.android.ENCODE");

        intent.putExtra("ENCODE_TYPE", type);
        intent.putExtra("ENCODE_DATA", text);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        attachMoreExtras(intent);
        startActivity(intent);
    }

    private static List<String> list(String... values) {
        return Collections.unmodifiableList(Arrays.asList(values));
    }

    private void attachMoreExtras(Intent intent) {
        for (Map.Entry<String,Object> entry : moreExtras.entrySet()) {
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
            } else {
                intent.putExtra(key, value.toString());
            }
        }
    }

}