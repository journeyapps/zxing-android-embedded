package com.journeyapps.barcodescanner;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Used as an alias for DecoratedBarcodeView, for backwards-compatibility.
 */
public class CompoundBarcodeView extends DecoratedBarcodeView {
    public CompoundBarcodeView(Context context) {
        super(context);
    }

    public CompoundBarcodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CompoundBarcodeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
