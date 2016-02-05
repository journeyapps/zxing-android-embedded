package com.journeyapps.barcodescanner.camera;

import android.graphics.Rect;
import android.util.Log;

import com.journeyapps.barcodescanner.Size;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Scales the size so that it fits exactly. Aspect ratio is NOT preserved.
 */
public class FitXYStrategy implements PreviewScalingStrategy {
    private static final String TAG = FitXYStrategy.class.getSimpleName();


    private static float absRatio(float ratio) {
        if(ratio < 1.0f) {
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
    private float getScore(Size size, Size desired) {
        float scaleX = absRatio(size.width * 1.0f / desired.width);
        float scaleY = absRatio(size.height * 1.0f / desired.height);

        float scaleScore = 1.0f / scaleX / scaleY;

        float distortion = absRatio((1.0f * size.width / size.height) / (1.0f * desired.width / desired.height));

        // Distortion is bad!
        float distortionScore = 1.0f / distortion / distortion / distortion;

        return scaleScore * distortionScore;
    }

    /**
     * Choose the best preview size, based on our display size.
     *
     * We prefer:
     * 1. less scaling
     * 2. less cropping
     *
     * @param sizes supported preview sizes, containing at least one size. Sizes are in natural camera orientation.
     * @param desired The desired display size, in the same orientation
     * @return the best preview size, never null
     */
    public Size getBestPreviewSize(List<Size> sizes, final Size desired) {
        // Sample of supported preview sizes:
        // http://www.kirill.org/ar/ar.php

        if (desired == null) {
            return sizes.get(0);
        }

        Collections.sort(sizes, new Comparator<Size>() {
            @Override
            public int compare(Size a, Size b) {
                float aScore = getScore(a, desired);
                float bScore = getScore(b, desired);
                // Bigger score first
                return Float.compare(bScore, aScore);
            }
        });

        Log.i(TAG, "Viewfinder size: " + desired);
        Log.i(TAG, "Preview in order of preference: " + sizes);

        return sizes.get(0);
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
