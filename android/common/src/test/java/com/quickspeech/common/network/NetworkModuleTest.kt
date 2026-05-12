package com.quickspeech.common.network

import com.quickspeech.common.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for network configuration.
 */
class NetworkModuleTest {

    @Test
    fun buildConfig_baseUrl_isProductionUrl() {
        assertEquals("https://api.quickspeech.com/", BuildConfig.BASE_URL)
    }

    @Test
    fun buildConfig_baseUrl_isHttps() {
        assertTrue("BASE_URL should use HTTPS", BuildConfig.BASE_URL.startsWith("https://"))
    }

    @Test
    fun buildConfig_baseUrl_isNotLocalhost() {
        assertTrue("BASE_URL should not be localhost", !BuildConfig.BASE_URL.contains("localhost"))
    }

    @Test
    fun buildConfig_wsUrl_isSecure() {
        assertEquals("wss://api.quickspeech.com/ws", BuildConfig.WS_URL)
        assertTrue("WS_URL should use WSS", BuildConfig.WS_URL.startsWith("wss://"))
    }
}
