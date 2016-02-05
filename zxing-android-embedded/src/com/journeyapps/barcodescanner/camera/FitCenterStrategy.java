package com.journeyapps.barcodescanner.camera;

import android.graphics.Rect;
import android.util.Log;

import com.journeyapps.barcodescanner.Size;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Scales the size so that both dimensions will be greater than or equal to the corresponding
 * dimension of the parent. One of width or height will fit exactly. Aspect ratio is preserved.
 */
public class FitCenterStrategy implements PreviewScalingStrategy {
    private static final String TAG = FitCenterStrategy.class.getSimpleName();


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
        Size scaled = size.scaleFit(desired);
        // Scaling preserves aspect ratio
        float scaleRatio = scaled.width * 1.0f / size.width;

        // Treat downscaling as slightly better than upscaling
        float scaleScore;
        if(scaleRatio > 1.0f) {
            // Upscaling
            scaleScore = 1.0f / scaleRatio * 0.9f;
        } else {
            // Downscaling
            scaleScore = scaleRatio;
        }

        // Ratio of scaledDimension / dimension.
        // Note that with scaleCrop, only one dimension is cropped.
        float cropRatio = desired.width * 1.0f / scaled.width +
                desired.height * 1.0f / scaled.height;

        // Cropping is very bad, since it's used-visible for centerFit
        // 1.0 means no cropping.
        float cropScore = 1.0f / cropRatio / cropRatio / cropRatio;

        return scaleScore * cropScore;
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
     * Scale the preview to cover the viewfinder, then center it.
     *
     * Aspect ratio is preserved.
     *
     * @param previewSize the size of the preview (camera), in current display orientation
     * @param viewfinderSize the size of the viewfinder (display), in current display orientation
     * @return a rect placing the preview
     */
    public Rect scalePreview(Size previewSize, Size viewfinderSize) {
        // We avoid scaling if feasible.
        Size scaledPreview = previewSize.scaleFit(viewfinderSize);
        Log.i(TAG, "Preview: " + previewSize + "; Scaled: " + scaledPreview + "; Want: " + viewfinderSize);

        int dx = (scaledPreview.width - viewfinderSize.width) / 2;
        int dy = (scaledPreview.height - viewfinderSize.height) / 2;

        return new Rect(-dx, -dy, scaledPreview.width - dx, scaledPreview.height - dy);
    }

}
