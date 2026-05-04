package com.quickspeech.wubi.engine

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WubiEngine @Inject constructor() {

    companion object {
        private const val TAG = "WubiEngine"
        var isNativeLoaded = false
            private set

        init {
            try {
                System.loadLibrary("wubi-engine")
                isNativeLoaded = true
                Log.d(TAG, "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "Failed to load native library: ${e.message}")
                isNativeLoaded = false
            }
        }
    }

    fun search(code: String): List<String> {
        if (code.isEmpty()) return emptyList()
        return if (isNativeLoaded) {
            nativeSearch(code)
        } else {
            emptyList()
        }
    }

    fun setScheme(scheme: WubiScheme) {
        if (isNativeLoaded) {
            nativeSetScheme(scheme.code)
        }
    }

    fun enableErrorCorrection(enabled: Boolean) {
        if (isNativeLoaded) {
            nativeEnableErrorCorrection(enabled)
        }
    }

    private external fun nativeSearch(code: String): List<String>
    private external fun nativeSetScheme(schemeCode: Int)
    private external fun nativeEnableErrorCorrection(enabled: Boolean)
}

enum class WubiScheme(val code: Int) {
    WUBI_86(0),
    WUBI_98(1)
}
