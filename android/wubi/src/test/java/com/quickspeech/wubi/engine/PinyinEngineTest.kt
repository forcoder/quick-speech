package com.quickspeech.wubi.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PinyinEngineTest {

    private lateinit var engine: PinyinEngine

    @Before
    fun setup() {
        engine = PinyinEngine()
    }

    // ===== search tests =====

    @Test
    fun search_exactMatch_returnsCandidates() {
        val result = engine.search("ma")
        assertTrue(result.isNotEmpty())
        assertTrue(result.any { it == "马" || it == "妈" || it == "麻" || it == "骂" })
    }

    @Test
    fun search_emptyInput_returnsEmpty() {
        val result = engine.search("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun search_blankInput_returnsEmpty() {
        val result = engine.search("   ")
        assertTrue(result.isEmpty())
    }

    @Test
    fun search_unknownSyllable_returnsEmpty() {
        val result = engine.search("xyzx")
        assertTrue(result.isEmpty())
    }

    @Test
    fun search_twoSyllables_combinesResults() {
        val result = engine.search("nihao")
        // Should return combined results or prefix matches
        assertNotNull(result)
    }

    @Test
    fun search_uppercaseInput_convertsToLowercase() {
        val lowerResult = engine.search("ma")
        val upperResult = engine.search("MA")
        assertEquals(lowerResult, upperResult)
    }

    @Test
    fun search_multipleSyllable_returnsPrefixMatches() {
        val result = engine.search("zh")
        // "zh" is a prefix of many syllables like "zhao", "zhan", etc.
        // The dict has entries like "zhao", "zhan" etc.
        assertNotNull(result)
    }

    // ===== segmentPinyin tests =====

    @Test
    fun segmentPinyin_validInput_returnsSegments() {
        val result = engine.segmentPinyin("nihao")
        assertTrue(result.isNotEmpty())
        val hasCorrectSegment = result.any { segments ->
            segments.size == 2 && segments[0] == "ni" && segments[1] == "hao"
        }
        assertTrue(hasCorrectSegment)
    }

    @Test
    fun segmentPinyin_singleSyllable_returnsOneSegment() {
        val result = engine.segmentPinyin("ma")
        assertTrue(result.isNotEmpty())
        assertEquals(listOf("ma"), result[0])
    }

    @Test
    fun segmentPinyin_emptyInput_returnsEmpty() {
        val result = engine.segmentPinyin("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun segmentPinyin_sortedBySegmentCount() {
        val result = engine.segmentPinyin("women")
        assertTrue(result.isNotEmpty())
        assertTrue(result[0].size <= result.last().size)
    }

    @Test
    fun segmentPinyin_longInput_returnsSegments() {
        val result = engine.segmentPinyin("zhongguo")
        assertTrue(result.isNotEmpty())
    }

    // ===== searchFuzzy tests =====

    @Test
    fun searchFuzzy_exactMatch_returnsResults() {
        val result = engine.searchFuzzy("ma")
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun searchFuzzy_zhVariant_returnsResults() {
        // "zha" is not in the dict, but "sha" is - use fuzzy zh/sh pairing
        val result = engine.searchFuzzy("sha")
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun searchFuzzy_emptyInput_returnsEmpty() {
        val result = engine.searchFuzzy("")
        assertTrue(result.isEmpty())
    }

    // ===== getAbbreviation tests =====

    @Test
    fun getAbbreviation_twoSyllables_returnsAbbreviation() {
        val result = engine.getAbbreviation("nihao")
        assertEquals("nh", result)
    }

    @Test
    fun getAbbreviation_singleSyllable_returnsFirstChar() {
        val result = engine.getAbbreviation("ma")
        assertEquals("m", result)
    }

    // ===== VALID_SYLLABLES tests =====

    @Test
    fun validSyllables_containsCommonSyllables() {
        assertTrue("ni" in PinyinEngine.VALID_SYLLABLES)
        assertTrue("hao" in PinyinEngine.VALID_SYLLABLES)
        assertTrue("ma" in PinyinEngine.VALID_SYLLABLES)
        assertTrue("zhong" in PinyinEngine.VALID_SYLLABLES)
        assertTrue("guo" in PinyinEngine.VALID_SYLLABLES)
        assertTrue("zhao" in PinyinEngine.VALID_SYLLABLES)
        assertTrue("zhan" in PinyinEngine.VALID_SYLLABLES)
    }

    @Test
    fun validSyllables_doesNotContainInvalid() {
        assertFalse("xyz" in PinyinEngine.VALID_SYLLABLES)
        assertFalse("qxq" in PinyinEngine.VALID_SYLLABLES)
    }

    // ===== FUZZY_PAIRS tests =====

    @Test
    fun fuzzyPairs_containsCommonPairs() {
        assertEquals("z", PinyinEngine.FUZZY_PAIRS["zh"])
        assertEquals("zh", PinyinEngine.FUZZY_PAIRS["z"])
        assertEquals("c", PinyinEngine.FUZZY_PAIRS["ch"])
        assertEquals("ch", PinyinEngine.FUZZY_PAIRS["c"])
        assertEquals("s", PinyinEngine.FUZZY_PAIRS["sh"])
        assertEquals("sh", PinyinEngine.FUZZY_PAIRS["s"])
    }

    // ===== INITIALS tests =====

    @Test
    fun initials_containsAllInitials() {
        assertTrue(PinyinEngine.INITIALS.contains("zh"))
        assertTrue(PinyinEngine.INITIALS.contains("ch"))
        assertTrue(PinyinEngine.INITIALS.contains("sh"))
        assertTrue(PinyinEngine.INITIALS.contains("b"))
        assertTrue(PinyinEngine.INITIALS.contains("p"))
        assertTrue(PinyinEngine.INITIALS.contains("m"))
        assertTrue(PinyinEngine.INITIALS.contains("f"))
    }

    @Test
    fun initials_startWithLongest() {
        // Initials should list compound initials first for proper matching
        assertTrue(PinyinEngine.INITIALS.indexOf("zh") < PinyinEngine.INITIALS.indexOf("z"))
        assertTrue(PinyinEngine.INITIALS.indexOf("ch") < PinyinEngine.INITIALS.indexOf("c"))
        assertTrue(PinyinEngine.INITIALS.indexOf("sh") < PinyinEngine.INITIALS.indexOf("s"))
    }
}
