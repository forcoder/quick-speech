package com.quickspeech.wubi.engine

import com.quickspeech.wubi.data.WubiDao
import com.quickspeech.wubi.data.WubiWordEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 五笔编码匹配器
 * 支持精确匹配、前缀匹配、模糊匹配（容错输入）
 */
class WubiMatcher(private val dao: WubiDao) {

    /**
     * 精确匹配：编码完全一致
     */
    suspend fun exactMatch(code: String): List<WubiWordEntry> = withContext(Dispatchers.IO) {
        if (code.isBlank()) return@withContext emptyList()
        try {
            dao.exactMatch(code.lowercase())
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 前缀匹配：输入过程中的实时候选
     * 例如输入 "w" 匹配所有以 "w" 开头的编码
     */
    suspend fun prefixMatch(prefix: String, limit: Int = 20): List<WubiWordEntry> = withContext(Dispatchers.IO) {
        if (prefix.isBlank()) return@withContext emptyList()
        try {
            dao.prefixMatch(prefix.lowercase(), limit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 模糊匹配（容错输入）
     * 1. z键通配：将z替换为任意字符进行匹配
     * 2. 常见错误编码自动修正
     */
    suspend fun fuzzyMatch(code: String, limit: Int = 20): List<WubiWordEntry> = withContext(Dispatchers.IO) {
        if (code.isBlank()) return@withContext emptyList()
        val lowerCode = code.lowercase()

        // 1. z键通配匹配：将z替换为_（SQL LIKE单字符通配符）
        if (lowerCode.contains('z')) {
            val pattern = lowerCode.replace('z', '_')
            try {
                val fuzzyResults = dao.fuzzyMatch(pattern, limit)
                if (fuzzyResults.isNotEmpty()) {
                    return@withContext fuzzyResults
                }
            } catch (e: Exception) {
                // 降级到前缀匹配
            }
        }

        // 2. 常见错误编码修正
        val correctedCode = correctCommonErrors(lowerCode)
        if (correctedCode != lowerCode) {
            try {
                val correctedResults = dao.exactMatch(correctedCode)
                if (correctedResults.isNotEmpty()) {
                    return@withContext correctedResults
                }
            } catch (e: Exception) {
                // 忽略
            }
        }

        // 3. 降级为前缀匹配
        try {
            dao.prefixMatch(lowerCode, limit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 智能匹配：结合精确匹配 + 前缀匹配 + 模糊匹配
     * 返回按优先级排序的候选词列表
     */
    suspend fun smartMatch(code: String, limit: Int = 20): MatchResult = withContext(Dispatchers.IO) {
        val lowerCode = code.lowercase()

        // 第一优先级：精确匹配
        val exactResults = try { dao.exactMatch(lowerCode) } catch (e: Exception) { emptyList() }
        if (exactResults.isNotEmpty()) {
            return@withContext MatchResult(lowerCode, exactResults, MatchType.EXACT)
        }

        // 第二优先级：z键通配
        if (lowerCode.contains('z')) {
            val pattern = lowerCode.replace('z', '_')
            val fuzzyResults = try { dao.fuzzyMatch(pattern, limit) } catch (e: Exception) { emptyList() }
            if (fuzzyResults.isNotEmpty()) {
                return@withContext MatchResult(lowerCode, fuzzyResults, MatchType.FUZZY)
            }
        }

        // 第三优先级：错误编码修正
        val correctedCode = correctCommonErrors(lowerCode)
        if (correctedCode != lowerCode) {
            val correctedResults = try { dao.exactMatch(correctedCode) } catch (e: Exception) { emptyList() }
            if (correctedResults.isNotEmpty()) {
                return@withContext MatchResult(lowerCode, correctedResults, MatchType.CORRECTED)
            }
        }

        // 第四优先级：前缀匹配
        val prefixResults = try { dao.prefixMatch(lowerCode, limit) } catch (e: Exception) { emptyList() }
        if (prefixResults.isNotEmpty()) {
            return@withContext MatchResult(lowerCode, prefixResults, MatchType.PREFIX)
        }

        MatchResult(lowerCode, emptyList(), MatchType.NONE)
    }

    /**
     * 常见错误编码修正表
     * 由于五笔86版中一些字根容易混淆，提供自动修正
     */
    private fun correctCommonErrors(code: String): String {
        var corrected = code

        // 常见错误修正
        val corrections = mapOf(
            "tf" to "tu", // 丿/竹 混淆
            "rq" to "rr", // 手/斤 末笔混淆
            "fn" to "fb", // 土/士 混淆
            "an" to "ab"  // 廾/艹 混淆
        )

        // 检查编码的前缀是否需要修正
        corrections.forEach { (wrong, right) ->
            if (corrected.startsWith(wrong)) {
                corrected = corrected.replaceFirst(wrong, right)
            }
        }

        return corrected
    }
}

/** 匹配结果 */
data class MatchResult(
    val query: String,
    val candidates: List<WubiWordEntry>,
    val matchType: MatchType
)

/** 匹配类型 */
enum class MatchType {
    EXACT,      // 精确匹配
    PREFIX,     // 前缀匹配
    FUZZY,      // 模糊匹配（z键通配）
    CORRECTED,  // 错误修正后匹配
    NONE        // 无匹配
}
