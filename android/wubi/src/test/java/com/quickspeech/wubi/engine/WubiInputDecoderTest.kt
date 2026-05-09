package com.quickspeech.wubi.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WubiInputDecoderTest {

    private lateinit var decoder: WubiInputDecoder

    @Before
    fun setup() {
        decoder = WubiInputDecoder()
    }

    @Test
    fun processKey_letterKey_appendsToCode() {
        val result = decoder.processKey('a')
        assertTrue(result is InputResult.Composing)
        assertEquals("a", (result as InputResult.Composing).code)
    }

    @Test
    fun processKey_multipleLetters_buildsCode() {
        decoder.processKey('w')
        decoder.processKey('g')
        val result = decoder.processKey('k')
        assertTrue(result is InputResult.Composing)
        assertEquals("wgk", (result as InputResult.Composing).code)
    }

    @Test
    fun processKey_maxLength_fourChars() {
        decoder.processKey('a')
        decoder.processKey('b')
        decoder.processKey('c')
        decoder.processKey('d')
        val result = decoder.processKey('e')
        assertEquals("abcd", (result as InputResult.Composing).code)
    }

    @Test
    fun processKey_space_confirmsInput() {
        decoder.processKey('a')
        decoder.processKey('b')
        val result = decoder.processKey(' ')
        assertTrue(result is InputResult.Confirmed)
        assertEquals("ab", (result as InputResult.Confirmed).code)
    }

    @Test
    fun processKey_space_emptyCode_outputsSpace() {
        val result = decoder.processKey(' ')
        assertTrue(result is InputResult.DirectText)
        assertEquals(" ", (result as InputResult.DirectText).text)
    }

    @Test
    fun processKey_backspace_removesLastChar() {
        decoder.processKey('a')
        decoder.processKey('b')
        val result = decoder.processKey('\b')
        assertTrue(result is InputResult.Composing)
        assertEquals("a", (result as InputResult.Composing).code)
    }

    @Test
    fun processKey_backspace_emptyBuffer_returnsBackspace() {
        val result = decoder.processKey('\b')
        assertTrue(result is InputResult.Backspace)
    }

    @Test
    fun processKey_backspace_singleChar_clearsBuffer() {
        decoder.processKey('a')
        val result = decoder.processKey('\b')
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
    fun processKey_numberNine_selectsIndexEight() {
        val result = decoder.processKey('9')
        assertTrue(result is InputResult.SelectCandidate)
        assertEquals(8, (result as InputResult.SelectCandidate).index)
    }

    @Test
    fun processKey_punctuation_directOutput() {
        val result = decoder.processKey('，')
        assertTrue(result is InputResult.DirectText)
        assertEquals("，", (result as InputResult.DirectText).text)
    }

    @Test
    fun processKey_period_directOutput() {
        val result = decoder.processKey('。')
        assertTrue(result is InputResult.DirectText)
        assertEquals("。", (result as InputResult.DirectText).text)
    }

    @Test
    fun processKey_uppercase_convertsToLowercase() {
        decoder.processKey('A')
        decoder.processKey('B')
        assertEquals("ab", decoder.getCurrentCode())
    }

    @Test
    fun processKey_esc_clearsBuffer() {
        decoder.processKey('a')
        decoder.processKey('b')
        val result = decoder.processKey(27.toChar())
        assertTrue(result is InputResult.Cleared)
    }

    @Test
    fun toggleMode_switchesToEnglish() {
        val mode = decoder.toggleMode()
        assertEquals(InputMode.ENGLISH, mode)
    }

    @Test
    fun toggleMode_switchesBackToChinese() {
        decoder.toggleMode()
        val mode = decoder.toggleMode()
        assertEquals(InputMode.CHINESE, mode)
    }

    @Test
    fun processKey_englishMode_directOutput() {
        decoder.toggleMode()
        val result = decoder.processKey('a')
        assertTrue(result is InputResult.DirectText)
        assertEquals("a", (result as InputResult.DirectText).text)
    }

    @Test
    fun processKey_englishMode_space_outputsSpace() {
        decoder.toggleMode()
        val result = decoder.processKey(' ')
        assertTrue(result is InputResult.DirectText)
        assertEquals(" ", (result as InputResult.DirectText).text)
    }

    @Test
    fun reset_clearsBufferAndResetsMode() {
        decoder.processKey('a')
        decoder.processKey('b')
        decoder.toggleMode()
        decoder.reset()
        assertEquals("", decoder.getCurrentCode())
        assertEquals(InputMode.CHINESE, decoder.inputMode)
    }

    @Test
    fun getCurrentCode_empty_returnsEmptyString() {
        assertEquals("", decoder.getCurrentCode())
    }

    @Test
    fun clear_emptiesBuffer() {
        decoder.processKey('a')
        decoder.processKey('b')
        decoder.clear()
        assertEquals("", decoder.getCurrentCode())
    }

    @Test
    fun setMode_english_clearsBuffer() {
        decoder.processKey('a')
        decoder.processKey('b')
        decoder.setMode(InputMode.ENGLISH)
        assertEquals("", decoder.getCurrentCode())
    }

    @Test
    fun processKey_unknownChar_ignored() {
        decoder.processKey('a')
        val result = decoder.processKey('#')
        assertTrue(result is InputResult.Ignored)
        assertEquals("a", decoder.getCurrentCode())
    }

    @Test
    fun processKey_exclamationMark_directOutput() {
        val result = decoder.processKey('！')
        assertTrue(result is InputResult.DirectText)
        assertEquals("！", (result as InputResult.DirectText).text)
    }

    @Test
    fun processKey_questionMark_directOutput() {
        val result = decoder.processKey('？')
        assertTrue(result is InputResult.DirectText)
        assertEquals("？", (result as InputResult.DirectText).text)
    }

    @Test
    fun processKey_buildFullWubiCode() {
        // Type a full 4-char Wubi code
        decoder.processKey('w')
        decoder.processKey('g')
        decoder.processKey('k')
        decoder.processKey('r')
        assertEquals("wgkr", decoder.getCurrentCode())
    }

    @Test
    fun processKey_backspaceSequence() {
        decoder.processKey('a')
        decoder.processKey('b')
        decoder.processKey('c')
        decoder.processKey('d')
        decoder.processKey('\b')
        assertEquals("abc", decoder.getCurrentCode())
        decoder.processKey('\b')
        assertEquals("ab", decoder.getCurrentCode())
        decoder.processKey('\b')
        assertEquals("a", decoder.getCurrentCode())
        decoder.processKey('\b')
        assertTrue(decoder.getCurrentCode().isEmpty())
    }

    @Test
    fun inputMode_default_isChinese() {
        assertEquals(InputMode.CHINESE, decoder.inputMode)
    }
}
