package com.journeyapps.barcodescanner.camera;

import android.graphics.Rect;

import com.journeyapps.barcodescanner.Size;

import java.util.List;

/**
 *
 */
public interface PreviewScalingStrategy {

    /**
     * Choose the best preview size, based on our viewfinder size.
     *
     * @param sizes supported preview sizes, containing at least one size. Sizes are in natural camera orientation.
     * @param desired The desired viewfinder size, in the same orientation
     * @return the best preview size, never null
     */
    Size getBestPreviewSize(List<Size> sizes, final Size desired);


    /**
     * Scale and position the preview relative to the viewfinder.
     *
     * @param previewSize the size of the preview (camera), in current display orientation
     * @param viewfinderSize the size of the viewfinder (display), in current display orientation
     * @return a rect placing the preview, relative to the viewfinder
     */
    Rect scalePreview(Size previewSize, Size viewfinderSize);
}
