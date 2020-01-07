package com.journeyapps.barcodescanner;

import android.content.Context;
import android.hardware.SensorManager;
import android.view.OrientationEventListener;
import android.view.WindowManager;

/**
 * Hack to detect when screen rotation is reversed, since that does not cause a configuration change.
 *
 * If it is changed through something other than the sensor (e.g. programmatically), this may not work.
 *
 * See http://stackoverflow.com/q/9909037
 */
public class RotationListener {
    private int lastRotation;

    private WindowManager windowManager;
    private OrientationEventListener orientationEventListener;
    private RotationCallback callback;

    public RotationListener() {
    }

    public void listen(Context context, RotationCallback callback) {
        // Stop to make sure we're not registering the listening twice.
        stop();

        // Only use the ApplicationContext. In case of a memory leak (e.g. from a framework bug),
        // this will result in less being leaked.
        context = context.getApplicationContext();

        this.callback = callback;

        this.windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);

        this.orientationEventListener = new OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                WindowManager localWindowManager = windowManager;
                RotationCallback localCallback = RotationListener.this.callback;
                if (windowManager != null && localCallback != null) {
                    int newRotation = localWindowManager.getDefaultDisplay().getRotation();
                    if (newRotation != lastRotation) {
                        lastRotation = newRotation;
                        localCallback.onRotationChanged(newRotation);
                    }
                }
            }
        };
        this.orientationEventListener.enable();

        lastRotation = windowManager.getDefaultDisplay().getRotation();
    }

    public void stop() {
        // To reduce the effect of possible leaks, we clear any references we have to external
        // objects.
        if (this.orientationEventListener != null) {
            this.orientationEventListener.disable();
        }
        this.orientationEventListener = null;
        this.windowManager = null;
        this.callback = null;
    }
}
