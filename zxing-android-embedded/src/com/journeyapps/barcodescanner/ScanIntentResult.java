/*
 * Based on IntentResult.
 *
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

package com.journeyapps.barcodescanner;

import android.app.Activity;
import android.content.Intent;

import com.google.zxing.client.android.Intents;
import com.google.zxing.integration.android.IntentResult;

/**
 * <p>Encapsulates the result of a barcode scan invoked through {@link ScanContract}.</p>
 *
 * @author Sean Owen
 */
public final class ScanIntentResult {

    private final String contents;
    private final String formatName;
    private final byte[] rawBytes;
    private final Integer orientation;
    private final String errorCorrectionLevel;
    private final String barcodeImagePath;
    private final Intent originalIntent;

    ScanIntentResult() {
        this(null, null, null, null, null, null, null);
    }

    ScanIntentResult(Intent intent) {
        this(null, null, null, null, null, null, intent);
    }

    ScanIntentResult(String contents,
                     String formatName,
                     byte[] rawBytes,
                     Integer orientation,
                     String errorCorrectionLevel,
                     String barcodeImagePath,
                     Intent originalIntent) {
        this.contents = contents;
        this.formatName = formatName;
        this.rawBytes = rawBytes;
        this.orientation = orientation;
        this.errorCorrectionLevel = errorCorrectionLevel;
        this.barcodeImagePath = barcodeImagePath;
        this.originalIntent = originalIntent;
    }

    /**
     * @return raw content of barcode
     */
    public String getContents() {
        return contents;
    }

    /**
     * @return name of format, like "QR_CODE", "UPC_A". See {@code BarcodeFormat} for more format names.
     */
    public String getFormatName() {
        return formatName;
    }

    /**
     * @return raw bytes of the barcode content, if applicable, or null otherwise
     */
    public byte[] getRawBytes() {
        return rawBytes;
    }

    /**
     * @return rotation of the image, in degrees, which resulted in a successful scan. May be null.
     */
    public Integer getOrientation() {
        return orientation;
    }

    /**
     * @return name of the error correction level used in the barcode, if applicable
     */
    public String getErrorCorrectionLevel() {
        return errorCorrectionLevel;
    }

    /**
     * @return path to a temporary file containing the barcode image, if applicable, or null otherwise
     */
    public String getBarcodeImagePath() {
        return barcodeImagePath;
    }

    /**
     * @return the original intent
     */
    public Intent getOriginalIntent() {
        return originalIntent;
    }

    @Override
    public String toString() {
        int rawBytesLength = rawBytes == null ? 0 : rawBytes.length;
        return "Format: " + formatName + '\n' +
            "Contents: " + contents + '\n' +
            "Raw bytes: (" + rawBytesLength + " bytes)\n" +
            "Orientation: " + orientation + '\n' +
            "EC level: " + errorCorrectionLevel + '\n' +
            "Barcode image: " + barcodeImagePath + '\n' +
            "Original intent: " + originalIntent + '\n';
    }


    /**
     * Parse activity result, without checking the request code.
     *
     * @param resultCode result code from {@code onActivityResult()}
     * @param intent     {@link Intent} from {@code onActivityResult()}
     * @return an {@link IntentResult} containing the result of the scan. If the user cancelled scanning,
     * the fields will be null.
     */
    public static ScanIntentResult parseActivityResult(int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_OK) {
            String contents = intent.getStringExtra(Intents.Scan.RESULT);
            String formatName = intent.getStringExtra(Intents.Scan.RESULT_FORMAT);
            byte[] rawBytes = intent.getByteArrayExtra(Intents.Scan.RESULT_BYTES);
            int intentOrientation = intent.getIntExtra(Intents.Scan.RESULT_ORIENTATION, Integer.MIN_VALUE);
            Integer orientation = intentOrientation == Integer.MIN_VALUE ? null : intentOrientation;
            String errorCorrectionLevel = intent.getStringExtra(Intents.Scan.RESULT_ERROR_CORRECTION_LEVEL);
            String barcodeImagePath = intent.getStringExtra(Intents.Scan.RESULT_BARCODE_IMAGE_PATH);
            return new ScanIntentResult(contents,
                    formatName,
                    rawBytes,
                    orientation,
                    errorCorrectionLevel,
                    barcodeImagePath,
                    intent);
        }
        return new ScanIntentResult(intent);
    }
}
