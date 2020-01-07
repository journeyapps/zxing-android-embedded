package com.journeyapps.barcodescanner.camera;

import android.graphics.Rect;

import com.journeyapps.barcodescanner.Size;

/**
 * Scales the size so that it fits exactly. Aspect ratio is NOT preserved.
 */
public class FitXYStrategy extends PreviewScalingStrategy {
    private static final String TAG = FitXYStrategy.class.getSimpleName();


    private static float absRatio(float ratio) {
        if (ratio < 1.0f) {
            return 1.0f / ratio;
        } else {
            return ratio;
        }
    }

    /**
     * Get a score for our size.
     *
     * Based on heuristics for penalizing scaling and cropping.
     *
     * 1.0 is perfect (exact match).
     * 0.0 means we can't use it at all.
     *
     * @param size the camera preview size (that can be scaled)
     * @param desired the viewfinder size
     * @return the score
     */
    @Override
    protected float getScore(Size size, Size desired) {
        if (size.width <= 0 || size.height <= 0) {
            return 0f;
        }
        float scaleX = absRatio(size.width * 1.0f / desired.width);
        float scaleY = absRatio(size.height * 1.0f / desired.height);

        float scaleScore = 1.0f / scaleX / scaleY;

        float distortion = absRatio((1.0f * size.width / size.height) / (1.0f * desired.width / desired.height));

        // Distortion is bad!
        float distortionScore = 1.0f / distortion / distortion / distortion;

        return scaleScore * distortionScore;
    }

    /**
     * Scale the preview to match the viewfinder exactly.
     *
     * @param previewSize the size of the preview (camera), in current display orientation
     * @param viewfinderSize the size of the viewfinder (display), in current display orientation
     * @return a rect placing the preview
     */
    public Rect scalePreview(Size previewSize, Size viewfinderSize) {
        return new Rect(0, 0, viewfinderSize.width, viewfinderSize.height);
    }
}
