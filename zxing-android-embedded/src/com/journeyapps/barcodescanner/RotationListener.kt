package com.journeyapps.barcodescanner

import android.content.Context
import android.hardware.SensorManager
import android.view.OrientationEventListener
import android.view.WindowManager

/**
 * Hack to detect when screen rotation is reversed, since that does not cause a configuration change.
 *
 * If it is changed through something other than the sensor (e.g. programmatically), this may not work.
 *
 * See http://stackoverflow.com/q/9909037
 */
class RotationListener {
    private var lastRotation = 0
    private var windowManager: WindowManager? = null
    private var orientationEventListener: OrientationEventListener? = null
    private var callback: RotationCallback? = null
    fun listen(con: Context, callback: RotationCallback?) {
        // Stop to make sure we're not registering the listening twice.
        var context = con
        stop()

        // Only use the ApplicationContext. In case of a memory leak (e.g. from a framework bug),
        // this will result in less being leaked.
        context = context.applicationContext
        this.callback = callback
        windowManager = context
                .getSystemService(Context.WINDOW_SERVICE) as WindowManager
        orientationEventListener = object : OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
            override fun onOrientationChanged(orientation: Int) {
                val localWindowManager = windowManager
                val localCallback = this@RotationListener.callback
                if (windowManager != null && localCallback != null) {
                    val newRotation = localWindowManager!!.defaultDisplay.rotation
                    if (newRotation != lastRotation) {
                        lastRotation = newRotation
                        localCallback.onRotationChanged(newRotation)
                    }
                }
            }
        }
        orientationEventListener?.enable()
        lastRotation = windowManager?.defaultDisplay?.rotation ?: lastRotation
    }

    fun stop() {
        // To reduce the effect of possible leaks, we clear any references we have to external
        // objects.
        orientationEventListener?.disable()
        orientationEventListener = null
        windowManager = null
        callback = null
    }
}