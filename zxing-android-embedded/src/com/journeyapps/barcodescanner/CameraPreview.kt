package com.journeyapps.barcodescanner

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.TextureView.SurfaceTextureListener
import com.google.zxing.client.android.R
import com.journeyapps.barcodescanner.Util.validateMainThread
import com.journeyapps.barcodescanner.camera.*
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * CameraPreview is a view that handles displaying of a camera preview on a SurfaceView. It is
 * intended to be used as a base for realtime processing of camera images, e.g. barcode decoding
 * or OCR, although none of this happens in CameraPreview itself.
 *
 * The camera is managed on a separate thread, using CameraInstance.
 *
 * Two methods MUST be called on CameraPreview to manage its state:
 * 1. resume() - initialize the camera and start the preview. Call from the Activity's onResume().
 * 2. pause() - stop the preview and release any resources. Call from the Activity's onPause().
 *
 * Startup sequence:
 *
 * 1. Create SurfaceView.
 * 2. open camera.
 * 2. layout this container, to get size
 * 3. set display config, according to the container size
 * 4. configure()
 * 5. wait for preview size to be ready
 * 6. set surface size according to preview size
 * 7. set surface and start preview
 */
open class CameraPreview : ViewGroup {
    interface StateListener {
        /**
         * Preview and frame sizes are determined.
         */
        fun previewSized()

        /**
         * Preview has started.
         */
        fun previewStarted()

        /**
         * Preview has stopped.
         */
        fun previewStopped()

        /**
         * The camera has errored, and cannot display a preview.
         *
         * @param error the error
         */
        fun cameraError(error: Exception?)

        /**
         * The camera has been closed.
         */
        fun cameraClosed()
    }

    /**
     * Get the current CameraInstance. This may be null, and may change when
     * pausing / resuming the preview.
     *
     * While the preview is active, getCameraInstance() will never be null.
     *
     * @return the current CameraInstance
     * @see .isPreviewActive
     */
    var cameraInstance: CameraInstance? = null
        private set
    private var windowManager: WindowManager? = null
    private var stateHandler: Handler? = null

    /**
     * Set to true to use TextureView instead of SurfaceView.
     *
     * Will only have an effect on API >= 14.
     *
     * @param useTextureView true to use TextureView.
     */
    var isUseTextureView = false
    private var surfaceView: SurfaceView? = null
    private var textureView: TextureView? = null

    /**
     * The preview typically starts being active a while after calling resume(), and stops
     * when calling pause().
     *
     * @return true if the preview is active
     */
    var isPreviewActive = false
        private set
    private var rotationListener: RotationListener? = null
    private var openedOrientation = -1
    private val stateListeners: MutableList<StateListener> = ArrayList()
    private var displayConfiguration: DisplayConfiguration? = null
    /**
     * @return the CameraSettings currently in use
     */
    /**
     * Set the CameraSettings. Use this to select a different camera, change exposure and torch
     * settings, and some other options.
     *
     * This has no effect if the camera is already open.
     *
     * @param cameraSettings the new settings
     */
    var cameraSettings = CameraSettings()

    // Size of this container, non-null after layout is performed
    private var containerSize: Size? = null

    // Size of the preview resolution
    var previewSize: Size? = null
        private set

    // Rect placing the preview surface
    private var surfaceRect: Rect? = null

    // Size of the current surface. non-null if the surface is ready
    private var currentSurfaceSize: Size? = null

    /**
     * The framing rectangle, relative to this view. Use to draw the rectangle.
     *
     * Will never be null while the preview is active.
     *
     * @return the framing rect, or null
     * @see .isPreviewActive
     */
    // Framing rectangle relative to this view
    var framingRect: Rect? = null
        private set

    /**
     * The framing rect, relative to the camera preview resolution.
     *
     * Will never be null while the preview is active.
     *
     * @return the preview rect, or null
     * @see .isPreviewActive
     */
    // Framing rectangle relative to the preview resolution
    var previewFramingRect: Rect? = null
        private set

    /**
     * Set an exact size for the framing rectangle. It will be centered in the view.
     *
     * @param framingRectSize the size
     */
    // Size of the framing rectangle. If null, defaults to using a margin percentage.
    var framingRectSize: Size? = null

    // Fraction of the width / heigth to use as a margin. This fraction is used on each size, so
    // must be smaller than 0.5;
    private var marginFraction = 0.1
    var previewScalingStrategy: PreviewScalingStrategy? = null
        private set
        get() {
            field?.let { return it }

            // If we are using SurfaceTexture, it is safe to use centerCrop.
            // For SurfaceView, it's better to use fitCenter, otherwise the preview may overlap to
            // other views.
            return if (textureView != null) {
                CenterCropStrategy()
            } else {
                FitCenterStrategy()
            }
        }
    private var torchOn = false

    @TargetApi(14)
    private fun surfaceTextureListener(): SurfaceTextureListener {
        // Cannot initialize automatically, since we may be API < 14
        return object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                onSurfaceTextureSizeChanged(surface, width, height)
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                currentSurfaceSize = Size(width, height)
                startPreviewIfReady()
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    private val surfaceCallback: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {}
        override fun surfaceDestroyed(holder: SurfaceHolder) {
            currentSurfaceSize = null
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            currentSurfaceSize = Size(width, height)
            startPreviewIfReady()
        }
    }
    private val stateCallback = Handler.Callback { message ->
        if (message.what == R.id.zxing_prewiew_size_ready) {
            // At this point, we have the camera preview size, and should have containerSize and
            // surfaceRect.
            previewSized(message.obj as Size)
            return@Callback true
        } else if (message.what == R.id.zxing_camera_error) {
            val error = message.obj as Exception
            if (isActive) {
                // This check prevents multiple errors from begin passed through.
                pause()
                fireState.cameraError(error)
            }
        } else if (message.what == R.id.zxing_camera_closed) {
            fireState.cameraClosed()
        }
        false
    }
    private val rotationCallback: RotationCallback = object : RotationCallback {
        override fun onRotationChanged(rotation: Int) {
            // Make sure this is run on the main thread.
            stateHandler?.postDelayed({ rotationChanged() }, ROTATION_LISTENER_DELAY_MS.toLong())
        }
    }

    constructor(context: Context) : super(context) {
        initialize(context, null, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize(context, attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialize(context, attrs, defStyleAttr, 0)
    }

    private fun initialize(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        if (background == null) {
            // Default to SurfaceView colour, so that there are less changes.
            setBackgroundColor(Color.BLACK)
        }
        initializeAttributes(attrs)
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        stateHandler = Handler(stateCallback)
        rotationListener = RotationListener()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupSurfaceView()
    }

    /**
     * Initialize from XML attributes.
     *
     * @param attrs the attributes
     */
    open fun initializeAttributes(attrs: AttributeSet?) {
        val styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.zxing_camera_preview)
        val framingRectWidth = styledAttributes.getDimension(R.styleable.zxing_camera_preview_zxing_framing_rect_width, -1f).toInt()
        val framingRectHeight = styledAttributes.getDimension(R.styleable.zxing_camera_preview_zxing_framing_rect_height, -1f).toInt()
        if (framingRectWidth > 0 && framingRectHeight > 0) {
            framingRectSize = Size(framingRectWidth, framingRectHeight)
        }
        isUseTextureView = styledAttributes.getBoolean(R.styleable.zxing_camera_preview_zxing_use_texture_view, true)

        // See zxing_attrs.xml for the enum values
        when (styledAttributes.getInteger(R.styleable.zxing_camera_preview_zxing_preview_scaling_strategy, -1)) {
            1 -> {
                previewScalingStrategy = CenterCropStrategy()
            }
            2 -> {
                previewScalingStrategy = FitCenterStrategy()
            }
            3 -> {
                previewScalingStrategy = FitXYStrategy()
            }
        }
        styledAttributes.recycle()
    }

    private fun rotationChanged() {
        // Confirm that it did actually change
        if (isActive && displayRotation != openedOrientation) {
            pause()
            resume()
        }
    }

    private fun setupSurfaceView() {
        if (isUseTextureView) {
            textureView = TextureView(context)
            textureView!!.surfaceTextureListener = surfaceTextureListener()
            addView(textureView)
        } else {
            surfaceView = SurfaceView(context)
            surfaceView!!.holder.addCallback(surfaceCallback)
            addView(surfaceView)
        }
    }

    /**
     * Add a listener to be notified of changes to the preview state, as well as camera errors.
     *
     * @param listener the listener
     */
    fun addStateListener(listener: StateListener) {
        stateListeners.add(listener)
    }

    private val fireState: StateListener = object : StateListener {
        override fun previewSized() {
            for (listener in stateListeners) {
                listener.previewSized()
            }
        }

        override fun previewStarted() {
            for (listener in stateListeners) {
                listener.previewStarted()
            }
        }

        override fun previewStopped() {
            for (listener in stateListeners) {
                listener.previewStopped()
            }
        }

        override fun cameraError(error: Exception?) {
            for (listener in stateListeners) {
                listener.cameraError(error)
            }
        }

        override fun cameraClosed() {
            for (listener in stateListeners) {
                listener.cameraClosed()
            }
        }
    }

    private fun calculateFrames() {
        if (containerSize == null || previewSize == null || displayConfiguration == null) {
            previewFramingRect = null
            framingRect = null
            surfaceRect = null
            throw IllegalStateException("containerSize or previewSize is not set yet")
        }
        val previewWidth = previewSize!!.width
        val previewHeight = previewSize!!.height
        val width = containerSize!!.width
        val height = containerSize!!.height
        val scaledPreview = displayConfiguration!!.scalePreview(previewSize!!)
        if (scaledPreview.width() <= 0 || scaledPreview.height() <= 0) {
            // Something is not ready yet - we can't start the preview.
            return
        }
        surfaceRect = scaledPreview
        val container = Rect(0, 0, width, height)
        framingRect = calculateFramingRect(container, surfaceRect)
        val frameInPreview = Rect(framingRect)
        frameInPreview.offset(-surfaceRect!!.left, -surfaceRect!!.top)
        previewFramingRect = Rect(frameInPreview.left * previewWidth / surfaceRect!!.width(),
                frameInPreview.top * previewHeight / surfaceRect!!.height(),
                frameInPreview.right * previewWidth / surfaceRect!!.width(),
                frameInPreview.bottom * previewHeight / surfaceRect!!.height())
        if (previewFramingRect == null || previewFramingRect!!.width() <= 0 || previewFramingRect!!.height() <= 0) {
            previewFramingRect = null
            framingRect = null
            Log.w(TAG, "Preview frame is too small")
        } else {
            fireState.previewSized()
        }
    }

    /**
     * Call this on the main thread, while the preview is running.
     *
     * @param on true to turn on the torch
     */
    fun setTorch(on: Boolean) {
        torchOn = on
        cameraInstance?.setTorch(on)
    }

    /**
     * Changes the settings for Camera.
     * Must be called after [.resume].
     *
     * @param callback [CameraParametersCallback]
     */
    fun changeCameraParameters(callback: CameraParametersCallback?) {
        cameraInstance?.changeCameraParameters(callback)
    }

    private fun containerSized(containerSize: Size) {
        this.containerSize = containerSize
        cameraInstance?.let {
            if (it.displayConfiguration == null) {
                it.displayConfiguration = DisplayConfiguration(displayRotation, containerSize)
                //cannot use apply method
                previewScalingStrategy?.let { strategy -> it.displayConfiguration!!.previewScalingStrategy = strategy }
                it.displayConfiguration = displayConfiguration
                it.configureCamera()
                if (torchOn)
                    it.setTorch(torchOn)
            }
        }
    }

    /**
     * Override the preview scaling strategy.
     *
     * @param previewScalingStrategy null for the default
     */
    fun setPreviewScalingStrategy(previewScalingStrategy: PreviewScalingStrategy?) {
        this.previewScalingStrategy = previewScalingStrategy
    }

    private fun previewSized(size: Size) {
        previewSize = size
        if (containerSize != null) {
            calculateFrames()
            requestLayout()
            startPreviewIfReady()
        }
    }

    /**
     * Calculate transformation for the TextureView.
     *
     * An identity matrix would cause the preview to be scaled up/down to fill the TextureView.
     *
     * @param textureSize the size of the textureView
     * @param previewSize the camera preview resolution
     * @return the transform matrix for the TextureView
     */
    protected open fun calculateTextureTransform(textureSize: Size, previewSize: Size): Matrix {
        val ratioTexture = textureSize.width.toFloat() / textureSize.height.toFloat()
        val ratioPreview = previewSize.width.toFloat() / previewSize.height.toFloat()
        val scaleX: Float
        val scaleY: Float

        // We scale so that either width or height fits exactly in the TextureView, and the other
        // is bigger (cropped).
        if (ratioTexture < ratioPreview) {
            scaleX = ratioPreview / ratioTexture
            scaleY = 1f
        } else {
            scaleX = 1f
            scaleY = ratioTexture / ratioPreview
        }
        val matrix = Matrix()
        matrix.setScale(scaleX, scaleY)

        // Center the preview
        val scaledWidth = textureSize.width * scaleX
        val scaledHeight = textureSize.height * scaleY
        val dx = (textureSize.width - scaledWidth) / 2
        val dy = (textureSize.height - scaledHeight) / 2

        // Perform the translation on the scaled preview
        matrix.postTranslate(dx, dy)
        return matrix
    }

    private fun startPreviewIfReady() {
        if (currentSurfaceSize != null && previewSize != null && surfaceRect != null) {
            if (surfaceView != null && currentSurfaceSize!! == Size(surfaceRect!!.width(), surfaceRect!!.height())) {
                startCameraPreview(CameraSurface(surfaceView!!.holder))
            } else if (textureView != null && textureView!!.surfaceTexture != null) {
                if (previewSize != null) {
                    val transform = calculateTextureTransform(Size(textureView!!.width, textureView!!.height), previewSize!!)
                    textureView!!.setTransform(transform)
                }
                startCameraPreview(CameraSurface(textureView!!.surfaceTexture))
            }
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        containerSized(Size(r - l, b - t))
        if (surfaceView != null) {
            if (surfaceRect == null) {
                // Match the container, to reduce the risk of issues. The preview should never be drawn
                // while the surface has this size.
                surfaceView!!.layout(0, 0, width, height)
            } else {
                surfaceView!!.layout(surfaceRect!!.left, surfaceRect!!.top, surfaceRect!!.right, surfaceRect!!.bottom)
            }
        } else if (textureView != null) {
            textureView!!.layout(0, 0, width, height)
        }
    }

    /**
     * Start the camera preview and decoding. Typically this should be called from the Activity's
     * onResume() method.
     *
     * Call from UI thread only.
     */
    fun resume() {
        // This must be safe to call multiple times
        validateMainThread()
        Log.d(TAG, "resume()")

        // initCamera() does nothing if called twice, but does log a warning
        initCamera()
        if (currentSurfaceSize != null) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            startPreviewIfReady()
        } else if (surfaceView != null) {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceView!!.holder.addCallback(surfaceCallback)
        } else if (textureView != null) {
            if (textureView!!.isAvailable) {
                surfaceTextureListener().onSurfaceTextureAvailable(textureView!!.surfaceTexture, textureView!!.width, textureView!!.height)
            } else {
                textureView!!.surfaceTextureListener = surfaceTextureListener()
            }
        }

        // To trigger surfaceSized again
        requestLayout()
        rotationListener?.listen(context, rotationCallback)
    }

    /**
     * Pause scanning and the camera preview. Typically this should be called from the Activity's
     * onPause() method.
     *
     * Call from UI thread only.
     */
    open fun pause() {
        // This must be safe to call multiple times.
        validateMainThread()
        Log.d(TAG, "pause()")
        openedOrientation = -1
        if (cameraInstance != null) {
            cameraInstance!!.close()
            cameraInstance = null
            isPreviewActive = false
        } else {
            stateHandler!!.sendEmptyMessage(R.id.zxing_camera_closed)
        }
        if (currentSurfaceSize == null && surfaceView != null) {
            val surfaceHolder = surfaceView!!.holder
            surfaceHolder.removeCallback(surfaceCallback)
        }
        if (currentSurfaceSize == null && textureView != null) {
            textureView!!.surfaceTextureListener = null
        }
        containerSize = null
        previewSize = null
        previewFramingRect = null
        rotationListener!!.stop()
        fireState.previewStopped()
    }

    /**
     * Pause scanning and preview; waiting for the Camera to be closed.
     *
     * This blocks the main thread.
     */
    fun pauseAndWait() {
        val instance = cameraInstance
        pause()
        val startTime = System.nanoTime()
        while (instance != null && !instance.isCameraClosed) {
            if (System.nanoTime() - startTime > 2000000000) {
                // Don't wait for longer than 2 seconds
                break
            }
            try {
                Thread.sleep(1)
            } catch (e: InterruptedException) {
                break
            }
        }
    }

    fun getMarginFraction(): Double {
        return marginFraction
    }

    /**
     * The the fraction of the width/height of view to be used as a margin for the framing rect.
     * This is ignored if framingRectSize is specified.
     *
     * @param marginFraction the fraction
     */
    fun setMarginFraction(marginFraction: Double) {
        require(marginFraction < 0.5) { "The margin fraction must be less than 0.5" }
        this.marginFraction = marginFraction
    }

    /**
     * Considered active if between resume() and pause().
     *
     * @return true if active
     */
    protected open val isActive: Boolean
        get() = cameraInstance != null

    private val displayRotation: Int
        get() = windowManager!!.defaultDisplay.rotation

    private fun initCamera() {
        if (cameraInstance != null) {
            Log.w(TAG, "initCamera called twice")
            return
        }
        cameraInstance = createCameraInstance()
        cameraInstance!!.setReadyHandler(stateHandler!!)
        cameraInstance!!.open()

        // Keep track of the orientation we opened at, so that we don't reopen the camera if we
        // don't need to.
        openedOrientation = displayRotation
    }

    /**
     * Create a new CameraInstance.
     *
     * Override to use a custom CameraInstance.
     *
     * @return a new CameraInstance
     */
    protected open fun createCameraInstance(): CameraInstance {
        val cameraInstance = CameraInstance(context)
        cameraInstance.cameraSettings = cameraSettings
        return cameraInstance
    }

    private fun startCameraPreview(surface: CameraSurface) {
        if (!isPreviewActive && cameraInstance != null) {
            Log.i(TAG, "Starting preview")
            cameraInstance!!.surface = surface
            cameraInstance!!.startPreview()
            isPreviewActive = true
            previewStarted()
            fireState.previewStarted()
        }
    }

    /**
     * Called when the preview is started. Override this to start decoding work.
     */
    protected open fun previewStarted() {}

    /**
     * Calculate framing rectangle, relative to the preview frame.
     *
     * Note that the SurfaceView may be larger than the container.
     *
     * Override this for more control over the framing rect calculations.
     *
     * @param container this container, with left = top = 0
     * @param surface   the SurfaceView, relative to this container
     * @return the framing rect, relative to this container
     */
    protected open fun calculateFramingRect(container: Rect?, surface: Rect?): Rect {
        // intersection is the part of the container that is used for the preview
        val intersection = Rect(container)
        if (framingRectSize != null) {
            // Specific size is specified. Make sure it's not larger than the container or surface.
            val horizontalMargin = max(0, (intersection.width() - framingRectSize!!.width) / 2)
            val verticalMargin = max(0, (intersection.height() - framingRectSize!!.height) / 2)
            intersection.inset(horizontalMargin, verticalMargin)
            return intersection
        }
        // margin as 10% (default) of the smaller of width, height
        val margin = min(intersection.width() * marginFraction, intersection.height() * marginFraction).toInt()
        intersection.inset(margin, margin)
        if (intersection.height() > intersection.width()) {
            // We don't want a frame that is taller than wide.
            intersection.inset(0, (intersection.height() - intersection.width()) / 2)
        }
        return intersection
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val myState = Bundle()
        myState.putParcelable("super", superState)
        myState.putBoolean("torch", torchOn)
        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is Bundle) {
            super.onRestoreInstanceState(state)
            return
        }
        val superState = state.getParcelable<Parcelable>("super")
        super.onRestoreInstanceState(superState)
        val torch = state.getBoolean("torch")
        setTorch(torch)
    }

    /**
     *
     * @return true if the camera has been closed in a background thread.
     */
    val isCameraClosed: Boolean
        get() = cameraInstance == null || cameraInstance!!.isCameraClosed

    companion object {
        private val TAG = CameraPreview::class.java.simpleName

        // Delay after rotation change is detected before we reorientate ourselves.
        // This is to avoid double-reinitialization when the Activity is destroyed and recreated.
        private const val ROTATION_LISTENER_DELAY_MS = 250
    }
}