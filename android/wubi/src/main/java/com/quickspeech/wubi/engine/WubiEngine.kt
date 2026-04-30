package com.quickspeech.wubi.engine

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WubiEngine @Inject constructor() {

    init {
        System.loadLibrary("wubi-engine")
    }

    fun search(code: String): List<String> {
        if (code.isEmpty()) return emptyList()
        return nativeSearch(code)
    }

    fun setScheme(scheme: WubiScheme) {
        nativeSetScheme(scheme.code)
    }

    fun enable纠错(enabled: Boolean) {
        nativeEnableErrorCorrection(enabled)
    }

    private external fun nativeSearch(code: String): List<String>
    private external fun nativeSetScheme(schemeCode: Int)
    private external fun nativeEnableErrorCorrection(enabled: Boolean)
}

enum class WubiScheme(val code: Int) {
    WUBI_86(0),
    WUBI_98(1)
}
