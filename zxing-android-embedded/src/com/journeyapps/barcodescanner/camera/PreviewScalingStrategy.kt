package com.journeyapps.barcodescanner.camera

import android.graphics.Rect
import android.util.Log
import com.journeyapps.barcodescanner.Size
import java.util.*

/**
 *
 */
abstract class PreviewScalingStrategy {
    /**
     * Choose the best preview size, based on our viewfinder size.
     *
     * The default implementation lets subclasses calculate a score for each size, the picks the one
     * with the best score.
     *
     * The sizes list may be reordered by this call.
     *
     * @param sizes supported preview sizes, containing at least one size. Sizes are in natural camera orientation.
     * @param desired The desired viewfinder size, in the same orientation
     * @return the best preview size, never null
     */
    open fun getBestPreviewSize(sizes: List<Size>, desired: Size): Size {
        // Sample of supported preview sizes:
        // http://www.kirill.org/ar/ar.php
        val ordered = getBestPreviewOrder(sizes, desired)
        Log.i(TAG, "Viewfinder size: $desired")
        Log.i(TAG, "Preview in order of preference: $ordered")
        return ordered[0]
    }

    /**
     * Sort previews based on their suitability.
     *
     * In most cases, [.getBestPreviewSize] should be used instead.
     *
     * The sizes list may be reordered by this call.
     *
     * @param sizes supported preview sizes, containing at least one size. Sizes are in natural camera orientation.
     * @param desired The desired viewfinder size, in the same orientation
     * @return an ordered list, best preview first
     */
    fun getBestPreviewOrder(sizes: List<Size>, desired: Size): List<Size> {
        Collections.sort(sizes) { a, b ->
            val aScore = getScore(a, desired)
            val bScore = getScore(b, desired)
            // Bigger score first
            bScore.compareTo(aScore)
        }
        return sizes
    }

    /**
     * Get a score for our size.
     *
     * 1.0 is perfect (exact match).
     * 0.0 means we can't use it at all.
     *
     * Subclasses should override this.
     *
     * @param size the camera preview size (that can be scaled)
     * @param desired the viewfinder size
     * @return the score
     */
    protected open fun getScore(size: Size, desired: Size): Float {
        return 0.5f
    }

    /**
     * Scale and position the preview relative to the viewfinder.
     *
     * @param previewSize the size of the preview (camera), in current display orientation
     * @param viewfinderSize the size of the viewfinder (display), in current display orientation
     * @return a rect placing the preview, relative to the viewfinder
     */
    abstract fun scalePreview(previewSize: Size, viewfinderSize: Size): Rect

    companion object {
        private val TAG = PreviewScalingStrategy::class.java.simpleName
    }
}