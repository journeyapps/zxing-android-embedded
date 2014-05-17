package com.google.zxing.integration.android;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.Intents;

/**
 * TODO: update these docs - a lot of it is not relevant anymore.
 *
 * <p>A utility class which helps ease integration with Barcode Scanner via {@link android.content.Intent}s. This is a simple
 * way to invoke barcode scanning and receive the result, without any need to integrate, modify, or learn the
 * project's source code.</p>
 * <p/>
 * <h2>Initiating a barcode scan</h2>
 * <p/>
 * <p>Integration is essentially as easy as calling {@link #initiateScan(android.app.Activity)} and waiting
 * for the result in your app.</p>
 * <p/>
 * <p>It does require that the Barcode Scanner application is installed. The
 * {@link #initiateScan(android.app.Activity)} method will prompt the user to download the application, if needed.</p>
 * <p/>
 * <p>There are a few steps to using this integration. First, your {@link android.app.Activity} must implement
 * the method {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)} and include a line of code like this:</p>
 * <p/>
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
 * <p/>
 * <p>This is where you will handle a scan result.
 * Second, just call this in response to a user action somewhere to begin the scan process:</p>
 * <p/>
 * <pre>{@code IntentIntegrator.initiateScan(yourActivity);}</pre>
 * <p/>
 * <h2>Sharing text via barcode</h2>
 * <p/>
 * <p>To share text, encoded as a QR Code on-screen, similarly, see {@link #shareText(android.app.Activity, CharSequence)}.</p>
 * <p/>
 * <p>Some code, particularly download integration, was contributed from the Anobiit application.</p>
 *
 * @author Sean Owen
 * @author Fred Lin
 * @author Isaac Potoczny-Jones
 * @author Brad Drehmer
 * @author gcstang
 */
public final class IntentIntegrator {

    public static final int REQUEST_CODE = 0x0000c0de; // Only use bottom 16 bits
    private static final String TAG = IntentIntegrator.class.getSimpleName();

    private static final String PACKAGE = "com.google.zxing.client.android";

    // supported barcode formats
    public static final String PRODUCT_CODE_TYPES = "UPC_A,UPC_E,EAN_8,EAN_13";
    public static final String ONE_D_CODE_TYPES = PRODUCT_CODE_TYPES + ",CODE_39,CODE_93,CODE_128";
    public static final String QR_CODE_TYPES = "QR_CODE";
    public static final String ALL_CODE_TYPES = null;

    private IntentIntegrator() {
    }

    public static void initiateScan(Activity activity) {
        initiateScan(activity, null);
    }


    /**
     * Change the layout used by the intent.
     *
     * @param intent the scanning intent
     * @param resourceId the layout resource id to use.
     */
    public static void setCaptureLayout(Intent intent, int resourceId) {
        intent.putExtra(CaptureActivity.ZXING_CAPTURE_LAYOUT_ID_KEY, resourceId);
    }

    /**
     * Set a prompt to display on the capture screen, instead of using the default.
     *
     * @param intent the scanning intent
     * @param prompt the prompt to display
     */
    public static void setPrompt(Intent intent, String prompt) {
        if (prompt != null) {
            intent.putExtra("PROMPT_MESSAGE", prompt);
        }
    }

    /**
     * Set the duration that the result should be displayed after scanning.
     *
     * @param intent the scanning intent
     * @param ms time to display the result in ms
     */
    public static void setResultDisplayDuration(Intent intent, long ms) {
        intent.putExtra("RESULT_DISPLAY_DURATION_MS", 0L);
    }

    public static void initiateScan(Activity activity, CharSequence stringDesiredBarcodeFormats) {
        initiateScan(activity, stringDesiredBarcodeFormats, null);
    }

    public static void startIntent(Intent intent, Activity activity) {
        activity.startActivityForResult(intent, REQUEST_CODE);
    }

    public static void startIntent(Intent intent, android.support.v4.app.Fragment fragment) {
        fragment.startActivityForResult(intent, REQUEST_CODE);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void startIntent(Intent intent, android.app.Fragment fragment) {
        fragment.startActivityForResult(intent, REQUEST_CODE);
    }

    /**
     * Invokes scanning.
     *
     * @param stringDesiredBarcodeFormats a comma separated list of codes you would
     *                                    like to scan for.
     */
    public static void initiateScan(Activity activity, CharSequence stringDesiredBarcodeFormats, String prompt) {
        Intent intent = createScanIntent(activity, stringDesiredBarcodeFormats, prompt);

        startIntent(intent, activity);
    }

    public static Intent createScanIntent(Context context, CharSequence formats, String prompt) {
        Intent intentScan = createScanIntent(context);

        // check which types of codes to scan for
        if (formats != null) {
            // set the desired barcode types
            intentScan.putExtra("SCAN_FORMATS", formats);

            // Hack to use a wider viewfinder for 1D barcodes. This should probably be in the main code instead.
            if(shouldBeWide(formats)) {
                setWide(context, intentScan);
            }
        }

        setPrompt(intentScan, prompt);
        setResultDisplayDuration(intentScan, 0L);

        return intentScan;
    }

    /**
     * Heuristics for whether or not the barcode scanning rectangle should be wide or not.
     *
     * Current heuristics make it wide if 1D barcode formats are scanned, and no QR codes.
     *
     * @param formats the barcode formats to scan, comma-separated
     * @return true if it should be wide
     */
    public static boolean shouldBeWide(CharSequence formats) {
        String formatsString = formats.toString();
        boolean scan1d = formatsString.contains("CODE") || formatsString.contains("EAN") || formatsString.contains("UPC");
        boolean scanQR = formatsString.contains(QR_CODE_TYPES);
        return scan1d && !scanQR;
    }

    /**
     * Set a scan intent to use a wide rectangle, suitable for 1D barcode formats.
     *
     * @param context the context, used to measure display size
     * @param intent the scanning intent
     */
    public static void setWide(Context context, Intent intent) {
        WindowManager window = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
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

        intent.putExtra("SCAN_WIDTH", desiredWidth);
        intent.putExtra("SCAN_HEIGHT", desiredHeight);
    }


    /**
     * <p>Call this from your {@link android.app.Activity}'s
     * {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)} method.</p>
     *
     * @return null if the event handled here was not related to this class, or
     *         else an {@link IntentResult} containing the result of the scan. If the user cancelled scanning,
     *         the fields will be null.
     */
    public static IntentResult parseActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                String contents = intent.getStringExtra("SCAN_RESULT");
                String formatName = intent.getStringExtra("SCAN_RESULT_FORMAT");
                return new IntentResult(contents, formatName);
            } else {
                return new IntentResult(null, null);
            }
        }
        return null;
    }

    /**
     * Shares the given text by encoding it as a barcode, such that another user can
     * scan the text off the screen of the device.
     *
     * @param text            the text string to encode as a barcode
     */
    public static void shareText(Activity activity, CharSequence text) {
        // TODO: fix this
        Intent intent = new Intent();
        intent.setAction(PACKAGE + ".ENCODE");

        intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
        intent.putExtra("ENCODE_DATA", text);

        activity.startActivity(intent);
    }

    /**
     * Creates the simplest possible scan intent, with no options set.
     *
     * @param context the activity
     * @return the scan intent
     */
    public static Intent createScanIntent(Context context) {
        Intent intent = new Intent(context, com.google.zxing.client.android.CaptureActivity.class);
        intent.setAction(Intents.Scan.ACTION);
        return intent;
    }
}