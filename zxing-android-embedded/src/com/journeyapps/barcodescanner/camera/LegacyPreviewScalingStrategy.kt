package com.journeyapps.barcodescanner.camera

import android.graphics.Rect
import android.util.Log
import com.journeyapps.barcodescanner.Size
import java.util.*

/**
 *
 */
class LegacyPreviewScalingStrategy : PreviewScalingStrategy() {
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
    override fun getBestPreviewSize(sizes: List<Size>, desired: Size): Size {
        // Sample of supported preview sizes:
        // http://www.kirill.org/ar/ar.php
        Collections.sort(sizes) { a, b ->
            val ascaled = scale(a, desired)
            val aScale = ascaled.width - a.width
            val bscaled = scale(b, desired)
            val bScale = bscaled.width - b.width
            if (aScale == 0 && bScale == 0) {
                // Both no scaling, pick the smaller one
                a.compareTo(b)
            } else if (aScale == 0) {
                // No scaling for a; pick a
                -1
            } else if (bScale == 0) {
                // No scaling for b; pick b
                1
            } else if (aScale < 0 && bScale < 0) {
                // Both downscaled. Pick the smaller one (less downscaling).
                a.compareTo(b)
            } else if (aScale > 0 && bScale > 0) {
                // Both upscaled. Pick the larger one (less upscaling).
                -a.compareTo(b)
            } else if (aScale < 0) {
                // a downscaled, b upscaled. Pick a.
                -1
            } else {
                // a upscaled, b downscaled. Pick b.
                1
            }
        }
        Log.i(TAG, "Viewfinder size: $desired")
        Log.i(TAG, "Preview in order of preference: $sizes")
        return sizes[0]
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
    override fun scalePreview(previewSize: Size, viewfinderSize: Size): Rect {
        // We avoid scaling if feasible.
        val scaledPreview = scale(previewSize, viewfinderSize)
        Log.i(TAG, "Preview: $previewSize; Scaled: $scaledPreview; Want: $viewfinderSize")
        val dx = (scaledPreview.width - viewfinderSize.width) / 2
        val dy = (scaledPreview.height - viewfinderSize.height) / 2
        return Rect(-dx, -dy, scaledPreview.width - dx, scaledPreview.height - dy)
    }

    companion object {
        private val TAG = LegacyPreviewScalingStrategy::class.java.simpleName

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
        fun scale(from: Size, to: Size): Size {
            var current = from
            if (!to.fitsIn(current)) {
                // Scale up
                while (true) {
                    val scaled150 = current.scale(3, 2)
                    val scaled200 = current.scale(2, 1)
                    current = when {
                        to.fitsIn(scaled150) -> {
                            // Scale by 3/2
                            return scaled150
                        }
                        to.fitsIn(scaled200) -> {
                            // Scale by 2/1
                            return scaled200
                        }
                        else -> {
                            // Scale by 2/1 and continue
                            scaled200
                        }
                    }
                }
            } else {
                // Scale down
                while (true) {
                    val scaled66 = current.scale(2, 3)
                    val scaled50 = current.scale(1, 2)
                    current = if (!to.fitsIn(scaled50)) {
                        return if (to.fitsIn(scaled66)) {
                            // Scale by 2/3
                            scaled66
                        } else {
                            // No more downscaling
                            current
                        }
                    } else {
                        // Scale by 1/2
                        scaled50
                    }
                }
            }
        }
    }
}