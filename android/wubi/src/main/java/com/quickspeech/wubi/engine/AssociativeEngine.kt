package com.quickspeech.wubi.engine

import com.quickspeech.wubi.data.WubiDao
import com.quickspeech.wubi.data.WubiWordEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 词组联想引擎
 * 输入一个字后联想常用词组
 */
class AssociativeEngine(private val dao: WubiDao) {

    /**
     * 根据输入的单字联想常用词组
     * @param char 输入的单个汉字
     * @param limit 返回词组数量限制
     * @return 联想的词组列表
     */
    suspend fun associate(char: String, limit: Int = 20): List<WubiWordEntry> = withContext(Dispatchers.IO) {
        if (char.length != 1) return@withContext emptyList()
        try {
            dao.associateWords(char, limit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 智能组词：根据输入编码自动组合词组
     * 例如输入 "wtyp" 可能组合出 "中华人民共和国"
     *
     * @param codes 编码列表（每个编码对应一个字的候选）
     * @return 智能组合的词组列表
     */
    suspend fun smartCompose(codes: List<List<WubiWordEntry>>): List<ComposedPhrase> = withContext(Dispatchers.Default) {
        if (codes.isEmpty()) return@withContext emptyList()

        val results = mutableListOf<ComposedPhrase>()

        // 二字词组合
        if (codes.size >= 2) {
            for (first in codes[0].take(5)) {
                for (second in codes[1].take(5)) {
                    val phrase = first.word + second.word
                    val combinedCode = first.code + second.code
                    results.add(
                        ComposedPhrase(
                            phrase = phrase,
                            code = combinedCode,
                            type = 2,
                            confidence = calculateConfidence(first, second)
                        )
                    )
                }
            }
        }

        // 三字词组合
        if (codes.size >= 3) {
            for (first in codes[0].take(3)) {
                for (second in codes[1].take(3)) {
                    for (third in codes[2].take(3)) {
                        val phrase = first.word + second.word + third.word
                        val combinedCode = first.code + second.code + third.code
                        results.add(
                            ComposedPhrase(
                                phrase = phrase,
                                code = combinedCode,
                                type = 3,
                                confidence = calculateConfidence(first, second, third)
                            )
                        )
                    }
                }
            }
        }

        // 按置信度排序，返回前20个
        results.sortedByDescending { it.confidence }.take(20)
    }

    /**
     * 计算组合词组的置信度
     * 基于各个字的词频综合计算
     */
    private fun calculateConfidence(vararg entries: WubiWordEntry): Int {
        if (entries.isEmpty()) return 0
        val avgFrequency = entries.map { it.frequency }.average()
        val minFrequency = entries.minOf { it.frequency }
        // 综合考虑平均词频和最低词频
        return ((avgFrequency * 0.7 + minFrequency * 0.3) / 100).toInt()
    }
}

/** 智能组合的词组 */
data class ComposedPhrase(
    val phrase: String,
    val code: String,
    val type: Int,
    val confidence: Int
)
