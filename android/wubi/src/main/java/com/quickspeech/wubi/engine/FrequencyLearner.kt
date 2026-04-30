package com.quickspeech.wubi.engine

import com.quickspeech.wubi.data.RecentWordEntry
import com.quickspeech.wubi.data.UserFrequencyEntry
import com.quickspeech.wubi.data.WubiDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 词频学习引擎
 * 记录用户选择行为，实现个性化词频调整
 */
class FrequencyLearner(private val dao: WubiDao) {

    /**
     * 记录用户选择了一个词
     * 1. 增加用户词频计数
     * 2. 记录到最近使用表
     *
     * @param word 用户选择的词
     * @param code 对应的编码
     */
    suspend fun recordSelection(word: String, code: String) = withContext(Dispatchers.IO) {
        try {
            // 更新或插入用户词频
            val existing = dao.getUserFrequency(word)
            if (existing != null) {
                dao.incrementFrequency(word)
            } else {
                dao.insertUserFrequency(
                    UserFrequencyEntry(
                        word = word,
                        code = code,
                        count = 1,
                        lastUsed = System.currentTimeMillis()
                    )
                )
            }

            // 记录到最近使用
            dao.insertRecentWord(
                RecentWordEntry(
                    word = word,
                    code = code,
                    timestamp = System.currentTimeMillis()
                )
            )

            // 清理过期的最近使用记录（超过30天）
            val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
            dao.cleanOldRecentWords(thirtyDaysAgo)
        } catch (e: Exception) {
            // 学习失败不影响输入功能
        }
    }

    /**
     * 获取用户词频数据（用于排序）
     */
    suspend fun getUserFrequencies(): Map<String, UserFrequencyEntry> = withContext(Dispatchers.IO) {
        try {
            dao.getAllUserFrequencies().associateBy { it.word }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * 获取最近使用的词集合
     */
    suspend fun getRecentWords(limit: Int = 50): Set<String> = withContext(Dispatchers.IO) {
        try {
            dao.getRecentWords(limit).map { it.word }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * 批量学习：从用户输入历史中学习
     * 可用于导入用户历史数据
     */
    suspend fun batchLearn(selections: List<Pair<String, String>>) = withContext(Dispatchers.IO) {
        selections.forEach { (word, code) ->
            try {
                val existing = dao.getUserFrequency(word)
                if (existing != null) {
                    dao.updateUserFrequency(existing.copy(
                        count = existing.count + 1,
                        lastUsed = System.currentTimeMillis()
                    ))
                } else {
                    dao.insertUserFrequency(
                        UserFrequencyEntry(word = word, code = code, count = 1)
                    )
                }
            } catch (e: Exception) {
                // 忽略单个错误
            }
        }
    }

    /**
     * 重置用户学习数据
     */
    suspend fun resetLearning() = withContext(Dispatchers.IO) {
        try {
            // 清空用户词频表
            val allFreqs = dao.getAllUserFrequencies()
            allFreqs.forEach { entry ->
                dao.insertUserFrequency(entry.copy(count = 0))
            }
        } catch (e: Exception) {
            // 忽略
        }
    }
}
