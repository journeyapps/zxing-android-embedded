package com.journeyapps.barcodescanner.camera;

import com.journeyapps.barcodescanner.SourceData;

/**
 * Callback for camera previews.
 */
public interface PreviewCallback {
    void onPreview(SourceData sourceData);
}
