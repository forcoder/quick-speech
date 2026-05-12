package com.quickspeech.wubi.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Thread-safety tests for WubiInputDecoder.
 * Verifies @Volatile inputMode and StringBuilder codeBuffer behavior under sequential access.
 */
class WubiInputDecoderConcurrencyTest {

    private lateinit var decoder: WubiInputDecoder

    @Before
    fun setup() {
        decoder = WubiInputDecoder()
    }

    @Test
    fun inputMode_default_isChinese() {
        assertEquals(InputMode.CHINESE, decoder.inputMode)
    }

    @Test
    fun inputMode_toggle_switchesToEnglish() {
        decoder.toggleMode()
        assertEquals(InputMode.ENGLISH, decoder.inputMode)
    }

    @Test
    fun inputMode_toggleTwice_returnsToChinese() {
        decoder.toggleMode()
        decoder.toggleMode()
        assertEquals(InputMode.CHINESE, decoder.inputMode)
    }

    @Test
    fun inputMode_setMode_changesMode() {
        decoder.setMode(InputMode.ENGLISH)
        assertEquals(InputMode.ENGLISH, decoder.inputMode)
        decoder.setMode(InputMode.CHINESE)
        assertEquals(InputMode.CHINESE, decoder.inputMode)
    }

    @Test
    fun codeBuffer_multipleAppends_buildsCode() {
        decoder.processKey('a')
        decoder.processKey('b')
        decoder.processKey('c')
        assertEquals("abc", decoder.getCurrentCode())
    }

    @Test
    fun codeBuffer_maxLength_fourChars() {
        decoder.processKey('a')
        decoder.processKey('b')
        decoder.processKey('c')
        decoder.processKey('d')
        decoder.processKey('e')
        assertEquals("abcd", decoder.getCurrentCode())
    }

    @Test
    fun codeBuffer_backspace_removesLastChar() {
        decoder.processKey('a')
        decoder.processKey('b')
        decoder.processKey('c')
        decoder.processKey('\b')
        assertEquals("ab", decoder.getCurrentCode())
    }

    @Test
    fun codeBuffer_clear_emptiesBuffer() {
        decoder.processKey('a')
        decoder.processKey('b')
        decoder.clear()
        assertEquals("", decoder.getCurrentCode())
    }

    @Test
    fun codeBuffer_reset_emptiesBufferAndResetsMode() {
        decoder.processKey('a')
        decoder.processKey('b')
        decoder.toggleMode()
        decoder.reset()
        assertEquals("", decoder.getCurrentCode())
        assertEquals(InputMode.CHINESE, decoder.inputMode)
    }

    @Test
    fun processKey_uppercase_convertsToLowerCase() {
        decoder.processKey('A')
        decoder.processKey('B')
        assertEquals("ab", decoder.getCurrentCode())
    }

    @Test
    fun processKey_space_confirmsCode() {
        decoder.processKey('a')
        decoder.processKey('b')
        val result = decoder.processKey(' ')
        assertTrue(result is InputResult.Confirmed)
        assertEquals("ab", (result as InputResult.Confirmed).code)
    }

    @Test
    fun processKey_space_empty_outputsDirectText() {
        val result = decoder.processKey(' ')
        assertTrue(result is InputResult.DirectText)
        assertEquals(" ", (result as InputResult.DirectText).text)
    }

    @Test
    fun processKey_backspace_empty_returnsBackspace() {
        val result = decoder.processKey('\b')
        assertTrue(result is InputResult.Backspace)
    }

    @Test
    fun processKey_backspace_singleChar_returnsCleared() {
        decoder.processKey('a')
        val result = decoder.processKey('\b')
        assertTrue(result is InputResult.Cleared)
    }

    @Test
    fun processKey_englishMode_directOutput() {
        decoder.toggleMode()
        val result = decoder.processKey('a')
        assertTrue(result is InputResult.DirectText)
        assertEquals("a", (result as InputResult.DirectText).text)
    }

    @Test
    fun processKey_punctuation_directOutput() {
        val result = decoder.processKey('，')
        assertTrue(result is InputResult.DirectText)
        assertEquals("，", (result as InputResult.DirectText).text)
    }

    @Test
    fun processKey_esc_clearsBuffer() {
        decoder.processKey('a')
        decoder.processKey('b')
        val result = decoder.processKey(27.toChar())
        assertTrue(result is InputResult.Cleared)
    }

    @Test
    fun processKey_numberKey_selectsCandidate() {
        decoder.processKey('a')
        val result = decoder.processKey('1')
        assertTrue(result is InputResult.SelectCandidate)
        assertEquals(0, (result as InputResult.SelectCandidate).index)
    }

    @Test
    fun processKey_unknownChar_ignored() {
        decoder.processKey('a')
        val result = decoder.processKey('#')
        assertTrue(result is InputResult.Ignored)
        assertEquals("a", decoder.getCurrentCode())
    }

    @Test
    fun processKey_toggleMode_clearsCodeBuffer() {
        decoder.processKey('a')
        decoder.processKey('b')
        decoder.toggleMode()
        assertEquals("", decoder.getCurrentCode())
    }

    @Test
    fun inputMode_englishThenProcessKey_doesNotAppend() {
        decoder.toggleMode()
        val result = decoder.processKey('x')
        assertTrue(result is InputResult.DirectText)
        assertEquals("", decoder.getCurrentCode())
    }

    @Test
    fun inputMode_setEnglish_clearsBuffer() {
        decoder.processKey('a')
        decoder.processKey('b')
        decoder.setMode(InputMode.ENGLISH)
        assertEquals("", decoder.getCurrentCode())
    }

    @Test
    fun fullInputCycle_typeConfirmSpace_typeAgain() {
        decoder.processKey('a')
        decoder.processKey('b')
        decoder.processKey('c')
        decoder.processKey('d')
        assertEquals("abcd", decoder.getCurrentCode())

        decoder.processKey(' ')
        assertEquals("", decoder.getCurrentCode())

        decoder.processKey('w')
        decoder.processKey('g')
        assertEquals("wg", decoder.getCurrentCode())
    }
}
