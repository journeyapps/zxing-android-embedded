package com.journeyapps.barcodescanner;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.KeyEvent;

import com.google.zxing.client.android.R;

/**
 *
 */
public class CaptureActivity extends Activity {
    private CaptureManager capture;
    private CompoundBarcodeView barcodeScannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        barcodeScannerView = initializeContent();

        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        capture.decode();
    }

    /**
     * Override to use a different layout.
     *
     * @return the CompoundBarcodeView
     */
    protected CompoundBarcodeView initializeContent() {
        setContentView(R.layout.zxing_capture);
        return (CompoundBarcodeView)findViewById(R.id.zxing_barcode_scanner);
    }

    @Override
    protected void onResume() {
        super.onResume();
        capture.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        capture.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        capture.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        capture.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        capture.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }
}
