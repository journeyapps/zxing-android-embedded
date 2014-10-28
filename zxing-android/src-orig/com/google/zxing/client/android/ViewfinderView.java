/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android;

import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {
    private static final String TAG = ViewfinderView.class.getSimpleName();

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private static final long ANIMATION_DELAY = 80L;
    private static final int MAX_RESULT_POINTS = 20;
    private static final int POINT_SIZE = 6;

    private CameraManager cameraManager;
    private final Paint paint;
    private Bitmap resultBitmap;
    private final int maskColor;
    private final int resultColor;
    private final int laserColor;
    private final int resultPointColor;
    private int scannerAlpha;
    private List<ResultPoint> possibleResultPoints;
    private List<ResultPoint> lastPossibleResultPoints;

    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Resources resources = getResources();
        maskColor = resources.getColor(R.color.zxing_viewfinder_mask);
        resultColor = resources.getColor(R.color.zxing_result_view);
        laserColor = resources.getColor(R.color.zxing_viewfinder_laser);
        resultPointColor = resources.getColor(R.color.zxing_possible_result_points);
        scannerAlpha = 0;
        possibleResultPoints = new ArrayList<>(5);
        lastPossibleResultPoints = null;
    }

    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        if (cameraManager == null) {
            return; // not ready yet, early draw before done configuring
        }
        Rect frame = cameraManager.getFramingRect();
        Rect previewFrame = cameraManager.getFramingRectInPreview();
        if (frame == null || previewFrame == null) {
            return;
        }

        // Draw the exterior (i.e. outside the framing rect) darkened
        if(cameraManager.isFrameEnabled()) {
            drawFramingRect(canvas, previewFrame, canvas.getWidth(), canvas.getHeight());
        }

        // if we have a result draw the overlay (assuming it's enabled)
        if (resultBitmap != null && cameraManager.isOverlayEnabled()) {
            drawResultOverlay(canvas, previewFrame);
        } else {
            if(cameraManager.isScannerLineEnabled()) {
                drawScannerLine(canvas, previewFrame);
            }

            if(cameraManager.isPotentialIndicatorsEnabled()) {
                drawPotentialIndicators(frame, previewFrame, canvas);
            }

            // Request another update at the animation interval, but only repaint the laser line,
            // not the entire viewfinder mask.
            postInvalidateDelayed(ANIMATION_DELAY,
                    frame.left - POINT_SIZE,
                    frame.top - POINT_SIZE,
                    frame.right + POINT_SIZE,
                    frame.bottom + POINT_SIZE);
        }
    }

    public void drawViewfinder() {
        Bitmap resultBitmap = this.resultBitmap;
        this.resultBitmap = null;
        if (resultBitmap != null) {
            resultBitmap.recycle();
        }
        invalidate();
    }

    /*
     * Draw the framing rect for helping the user target the scanner code
     */
    private void drawFramingRect(Canvas canvas, Rect previewFrame, int width, int height) {
        paint.setColor(resultBitmap != null ? resultColor : maskColor);
        canvas.drawRect(0, 0, width, previewFrame.top, paint);
        canvas.drawRect(0, previewFrame.top, previewFrame.left, previewFrame.bottom + 1, paint);
        canvas.drawRect(previewFrame.right + 1, previewFrame.top, width,
                previewFrame.bottom + 1, paint);
        canvas.drawRect(0, previewFrame.bottom + 1, width, height, paint);
    }

    /*
     * draw the image of the scanned result over the camera preview
     */
    private void drawResultOverlay(Canvas canvas, Rect previewFrame) {
        // Draw the opaque result bitmap over the scanning rectangle
        paint.setAlpha(cameraManager.getOverlayOpacity());

        if(cameraManager.getCurrentOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            //need to rotate the image for portrait mode
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            //source, x, y, width, height, filterMatrix, shouldBeFiltered
            Bitmap rotatedResult = Bitmap.createBitmap(resultBitmap, 0, 0, resultBitmap.getWidth(),
                    resultBitmap.getHeight(), matrix, true);

            canvas.drawBitmap(rotatedResult, null, previewFrame, paint);
        } else {
            //no changes needed for a landscape orientation
            canvas.drawBitmap(resultBitmap, null, previewFrame, paint);
        }
    }

    /*
     * Draw a scanner line over the camera preview
     */
    private void drawScannerLine(Canvas canvas, Rect previewFrame) {
        paint.setColor(laserColor);
        paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
        scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
        int middle = previewFrame.height() / 2 + previewFrame.top;
        canvas.drawRect(previewFrame.left + 2, middle - 1, previewFrame.right - 1,
                middle + 2, paint);
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    public void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        List<ResultPoint> points = possibleResultPoints;

        synchronized (possibleResultPoints) {
            points.add(point);
            int size = points.size();
            if (size > MAX_RESULT_POINTS) {
                // trim it
                points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
            }
        }
    }

    private void drawPotentialIndicators(Rect previewSize, Rect displaySize, Canvas canvas) {
        /*
         * Draw dots on result points
         */
        List<ResultPoint> currentPossible = possibleResultPoints;
        List<ResultPoint> currentLast = lastPossibleResultPoints;

        if (currentPossible.isEmpty()) {
            lastPossibleResultPoints = null;
        } else {
            possibleResultPoints = new ArrayList<>(5);
            lastPossibleResultPoints = currentPossible;
            paint.setAlpha(cameraManager.getOverlayOpacity());
            paint.setColor(resultPointColor);

            synchronized (currentPossible) {
                for (ResultPoint point : currentPossible) {
                    drawPotentialPoint(canvas, paint, POINT_SIZE, point, previewSize, displaySize,
                            cameraManager.getCurrentOrientation());
                }
            }
        }
        if (currentLast != null) {
            paint.setAlpha(cameraManager.getOverlayOpacity() / 2);
            paint.setColor(resultPointColor);
            synchronized (currentLast) {
                float radius = POINT_SIZE / 2.0f;
                for (ResultPoint point : currentLast) {
                    drawPotentialPoint(canvas, paint, radius, point, previewSize, displaySize,
                            cameraManager.getCurrentOrientation());
                }
            }
        }
    }

    private void drawPotentialPoint(Canvas canvas, Paint paint, float pointRadius, ResultPoint point,
                                    Rect previewSize, Rect displaySize, int orientation) {
        boolean isPortrait = orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

        int frameLeft = displaySize.left;
        int frameTop = displaySize.top;
        float scaleX;
        float scaleY;

        //get the proper scaling depending on the preview image orientation
        //TODO not sure of the portrait scales
        if(isPortrait) {
            scaleX = (float) displaySize.height() / previewSize.width();
            scaleY = (float) displaySize.width() / previewSize.height();
        } else {
            scaleX = previewSize.width() / (float) displaySize.width();
            scaleY = previewSize.height() / (float) displaySize.height();
        }

        if(isPortrait) {
            //TODO I think this is right but it's really hard to test
            canvas.drawCircle(frameLeft + (int) (point.getY() * scaleX),
                    frameTop + (int) ((previewSize.width() - point.getX()) * scaleY),
                    pointRadius, paint);
        } else {
            canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                    frameTop + (int) (point.getY() * scaleY),
                    pointRadius, paint);
        }
    }

}
