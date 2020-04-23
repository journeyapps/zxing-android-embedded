package com.journeyapps.barcodescanner.camera

import android.graphics.Rect
import android.util.Log
import com.journeyapps.barcodescanner.Size
import kotlin.math.pow

/**
 * Scales the size so that both dimensions will be greater than or equal to the corresponding
 * dimension of the parent. One of width or height will fit exactly. Aspect ratio is preserved.
 */
class FitCenterStrategy : PreviewScalingStrategy() {
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
    override fun getScore(size: Size, desired: Size): Float {
        if (size.width <= 0 || size.height <= 0) {
            return 0f
        }
        val scaled = size.scaleFit(desired)
        // Scaling preserves aspect ratio
        val scaleRatio = scaled.width * 1.0f / size.width

        // Treat downscaling as slightly better than upscaling
        val scaleScore: Float
        scaleScore = if (scaleRatio > 1.0f) {
            // Upscaling
            (1.0f / scaleRatio.toDouble()).pow(1.1).toFloat()
        } else {
            // Downscaling
            scaleRatio
        }

        // Ratio of scaledDimension / dimension.
        // Note that with scaleCrop, only one dimension is cropped.
        val cropRatio = desired.width * 1.0f / scaled.width *
                (desired.height * 1.0f / scaled.height)

        // Cropping is very bad, since it's used-visible for centerFit
        // 1.0 means no cropping.
        val cropScore = 1.0f / cropRatio / cropRatio / cropRatio
        return scaleScore * cropScore
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
        val scaledPreview = previewSize.scaleFit(viewfinderSize)
        Log.i(TAG, "Preview: $previewSize; Scaled: $scaledPreview; Want: $viewfinderSize")
        val dx = (scaledPreview.width - viewfinderSize.width) / 2
        val dy = (scaledPreview.height - viewfinderSize.height) / 2
        return Rect(-dx, -dy, scaledPreview.width - dx, scaledPreview.height - dy)
    }

    companion object {
        private val TAG = FitCenterStrategy::class.java.simpleName
    }
}