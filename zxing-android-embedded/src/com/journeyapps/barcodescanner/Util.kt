package com.journeyapps.barcodescanner

import android.os.Looper

/**
 *
 */
class Util {
    companion object {
        @JvmStatic
        fun validateMainThread() {
            check(Looper.getMainLooper() == Looper.myLooper()) { "Must be called from the main thread." }
        }
    }
}