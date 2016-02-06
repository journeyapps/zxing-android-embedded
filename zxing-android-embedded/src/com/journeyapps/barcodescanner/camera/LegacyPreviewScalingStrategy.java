package com.journeyapps.barcodescanner.camera;

import android.graphics.Rect;
import android.util.Log;

import com.journeyapps.barcodescanner.Size;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 *
 */
public class LegacyPreviewScalingStrategy extends PreviewScalingStrategy {
    private static final String TAG = LegacyPreviewScalingStrategy.class.getSimpleName();

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
                Size ascaled = scale(a, desired);
                int aScale = ascaled.width - a.width;
                Size bscaled = scale(b, desired);
                int bScale = bscaled.width - b.width;

                if (aScale == 0 && bScale == 0) {
                    // Both no scaling, pick the smaller one
                    return a.compareTo(b);
                } else if (aScale == 0) {
                    // No scaling for a; pick a
                    return -1;
                } else if (bScale == 0) {
                    // No scaling for b; pick b
                    return 1;
                } else if (aScale < 0 && bScale < 0) {
                    // Both downscaled. Pick the smaller one (less downscaling).
                    return a.compareTo(b);
                } else if (aScale > 0 && bScale > 0) {
                    // Both upscaled. Pick the larger one (less upscaling).
                    return -a.compareTo(b);
                } else if (aScale < 0) {
                    // a downscaled, b upscaled. Pick a.
                    return -1;
                } else {
                    // a upscaled, b downscaled. Pick b.
                    return 1;
                }
            }
        });

        Log.i(TAG, "Viewfinder size: " + desired);
        Log.i(TAG, "Preview in order of preference: " + sizes);

        return sizes.get(0);
    }



    /**
     * Scale from so that to.fitsIn(size). Tries to scale by powers of two, or by 3/2. Aspect ratio
     * is preserved.
     *
     * These scaling factors will theoretically result in fast scaling with minimal quality loss.
     *
     * TODO: confirm whether or not this is the case in practice.
     *
     * @param from the start size
     * @param to   the minimum desired size
     * @return the scaled size
     */
    public static Size scale(Size from, Size to) {
        Size current = from;

        if (!to.fitsIn(current)) {
            // Scale up
            while (true) {
                Size scaled150 = current.scale(3, 2);
                Size scaled200 = current.scale(2, 1);
                if (to.fitsIn(scaled150)) {
                    // Scale by 3/2
                    return scaled150;
                } else if (to.fitsIn(scaled200)) {
                    // Scale by 2/1
                    return scaled200;
                } else {
                    // Scale by 2/1 and continue
                    current = scaled200;
                }
            }
        } else {
            // Scale down
            while (true) {
                Size scaled66 = current.scale(2, 3);
                Size scaled50 = current.scale(1, 2);

                if (!to.fitsIn(scaled50)) {
                    if (to.fitsIn(scaled66)) {
                        // Scale by 2/3
                        return scaled66;
                    } else {
                        // No more downscaling
                        return current;
                    }
                } else {
                    // Scale by 1/2
                    current = scaled50;
                }
            }
        }
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
        Size scaledPreview = scale(previewSize, viewfinderSize);
        Log.i(TAG, "Preview: " + previewSize + "; Scaled: " + scaledPreview + "; Want: " + viewfinderSize);

        int dx = (scaledPreview.width - viewfinderSize.width) / 2;
        int dy = (scaledPreview.height - viewfinderSize.height) / 2;

        return new Rect(-dx, -dy, scaledPreview.width - dx, scaledPreview.height - dy);
    }

}
