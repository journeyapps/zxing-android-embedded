package com.journeyapps.barcodescanner.camera

import com.journeyapps.barcodescanner.SourceData

/**
 * Callback for camera previews.
 */
interface PreviewCallback {
    fun onPreview(sourceData: SourceData)
    fun onPreviewError(e: Exception?)
}