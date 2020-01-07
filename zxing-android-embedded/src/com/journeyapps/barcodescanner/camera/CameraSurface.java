package com.journeyapps.barcodescanner.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.view.SurfaceHolder;

import java.io.IOException;

/**
 * A surface on which a camera preview is displayed.
 *
 * This wraps either a SurfaceHolder or a SurfaceTexture.
 */
public class CameraSurface {
    private SurfaceHolder surfaceHolder;
    private SurfaceTexture surfaceTexture;

    public CameraSurface(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalArgumentException("surfaceHolder may not be null");
        }
        this.surfaceHolder = surfaceHolder;
    }

    public CameraSurface(SurfaceTexture surfaceTexture) {
        if (surfaceTexture == null) {
            throw new IllegalArgumentException("surfaceTexture may not be null");
        }
        this.surfaceTexture = surfaceTexture;
    }

    public SurfaceHolder getSurfaceHolder() {
        return surfaceHolder;
    }

    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }

    public void setPreview(Camera camera) throws IOException {
        if (surfaceHolder != null) {
            camera.setPreviewDisplay(surfaceHolder);
        } else {
            camera.setPreviewTexture(surfaceTexture);
        }
    }
}
