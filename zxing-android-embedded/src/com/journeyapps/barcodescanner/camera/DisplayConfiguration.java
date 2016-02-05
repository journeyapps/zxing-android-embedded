package com.journeyapps.barcodescanner.camera;

import android.graphics.Rect;

import com.journeyapps.barcodescanner.Size;

import java.util.List;

/**
 *
 */
public class DisplayConfiguration {
    private static final String TAG = DisplayConfiguration.class.getSimpleName();

    private Size viewfinderSize;
    private int rotation;
    private boolean center = false;
    private PreviewScalingStrategy previewScalingStrategy = new FitCenterStrategy();

    public DisplayConfiguration(int rotation) {
        this.rotation = rotation;
    }

    public DisplayConfiguration(int rotation, Size viewfinderSize) {
        this.rotation = rotation;
        this.viewfinderSize = viewfinderSize;
    }

    public int getRotation() {
        return rotation;
    }

    public Size getViewfinderSize() {
        return viewfinderSize;
    }

    public PreviewScalingStrategy getPreviewScalingStrategy() {
        return previewScalingStrategy;
    }

    public void setPreviewScalingStrategy(PreviewScalingStrategy previewScalingStrategy) {
        this.previewScalingStrategy = previewScalingStrategy;
    }

    /**
     * @param rotate true to rotate the preview size
     * @return desired preview size in natural camera orientation.
     */
    public Size getDesiredPreviewSize(boolean rotate) {
        if (viewfinderSize == null) {
            return null;
        } else if (rotate) {
            return viewfinderSize.rotate();
        } else {
            return viewfinderSize;
        }
    }

    /**
     * Choose the best preview size, based on our display size.
     *
     * We prefer:
     * 1. no scaling
     * 2. least downscaling
     * 3. least upscaling
     *
     * We do not care much about aspect ratio, since we just crop away extra pixels. We only choose
     * the size to minimize scaling.
     *
     * In the future we may consider choosing the biggest possible preview size, to maximize the
     * resolution we have for decoding. We need more testing to see whether or not that is feasible.
     *
     * @param sizes supported preview sizes, containing at least one size. Sizes are in natural camera orientation.
     * @param isRotated true if the camera is rotated perpendicular to the current display orientation
     * @return the best preview size, never null
     */
    public Size getBestPreviewSize(List<Size> sizes, boolean isRotated) {
        // Sample of supported preview sizes:
        // http://www.kirill.org/ar/ar.php


        final Size desired = getDesiredPreviewSize(isRotated);

        return previewScalingStrategy.getBestPreviewSize(sizes, desired);
    }

    /**
     * Scale the preview to cover the viewfinder, then center it.
     *
     * Aspect ratio is preserved.
     *
     * @param previewSize the size of the preview (camera), in current display orientation
     * @return a rect placing the preview
     */
    public Rect scalePreview(Size previewSize) {
        return previewScalingStrategy.scalePreview(previewSize, viewfinderSize);
    }
}
