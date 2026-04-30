package com.quickspeech.wubi.engine

import com.quickspeech.wubi.data.UserFrequencyEntry
import com.quickspeech.wubi.data.WubiWordEntry

/**
 * 候选词排序器
 * 词频优先 + 最近使用优先 + 用户习惯优先
 */
class CandidateSorter {

    /**
     * 对候选词列表进行综合排序
     *
     * @param candidates 原始候选词列表
     * @param userFrequencies 用户词频数据（word -> frequency entry）
     * @param recentWords 最近使用词列表
     * @param inputCode 当前输入编码（用于简码优先）
     */
    fun sort(
        candidates: List<WubiWordEntry>,
        userFrequencies: Map<String, UserFrequencyEntry> = emptyMap(),
        recentWords: Set<String> = emptySet(),
        inputCode: String = ""
    ): List<RankedCandidate> {
        return candidates
            .map { entry ->
                val score = calculateScore(entry, userFrequencies, recentWords, inputCode)
                RankedCandidate(entry, score)
            }
            .sortedByDescending { it.score }
            .mapIndexed { index, ranked -> ranked.copy(rank = index + 1) }
    }

    /**
     * 计算候选词综合得分
     *
     * 得分组成：
     * - 基础词频分 (0-10000): 来自词库预设频率
     * - 用户习惯分 (0-5000): 用户选择次数 * 权重
     * - 最近使用分 (0-3000): 最近使用加分
     * - 简码优先分 (0-2000): 简码词额外加分
     * - 类型优先分 (0-500): 单字 > 二字词 > 三字词 > 多字词
     */
    private fun calculateScore(
        entry: WubiWordEntry,
        userFrequencies: Map<String, UserFrequencyEntry>,
        recentWords: Set<String>,
        inputCode: String
    ): Int {
        var score = 0

        // 1. 基础词频分 (0-10000)
        score += entry.frequency

        // 2. 用户习惯分 (0-5000)
        val userFreq = userFrequencies[entry.word]
        if (userFreq != null) {
            // 选择次数越多，分数越高
            score += (userFreq.count * 100).coerceAtMost(5000)
            // 最近使用的额外加分
            val daysSinceUsed = (System.currentTimeMillis() - userFreq.lastUsed) / (1000 * 60 * 60 * 24)
            if (daysSinceUsed < 7) {
                score += 500 // 一周内使用过
            } else if (daysSinceUsed < 30) {
                score += 200 // 一月内使用过
            }
        }

        // 3. 最近使用分 (0-3000)
        if (entry.word in recentWords) {
            score += 3000
        }

        // 4. 简码优先分
        if (entry.simpleCode) {
            score += 2000
        }

        // 5. 类型优先分：单字 > 二字词 > 三字词
        score += when (entry.type) {
            0 -> 500   // 单字
            1 -> 300   // 二字词
            2 -> 100   // 三字词
            else -> 0  // 多字词
        }

        // 6. 编码长度匹配加分：编码完全匹配输入的优先
        if (entry.code == inputCode) {
            score += 1500
        }

        return score
    }
}

/** 带排名的候选词 */
data class RankedCandidate(
    val entry: WubiWordEntry,
    val score: Int,
    val rank: Int = 0
)
