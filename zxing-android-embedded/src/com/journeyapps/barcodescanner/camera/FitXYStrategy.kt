package com.journeyapps.barcodescanner.camera

import android.graphics.Rect
import com.journeyapps.barcodescanner.Size

/**
 * Scales the size so that it fits exactly. Aspect ratio is NOT preserved.
 */
class FitXYStrategy : PreviewScalingStrategy() {
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
        val scaleX = absRatio(size.width * 1.0f / desired.width)
        val scaleY = absRatio(size.height * 1.0f / desired.height)
        val scaleScore = 1.0f / scaleX / scaleY
        val distortion = absRatio(1.0f * size.width / size.height / (1.0f * desired.width / desired.height))

        // Distortion is bad!
        val distortionScore = 1.0f / distortion / distortion / distortion
        return scaleScore * distortionScore
    }

    /**
     * Scale the preview to match the viewfinder exactly.
     *
     * @param previewSize the size of the preview (camera), in current display orientation
     * @param viewfinderSize the size of the viewfinder (display), in current display orientation
     * @return a rect placing the preview
     */
    override fun scalePreview(previewSize: Size, viewfinderSize: Size): Rect {
        return Rect(0, 0, viewfinderSize.width, viewfinderSize.height)
    }

    companion object {
        private val TAG = FitXYStrategy::class.java.simpleName
        private fun absRatio(ratio: Float): Float {
            return if (ratio < 1.0f) {
                1.0f / ratio
            } else {
                ratio
            }
        }
    }
}