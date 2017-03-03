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

/**
 * This class provides the constants to use when sending an Intent to Barcode Scanner.
 * These strings are effectively API and cannot be changed.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class Intents {
    private Intents() {
    }

    public static final class Scan {
        /**
         * Send this intent to open the Barcodes app in scanning mode, find a barcode, and return
         * the results.
         */
        public static final String ACTION = "com.google.zxing.client.android.SCAN";

        /**
         * By default, sending this will decode all barcodes that we understand. However it
         * may be useful to limit scanning to certain formats. Use
         * {@link android.content.Intent#putExtra(String, String)} with one of the values below.
         * <p>
         * Setting this is effectively shorthand for setting explicit formats with {@link #FORMATS}.
         * It is overridden by that setting.
         */
        public static final String MODE = "SCAN_MODE";

        /**
         * Decode only UPC and EAN barcodes. This is the right choice for shopping apps which get
         * prices, reviews, etc. for products.
         */
        public static final String PRODUCT_MODE = "PRODUCT_MODE";

        /**
         * Decode only 1D barcodes.
         */
        public static final String ONE_D_MODE = "ONE_D_MODE";

        /**
         * Decode only QR codes.
         */
        public static final String QR_CODE_MODE = "QR_CODE_MODE";

        /**
         * Decode only Data Matrix codes.
         */
        public static final String DATA_MATRIX_MODE = "DATA_MATRIX_MODE";

        /**
         * Decode only Aztec.
         */
        public static final String AZTEC_MODE = "AZTEC_MODE";

        /**
         * Decode only PDF417.
         */
        public static final String PDF417_MODE = "PDF417_MODE";

        /**
         * Comma-separated list of formats to scan for. The values must match the names of
         * {@link com.google.zxing.BarcodeFormat}s, e.g. {@link com.google.zxing.BarcodeFormat#EAN_13}.
         * Example: "EAN_13,EAN_8,QR_CODE". This overrides {@link #MODE}.
         */
        public static final String FORMATS = "SCAN_FORMATS";

        /**
         * Optional parameter to specify the id of the camera from which to recognize barcodes.
         * Overrides the default camera that would otherwise would have been selected.
         * If provided, should be an int.
         */
        public static final String CAMERA_ID = "SCAN_CAMERA_ID";

        /**
         * @see com.google.zxing.DecodeHintType#CHARACTER_SET
         */
        public static final String CHARACTER_SET = "CHARACTER_SET";

        /**
         * Set to false to disable beep. Defaults to true.
         */
        public static final String BEEP_ENABLED = "BEEP_ENABLED";

        /**
         * Set beep resource.
         */
        public static final String BEEP_RESOURCE = "BEEP_RESOURCE";

        /**
         * Set to true to enable vibrate. Defaults to false.
         */
        public static final String VIBRATE_ENABLED = "VIBRATE_ENABLED";

        /**
         * Set to true to return a path to the barcode's image as it was captured. Defaults to false.
         */
        public static final String BARCODE_IMAGE_ENABLED = "BARCODE_IMAGE_ENABLED";

        /**
         * Set the time to finish the scan screen.
         */
        public static final String TIMEOUT = "TIMEOUT";

        /**
         * Whether or not the orientation should be locked when the activity is first started.
         * Defaults to true.
         */
        public static final String ORIENTATION_LOCKED = "SCAN_ORIENTATION_LOCKED";

        /**
         * Prompt to show on-screen when scanning by intent. Specified as a {@link String}.
         */
        public static final String PROMPT_MESSAGE = "PROMPT_MESSAGE";

        /**
         * If a barcode is found, Barcodes returns {@link android.app.Activity#RESULT_OK} to
         * {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
         * of the app which requested the scan via
         * {@link android.app.Activity#startActivityForResult(android.content.Intent, int)}
         * The barcodes contents can be retrieved with
         * {@link android.content.Intent#getStringExtra(String)}.
         * If the user presses Back, the result code will be {@link android.app.Activity#RESULT_CANCELED}.
         */
        public static final String RESULT = "SCAN_RESULT";

        /**
         * Call {@link android.content.Intent#getStringExtra(String)} with {@link #RESULT_FORMAT}
         * to determine which barcode format was found.
         * See {@link com.google.zxing.BarcodeFormat} for possible values.
         */
        public static final String RESULT_FORMAT = "SCAN_RESULT_FORMAT";

        /**
         * Call {@link android.content.Intent#getStringExtra(String)} with {@link #RESULT_UPC_EAN_EXTENSION}
         * to return the content of any UPC extension barcode that was also found. Only applicable
         * to {@link com.google.zxing.BarcodeFormat#UPC_A} and {@link com.google.zxing.BarcodeFormat#EAN_13}
         * formats.
         */
        public static final String RESULT_UPC_EAN_EXTENSION = "SCAN_RESULT_UPC_EAN_EXTENSION";

        /**
         * Call {@link android.content.Intent#getByteArrayExtra(String)} with {@link #RESULT_BYTES}
         * to get a {@code byte[]} of raw bytes in the barcode, if available.
         */
        public static final String RESULT_BYTES = "SCAN_RESULT_BYTES";

        /**
         * Key for the value of {@link com.google.zxing.ResultMetadataType#ORIENTATION}, if available.
         * Call {@link android.content.Intent#getIntArrayExtra(String)} with {@link #RESULT_ORIENTATION}.
         */
        public static final String RESULT_ORIENTATION = "SCAN_RESULT_ORIENTATION";

        /**
         * Key for the value of {@link com.google.zxing.ResultMetadataType#ERROR_CORRECTION_LEVEL}, if available.
         * Call {@link android.content.Intent#getStringExtra(String)} with {@link #RESULT_ERROR_CORRECTION_LEVEL}.
         */
        public static final String RESULT_ERROR_CORRECTION_LEVEL = "SCAN_RESULT_ERROR_CORRECTION_LEVEL";

        /**
         * Prefix for keys that map to the values of {@link com.google.zxing.ResultMetadataType#BYTE_SEGMENTS},
         * if available. The actual values will be set under a series of keys formed by adding 0, 1, 2, ...
         * to this prefix. So the first byte segment is under key "SCAN_RESULT_BYTE_SEGMENTS_0" for example.
         * Call {@link android.content.Intent#getByteArrayExtra(String)} with these keys.
         */
        public static final String RESULT_BYTE_SEGMENTS_PREFIX = "SCAN_RESULT_BYTE_SEGMENTS_";

        /**
         * Call {@link android.content.Intent#getStringExtra(String)} with {@link #RESULT_BARCODE_IMAGE_PATH}
         * to get a {@code String} path to a cropped and compressed png file of the barcode's image
         * as it was displayed. Only available if
         * {@link com.google.zxing.integration.android.IntentIntegrator#setBarcodeImageEnabled(boolean)}
         * is called with true.
         */
        public static final String RESULT_BARCODE_IMAGE_PATH = "SCAN_RESULT_IMAGE_PATH";

        /***
         * The scan should be inverted. White becomes black, black becomes white.
         */
        public static final String INVERTED_SCAN = "INVERTED_SCAN";

        private Scan() {
        }
    }
}
