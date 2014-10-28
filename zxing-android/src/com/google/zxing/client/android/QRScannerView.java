package com.google.zxing.client.android;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.google.zxing.client.android.camera.CameraManager;

/**
 * Created by hans.reichenbach on 9/30/14.
 *
 * Largely copied from Android SDK sample API 10 APIDemos' CameraPreview class. This class is
 * designed to be used as a SurfaceView for holding the Preview in situations where the size of
 * the surface view may be smaller than the preview size. It will center the preview instead of
 * degrading preview resolution so that the actual QR scanning functionality isn't impacted.
 */
public class QRScannerView extends ViewGroup {
    private static final String TAG = QRScannerView.class.getSimpleName();

    SurfaceView mSurfaceView;
    Camera.Size mPreviewSize;
    CameraManager mCameraManager;

    public QRScannerView(Context context) {
        super(context);

        init(context);
    }

    public QRScannerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public QRScannerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init(context);
    }

    private void init(Context context) {
        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);
    }

    public SurfaceHolder getHolder() {
        return mSurfaceView.getHolder();
    }

    public void setCameraManager(CameraManager manager) {
        mCameraManager = manager;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //disregard any child measurements because we don't care how big the preview is, we just
        //want to center it
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if(mCameraManager != null) {
            mPreviewSize = mCameraManager.getPreviewSize();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if(changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }

            // Center the child SurfaceView within the parent.
            //TODO doesn't actually center and prevent scaling like I want. Need to fix this.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            }

        }
    }
}
