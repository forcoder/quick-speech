package com.quickspeech.inputmethod.ai

import com.quickspeech.common.db.BehaviorRecordDao
import com.quickspeech.common.db.BehaviorRecordEntity
import com.quickspeech.common.db.StyleProfileDao
import com.quickspeech.common.db.StyleProfileEntity
import com.quickspeech.common.db.UserActionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StyleAnalyzer @Inject constructor(
    private val behaviorRecordDao: BehaviorRecordDao,
    private val styleProfileDao: StyleProfileDao
) {

    suspend fun analyzeAndUpdate() = withContext(Dispatchers.Default) {
        val oneWeekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        val recentRecords = behaviorRecordDao.getRecordsBetween(oneWeekAgo, System.currentTimeMillis())
        if (recentRecords.isEmpty()) return@withContext

        val acceptedTexts = recentRecords
            .filter { it.userAction == UserActionType.ACCEPTED || it.userAction == UserActionType.MODIFIED }
            .mapNotNull { if (it.userAction == UserActionType.MODIFIED) it.modifiedReply else it.originalReply }

        val selfWrittenTexts = recentRecords
            .filter { it.userAction == UserActionType.SELF_WRITTEN }
            .mapNotNull { it.selfWrittenReply }

        val analysisTexts = acceptedTexts + selfWrittenTexts
        if (analysisTexts.isEmpty()) return@withContext

        val formality = analyzeFormality(analysisTexts)
        val conciseness = analyzeConciseness(analysisTexts)
        val commonPhrases = extractCommonPhrases(analysisTexts)
        val sentencePatterns = extractSentencePatterns(analysisTexts)
        val punctuationHabits = analyzePunctuation(analysisTexts)

        val emailStyle = analyzeSceneStyle(recentRecords, "email")
        val imStyle = analyzeSceneStyle(recentRecords, "im")
        val documentStyle = analyzeSceneStyle(recentRecords, "document")

        val existing = styleProfileDao.getProfileSync()
        val mergedPhrases = mergeLists(existing?.commonPhrases ?: emptyList(), commonPhrases)
        val mergedPatterns = mergeLists(existing?.sentencePatterns ?: emptyList(), sentencePatterns)

        val updatedProfile = StyleProfileEntity(
            id = "default",
            formalityLevel = formality,
            concisenessLevel = conciseness,
            commonPhrases = mergedPhrases.take(50),
            sentencePatterns = mergedPatterns.take(30),
            punctuationHabits = punctuationHabits.take(20),
            emailStyle = emailStyle,
            imStyle = imStyle,
            documentStyle = documentStyle,
            lastUpdated = System.currentTimeMillis(),
            totalSamples = (existing?.totalSamples ?: 0) + recentRecords.size
        )
        styleProfileDao.insert(updatedProfile)
    }

    private fun analyzeFormality(texts: List<String>): Int {
        var formalCount = 0
        var informalCount = 0
        val formalIndicators = setOf("请", "您好", "贵", "敬请", "此致", "敬礼", "谨", "呈", "兹", "鉴于")
        val informalIndicators = setOf("哈哈", "嗯", "哦", "呀", "嘛", "吧", "呢", "哈", "嘿", "嗨")
        for (text in texts) {
            for (indicator in formalIndicators) {
                if (text.contains(indicator)) formalCount++
            }
            for (indicator in informalIndicators) {
                if (text.contains(indicator)) informalCount++
            }
        }
        val total = formalCount + informalCount
        if (total == 0) return 5
        return ((formalCount.toFloat() / total) * 10).toInt().coerceIn(1, 10)
    }

    private fun analyzeConciseness(texts: List<String>): Int {
        if (texts.isEmpty()) return 5
        val avgLength = texts.map { it.length }.average()
        return when {
            avgLength < 10 -> 9
            avgLength < 20 -> 7
            avgLength < 50 -> 5
            avgLength < 100 -> 3
            else -> 2
        }
    }

    private fun extractCommonPhrases(texts: List<String>): List<String> {
        val phraseCount = mutableMapOf<String, Int>()
        for (text in texts) {
            for (len in 2..6) {
                for (i in 0..(text.length - len)) {
                    val phrase = text.substring(i, i + len)
                    if (phrase.all { it.isLetterOrDigit() || it == '，' || it == '。' || it == '！' }) {
                        phraseCount[phrase] = (phraseCount[phrase] ?: 0) + 1
                    }
                }
            }
        }
        return phraseCount.entries
            .filter { it.value >= 2 }
            .sortedByDescending { it.value }
            .take(50)
            .map { it.key }
    }

    private fun extractSentencePatterns(texts: List<String>): List<String> {
        val patterns = mutableMapOf<String, Int>()
        for (text in texts) {
            val sentences = text.split(Regex("[。！？\n]"))
            for (sentence in sentences) {
                val trimmed = sentence.trim()
                if (trimmed.length in 3..20) {
                    patterns[trimmed] = (patterns[trimmed] ?: 0) + 1
                }
            }
        }
        return patterns.entries
            .filter { it.value >= 2 }
            .sortedByDescending { it.value }
            .take(30)
            .map { it.key }
    }

    private fun analyzePunctuation(texts: List<String>): List<String> {
        val habits = mutableListOf<String>()
        val exclamationCount = texts.sumOf { it.count { c -> c == '！' || c == '!' } }
        val questionCount = texts.sumOf { it.count { c -> c == '？' || c == '?' } }
        val ellipsisCount = texts.sumOf { it.count { c -> c == '…' } }
        val commaCount = texts.sumOf { it.count { c -> c == '，' || c == ',' } }
        val totalChars = texts.sumOf { it.length }.coerceAtLeast(1)

        if (exclamationCount * 20 > totalChars) habits.add("frequent_exclamation")
        if (questionCount * 20 > totalChars) habits.add("frequent_question")
        if (ellipsisCount * 15 > totalChars) habits.add("frequent_ellipsis")
        if (commaCount * 5 > totalChars) habits.add("frequent_comma")
        if (habits.isEmpty()) habits.add("standard_punctuation")
        return habits
    }

    private fun analyzeSceneStyle(records: List<BehaviorRecordEntity>, scene: String): String {
        val sceneRecords = records.filter { it.sceneType == scene }
        if (sceneRecords.isEmpty()) return when (scene) {
            "email" -> "formal"
            "im" -> "casual"
            "document" -> "neutral"
            else -> "neutral"
        }
        val texts = sceneRecords.mapNotNull {
            when (it.userAction) {
                UserActionType.ACCEPTED -> it.originalReply
                UserActionType.MODIFIED -> it.modifiedReply
                UserActionType.SELF_WRITTEN -> it.selfWrittenReply
                else -> null
            }
        }
        if (texts.isEmpty()) return "neutral"
        val formality = analyzeFormality(texts)
        return when {
            formality >= 7 -> "formal"
            formality >= 4 -> "neutral"
            else -> "casual"
        }
    }

    private fun mergeLists(existing: List<String>, newItems: List<String>): List<String> {
        val merged = (existing + newItems).toSet().toMutableList()
        val newSet = newItems.toSet()
        merged.sortByDescending { if (it in newSet) 1 else 0 }
        return merged
    }
}
