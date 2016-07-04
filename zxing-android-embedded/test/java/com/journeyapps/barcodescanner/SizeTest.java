package com.journeyapps.barcodescanner;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class SizeTest {

    @Test
    public void testScale() {
        Size a = new Size(12, 9);
        Size scaled = a.scale(2, 3);

        assertEquals(8, scaled.width);
        assertEquals(6, scaled.height);

        assertEquals(12, a.width);
        assertEquals(9, a.height);
    }

    @Test
    public void testFitsIn() {
        Size a = new Size(12, 9);
        assertTrue(a.fitsIn(a));
        assertTrue(a.fitsIn(new Size(13, 10)));
        assertTrue(a.fitsIn(new Size(13, 9)));
        assertTrue(a.fitsIn(new Size(12, 10)));
        assertFalse(a.fitsIn(new Size(120, 8)));
        assertFalse(a.fitsIn(new Size(11, 900)));
    }

    @Test
    public void testCompare() {
        Size a = new Size(12, 9);
        assertEquals(0, a.compareTo(new Size(12, 9)));
        assertEquals(0, a.compareTo(new Size(9, 12)));
        assertEquals(-1, a.compareTo(new Size(10, 11)));
        assertEquals(1, a.compareTo(new Size(10, 10)));
    }

    @Test
    public void testRotate() {
        Size a = new Size(12, 9);
        assertEquals(new Size(9, 12), a.rotate());
    }

    @Test
    public void testScaleCrop() {
        Size a = new Size(12, 9);
        assertEquals(new Size(120, 90), a.scaleCrop(new Size(120, 90)));
        assertEquals(new Size(120, 90), a.scaleCrop(new Size(120, 80)));
        assertEquals(new Size(120, 90), a.scaleCrop(new Size(110, 90)));
        assertEquals(new Size(110, 82), a.scaleCrop(new Size(110, 0)));
    }

    @Test
    public void testScaleFit() {
        Size a = new Size(12, 9);
        assertEquals(new Size(120, 90), a.scaleFit(new Size(120, 90)));
        assertEquals(new Size(120, 90), a.scaleFit(new Size(120, 100)));
        assertEquals(new Size(120, 90), a.scaleFit(new Size(130, 90)));
        assertEquals(new Size(0, 0), a.scaleFit(new Size(110, 0)));
    }
}
