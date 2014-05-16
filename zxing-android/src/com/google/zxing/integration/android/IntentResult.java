package com.google.zxing.integration.android;

/**
 * <p>Encapsulates the result of a barcode scan invoked through {@link IntentIntegrator}.</p>
 *
 * @author Sean Owen
 */
public final class IntentResult {

    private final String contents;
    private final String formatName;

    IntentResult(String contents, String formatName) {
        this.contents = contents;
        this.formatName = formatName;
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

}