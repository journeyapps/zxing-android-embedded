package com.journeyapps.barcodescanner

import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import com.google.zxing.LuminanceSource
import com.google.zxing.Result
import com.google.zxing.client.android.R
import com.journeyapps.barcodescanner.camera.CameraInstance
import com.journeyapps.barcodescanner.camera.PreviewCallback

/**
 *
 */
open class DecoderThread(cameraInstance: CameraInstance?, decoder: Decoder?, resultHandler: Handler?) {
    private val cameraInstance: CameraInstance?
    private var thread: HandlerThread? = null
    private var handler: Handler? = null
    var decoder: Decoder?
    private val resultHandler: Handler?
    var cropRect: Rect? = null
    private var running = false
    private val LOCK = Any()
    private val callback = Handler.Callback { message ->
        if (message.what == R.id.zxing_decode) {
            decode(message.obj as SourceData)
        } else if (message.what == R.id.zxing_preview_failed) {
            // Error already logged. Try again.
            requestNextPreview()
        }
        true
    }

    /**
     * Start decoding.
     *
     * This must be called from the UI thread.
     */
    fun start() {
        Util.validateMainThread()
        thread = HandlerThread(TAG)
        thread!!.start()
        handler = Handler(thread!!.looper, callback)
        running = true
        requestNextPreview()
    }

    /**
     * Stop decoding.
     *
     * This must be called from the UI thread.
     */
    fun stop() {
        Util.validateMainThread()
        synchronized(LOCK) {
            running = false
            handler?.removeCallbacksAndMessages(null)
            thread?.quit()
        }
    }

    private val previewCallback: PreviewCallback = object : PreviewCallback {
        override fun onPreview(sourceData: SourceData) {
            // Only post if running, to prevent a warning like this:
            //   java.lang.RuntimeException: Handler (android.os.Handler) sending message to a Handler on a dead thread

            // synchronize to handle cases where this is called concurrently with stop()
            synchronized(LOCK) {
                if (running) {
                    // Post to our thread.
                    handler?.obtainMessage(R.id.zxing_decode, sourceData)?.sendToTarget()
                }
            }
        }

        override fun onPreviewError(e: Exception?) {
            synchronized(LOCK) {
                if (running) {
                    // Post to our thread.
                    handler?.obtainMessage(R.id.zxing_preview_failed)?.sendToTarget()
                }
            }
        }
    }

    private fun requestNextPreview() {
        cameraInstance?.requestPreview(previewCallback)
    }

    protected open fun createSource(sourceData: SourceData): LuminanceSource? {
        return if (cropRect == null) {
            null
        } else {
            sourceData.createSource()
        }
    }

    private fun decode(sourceData: SourceData) {
        val start = System.currentTimeMillis()
        var rawResult: Result? = null
        sourceData.cropRect = cropRect
        val source = createSource(sourceData)
        source?.let { rawResult = decoder?.decode(it) }

        if (rawResult != null) {
            // Don't log the barcode contents for security.
            val end = System.currentTimeMillis()
            Log.d(TAG, "Found barcode in " + (end - start) + " ms")

            resultHandler?.let {
                val barcodeResult = BarcodeResult(rawResult!!, sourceData)
                val message = Message.obtain(it, R.id.zxing_decode_succeeded, barcodeResult)
                val bundle = Bundle()
                message.data = bundle
                message.sendToTarget()
            }
        } else {
            resultHandler?.let {
                val message = Message.obtain(it, R.id.zxing_decode_failed)
                message.sendToTarget()
            }
        }
        if (resultHandler != null && decoder != null) {
            val resultPoints = BarcodeResult.transformResultPoints(decoder!!.getPossibleResultPoints(), sourceData)
            val message = Message.obtain(resultHandler, R.id.zxing_possible_result_points, resultPoints)
            message.sendToTarget()
        }

        requestNextPreview()
    }

    companion object {
        private val TAG = DecoderThread::class.java.simpleName
    }

    init {
        Util.validateMainThread()
        this.cameraInstance = cameraInstance
        this.decoder = decoder
        this.resultHandler = resultHandler
    }
}