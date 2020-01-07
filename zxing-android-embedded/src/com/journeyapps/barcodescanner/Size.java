package com.journeyapps.barcodescanner;

import androidx.annotation.NonNull;

/**
 *
 */
public class Size implements Comparable<Size> {
    public final int width;
    public final int height;

    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Swap width and height.
     *
     * @return a new Size with swapped width and height
     */
    public Size rotate() {
        //noinspection SuspiciousNameCombination
        return new Size(height, width);
    }

    /**
     * Scale by n / d.
     *
     * @param n numerator
     * @param d denominator
     * @return the scaled size
     */
    public Size scale(int n, int d) {
        return new Size(width * n / d, height * n / d);
    }

    /**
     * Scales the dimensions so that it fits entirely inside the parent.One of width or height will
     * fit exactly. Aspect ratio is preserved.
     *
     * @param into the parent to fit into
     * @return the scaled size
     */
    public Size scaleFit(Size into) {
        if (width * into.height >= into.width * height) {
            // match width
            return new Size(into.width, height * into.width / width);
        } else {
            // match height
            return new Size(width * into.height / height, into.height);
        }
    }
    /**
     * Scales the size so that both dimensions will be greater than or equal to the corresponding
     * dimension of the parent. One of width or height will fit exactly. Aspect ratio is preserved.
     *
     * @param into the parent to fit into
     * @return the scaled size
     */
    public Size scaleCrop(Size into) {
        if (width * into.height <= into.width * height) {
            // match width
            return new Size(into.width, height * into.width / width);
        } else {
            // match height
            return new Size(width * into.height / height, into.height);
        }
    }

    /**
     * Checks if both dimensions of the other size are at least as large as this size.
     *
     * @param other the size to compare with
     * @return true if this size fits into the other size
     */
    public boolean fitsIn(Size other) {
        return width <= other.width && height <= other.height;
    }

    /**
     * Default sort order is ascending by size.
     */
    @Override
    public int compareTo(@NonNull Size other) {
        int aPixels = this.height * this.width;
        int bPixels = other.height * other.width;
        if (bPixels < aPixels) {
            return 1;
        }
        if (bPixels > aPixels) {
            return -1;
        }
        return 0;
    }

    public String toString() {
        return width + "x" + height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Size size = (Size) o;

        return width == size.width && height == size.height;
    }

    @Override
    public int hashCode() {
        int result = width;
        result = 31 * result + height;
        return result;
    }
}
