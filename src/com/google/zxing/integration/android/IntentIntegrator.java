package com.google.zxing.integration.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.Display;
import com.google.zxing.client.android.Intents;

import java.lang.CharSequence;import java.lang.Math;import java.lang.String;import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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


    public static void initiateScan(Activity activity, CharSequence stringDesiredBarcodeFormats) {
        initiateScan(activity, stringDesiredBarcodeFormats, null);
    }


    /**
     * Invokes scanning.
     *
     * @param stringDesiredBarcodeFormats a comma separated list of codes you would
     *                                    like to scan for.
     */
    public static void initiateScan(Activity activity, CharSequence stringDesiredBarcodeFormats, String prompt) {
        Intent intentScan = createScanIntent(activity);

        // check which types of codes to scan for
        if (stringDesiredBarcodeFormats != null) {
            // set the desired barcode types
            intentScan.putExtra("SCAN_FORMATS", stringDesiredBarcodeFormats);

            String formats = stringDesiredBarcodeFormats.toString();
            // Hack to use a wider viewfinder for CODE 39 barcodes. This should probably be in the main code instead.
            if (formats.contains("CODE_39") && !formats.contains(QR_CODE_TYPES)) {
                Display display = activity.getWindowManager().getDefaultDisplay();
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

                intentScan.putExtra("SCAN_WIDTH", desiredWidth);
                intentScan.putExtra("SCAN_HEIGHT", desiredHeight);
            }
        }

        if (prompt != null) {
            intentScan.putExtra("PROMPT_MESSAGE", prompt);
        }
        intentScan.putExtra("RESULT_DISPLAY_DURATION_MS", 0L);


        activity.startActivityForResult(intentScan, REQUEST_CODE);
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

    private static Intent createScanIntent(Activity activity) {
        Intent intent = new Intent(activity, com.google.zxing.client.android.CaptureActivity.class);
        intent.setAction(Intents.Scan.ACTION);
        return intent;
    }
}