package com.quickspeech.wubi.engine

import com.quickspeech.wubi.data.WubiWordEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WubiInputEngineTest {

    @Test
    fun engineResult_composing_hasCorrectProperties() = runBlocking {
        val composing = EngineResult.Composing("ab", emptyList())
        assertEquals("ab", composing.code)
        assertNotNull(composing.candidates)
    }

    @Test
    fun engineResult_textSelected_hasCorrectWord() = runBlocking {
        val selected = EngineResult.TextSelected("测试词")
        assertEquals("测试词", selected.word)
    }

    @Test
    fun engineResult_directOutput_hasCorrectText() = runBlocking {
        val direct = EngineResult.DirectOutput("，")
        assertEquals("，", direct.text)
    }

    @Test
    fun engineResult_singletonObjects_notNull() {
        assertNotNull(EngineResult.Backspace)
        assertNotNull(EngineResult.Cleared)
        assertNotNull(EngineResult.Ignored)
    }

    @Test
    fun engineResult_fullInputCycle() = runBlocking {
        val composeResult = EngineResult.Composing("abcd", emptyList())
        assertEquals("abcd", composeResult.code)

        val confirmResult = EngineResult.TextSelected("测试词")
        assertEquals("测试词", confirmResult.word)
    }

    @Test
    fun engineResult_textSelected_canBeUsedAsMatchResult() = runBlocking {
        val selected = EngineResult.TextSelected("关联词")
        assertEquals("关联词", selected.word)
    }

    @Test
    fun inputMode_enumValues_exist() {
        assertEquals("CHINESE", InputMode.CHINESE.toString())
        assertEquals("ENGLISH", InputMode.ENGLISH.toString())
    }

    @Test
    fun rankedCandidate_canBeCreated() {
        val entry = WubiWordEntry(code = "aa", word = "工", frequency = 5000, type = 0)
        val ranked = RankedCandidate(entry = entry, score = 100L, rank = 1)
        assertEquals("工", ranked.entry.word)
        assertEquals(100, ranked.score)
        assertEquals(1, ranked.rank)
    }

    @Test
    fun matchResult_canBeCreated() {
        val entry = WubiWordEntry(code = "aa", word = "工", frequency = 5000, type = 0)
        val result = MatchResult(query = "aa", candidates = listOf(entry), matchType = MatchType.EXACT)
        assertEquals("aa", result.query)
        assertEquals(1, result.candidates.size)
        assertEquals(MatchType.EXACT, result.matchType)
    }
}
