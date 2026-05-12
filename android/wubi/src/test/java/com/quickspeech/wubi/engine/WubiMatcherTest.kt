package com.quickspeech.wubi.engine

import com.quickspeech.wubi.data.WubiDao
import com.quickspeech.wubi.data.WubiWordEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WubiMatcherTest {

    private lateinit var matcher: WubiMatcher
    private lateinit var fakeDao: FakeMatcherDao

    private class FakeMatcherDao : WubiDao {
        val exactResults = mutableMapOf<String, List<WubiWordEntry>>()
        val prefixResults = mutableMapOf<String, List<WubiWordEntry>>()
        val fuzzyResults = mutableMapOf<String, List<WubiWordEntry>>()

        override suspend fun exactMatch(code: String): List<WubiWordEntry> =
            exactResults[code] ?: emptyList()
        override suspend fun prefixMatch(pattern: String, limit: Int): List<WubiWordEntry> =
            (prefixResults[pattern] ?: emptyList()).take(limit)
        override suspend fun fuzzyMatch(pattern: String, limit: Int): List<WubiWordEntry> =
            (fuzzyResults[pattern] ?: emptyList()).take(limit)
        override fun exactMatchFlow(code: String) = throw NotImplementedError()
        override fun prefixMatchFlow(pattern: String, limit: Int) = throw NotImplementedError()
        override suspend fun reverseLookup(word: String): List<WubiWordEntry> = emptyList()
        override suspend fun associateWords(pattern: String, limit: Int): List<WubiWordEntry> = emptyList()
        override suspend fun getUserFrequency(word: String) = null
        override suspend fun getAllUserFrequencies() = emptyList<com.quickspeech.wubi.data.UserFrequencyEntry>()
        override suspend fun insertUserFrequency(entry: com.quickspeech.wubi.data.UserFrequencyEntry) {}
        override suspend fun updateUserFrequency(entry: com.quickspeech.wubi.data.UserFrequencyEntry) {}
        override suspend fun incrementFrequency(word: String, timestamp: Long) {}
        override suspend fun getRecentWords(limit: Int) = emptyList<com.quickspeech.wubi.data.RecentWordEntry>()
        override suspend fun insertRecentWord(entry: com.quickspeech.wubi.data.RecentWordEntry) {}
        override suspend fun cleanOldRecentWords(expireTime: Long) {}
        override suspend fun getWordCount(): Int = 0
        override suspend fun insertWords(words: List<WubiWordEntry>) {}
    }

    @Before
    fun setup() {
        fakeDao = FakeMatcherDao()
        matcher = WubiMatcher(fakeDao)
    }

    private fun entry(code: String, word: String, frequency: Int = 1000) =
        WubiWordEntry(code = code, word = word, frequency = frequency, type = 0)

    // ===== smartMatch tests =====

    @Test
    fun smartMatch_exactMatch_returnsExact() = runBlocking {
        fakeDao.exactResults["aa"] = listOf(entry("aa", "工作", 5000))
        val result = matcher.smartMatch("aa")
        assertEquals(MatchType.EXACT, result.matchType)
        assertEquals(1, result.candidates.size)
        assertEquals("工作", result.candidates[0].word)
    }

    @Test
    fun smartMatch_fourCodeSingleMatch_returnsExact() = runBlocking {
        fakeDao.exactResults["wgkr"] = listOf(entry("wgkr", "五笔", 3000))
        val result = matcher.smartMatch("wgkr")
        assertEquals(MatchType.EXACT, result.matchType)
        assertEquals(1, result.candidates.size)
    }

    @Test
    fun smartMatch_fourCodeMultipleMatches_notAutoCommit() = runBlocking {
        fakeDao.exactResults["abcd"] = listOf(
            entry("abcd", "词1", 3000),
            entry("abcd", "词2", 2000)
        )
        val result = matcher.smartMatch("abcd")
        // Should still return exact match (multiple candidates shown to user)
        assertEquals(MatchType.EXACT, result.matchType)
        assertEquals(2, result.candidates.size)
    }

    @Test
    fun smartMatch_noExactMatch_fallsBackToPrefix() = runBlocking {
        fakeDao.prefixResults["a%"] = listOf(entry("aa", "工", 5000), entry("ab", "人", 4000))
        val result = matcher.smartMatch("a")
        assertEquals(MatchType.PREFIX, result.matchType)
        assertEquals(2, result.candidates.size)
    }

    @Test
    fun smartMatch_zKeyWildcard_returnsFuzzy() = runBlocking {
        fakeDao.fuzzyResults["a_b"] = listOf(entry("acb", "阿", 3000), entry("adb", "啊", 2000))
        val result = matcher.smartMatch("azb")
        assertEquals(MatchType.FUZZY, result.matchType)
        assertEquals(2, result.candidates.size)
    }

    @Test
    fun smartMatch_zKeyReplacesWithUnderscore() = runBlocking {
        fakeDao.fuzzyResults["_a"] = listOf(entry("ba", "吧", 3000))
        val result = matcher.smartMatch("za")
        assertEquals(MatchType.FUZZY, result.matchType)
    }

    @Test
    fun smartMatch_noMatch_returnsNone() = runBlocking {
        val result = matcher.smartMatch("xyzx")
        assertEquals(MatchType.NONE, result.matchType)
        assertTrue(result.candidates.isEmpty())
    }

    @Test
    fun smartMatch_emptyCode_returnsNone() = runBlocking {
        val result = matcher.smartMatch("")
        assertEquals(MatchType.NONE, result.matchType)
    }

    @Test
    fun smartMatch_correctedCode_returnsCorrected() = runBlocking {
        // "tf" should be corrected to "tu" by correctCommonErrors
        fakeDao.exactResults["tu"] = listOf(entry("tu", "竹", 3000))
        val result = matcher.smartMatch("tf")
        assertEquals(MatchType.CORRECTED, result.matchType)
        assertEquals(1, result.candidates.size)
    }

    @Test
    fun smartMatch_priority_exactOverPrefix() = runBlocking {
        fakeDao.exactResults["ab"] = listOf(entry("ab", "工", 5000))
        fakeDao.prefixResults["ab%"] = listOf(
            entry("ab", "工", 5000),
            entry("abc", "工人", 4000),
            entry("abcd", "工作室", 3000)
        )
        val result = matcher.smartMatch("ab")
        assertEquals(MatchType.EXACT, result.matchType)
    }

    @Test
    fun smartMatch_adjacentKeyCorrection_works() = runBlocking {
        // User types "wg" but meant "wf" (adjacent keys)
        fakeDao.exactResults["wf"] = listOf(entry("wf", "全", 5000))
        val result = matcher.smartMatch("wg")
        // "g" is adjacent to "f" on QWERTY keyboard
        assertEquals(MatchType.CORRECTED, result.matchType)
        assertTrue(result.candidates.isNotEmpty())
    }

    // ===== exactMatch tests =====

    @Test
    fun exactMatch_findsMatch() = runBlocking {
        fakeDao.exactResults["aa"] = listOf(entry("aa", "工作", 5000))
        val result = matcher.exactMatch("aa")
        assertEquals(1, result.size)
        assertEquals("工作", result[0].word)
    }

    @Test
    fun exactMatch_noMatch_returnsEmpty() = runBlocking {
        val result = matcher.exactMatch("zzzz")
        assertTrue(result.isEmpty())
    }

    @Test
    fun exactMatch_blankCode_returnsEmpty() = runBlocking {
        val result = matcher.exactMatch("")
        assertTrue(result.isEmpty())
    }

    // ===== prefixMatch tests =====

    @Test
    fun prefixMatch_findsMatches() = runBlocking {
        fakeDao.prefixResults["a%"] = listOf(
            entry("aa", "工", 5000),
            entry("ab", "人", 4000)
        )
        val result = matcher.prefixMatch("a")
        assertEquals(2, result.size)
    }

    @Test
    fun prefixMatch_respectsLimit() = runBlocking {
        fakeDao.prefixResults["a%"] = (1..30).map { entry("a$it", "词$it", 1000) }
        val result = matcher.prefixMatch("a", limit = 5)
        assertEquals(5, result.size)
    }

    // ===== fuzzyMatch tests =====

    @Test
    fun fuzzyMatch_zKeyWildcard_findsMatches() = runBlocking {
        fakeDao.fuzzyResults["w_r"] = listOf(entry("war", "打", 3000))
        val result = matcher.fuzzyMatch("wzr")
        assertEquals(1, result.size)
        assertEquals("打", result[0].word)
    }

    @Test
    fun fuzzyMatch_noZKey_fallsBackToPrefix() = runBlocking {
        fakeDao.prefixResults["abc%"] = listOf(entry("abc", "工", 5000))
        val result = matcher.fuzzyMatch("abc")
        assertEquals(1, result.size)
    }

    // ===== adjacentKeyMatch tests =====

    @Test
    fun adjacentKeyMatch_singleChar_corrections() = runBlocking {
        fakeDao.exactResults["w"] = listOf(entry("w", "人", 5000))
        // "q" is adjacent to "w" on QWERTY
        val result = matcher.adjacentKeyMatch("q")
        assertTrue(result.candidates.isNotEmpty())
    }

    @Test
    fun adjacentKeyMatch_tooLongCode_returnsNone() = runBlocking {
        val result = matcher.adjacentKeyMatch("abcde")
        assertEquals(MatchType.NONE, result.matchType)
    }

    // ===== z-key fallback tests =====

    @Test
    fun smartMatch_zKeyFuzzyNoMatch_fallsBackToPrefixWithoutZ() = runBlocking {
        // "az" - fuzzy match for "a_" returns nothing, should fall back to prefix "a"
        fakeDao.prefixResults["a%"] = listOf(entry("aa", "工", 5000), entry("ab", "人", 4000))
        val result = matcher.smartMatch("az")
        assertEquals(MatchType.PREFIX, result.matchType)
        assertEquals(2, result.candidates.size)
    }

    @Test
    fun smartMatch_allZCode_fuzzyNoMatch_returnsNone() = runBlocking {
        // "zzzz" - all z, after removing z for prefix fallback, empty string -> NONE
        val result = matcher.smartMatch("zzzz")
        assertEquals(MatchType.NONE, result.matchType)
    }

    @Test
    fun fuzzyMatch_zKeyNoMatch_fallsBackToPrefixWithoutZ() = runBlocking {
        fakeDao.prefixResults["ab%"] = listOf(entry("abc", "工", 5000))
        val result = matcher.fuzzyMatch("azb")
        assertEquals(1, result.size)
        assertEquals("工", result[0].word)
    }

    @Test
    fun fuzzyMatch_allZCode_noFallback_returnsEmpty() = runBlocking {
        val result = matcher.fuzzyMatch("zzz")
        assertTrue(result.isEmpty())
    }
}
