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
package com.journeyapps.barcodescanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.zxing.ResultPoint
import com.google.zxing.client.android.R
import com.journeyapps.barcodescanner.CameraPreview.StateListener
import java.util.*

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
open class ViewfinderView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    protected open val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    protected open var resultBitmap: Bitmap? = null
    var maskColor: Int
    val resultColor: Int
    val laserColor: Int
    val resultPointColor: Int
    var laserVisibility: Boolean
    var pointVisibility: Boolean
    var scannerAlpha: Int
    var possibleResultPoints: MutableList<ResultPoint>
    var lastPossibleResultPoints: MutableList<ResultPoint>
    var cameraPreview: CameraPreview? = null
        set(value) {
            field = value
            field?.addStateListener(object : StateListener {
                override fun previewSized() {
                    refreshSizes()
                    invalidate()
                }

                override fun previewStarted() {}
                override fun previewStopped() {}
                override fun cameraError(error: Exception?) {}
                override fun cameraClosed() {}
            })
        }

    // Cache the framingRect and previewSize, so that we can still draw it after the preview
    // stopped.
    protected open var framingRect: Rect? = null
    protected var previewSize: Size? = null

    protected fun refreshSizes() {
        if (cameraPreview == null) {
            return
        }
        val framingRect = cameraPreview!!.framingRect
        val previewSize = cameraPreview!!.previewSize
        if (framingRect != null && previewSize != null) {
            this.framingRect = framingRect
            this.previewSize = previewSize
        }
    }

    public override fun onDraw(canvas: Canvas) {
        refreshSizes()
        if (framingRect == null || previewSize == null) {
            return
        }
        val frame: Rect = framingRect!!
        val preview = previewSize!!
        val width = canvas.width
        val height = canvas.height

        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.color = if (resultBitmap != null) resultColor else maskColor
        canvas.drawRect(0f, 0f, width.toFloat(), frame.top.toFloat(), paint)
        canvas.drawRect(0f, frame.top.toFloat(), frame.left.toFloat(), frame.bottom + 1.toFloat(), paint)
        canvas.drawRect(frame.right + 1.toFloat(), frame.top.toFloat(), width.toFloat(), frame.bottom + 1.toFloat(), paint)
        canvas.drawRect(0f, frame.bottom + 1.toFloat(), width.toFloat(), height.toFloat(), paint)

        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            paint.alpha = CURRENT_POINT_OPACITY
            canvas.drawBitmap(resultBitmap!!, null, frame, paint)
        } else {
            // If wanted, draw a red "laser scanner" line through the middle to show decoding is active
            if (laserVisibility) {
                paint.color = laserColor
                paint.alpha = SCANNER_ALPHA[scannerAlpha]
                scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.size

                val middle = frame.height() / 2 + frame.top
                canvas.drawRect(frame.left + 2.toFloat(), middle - 1.toFloat(), frame.right - 1.toFloat(), middle + 2.toFloat(), paint)
            }

            val scaleX = this.width / preview.width.toFloat()
            val scaleY = this.height / preview.height.toFloat()

            // draw the last possible result points
            if (lastPossibleResultPoints.isNotEmpty() && pointVisibility) {
                paint.alpha = CURRENT_POINT_OPACITY / 2
                paint.color = resultPointColor
                val radius = POINT_SIZE / 2.0f
                for (point in lastPossibleResultPoints) {
                    canvas.drawCircle(
                            (point.x * scaleX),
                            (point.y * scaleY),
                            radius, paint
                    )
                }
                lastPossibleResultPoints.clear()
            }

            // draw current possible result points
            if (possibleResultPoints.isNotEmpty() && pointVisibility) {
                paint.alpha = CURRENT_POINT_OPACITY
                paint.color = resultPointColor
                for (point in possibleResultPoints) {
                    canvas.drawCircle(
                            (point.x * scaleX),
                            (point.y * scaleY),
                            POINT_SIZE.toFloat(), paint
                    )
                }

                // swap and clear buffers
                val temp = possibleResultPoints
                possibleResultPoints = lastPossibleResultPoints
                lastPossibleResultPoints = temp
                possibleResultPoints.clear()
            }

            // Request another update at the animation interval, but only repaint the laser line,
            // not the entire viewfinder mask.
            postInvalidateDelayed(ANIMATION_DELAY,
                    frame.left - POINT_SIZE,
                    frame.top - POINT_SIZE,
                    frame.right + POINT_SIZE,
                    frame.bottom + POINT_SIZE)
        }
    }

    fun drawViewfinder() {
        val resultBitmap = this.resultBitmap
        this.resultBitmap = null
        resultBitmap?.recycle()
        invalidate()
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param result An image of the result.
     */
    fun drawResultBitmap(result: Bitmap) {
        resultBitmap = result
        invalidate()
    }

    /**
     * Only call from the UI thread.
     *
     * @param point a point to draw, relative to the preview frame
     */
    fun addPossibleResultPoint(point: ResultPoint) {
        if (possibleResultPoints.size < MAX_RESULT_POINTS) possibleResultPoints.add(point)
    }

    companion object {
        protected val TAG = ViewfinderView::class.java.simpleName
        protected val SCANNER_ALPHA = intArrayOf(0, 64, 128, 192, 255, 192, 128, 64)
        protected const val ANIMATION_DELAY = 80L
        protected const val CURRENT_POINT_OPACITY = 0xA0
        protected const val MAX_RESULT_POINTS = 20
        protected const val POINT_SIZE = 6
    }

    init {
        val attributes = getContext().obtainStyledAttributes(attrs, R.styleable.zxing_finder)
        maskColor = attributes.getColor(R.styleable.zxing_finder_zxing_viewfinder_mask, ContextCompat.getColor(this.context, R.color.zxing_viewfinder_mask))
        resultColor = attributes.getColor(R.styleable.zxing_finder_zxing_result_view, ContextCompat.getColor(this.context, R.color.zxing_result_view))
        laserColor = attributes.getColor(R.styleable.zxing_finder_zxing_viewfinder_laser, ContextCompat.getColor(this.context, R.color.zxing_viewfinder_laser))
        resultPointColor = attributes.getColor(R.styleable.zxing_finder_zxing_possible_result_points, ContextCompat.getColor(this.context, R.color.zxing_possible_result_points))
        laserVisibility = attributes.getBoolean(R.styleable.zxing_finder_zxing_viewfinder_laser_visibility, true)
        pointVisibility = attributes.getBoolean(R.styleable.zxing_finder_zxing_viewfinder_point_visibility, true)
        attributes.recycle()
        scannerAlpha = 0
        possibleResultPoints = ArrayList(MAX_RESULT_POINTS)
        lastPossibleResultPoints = ArrayList(MAX_RESULT_POINTS)
    }
}