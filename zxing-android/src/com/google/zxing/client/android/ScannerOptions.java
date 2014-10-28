package com.google.zxing.client.android;

import android.os.Bundle;

/**
 * Created by hans.reichenbach on 10/23/14.
 */
public class ScannerOptions {
    /*
     * bundle keys
     *
     * Modify these with extreme care! These same variables are NOT used in the IntentIntegrator
     * class so you must match them exactly here and there.
     */
    public static final String CAPTURE_LAYOUT_ID_KEY = "CAPTURE_LAYOUT_ID";
    public static final String BEEP_ON_KEY = "BEEP_ON";
    public static final String ORIENTATION_KEY = "ORIENTATION";
    public static final String SCANNER_LINE_KEY = "SCANNER_LINE";
    public static final String FRAME_ENABLED_KEY = "FRAME_ON";
    public static final String OVERLAY_ENABLED_KEY = "OVERLAY_ON";
    public static final String POTENTIAL_INDICATORS_KEY = "POTENTIAL_INDICATORS_ON";
    public static final String RESULT_INDICATORS_KEY = "RESULT_INDICATORS_ON";
    public static final String OVERLAY_OPACITY_KEY = "OVERLAY_OPACITY";

    public static final String ORIENTATION_LANDSCAPE = "LANDSCAPE";
    public static final String ORIENTATION_PORTRAIT = "PORTRAIT";

    private static final int DEFAULT_OPACITY = 0xA0;

    /*
     * stored variables
     */
    private boolean isScannerLineEnabled;
    private boolean isFrameEnabled;
    private boolean isOverlayEnabled;
    private boolean isPotentialIndicatorsEnabled;
    private boolean isResultIndicatorsEnabled;
    private int captureLayoutId;
    private boolean isBeepOn;
    private String orientation;
    private int overlayOpacity;

    public ScannerOptions() {
        //set the defaults
        setScannerLineEnabled(true);
        setFrameEnabled(true);
        setOverlayEnabled(true);
        setPotentialIndicatorsEnabled(true);
        setResultIndicatorsEnabled(true);
        setBeepOn(true);
        setCaptureLayoutId(R.layout.zxing_capture);
        setOrientation(null); //already catches null and replaces it with default
        setOverlayOpacity(DEFAULT_OPACITY);

        /*
         * !!!!!!!!! if you add new options make sure to add their default values here!!!!!!!!!!
         */
    }

    public static ScannerOptions scannerOptionsFromBundle(Bundle bundle) {
        ScannerOptions options = new ScannerOptions();

        /*
         * These should mirror the same options from getAsBundle()
         */
        options.setScannerLineEnabled(bundle.getBoolean(SCANNER_LINE_KEY, true));
        options.setFrameEnabled(bundle.getBoolean(FRAME_ENABLED_KEY, true));
        options.setOverlayEnabled(bundle.getBoolean(OVERLAY_ENABLED_KEY, true));
        options.setPotentialIndicatorsEnabled(bundle.getBoolean(POTENTIAL_INDICATORS_KEY, true));
        options.setResultIndicatorsEnabled(bundle.getBoolean(RESULT_INDICATORS_KEY, true));
        options.setBeepOn(bundle.getBoolean(BEEP_ON_KEY, true));
        options.setCaptureLayoutId(bundle.getInt(CAPTURE_LAYOUT_ID_KEY, R.layout.zxing_capture));
        options.setOrientation(bundle.getString(ORIENTATION_KEY));
        options.setOverlayOpacity(bundle.getInt(OVERLAY_OPACITY_KEY, DEFAULT_OPACITY));

        return options;
    }

    public Bundle getAsBundle() {
        Bundle bundle = new Bundle();

        /*
         * These should mirror the same options from scannerOptionsFromBundle()
         */
        bundle.putBoolean(SCANNER_LINE_KEY, isScannerLineEnabled());
        bundle.putBoolean(FRAME_ENABLED_KEY, isFrameEnabled());
        bundle.putBoolean(OVERLAY_ENABLED_KEY, isOverlayEnabled());
        bundle.putBoolean(POTENTIAL_INDICATORS_KEY, isPotentialIndicatorsEnabled());
        bundle.putBoolean(RESULT_INDICATORS_KEY, isResultIndicatorsEnabled());
        bundle.putBoolean(BEEP_ON_KEY, isBeepOn());
        bundle.putInt(CAPTURE_LAYOUT_ID_KEY, getCaptureLayoutId());
        bundle.putString(ORIENTATION_KEY, getOrientation());
        bundle.putInt(OVERLAY_OPACITY_KEY, getOverlayOpacity());

        return bundle;
    }

    public boolean isScannerLineEnabled() {
        return isScannerLineEnabled;
    }

    public void setScannerLineEnabled(boolean isScannerLineEnabled) {
        this.isScannerLineEnabled = isScannerLineEnabled;
    }

    public boolean isFrameEnabled() {
        return isFrameEnabled;
    }

    public void setFrameEnabled(boolean isFrameEnabled) {
        this.isFrameEnabled = isFrameEnabled;
    }

    public boolean isOverlayEnabled() {
        return isOverlayEnabled;
    }

    public void setOverlayEnabled(boolean isOverlayEnabled) {
        this.isOverlayEnabled = isOverlayEnabled;
    }

    public boolean isPotentialIndicatorsEnabled() {
        return isPotentialIndicatorsEnabled;
    }

    public void setPotentialIndicatorsEnabled(boolean isPotentialIndicatorsEnabled) {
        this.isPotentialIndicatorsEnabled = isPotentialIndicatorsEnabled;
    }

    public boolean isResultIndicatorsEnabled() {
        return isResultIndicatorsEnabled;
    }

    public void setResultIndicatorsEnabled(boolean isResultIndicatorsEnabled) {
        this.isResultIndicatorsEnabled = isResultIndicatorsEnabled;
    }

    public int getCaptureLayoutId() {
        return captureLayoutId;
    }

    public void setCaptureLayoutId(int captureLayoutId) {
        this.captureLayoutId = captureLayoutId;
    }

    public boolean isBeepOn() {
        return isBeepOn;
    }

    public void setBeepOn(boolean isBeepOn) {
        this.isBeepOn = isBeepOn;
    }

    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(String orientation) {
        if(orientation == null) {
            this.orientation = ORIENTATION_LANDSCAPE;
        } else {
            this.orientation = orientation;
        }
    }

    public int getOverlayOpacity() {
        return overlayOpacity;
    }

    public void setOverlayOpacity(int overlayOpacity) {
        this.overlayOpacity = overlayOpacity;
    }
}
