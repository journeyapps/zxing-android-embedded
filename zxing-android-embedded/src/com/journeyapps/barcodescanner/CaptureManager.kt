package com.journeyapps.barcodescanner

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.ResultMetadataType
import com.google.zxing.ResultPoint
import com.google.zxing.client.android.BeepManager
import com.google.zxing.client.android.InactivityTimer
import com.google.zxing.client.android.Intents
import com.google.zxing.client.android.R
import com.journeyapps.barcodescanner.CameraPreview.StateListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Manages barcode scanning for a CaptureActivity. This class may be used to have a custom Activity
 * (e.g. with a customized look and feel, or a different superclass), but not the barcode scanning
 * process itself.
 *
 * This is intended for an Activity that is dedicated to capturing a single barcode and returning
 * it via setResult(). For other use cases, use DefaultBarcodeScannerView or BarcodeView directly.
 *
 * The following is managed by this class:
 * - Orientation lock
 * - InactivityTimer
 * - BeepManager
 * - Initializing from an Intent (via IntentIntegrator)
 * - Setting the result and finishing the Activity when a barcode is scanned
 * - Displaying camera errors
 */
open class CaptureManager(private val activity: Activity, private val barcodeView: DecoratedBarcodeView) {
    private var orientationLock = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    private var returnBarcodeImagePath = false
    private var showDialogIfMissingCameraPermission = true
    private var missingCameraPermissionDialogMessage = String()
    private var destroyed = false
    private val inactivityTimer: InactivityTimer
    private val beepManager: BeepManager
    private val handler: Handler
    private var finishWhenClosed = false
    private val stateListener: StateListener = object : StateListener {
        override fun previewSized() {}
        override fun previewStarted() {}
        override fun previewStopped() {}
        override fun cameraError(error: Exception?) {
            displayFrameworkBugMessageAndExit(
                    activity.getString(R.string.zxing_msg_camera_framework_bug)
            )
        }

        override fun cameraClosed() {
            if (finishWhenClosed) {
                Log.d(TAG, "Camera closed; finishing activity")
                finish()
            }
        }
    }

    init {
        barcodeView.barcodeView.addStateListener(stateListener)
        handler = Handler()
        inactivityTimer = InactivityTimer(activity, Runnable {
            Log.d(TAG, "Finishing due to inactivity")
            finish()
        })
        beepManager = BeepManager(activity)
    }

    private val callback: BarcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            barcodeView.pause()
            beepManager.playBeepSoundAndVibrate()
            handler.post { returnResult(result) }
        }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
    }

    /**
     * Perform initialization, according to preferences set in the intent.
     *
     * @param intent the intent containing the scanning preferences
     * @param savedInstanceState saved state, containing orientation lock
     */
    fun initializeFromIntent(intent: Intent?, savedInstanceState: Bundle?) {
        val window = activity.window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (savedInstanceState != null) {
            // If the screen was locked and unlocked again, we may start in a different orientation
            // (even one not allowed by the manifest). In this case we restore the orientation we were
            // previously locked to.
            orientationLock = savedInstanceState.getInt(SAVED_ORIENTATION_LOCK, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
        }
        if (intent != null) {
            // Only lock the orientation if it's not locked to something else yet
            val orientationLocked = intent.getBooleanExtra(Intents.Scan.ORIENTATION_LOCKED, true)
            if (orientationLocked) {
                lockOrientation()
            }
            if (Intents.Scan.ACTION == intent.action) {
                barcodeView.initializeFromIntent(intent)
            }
            if (!intent.getBooleanExtra(Intents.Scan.BEEP_ENABLED, true)) {
                beepManager.isBeepEnabled = false
            }
            if (intent.hasExtra(Intents.Scan.SHOW_MISSING_CAMERA_PERMISSION_DIALOG)) {
                setShowMissingCameraPermissionDialog(
                        intent.getBooleanExtra(Intents.Scan.SHOW_MISSING_CAMERA_PERMISSION_DIALOG, true),
                        intent.getStringExtra(Intents.Scan.MISSING_CAMERA_PERMISSION_DIALOG_MESSAGE)
                )
            }
            if (intent.hasExtra(Intents.Scan.TIMEOUT)) {
                handler.postDelayed({ returnResultTimeout() }, intent.getLongExtra(Intents.Scan.TIMEOUT, 0L))
            }
            if (intent.getBooleanExtra(Intents.Scan.BARCODE_IMAGE_ENABLED, false)) {
                returnBarcodeImagePath = true
            }
        }
    }

    /**
     * Lock display to current orientation.
     */
    protected open fun lockOrientation() {
        // Only get the orientation if it's not locked to one yet.
        if (orientationLock == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            // Adapted from http://stackoverflow.com/a/14565436
            val display = activity.windowManager.defaultDisplay
            val rotation = display.rotation
            val baseOrientation = activity.resources.configuration.orientation
            var orientation = 0
            if (baseOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                orientation = if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                }
            } else if (baseOrientation == Configuration.ORIENTATION_PORTRAIT) {
                orientation = if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_270) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                }
            }
            orientationLock = orientation
        }
        activity.requestedOrientation = orientationLock
    }

    /**
     * Start decoding.
     */
    fun decode() {
        barcodeView.decodeSingle(callback)
    }

    /**
     * Call from Activity#onResume().
     */
    fun onResume() {
        if (Build.VERSION.SDK_INT >= 23) {
            openCameraWithPermission()
        } else {
            barcodeView.resume()
        }
        inactivityTimer.start()
    }

    private var askedPermission = false

    @TargetApi(23)
    private fun openCameraWithPermission() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            barcodeView.resume()
        } else if (!askedPermission) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA),
                    cameraPermissionReqCode)
            askedPermission = true
        } // else wait for permission result
    }

    /**
     * Call from Activity#onRequestPermissionsResult
     * @param requestCode The request code passed in [androidx.core.app.ActivityCompat.requestPermissions].
     * @param permissions The requested permissions.
     * @param grantResults The grant results for the corresponding permissions
     * which is either [android.content.pm.PackageManager.PERMISSION_GRANTED]
     * or [android.content.pm.PackageManager.PERMISSION_DENIED]. Never null.
     */
    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == cameraPermissionReqCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
                barcodeView.resume()
            } else {
                setMissingCameraPermissionResult()
                if (showDialogIfMissingCameraPermission) {
                    displayFrameworkBugMessageAndExit(missingCameraPermissionDialogMessage)
                } else {
                    closeAndFinish()
                }
            }
        }
    }

    /**
     * Call from Activity#onPause().
     */
    fun onPause() {
        inactivityTimer.cancel()
        barcodeView.pauseAndWait()
    }

    /**
     * Call from Activity#onDestroy().
     */
    fun onDestroy() {
        destroyed = true
        inactivityTimer.cancel()
        handler.removeCallbacksAndMessages(null)
    }

    /**
     * Call from Activity#onSaveInstanceState().
     */
    fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(SAVED_ORIENTATION_LOCK, orientationLock)
    }

    /**
     * Save the barcode image to a temporary file stored in the application's cache, and return its path.
     * Only does so if returnBarcodeImagePath is enabled.
     *
     * @param rawResult the BarcodeResult, must not be null
     * @return the path or null
     */
    private fun getBarcodeImagePath(rawResult: BarcodeResult): String? {
        var barcodeImagePath: String? = null
        if (returnBarcodeImagePath) {
            val bmp = rawResult.bitmap
            try {
                val bitmapFile = File.createTempFile("barcodeimage", ".jpg", activity.cacheDir)
                val outputStream = FileOutputStream(bitmapFile)
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.close()
                barcodeImagePath = bitmapFile.absolutePath
            } catch (e: IOException) {
                Log.w(TAG, "Unable to create temporary file and store bitmap! $e")
            }
        }
        return barcodeImagePath
    }

    private fun finish() {
        activity.finish()
    }

    protected open fun closeAndFinish() {
        if (barcodeView.barcodeView.isCameraClosed) {
            finish()
        } else {
            finishWhenClosed = true
        }
        barcodeView.pause()
        inactivityTimer.cancel()
    }

    private fun setMissingCameraPermissionResult() {
        val intent = Intent(Intents.Scan.ACTION)
        intent.putExtra(Intents.Scan.MISSING_CAMERA_PERMISSION, true)
        activity.setResult(Activity.RESULT_CANCELED, intent)
    }

    protected open fun returnResultTimeout() {
        val intent = Intent(Intents.Scan.ACTION)
        intent.putExtra(Intents.Scan.TIMEOUT, true)
        activity.setResult(Activity.RESULT_CANCELED, intent)
        closeAndFinish()
    }

    protected open fun returnResult(rawResult: BarcodeResult) {
        val intent = resultIntent(rawResult, getBarcodeImagePath(rawResult))
        activity.setResult(Activity.RESULT_OK, intent)
        closeAndFinish()
    }

    protected open fun displayFrameworkBugMessageAndExit(message: String) {
        var messageTemp = message
        if (activity.isFinishing || destroyed || finishWhenClosed) {
            return
        }
        if (messageTemp.isEmpty()) {
            messageTemp = activity.getString(R.string.zxing_msg_camera_framework_bug)
        }
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(activity.getString(R.string.zxing_app_name))
        builder.setMessage(messageTemp)
        builder.setPositiveButton(R.string.zxing_button_ok) { _: DialogInterface?, _: Int -> finish() }
        builder.setOnCancelListener { finish() }
        builder.show()
    }

    /**
     * If set to true, shows the default error dialog if camera permission is missing.
     *
     *
     * If set to false, instead the capture manager just finishes.
     *
     *
     * In both cases, the activity result is set to [Intents.Scan.MISSING_CAMERA_PERMISSION]
     * and cancelled
     */
    fun setShowMissingCameraPermissionDialog(visible: Boolean) {
        setShowMissingCameraPermissionDialog(visible, String())
    }

    /**
     * If set to true, shows the specified error dialog message if camera permission is missing.
     *
     *
     * If set to false, instead the capture manager just finishes.
     *
     *
     * In both cases, the activity result is set to [Intents.Scan.MISSING_CAMERA_PERMISSION]
     * and cancelled
     */
    fun setShowMissingCameraPermissionDialog(visible: Boolean, message: String?) {
        showDialogIfMissingCameraPermission = visible
        missingCameraPermissionDialogMessage = message ?: String()
    }

    companion object {
        private val TAG = CaptureManager::class.java.simpleName
        var cameraPermissionReqCode = 250
        private const val SAVED_ORIENTATION_LOCK = "SAVED_ORIENTATION_LOCK"

        /**
         * Create a intent to return as the Activity result.
         *
         * @param rawResult the BarcodeResult, must not be null.
         * @param barcodeImagePath a path to an exported file of the Barcode Image, can be null.
         * @return the Intent
         */
        fun resultIntent(rawResult: BarcodeResult, barcodeImagePath: String?): Intent {
            val intent = Intent(Intents.Scan.ACTION)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
            intent.putExtra(Intents.Scan.RESULT, rawResult.toString())
            intent.putExtra(Intents.Scan.RESULT_FORMAT, rawResult.barcodeFormat.toString())
            val rawBytes = rawResult.rawBytes
            if (rawBytes.isNotEmpty()) {
                intent.putExtra(Intents.Scan.RESULT_BYTES, rawBytes)
            }

            val metadata = rawResult.resultMetadata
            metadata.let { data ->
                if (data.containsKey(ResultMetadataType.UPC_EAN_EXTENSION)) {
                    intent.putExtra(Intents.Scan.RESULT_UPC_EAN_EXTENSION,
                            data[ResultMetadataType.UPC_EAN_EXTENSION].toString())
                }
                val orientation = data[ResultMetadataType.ORIENTATION] as Number?
                orientation?.let { intent.putExtra(Intents.Scan.RESULT_ORIENTATION, it) }

                val ecLevel = data[ResultMetadataType.ERROR_CORRECTION_LEVEL] as String?
                ecLevel?.let { intent.putExtra(Intents.Scan.RESULT_ERROR_CORRECTION_LEVEL, ecLevel) }

                val byteSegments = data[ResultMetadataType.BYTE_SEGMENTS] as Iterable<ByteArray>?
                byteSegments?.let {
                    for ((i, byteSegment) in it.withIndex()) {
                        intent.putExtra(Intents.Scan.RESULT_BYTE_SEGMENTS_PREFIX + i, byteSegment)
                    }
                }
            }

            barcodeImagePath?.let {
                intent.putExtra(Intents.Scan.RESULT_BARCODE_IMAGE_PATH, barcodeImagePath)
            }
            return intent
        }
    }
}