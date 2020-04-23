package com.journeyapps.barcodescanner

import android.os.Looper

/**
 *
 */
object Util {
    @JvmStatic
    fun validateMainThread() {
        check(Looper.getMainLooper() == Looper.myLooper()) { "Must be called from the main thread." }
    }
}