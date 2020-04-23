package com.journeyapps.barcodescanner

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import com.google.zxing.client.android.R

/**
 *
 */
open class CaptureActivity : Activity() {
    private var capture: CaptureManager? = null
    private var barcodeScannerView: DecoratedBarcodeView? = null
    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        barcodeScannerView = initializeContent()
        capture = CaptureManager(this, barcodeScannerView!!)
        capture!!.initializeFromIntent(intent, savedInstanceState)
        capture!!.decode()
    }

    /**
     * Override to use a different layout.
     *
     * @return the DecoratedBarcodeView
     */
    protected open fun initializeContent(): DecoratedBarcodeView? {
        setContentView(R.layout.zxing_capture)
        return findViewById<View>(R.id.zxing_barcode_scanner) as DecoratedBarcodeView
    }

    override fun onResume() {
        super.onResume()
        capture?.onResume()
    }

    override fun onPause() {
        super.onPause()
        capture?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        capture?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        capture?.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        capture?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return barcodeScannerView?.onKeyDown(keyCode, event) ?: super.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
    }
}