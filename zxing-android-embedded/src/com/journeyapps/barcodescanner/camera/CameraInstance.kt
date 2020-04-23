package com.journeyapps.barcodescanner.camera

import android.content.Context
import android.os.Handler
import android.util.Log
import android.view.SurfaceHolder
import com.google.zxing.client.android.R
import com.journeyapps.barcodescanner.Size
import com.journeyapps.barcodescanner.Util.validateMainThread
import com.journeyapps.barcodescanner.camera.CameraInstance
import com.journeyapps.barcodescanner.camera.CameraThread.Companion.instance

/**
 * Manage a camera instance using a background thread.
 *
 * All methods must be called from the main thread.
 */
open class CameraInstance {
    /**
     *
     * @return the CameraThread used to manage the camera
     */
    protected open lateinit var cameraThread: CameraThread

    /**
     *
     * @return the surface om which the preview is displayed
     */
    open var surface: CameraSurface? = null

    /**
     * Returns the CameraManager used to control the camera.
     *
     * The CameraManager is not thread-safe, and must only be used from the CameraThread.
     *
     * @return the CameraManager used
     */
    protected open lateinit var cameraManager: CameraManager
    private var readyHandler: Handler? = null
    var displayConfiguration: DisplayConfiguration? = null
        set(configuration) {
            field = configuration
            cameraManager.displayConfiguration = configuration
        }
    var isOpen = false
        private set
    var isCameraClosed = true
        private set
    private var mainHandler: Handler? = null
    var cameraSettings = CameraSettings()
        set(value) {
            if (!isOpen) {
                field = value
                cameraManager.cameraSettings = value
            }
        }

    /**
     * Construct a new CameraInstance.
     *
     * A new CameraManager is created.
     *
     * @param context the Android Context
     */
    constructor(context: Context) {
        validateMainThread()
        cameraThread = instance!!
        cameraManager = CameraManager(context)
        cameraManager.cameraSettings = cameraSettings
        mainHandler = Handler()
    }

    /**
     * Construct a new CameraInstance with a specific CameraManager.
     *
     * @param cameraManager the CameraManager to use
     */
    constructor(cameraManager: CameraManager) {
        validateMainThread()
        this.cameraManager = cameraManager
    }

    fun setReadyHandler(readyHandler: Handler) {
        this.readyHandler = readyHandler
    }

    fun setSurfaceHolder(surfaceHolder: SurfaceHolder) {
        surface = CameraSurface(surfaceHolder)
    }

    /**
     * Actual preview size in current rotation. null if not determined yet.
     *
     * @return preview size
     */
    private val previewSize: Size?
        get() = cameraManager.getPreviewSize()

    /**
     *
     * @return the camera rotation relative to display rotation, in degrees. Typically 0 if the
     * display is in landscape orientation.
     */
    val cameraRotation: Int
        get() = cameraManager.cameraRotation

    fun open() {
        validateMainThread()
        isOpen = true
        isCameraClosed = false
        cameraThread.incrementAndEnqueue(opener)
    }

    fun configureCamera() {
        validateMainThread()
        validateOpen()
        cameraThread.enqueue(configure)
    }

    fun startPreview() {
        validateMainThread()
        validateOpen()
        cameraThread.enqueue(previewStarter)
    }

    fun setTorch(on: Boolean) {
        validateMainThread()
        if (isOpen) {
            cameraThread.enqueue(Runnable { cameraManager.setTorch(on) })
        }
    }

    /**
     * Changes the settings for Camera.
     *
     * @param callback [CameraParametersCallback]
     */
    fun changeCameraParameters(callback: CameraParametersCallback?) {
        validateMainThread()
        if (isOpen) {
            cameraThread.enqueue(Runnable { cameraManager.changeCameraParameters(callback) })
        }
    }

    fun close() {
        validateMainThread()
        if (isOpen) {
            cameraThread.enqueue(closer)
        } else {
            isCameraClosed = true
        }
        isOpen = false
    }

    fun requestPreview(callback: PreviewCallback) {
        mainHandler?.post {
            if (!isOpen) {
                Log.d(TAG, "Camera is closed, not requesting preview")
                return@post
            }
            cameraThread.enqueue(Runnable { cameraManager.requestPreviewFrame(callback) })
        }
    }

    private fun validateOpen() {
        check(isOpen) { "CameraInstance is not open" }
    }

    private val opener = Runnable {
        try {
            Log.d(TAG, "Opening camera")
            cameraManager.open()
        } catch (e: Exception) {
            notifyError(e)
            Log.e(TAG, "Failed to open camera", e)
        }
    }
    private val configure = Runnable {
        try {
            Log.d(TAG, "Configuring camera")
            cameraManager.configure()
            readyHandler?.obtainMessage(R.id.zxing_prewiew_size_ready, previewSize)?.sendToTarget()

        } catch (e: Exception) {
            notifyError(e)
            Log.e(TAG, "Failed to configure camera", e)
        }
    }
    private val previewStarter = Runnable {
        try {
            Log.d(TAG, "Starting preview")
            cameraManager.setPreviewDisplay(surface!!)
            cameraManager.startPreview()
        } catch (e: Exception) {
            notifyError(e)
            Log.e(TAG, "Failed to start preview", e)
        }
    }
    private val closer: Runnable = Runnable {
        try {
            Log.d(TAG, "Closing camera")
            cameraManager.stopPreview()
            cameraManager.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close camera", e)
        }
        isCameraClosed = true
        readyHandler?.sendEmptyMessage(R.id.zxing_camera_closed)
        cameraThread.decrementInstances()
    }

    private fun notifyError(error: Exception) {
        readyHandler?.obtainMessage(R.id.zxing_camera_error, error)?.sendToTarget()
    }

    companion object {
        private val TAG = CameraInstance::class.java.simpleName
    }
}