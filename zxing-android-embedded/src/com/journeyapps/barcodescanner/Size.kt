package com.journeyapps.barcodescanner

/**
 *
 */
class Size(val width: Int, val height: Int) : Comparable<Size> {

    /**
     * Swap width and height.
     *
     * @return a new Size with swapped width and height
     */
    fun rotate(): Size {
        return Size(height, width)
    }

    /**
     * Scale by n / d.
     *
     * @param n numerator
     * @param d denominator
     * @return the scaled size
     */
    fun scale(n: Int, d: Int): Size {
        return Size(width * n / d, height * n / d)
    }

    /**
     * Scales the dimensions so that it fits entirely inside the parent.One of width or height will
     * fit exactly. Aspect ratio is preserved.
     *
     * @param into the parent to fit into
     * @return the scaled size
     */
    fun scaleFit(into: Size): Size {
        return if (width * into.height >= into.width * height) {
            // match width
            Size(into.width, height * into.width / width)
        } else {
            // match height
            Size(width * into.height / height, into.height)
        }
    }

    /**
     * Scales the size so that both dimensions will be greater than or equal to the corresponding
     * dimension of the parent. One of width or height will fit exactly. Aspect ratio is preserved.
     *
     * @param into the parent to fit into
     * @return the scaled size
     */
    fun scaleCrop(into: Size): Size {
        return if (width * into.height <= into.width * height) {
            // match width
            Size(into.width, height * into.width / width)
        } else {
            // match height
            Size(width * into.height / height, into.height)
        }
    }

    /**
     * Checks if both dimensions of the other size are at least as large as this size.
     *
     * @param other the size to compare with
     * @return true if this size fits into the other size
     */
    fun fitsIn(other: Size): Boolean {
        return width <= other.width && height <= other.height
    }

    /**
     * Default sort order is ascending by size.
     */
    override fun compareTo(other: Size): Int {
        val aPixels = height * width
        val bPixels = other.height * other.width
        if (bPixels < aPixels) {
            return 1
        }
        return if (bPixels > aPixels) {
            -1
        } else 0
    }

    override fun toString(): String {
        return width.toString() + "x" + height
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val size = o as Size
        return width == size.width && height == size.height
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        return result
    }

}