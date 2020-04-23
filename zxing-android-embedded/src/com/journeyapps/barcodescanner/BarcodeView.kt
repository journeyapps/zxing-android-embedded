package com.journeyapps.barcodescanner

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import com.google.zxing.DecodeHintType
import com.google.zxing.ResultPoint
import com.google.zxing.client.android.R
import java.util.*

/**
 * A view for scanning barcodes.
 *
 * Two methods MUST be called to manage the state:
 * 1. resume() - initialize the camera and start the preview. Call from the Activity's onResume().
 * 2. pause() - stop the preview and release any resources. Call from the Activity's onPause().
 *
 * Start decoding with decodeSingle() or decodeContinuous(). Stop decoding with stopDecoding().
 *
 * @see CameraPreview for more details on the preview lifecycle.
 */
open class BarcodeView : CameraPreview {
    private enum class DecodeMode {
        NONE, SINGLE, CONTINUOUS
    }

    private var decodeMode = DecodeMode.NONE
    private var callback: BarcodeCallback? = null
    private var decoderThread: DecoderThread? = null

    var decoderFactory: DecoderFactory? = null
        set(value) {
            Util.validateMainThread()
            field = value
            decoderThread?.decoder = createDecoder()
        }

    private var resultHandler: Handler? = null
    private val resultCallback = Handler.Callback { message ->
        when (message.what) {
            R.id.zxing_decode_succeeded -> {
                val result = message.obj as BarcodeResult
                if (decodeMode != DecodeMode.NONE) {
                    callback?.barcodeResult(result)
                    if (decodeMode == DecodeMode.SINGLE) {
                        stopDecoding()
                    }
                }
                return@Callback true
            }
            R.id.zxing_decode_failed -> {
                // Failed. Next preview is automatically tried.
                return@Callback true
            }
            R.id.zxing_possible_result_points -> {
                val resultPoints = message.obj as List<ResultPoint>
                if (decodeMode != DecodeMode.NONE) {
                    callback?.possibleResultPoints(resultPoints)
                }
                return@Callback true
            }
            else -> false
        }
    }

    constructor(context: Context) : super(context) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialize()
    }

    private fun initialize() {
        decoderFactory = DefaultDecoderFactory()
        resultHandler = Handler(resultCallback)
    }

    private fun createDecoder(): Decoder? {
        if (decoderFactory == null) {
            decoderFactory = createDefaultDecoderFactory()
        }
        val hints: MutableMap<DecodeHintType, Any?> = HashMap()
        hints[DecodeHintType.NEED_RESULT_POINT_CALLBACK] = callback
        val decoder = decoderFactory?.createDecoder(hints.toMap())
        val callback = DecoderResultPointCallback(decoder)
        return decoder
    }

    /**
     * Decode a single barcode, then stop decoding.
     *
     * The callback will only be called on the UI thread.
     *
     * @param callback called with the barcode result, as well as possible ResultPoints
     */
    fun decodeSingle(callback: BarcodeCallback) {
        decodeMode = DecodeMode.SINGLE
        this.callback = callback
        startDecoderThread()
    }

    /**
     * Continuously decode barcodes. The same barcode may be returned multiple times per second.
     *
     * The callback will only be called on the UI thread.
     *
     * @param callback called with the barcode result, as well as possible ResultPoints
     */
    fun decodeContinuous(callback: BarcodeCallback) {
        decodeMode = DecodeMode.CONTINUOUS
        this.callback = callback
        startDecoderThread()
    }

    /**
     * Stop decoding, but do not stop the preview.
     */
    fun stopDecoding() {
        decodeMode = DecodeMode.NONE
        callback = null
        stopDecoderThread()
    }

    protected open fun createDefaultDecoderFactory(): DecoderFactory {
        return DefaultDecoderFactory()
    }

    private fun startDecoderThread() {
        stopDecoderThread() // To be safe
        if (decodeMode != DecodeMode.NONE && isPreviewActive) {
            // We only start the thread if both:
            // 1. decoding was requested
            // 2. the preview is active
            decoderThread = DecoderThread(cameraInstance, createDecoder(), resultHandler)
            decoderThread!!.cropRect = previewFramingRect
            decoderThread!!.start()
        }
    }

    override fun previewStarted() {
        super.previewStarted()
        startDecoderThread()
    }

    private fun stopDecoderThread() {
        decoderThread?.stop()
        decoderThread = null
    }

    /**
     * Stops the live preview and decoding.
     *
     * Call from the Activity's onPause() method.
     */
    override fun pause() {
        stopDecoderThread()
        super.pause()
    }
}