package com.journeyapps.barcodescanner;

import android.content.Context;
import android.hardware.SensorManager;
import android.view.OrientationEventListener;
import android.view.WindowManager;

/**
 * Hack to detect when screen rotation is reversed, since that does not cause a configuration change.
 */
public abstract class RotationListener extends OrientationEventListener {
  private int lastRotation;
  private WindowManager windowManager;

  public RotationListener(Context context) {
    super(context, SensorManager.SENSOR_DELAY_NORMAL);
    this.windowManager = (WindowManager) context
            .getSystemService(Context.WINDOW_SERVICE);

    lastRotation = getRotation();
  }

  @Override
  public void onOrientationChanged(int orientation) {
    int newRotation = getRotation();
    if(newRotation != lastRotation) {
      lastRotation = newRotation;
      onRotationChanged(newRotation);
    }
  }

  public abstract void onRotationChanged(int rotation);

  public int getRotation() {
    return windowManager.getDefaultDisplay().getRotation();
  }
}
