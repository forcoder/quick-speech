package com.quickspeech.wubi.engine

import com.quickspeech.wubi.data.RecentWordEntry
import com.quickspeech.wubi.data.UserFrequencyEntry
import com.quickspeech.wubi.data.WubiDao
import com.quickspeech.wubi.data.WubiWordEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FrequencyLearnerTest {

    private lateinit var learner: FrequencyLearner
    private lateinit var fakeDao: FakeLearnerDao

    private class FakeLearnerDao : WubiDao {
        var userFrequencies = mutableListOf<UserFrequencyEntry>()
        var recentWords = mutableListOf<RecentWordEntry>()
        var insertFrequencyCalled = false
        var incrementCalledWord: String? = null
        var insertRecentCalled = false
        var cleanOldCalled = false

        override suspend fun exactMatch(code: String): List<WubiWordEntry> = emptyList()
        override suspend fun prefixMatch(pattern: String, limit: Int): List<WubiWordEntry> = emptyList()
        override suspend fun fuzzyMatch(pattern: String, limit: Int): List<WubiWordEntry> = emptyList()
        override fun exactMatchFlow(code: String) = throw NotImplementedError()
        override fun prefixMatchFlow(pattern: String, limit: Int) = throw NotImplementedError()
        override suspend fun reverseLookup(word: String): List<WubiWordEntry> = emptyList()
        override suspend fun associateWords(pattern: String, limit: Int): List<WubiWordEntry> = emptyList()
        override suspend fun getUserFrequency(word: String): UserFrequencyEntry? =
            userFrequencies.find { it.word == word }
        override suspend fun getAllUserFrequencies(): List<UserFrequencyEntry> = userFrequencies.toList()
        override suspend fun insertUserFrequency(entry: UserFrequencyEntry) {
            insertFrequencyCalled = true
            userFrequencies.add(entry)
        }
        override suspend fun updateUserFrequency(entry: UserFrequencyEntry) {
            val idx = userFrequencies.indexOfFirst { it.word == entry.word }
            if (idx >= 0) userFrequencies[idx] = entry
        }
        override suspend fun incrementFrequency(word: String, timestamp: Long) {
            incrementCalledWord = word
            val idx = userFrequencies.indexOfFirst { it.word == word }
            if (idx >= 0) {
                userFrequencies[idx] = userFrequencies[idx].copy(
                    count = userFrequencies[idx].count + 1,
                    lastUsed = timestamp
                )
            }
        }
        override suspend fun getRecentWords(limit: Int): List<RecentWordEntry> =
            recentWords.sortedByDescending { it.timestamp }.take(limit)
        override suspend fun insertRecentWord(entry: RecentWordEntry) {
            insertRecentCalled = true
            recentWords.add(entry)
        }
        override suspend fun cleanOldRecentWords(expireTime: Long) {
            cleanOldCalled = true
            recentWords.removeAll { it.timestamp < expireTime }
        }
        override suspend fun getWordCount(): Int = 0
        override suspend fun insertWords(words: List<WubiWordEntry>) {}
    }

    @Before
    fun setup() {
        fakeDao = FakeLearnerDao()
        learner = FrequencyLearner(fakeDao)
    }

    @Test
    fun recordSelection_newWord_insertsFrequency() = runBlocking {
        learner.recordSelection("工作", "aa")
        assertTrue(fakeDao.insertFrequencyCalled)
        assertEquals("工作", fakeDao.userFrequencies[0].word)
        assertEquals(1, fakeDao.userFrequencies[0].count)
    }

    @Test
    fun recordSelection_newWord_insertsRecentWord() = runBlocking {
        learner.recordSelection("工作", "aa")
        assertTrue(fakeDao.insertRecentCalled)
        assertEquals("工作", fakeDao.recentWords[0].word)
    }

    @Test
    fun recordSelection_newWord_cleansOldWords() = runBlocking {
        learner.recordSelection("工作", "aa")
        assertTrue(fakeDao.cleanOldCalled)
    }

    @Test
    fun recordSelection_existingWord_incrementsCount() = runBlocking {
        fakeDao.userFrequencies.add(UserFrequencyEntry(word = "工作", code = "aa", count = 5))
        learner.recordSelection("工作", "aa")
        assertEquals("工作", fakeDao.incrementCalledWord)
        assertEquals(6, fakeDao.userFrequencies[0].count)
    }

    @Test
    fun getUserFrequencies_returnsMap() = runBlocking {
        fakeDao.userFrequencies.add(UserFrequencyEntry(word = "工作", code = "aa", count = 5))
        fakeDao.userFrequencies.add(UserFrequencyEntry(word = "中国", code = "khlg", count = 3))
        val result = learner.getUserFrequencies()
        assertEquals(2, result.size)
        assertNotNull(result["工作"])
        assertNotNull(result["中国"])
        assertEquals(5, result["工作"]!!.count)
    }

    @Test
    fun getUserFrequencies_empty_returnsEmpty() = runBlocking {
        val result = learner.getUserFrequencies()
        assertTrue(result.isEmpty())
    }

    @Test
    fun getRecentWords_returnsSet() = runBlocking {
        fakeDao.recentWords.add(RecentWordEntry(word = "工作", code = "aa", timestamp = System.currentTimeMillis()))
        fakeDao.recentWords.add(RecentWordEntry(word = "中国", code = "khlg", timestamp = System.currentTimeMillis()))
        val result = learner.getRecentWords()
        assertEquals(2, result.size)
        assertTrue(result.contains("工作"))
        assertTrue(result.contains("中国"))
    }

    @Test
    fun getRecentWords_respectsLimit() = runBlocking {
        for (i in 1..100) {
            fakeDao.recentWords.add(RecentWordEntry(word = "词$i", code = "x$i", timestamp = System.currentTimeMillis() + i))
        }
        val result = learner.getRecentWords(limit = 10)
        assertEquals(10, result.size)
    }

    @Test
    fun batchLearn_multipleWords_learnsAll() = runBlocking {
        val selections = listOf("工作" to "aa", "中国" to "khlg", "人民" to "wwnn")
        learner.batchLearn(selections)
        assertEquals(3, fakeDao.userFrequencies.size)
    }

    @Test
    fun batchLearn_duplicateWords_incrementsCount() = runBlocking {
        val selections = listOf("工作" to "aa", "工作" to "aa", "工作" to "aa")
        learner.batchLearn(selections)
        assertEquals(1, fakeDao.userFrequencies.size)
        assertEquals(3, fakeDao.userFrequencies[0].count)
    }

    @Test
    fun resetLearning_insertsZeroCountEntries() = runBlocking {
        fakeDao.userFrequencies.add(UserFrequencyEntry(word = "工作", code = "aa", count = 10, id = 1))
        fakeDao.userFrequencies.add(UserFrequencyEntry(word = "中国", code = "khlg", count = 5, id = 2))
        learner.resetLearning()
        // resetLearning calls insertUserFrequency with count=0 for each existing entry
        assertTrue(fakeDao.userFrequencies.any { it.word == "工作" && it.count == 0 })
        assertTrue(fakeDao.userFrequencies.any { it.word == "中国" && it.count == 0 })
    }
}
