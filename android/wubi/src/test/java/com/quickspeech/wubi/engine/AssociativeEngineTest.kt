package com.quickspeech.wubi.engine

import com.quickspeech.wubi.data.WubiDao
import com.quickspeech.wubi.data.WubiWordEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AssociativeEngineTest {

    private lateinit var engine: AssociativeEngine
    private lateinit var fakeDao: FakeAssociativeDao

    private class FakeAssociativeDao : WubiDao {
        var associateResults = mutableMapOf<String, List<WubiWordEntry>>()

        override suspend fun exactMatch(code: String): List<WubiWordEntry> = emptyList()
        override suspend fun prefixMatch(pattern: String, limit: Int): List<WubiWordEntry> = emptyList()
        override suspend fun fuzzyMatch(pattern: String, limit: Int): List<WubiWordEntry> = emptyList()
        override fun exactMatchFlow(code: String) = throw NotImplementedError()
        override fun prefixMatchFlow(pattern: String, limit: Int) = throw NotImplementedError()
        override suspend fun reverseLookup(word: String): List<WubiWordEntry> = emptyList()
        override suspend fun associateWords(pattern: String, limit: Int): List<WubiWordEntry> =
            (associateResults[pattern] ?: emptyList()).take(limit)
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
        fakeDao = FakeAssociativeDao()
        engine = AssociativeEngine(fakeDao)
    }

    private fun entry(code: String, word: String, frequency: Int = 1000) =
        WubiWordEntry(code = code, word = word, frequency = frequency, type = 0)

    @Test
    fun associate_singleChar_returnsAssociations() = runBlocking {
        fakeDao.associateResults["%中%"] = listOf(entry("k", "中", 5000))
        val result = engine.associate("中")
        assertEquals(1, result.size)
        assertEquals("中", result[0].word)
    }

    @Test
    fun associate_multiCharInput_returnsEmpty() = runBlocking {
        val result = engine.associate("中国")
        assertTrue(result.isEmpty())
    }

    @Test
    fun associate_noMatch_returnsEmpty() = runBlocking {
        val result = engine.associate("中")
        assertTrue(result.isEmpty())
    }

    @Test
    fun associate_respectsLimit() = runBlocking {
        fakeDao.associateResults["%中%"] = (1..30).map { entry("k$it", "词$it", 1000) }
        val result = engine.associate("中", limit = 5)
        assertEquals(5, result.size)
    }

    @Test
    fun smartCompose_emptyCodes_returnsEmpty() = runBlocking {
        val result = engine.smartCompose(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun smartCompose_twoChars_composesTwoCharPhrases() = runBlocking {
        val codes = listOf(
            listOf(entry("w", "工", 5000)),
            listOf(entry("a", "人", 5000))
        )
        val result = engine.smartCompose(codes)
        assertTrue(result.isNotEmpty())
        assertEquals("工人", result[0].phrase)
        assertEquals(2, result[0].type)
    }

    @Test
    fun smartCompose_threeChars_composesThreeCharPhrases() = runBlocking {
        val codes = listOf(
            listOf(entry("w", "工", 5000)),
            listOf(entry("a", "人", 5000)),
            listOf(entry("b", "大", 3000))
        )
        val result = engine.smartCompose(codes)
        val threeChar = result.filter { it.type == 3 }
        assertTrue(threeChar.isNotEmpty())
        assertEquals("工人大", threeChar[0].phrase)
    }

    @Test
    fun smartCompose_sortedByConfidence() = runBlocking {
        val codes = listOf(
            listOf(
                entry("w", "高", 5000),
                entry("x", "低", 100)
            ),
            listOf(
                entry("a", "频", 4000),
                entry("b", "弱", 200)
            )
        )
        val result = engine.smartCompose(codes)
        assertTrue(result.size >= 2)
        // Higher frequency words should have higher confidence
        assertTrue(result[0].confidence >= result.last().confidence)
    }
}
