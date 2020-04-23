package com.journeyapps.barcodescanner

import android.content.Context
import android.util.AttributeSet

/**
 * Used as an alias for DecoratedBarcodeView, for backwards-compatibility.
 */
class CompoundBarcodeView : DecoratedBarcodeView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr)
}