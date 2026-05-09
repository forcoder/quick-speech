package com.quickspeech.wubi.engine

import com.quickspeech.wubi.data.UserFrequencyEntry
import com.quickspeech.wubi.data.WubiWordEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CandidateSorterTest {

    private lateinit var sorter: CandidateSorter

    @Before
    fun setup() {
        sorter = CandidateSorter()
    }

    @Test
    fun sort_emptyList_returnsEmpty() {
        val result = sorter.sort(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun sort_singleCandidate_returnsOne() {
        val candidates = listOf(
            WubiWordEntry(code = "a", word = "工", frequency = 5000, type = 0)
        )
        val result = sorter.sort(candidates)
        assertEquals(1, result.size)
        assertEquals("工", result[0].entry.word)
    }

    @Test
    fun sort_byFrequency_higherFirst() {
        val candidates = listOf(
            WubiWordEntry(code = "a", word = "低频", frequency = 100, type = 0),
            WubiWordEntry(code = "b", word = "高频", frequency = 5000, type = 0)
        )
        val result = sorter.sort(candidates)
        assertEquals("高频", result[0].entry.word)
        assertEquals("低频", result[1].entry.word)
    }

    @Test
    fun sort_byType_singleCharFirst() {
        // Single char (type=0) should rank higher than two-char word (type=1) with same frequency
        val candidates = listOf(
            WubiWordEntry(code = "ab", word = "词组", frequency = 5000, type = 1),
            WubiWordEntry(code = "a", word = "单字", frequency = 5000, type = 0)
        )
        val result = sorter.sort(candidates)
        assertEquals("单字", result[0].entry.word)
    }

    @Test
    fun sort_byUserFrequency_boostsRanking() {
        val candidates = listOf(
            WubiWordEntry(code = "a", word = "常用", frequency = 1000, type = 0),
            WubiWordEntry(code = "b", word = "不常用", frequency = 5000, type = 0)
        )
        val userFreqs = mapOf(
            "常用" to UserFrequencyEntry(word = "常用", count = 50, lastUsed = System.currentTimeMillis())
        )
        val result = sorter.sort(candidates, userFrequencies = userFreqs)
        // "常用" has lower base freq but high user frequency
        assertEquals("常用", result[0].entry.word)
    }

    @Test
    fun sort_byRecentWords_boostsRanking() {
        // recentWords adds 3000, so "最近" (1000+3000+500=4500) beats "普通" (5000+500=5500)
        // Need: freq + 3000 + typeBonus > otherFreq + typeBonus
        // "最近": 3000 + 3000 + 500 = 6500, "普通": 5000 + 500 = 5500
        val candidates = listOf(
            WubiWordEntry(code = "a", word = "最近", frequency = 3000, type = 0),
            WubiWordEntry(code = "b", word = "普通", frequency = 5000, type = 0)
        )
        val recentWords = setOf("最近")
        val result = sorter.sort(candidates, recentWords = recentWords)
        assertEquals("最近", result[0].entry.word)
    }

    @Test
    fun sort_exactCodeMatch_boostsRanking() {
        // exact match adds 1500, so "精确" (4000+1500+500=6000) beats "不精确" (5000+500=5500)
        val candidates = listOf(
            WubiWordEntry(code = "abcd", word = "精确", frequency = 4000, type = 0),
            WubiWordEntry(code = "abce", word = "不精确", frequency = 5000, type = 0)
        )
        val result = sorter.sort(candidates, inputCode = "abcd")
        assertEquals("精确", result[0].entry.word)
    }

    @Test
    fun sort_simpleCode_boostsRanking() {
        val candidates = listOf(
            WubiWordEntry(code = "a", word = "简码", frequency = 1000, type = 0, simpleCode = true),
            WubiWordEntry(code = "bc", word = "全码", frequency = 1000, type = 0, simpleCode = false)
        )
        val result = sorter.sort(candidates)
        assertEquals("简码", result[0].entry.word)
    }

    @Test
    fun sort_assignsCorrectRanks() {
        val candidates = listOf(
            WubiWordEntry(code = "a", word = "第一", frequency = 5000, type = 0),
            WubiWordEntry(code = "b", word = "第二", frequency = 4000, type = 0),
            WubiWordEntry(code = "c", word = "第三", frequency = 3000, type = 0)
        )
        val result = sorter.sort(candidates)
        assertEquals(1, result[0].rank)
        assertEquals(2, result[1].rank)
        assertEquals(3, result[2].rank)
    }

    @Test
    fun sort_scoresAreDescending() {
        val candidates = listOf(
            WubiWordEntry(code = "a", word = "a", frequency = 5000, type = 0),
            WubiWordEntry(code = "b", word = "b", frequency = 4000, type = 0),
            WubiWordEntry(code = "c", word = "c", frequency = 3000, type = 0),
            WubiWordEntry(code = "d", word = "d", frequency = 2000, type = 0)
        )
        val result = sorter.sort(candidates)
        for (i in 0 until result.size - 1) {
            assertTrue("Score at $i should be >= score at ${i + 1}",
                result[i].score >= result[i + 1].score)
        }
    }

    @Test
    fun sort_threeCharWord_lowerPriorityThanTwoChar() {
        val candidates = listOf(
            WubiWordEntry(code = "abc", word = "三字", frequency = 5000, type = 2),
            WubiWordEntry(code = "ab", word = "两字", frequency = 5000, type = 1)
        )
        val result = sorter.sort(candidates)
        assertEquals("两字", result[0].entry.word)
    }

    @Test
    fun sort_multiCharWord_lowestPriority() {
        // type 0 adds 500, type 3 adds 0
        // Give "单字" high freq to beat type penalty
        // "单字": 6000 + 500 = 6500, "四字": 4000 + 0 = 4000
        val candidates = listOf(
            WubiWordEntry(code = "abcd", word = "四字", frequency = 4000, type = 3),
            WubiWordEntry(code = "a", word = "单字", frequency = 6000, type = 0)
        )
        val result = sorter.sort(candidates)
        assertEquals("单字", result[0].entry.word)
    }
}
