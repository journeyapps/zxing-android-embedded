package com.journeyapps.barcodescanner

import android.graphics.*
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ResultPoint
import java.io.ByteArrayOutputStream

/**
 * Raw preview data from a camera.
 */
class SourceData(data: ByteArray, dataWidth: Int, dataHeight: Int, val imageFormat: Int, private val rotation: Int) {
    private val data: RawImageData = RawImageData(data, dataWidth, dataHeight)

    /**
     * Set the crop rectangle.
     *
     * @param cropRect the new crop rectangle.
     */
    /** Crop rectangle, in display orientation.  */
    var cropRect: Rect? = null

    /**
     * Factor by which to scale down before decoding.
     */
    var scalingFactor = 1
    var isPreviewMirrored = false

    fun getData(): ByteArray {
        return data.data
    }

    /**
     *
     * @return width of the data
     */
    val dataWidth: Int
        get() = data.width

    /**
     *
     * @return height of the data
     */
    val dataHeight: Int
        get() = data.height

    fun translateResultPoint(point: ResultPoint): ResultPoint {
        var x = point.x * scalingFactor + cropRect!!.left
        val y = point.y * scalingFactor + cropRect!!.top
        if (isPreviewMirrored) {
            x = data.width - x
        }
        return ResultPoint(x, y)
    }

    /**
     *
     * @return true if the preview image is rotated orthogonal to the display
     */
    val isRotated: Boolean
        get() = rotation % 180 != 0

    fun createSource(): PlanarYUVLuminanceSource {
        val rotated = data.rotateCameraPreview(rotation)
        val scaled = rotated.cropAndScale(cropRect!!, scalingFactor)

        // not the preview for decoding.
        return PlanarYUVLuminanceSource(scaled.data, scaled.width, scaled.height, 0, 0, scaled.width, scaled.height, false)
    }

    /**
     * Return the source bitmap (cropped; in display orientation).
     *
     * @return the bitmap
     */
    val bitmap: Bitmap
        get() = getBitmap(1)

    /**
     * Return the source bitmap (cropped; in display orientation).
     *
     * @param scaleFactor factor to scale down by. Must be a power of 2.
     * @return the bitmap
     */
    fun getBitmap(scaleFactor: Int): Bitmap {
        return getBitmap(cropRect, scaleFactor)
    }

    fun getBitmap(cropRect: Rect?, scaleFactor: Int): Bitmap {
        var cropRectTemp = cropRect
        if (cropRectTemp == null) {
            cropRectTemp = Rect(0, 0, data.width, data.height)
        } else if (isRotated) {
            cropRectTemp = Rect(cropRectTemp.top, cropRectTemp.left, cropRectTemp.bottom, cropRectTemp.right)
        }

        // TODO: there should be a way to do this without JPEG compression / decompression cycle.
        val img = YuvImage(data.data, imageFormat, data.width, data.height, null)
        val buffer = ByteArrayOutputStream()
        img.compressToJpeg(cropRectTemp, 90, buffer)
        val jpegData = buffer.toByteArray()
        val options = BitmapFactory.Options()
        options.inSampleSize = scaleFactor
        var bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size, options)

        // Rotate if required
        if (rotation != 0) {
            val imageMatrix = Matrix()
            imageMatrix.postRotate(rotation.toFloat())
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, imageMatrix, false)
        }
        return bitmap
    }

    /**
     *
     * @param data the image data
     * @param dataWidth width of the data
     * @param dataHeight height of the data
     * @param imageFormat ImageFormat.NV21 or ImageFormat.YUY2
     * @param rotation camera rotation relative to display rotation, in degrees (0, 90, 180 or 270).
     */
    init {
        require(dataWidth * dataHeight <= data.size) { "Image data does not match the resolution. " + dataWidth + "x" + dataHeight + " > " + data.size }
    }
}