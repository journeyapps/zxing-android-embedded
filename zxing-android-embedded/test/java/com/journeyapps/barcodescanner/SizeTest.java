package com.journeyapps.barcodescanner;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class SizeTest {

    private Size a;
    private Size scaled;

    @Before
    public void setUp() {
        a = new Size(12, 9);
    }

    @Test
    public void testOriginalWidth() {
        assertEquals(12, a.width);
    }

    @Test
    public void testOriginalHeight() {
        assertEquals(9, a.height);
    }

    @Test
    public void testScaledWidth() {
        scaled = a.scale(2, 3);
        assertEquals(8, scaled.width);
    }

    @Test
    public void testScaledHeight() {
        scaled = a.scale(2, 3);
        assertEquals(6, scaled.height);
    }

    @Test
    public void testSizeFitsItself() {
        assertTrue(a.fitsIn(a));
    }

    @Test
    public void size12x9FitsInSize13x10Test() {
        assertTrue(a.fitsIn(new Size(13, 10)));
    }

    @Test
    public void size12x9FitsInSize13x9Test() {
        assertTrue(a.fitsIn(new Size(13, 9)));
    }

    @Test
    public void size12x9FitsInSize12x10Test() {
        assertTrue(a.fitsIn(new Size(12, 10)));
    }

    @Test
    public void size12x9DoesNotFitsInSize120x8Test() {
        assertFalse(a.fitsIn(new Size(120, 8)));
    }

    @Test
    public void size12x9DoesNotFitsInSize11x900Test() {
        assertFalse(a.fitsIn(new Size(11, 900)));
    }

    @Test
    public void size12x9ComparedTo12x9Test() {
        assertEquals(0, a.compareTo(new Size(12, 9)));
    }

    @Test
    public void size12x9ComparedTo9x12Test() {
        assertEquals(0, a.compareTo(new Size(9, 12)));
    }

    @Test
    public void size12x9ComparedTo10x11Test() {
        assertEquals(-1, a.compareTo(new Size(10, 11)));
    }

    @Test
    public void size12x9ComparedTo10x10Test() {
        assertEquals(1, a.compareTo(new Size(10, 10)));
    }

    @Test
    public void testRotate() {
        assertEquals(new Size(9, 12), a.rotate());
    }

    @Test
    public void size12x9ScaleCropTo120x90() {
        assertEquals(new Size(120, 90), a.scaleCrop(new Size(120, 90)));
    }

    @Test
    public void size12x9ScaleCropTo120x80() {
        assertEquals(new Size(120, 90), a.scaleCrop(new Size(120, 90)));
    }

    @Test
    public void size12x9ScaleCropTo110x90() {
        assertEquals(new Size(120, 90), a.scaleCrop(new Size(110, 90)));
    }

    @Test
    public void size12x9ScaleCropTo110x0() {
        assertEquals(new Size(110, 82), a.scaleCrop(new Size(110, 0)));
    }

    @Test
    public void size12x9ScaleFitTo120x90() {
        assertEquals(new Size(120, 90), a.scaleFit(new Size(120, 90)));
    }

    @Test
    public void size12x9ScaleFitTo120x100() {
        assertEquals(new Size(120, 90), a.scaleFit(new Size(120, 100)));
    }

    @Test
    public void size12x9ScaleFitTo130x90() {
        assertEquals(new Size(120, 90), a.scaleFit(new Size(130, 90)));
    }

    @Test
    public void size12x9ScaleFitTo110x0() {
        assertEquals(new Size(0, 0), a.scaleFit(new Size(110, 0)));
    }
}
