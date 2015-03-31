package com.journeyapps.barcodescanner.camera;

import android.graphics.Point;
import android.view.Surface;

/**
 *
 */
public class DisplayConfiguration {
  private Point desiredPreviewSize;
  private int rotation;

  public DisplayConfiguration(int rotation) {
    this.rotation = rotation;
  }

  public DisplayConfiguration(int rotation, Point desiredPreviewSize) {
    this.rotation = rotation;
    this.desiredPreviewSize = desiredPreviewSize;
  }

  public int getRotation() {
    return rotation;
  }

  public Point getDesiredPreviewSize() {
    return desiredPreviewSize;
  }

  public boolean isRotated() {
    switch (rotation) {
      case Surface.ROTATION_0:
      case Surface.ROTATION_180:
        return true;
      case Surface.ROTATION_90:
      case Surface.ROTATION_270:
        return false;
    }
    return false;
  }

  /**
   * @return desired preview size in landscape orientation.
   */
  public Point getDesiredLandscapePreviewSize() {
    if(desiredPreviewSize == null) {
      return null;
    } else if(isRotated()) {
      //noinspection SuspiciousNameCombination
      return new Point(desiredPreviewSize.y, desiredPreviewSize.x);
    } else {
      return desiredPreviewSize;
    }
  }
}
