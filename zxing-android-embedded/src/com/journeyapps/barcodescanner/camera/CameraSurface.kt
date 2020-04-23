package com.journeyapps.barcodescanner.camera

import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.view.SurfaceHolder
import java.io.IOException

/**
 * A surface on which a camera preview is displayed.
 *
 * This wraps either a SurfaceHolder or a SurfaceTexture.
 */
class CameraSurface {
    var surfaceHolder: SurfaceHolder? = null
        private set
    var surfaceTexture: SurfaceTexture? = null
        private set

    constructor(surfaceHolder: SurfaceHolder) {
        this.surfaceHolder = surfaceHolder
    }

    constructor(surfaceTexture: SurfaceTexture) {
        this.surfaceTexture = surfaceTexture
    }

    @Throws(IOException::class)
    fun setPreview(camera: Camera) {
        if (surfaceHolder != null) {
            camera.setPreviewDisplay(surfaceHolder)
        } else {
            camera.setPreviewTexture(surfaceTexture)
        }
    }
}