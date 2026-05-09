package com.quickspeech.input.ai.engine

import com.quickspeech.common.db.BehaviorRecordDao
import com.quickspeech.common.db.BehaviorRecordEntity
import com.quickspeech.common.db.StyleProfileDao
import com.quickspeech.common.db.StyleProfileEntity
import com.quickspeech.common.db.UserActionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive user style profile containing all learned style preferences.
 */
data class UserStyleProfile(
    val formalityScore: Float = 0.5f,       // 0.0 = very casual, 1.0 = very formal
    val avgSentenceLength: Float = 20f,
    val vocabularyRichness: Float = 0.5f,   // 0.0 = simple, 1.0 = rich vocabulary
    val punctuationStyle: PunctuationStyle = PunctuationStyle(),
    val commonPhrases: List<String> = emptyList(),
    val responsePatterns: Map<String, List<String>> = emptyMap(),
    val perSceneProfiles: Map<String, SceneStyleProfile> = emptyMap(),
    val emojiUsage: EmojiUsage = EmojiUsage(),
    val timeBasedPatterns: Map<Int, Float> = emptyMap(), // hour -> formality score
    val lastUpdated: Long = System.currentTimeMillis(),
    val totalSamples: Int = 0
)

data class SceneStyleProfile(
    val scene: String,  // email, im, document
    val formalityScore: Float = 0.5f,
    val commonPhrases: List<String> = emptyList(),
    val avgResponseLength: Float = 20f
)

data class PunctuationStyle(
    val frequentExclamation: Boolean = false,
    val frequentQuestion: Boolean = false,
    val frequentEllipsis: Boolean = false,
    val frequentComma: Boolean = false,
    val usesPeriod: Boolean = true,
    val exclamationRatio: Float = 0f,
    val questionRatio: Float = 0f,
    val ellipsisRatio: Float = 0f,
    val commaRatio: Float = 0f
)

data class EmojiUsage(
    val usesEmoji: Boolean = false,
    val emojiRatio: Float = 0f,
    val commonEmojis: List<String> = emptyList(),
    val usesEmoticons: Boolean = false,  // e.g., :) :D ^_^
    val commonEmoticons: List<String> = emptyList()
)

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

    /**
     * Produce a comprehensive [UserStyleProfile] from all available behavior records.
     * This is the primary API for the style learning engine.
     */
    suspend fun analyzeComprehensive(): UserStyleProfile = withContext(Dispatchers.Default) {
        val ninetyDaysAgo = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
        val allRecords = behaviorRecordDao.getRecordsBetween(ninetyDaysAgo, System.currentTimeMillis())

        val acceptedTexts = allRecords
            .filter { it.userAction == UserActionType.ACCEPTED || it.userAction == UserActionType.MODIFIED }
            .mapNotNull { if (it.userAction == UserActionType.MODIFIED) it.modifiedReply else it.originalReply }

        val selfWrittenTexts = allRecords
            .filter { it.userAction == UserActionType.SELF_WRITTEN }
            .mapNotNull { it.selfWrittenReply }

        val analysisTexts = acceptedTexts + selfWrittenTexts

        if (analysisTexts.isEmpty()) {
            return@withContext loadExistingProfile() ?: UserStyleProfile()
        }

        val formalityScore = analyzeFormalityNormalized(analysisTexts)
        val avgSentenceLength = analyzeAvgSentenceLength(analysisTexts)
        val vocabularyRichness = analyzeVocabularyRichness(analysisTexts)
        val punctuationStyle = analyzePunctuationDetailed(analysisTexts)
        val commonPhrases = extractCommonPhrases(analysisTexts).take(30)
        val responsePatterns = analyzeResponsePatterns(allRecords)
        val perSceneProfiles = analyzePerSceneProfiles(allRecords)
        val emojiUsage = analyzeEmojiUsage(analysisTexts)
        val timeBasedPatterns = analyzeTimeBasedPatterns(allRecords)

        UserStyleProfile(
            formalityScore = formalityScore,
            avgSentenceLength = avgSentenceLength,
            vocabularyRichness = vocabularyRichness,
            punctuationStyle = punctuationStyle,
            commonPhrases = commonPhrases,
            responsePatterns = responsePatterns,
            perSceneProfiles = perSceneProfiles,
            emojiUsage = emojiUsage,
            timeBasedPatterns = timeBasedPatterns,
            lastUpdated = System.currentTimeMillis(),
            totalSamples = allRecords.size
        )
    }

    /**
     * Load the existing profile from the database and convert to UserStyleProfile.
     */
    suspend fun loadExistingProfile(): UserStyleProfile? = withContext(Dispatchers.Default) {
        val entity = styleProfileDao.getProfileSync() ?: return@withContext null
        UserStyleProfile(
            formalityScore = entity.formalityLevel / 10f,
            avgSentenceLength = analyzeAvgSentenceLengthFromPhrases(entity.commonPhrases),
            vocabularyRichness = calculateVocabularyRichnessFromPhrases(entity.commonPhrases),
            punctuationStyle = parsePunctuationStyle(entity.punctuationHabits),
            commonPhrases = entity.commonPhrases,
            responsePatterns = emptyMap(),
            perSceneProfiles = mapOf(
                "email" to SceneStyleProfile("email", formalityScore = parseSceneFormality(entity.emailStyle), commonPhrases = entity.commonPhrases),
                "im" to SceneStyleProfile("im", formalityScore = parseSceneFormality(entity.imStyle), commonPhrases = entity.commonPhrases),
                "document" to SceneStyleProfile("document", formalityScore = parseSceneFormality(entity.documentStyle), commonPhrases = entity.commonPhrases)
            ),
            lastUpdated = entity.lastUpdated,
            totalSamples = entity.totalSamples
        )
    }

    // ========== Formality Analysis ==========

    private fun analyzeFormality(texts: List<String>): Int {
        var formalCount = 0
        var informalCount = 0
        val formalIndicators = setOf("请", "您好", "贵", "敬请", "此致", "敬礼", "谨", "呈", "兹", "鉴于",
            "尊敬的", "阁下", "惠赐", "承蒙", "烦请", "恳请", "谨此", "顺颂", "为盼", "台鉴")
        val informalIndicators = setOf("哈哈", "嗯", "哦", "呀", "嘛", "吧", "呢", "哈", "嘿", "嗨",
            "呵呵", "嘻嘻", "嘿嘿", "啦啦", "么么", "哒", "咯", "呐", "喂", "哎")
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

    private fun analyzeFormalityNormalized(texts: List<String>): Float {
        var formalCount = 0
        var informalCount = 0
        val formalIndicators = setOf("请", "您好", "贵", "敬请", "此致", "敬礼", "谨", "呈", "兹", "鉴于",
            "尊敬的", "阁下", "惠赐", "承蒙", "烦请", "恳请", "谨此", "顺颂", "为盼", "台鉴")
        val informalIndicators = setOf("哈哈", "嗯", "哦", "呀", "嘛", "吧", "呢", "哈", "嘿", "嗨",
            "呵呵", "嘻嘻", "嘿嘿", "啦啦", "么么", "哒", "咯", "呐", "喂", "哎")
        for (text in texts) {
            for (indicator in formalIndicators) {
                if (text.contains(indicator)) formalCount++
            }
            for (indicator in informalIndicators) {
                if (text.contains(indicator)) informalCount++
            }
        }
        val total = formalCount + informalCount
        if (total == 0) return 0.5f
        return (formalCount.toFloat() / total).coerceIn(0f, 1f)
    }

    // ========== Sentence Length Analysis ==========

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

    private fun analyzeAvgSentenceLength(texts: List<String>): Float {
        if (texts.isEmpty()) return 20f
        val allSentences = texts.flatMap { text ->
            text.split(Regex("[。！？\n；;!?.]+")).map { it.trim() }.filter { it.isNotEmpty() }
        }
        if (allSentences.isEmpty()) return texts.map { it.length }.average().toFloat()
        return allSentences.map { it.length }.average().toFloat().coerceIn(1f, 200f)
    }

    // ========== Vocabulary Richness Analysis ==========

    private fun analyzeVocabularyRichness(texts: List<String>): Float {
        if (texts.isEmpty()) return 0.5f
        val allChars = texts.joinToString("")
        if (allChars.isEmpty()) return 0.5f
        val uniqueChars = allChars.toSet().size.toFloat()
        val totalChars = allChars.length.toFloat()
        // Type-token ratio for characters
        val charRatio = (uniqueChars / totalChars).coerceIn(0f, 1f)

        // Also measure word-level richness using common Chinese word boundaries
        val wordCount = countWords(allChars)
        val uniqueWordCount = extractWords(allChars).toSet().size.toFloat()
        val wordRatio = if (wordCount > 0) (uniqueWordCount / wordCount).coerceIn(0f, 1f) else 0.5f

        return ((charRatio + wordRatio) / 2f).coerceIn(0f, 1f)
    }

    private fun countWords(text: String): Int {
        // Simple word count: Chinese characters + English words
        val chineseCount = text.count { it in '一'..'鿿' }
        val englishWords = Regex("[a-zA-Z]+").findAll(text).count()
        return (chineseCount + englishWords).coerceAtLeast(1)
    }

    private fun extractWords(text: String): List<String> {
        val words = mutableListOf<String>()
        // Extract English words
        words.addAll(Regex("[a-zA-Z]+").findAll(text).map { it.value })
        // Extract Chinese bigrams as simple word approximation
        val chineseChars = text.filter { it in '一'..'鿿' }
        for (i in 0 until chineseChars.length - 1) {
            words.add("${chineseChars[i]}${chineseChars[i + 1]}")
        }
        return words
    }

    // ========== Phrase & Pattern Extraction ==========

    private fun extractCommonPhrases(texts: List<String>): List<String> {
        val phraseCount = mutableMapOf<String, Int>()
        for (text in texts) {
            for (len in 2..6) {
                for (i in 0..(text.length - len)) {
                    val phrase = text.substring(i, i + len)
                    // Skip phrases that are mostly punctuation
                    val punctuations = "，。！？、；：" + "“" + "”" + "‘" + "’" + "（）【】《》" + " \n\t"
                        val punctCount = phrase.count { punctuations.contains(it) }
                    if (punctCount > phrase.length / 2) continue
                    phraseCount[phrase] = (phraseCount[phrase] ?: 0) + 1
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

    // ========== Punctuation Analysis ==========

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

    private fun analyzePunctuationDetailed(texts: List<String>): PunctuationStyle {
        if (texts.isEmpty()) return PunctuationStyle()

        val exclamationCount = texts.sumOf { it.count { c -> c == '！' || c == '!' } }
        val questionCount = texts.sumOf { it.count { c -> c == '？' || c == '?' } }
        val ellipsisCount = texts.sumOf { it.count { c -> c == '…' } }
        val commaCount = texts.sumOf { it.count { c -> c == '，' || c == ',' } }
        val periodCount = texts.sumOf { it.count { c -> c == '。' || c == '.' } }
        val totalChars = texts.sumOf { it.length }.coerceAtLeast(1).toFloat()

        return PunctuationStyle(
            frequentExclamation = exclamationCount * 20 > totalChars,
            frequentQuestion = questionCount * 20 > totalChars,
            frequentEllipsis = ellipsisCount * 15 > totalChars,
            frequentComma = commaCount * 5 > totalChars,
            usesPeriod = periodCount > 0,
            exclamationRatio = (exclamationCount / totalChars).coerceIn(0f, 1f),
            questionRatio = (questionCount / totalChars).coerceIn(0f, 1f),
            ellipsisRatio = (ellipsisCount / totalChars).coerceIn(0f, 1f),
            commaRatio = (commaCount / totalChars).coerceIn(0f, 1f)
        )
    }

    // ========== Scene Style Analysis ==========

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

    // ========== Response Pattern Analysis ==========

    private fun analyzeResponsePatterns(records: List<BehaviorRecordEntity>): Map<String, List<String>> {
        val patterns = mutableMapOf<String, MutableList<String>>()

        for (record in records) {
            val category = categorizeContext(record.contextPrompt ?: record.originalReply)
            val response = when (record.userAction) {
                UserActionType.ACCEPTED -> record.originalReply
                UserActionType.MODIFIED -> record.modifiedReply
                UserActionType.SELF_WRITTEN -> record.selfWrittenReply
                else -> null
            } ?: continue
            patterns.getOrPut(category) { mutableListOf() }.add(response)
        }

        // Keep only top patterns per category
        return patterns.mapValues { (_, responses) ->
            responses.groupBy { it }.entries
                .sortedByDescending { it.value.size }
                .take(5)
                .map { it.key }
        }
    }

    private fun categorizeContext(context: String): String {
        val lower = context.lowercase()
        return when {
            lower.contains("你好") || lower.contains("hello") || lower.contains("hi") ||
                    lower.contains("早上好") || lower.contains("晚上好") -> "greeting"
            lower.contains("谢谢") || lower.contains("感谢") || lower.contains("thank") -> "thanks"
            lower.contains("再见") || lower.contains("bye") || lower.contains("拜拜") -> "farewell"
            lower.contains("吗") || lower.contains("什么") || lower.contains("如何") ||
                    lower.contains("?") || lower.contains("？") -> "question"
            lower.contains("好的") || lower.contains("可以") || lower.contains("同意") ||
                    lower.contains("ok") || lower.contains("行") -> "confirmation"
            else -> "general"
        }
    }

    // ========== Per-Scene Profile Analysis ==========

    private fun analyzePerSceneProfiles(records: List<BehaviorRecordEntity>): Map<String, SceneStyleProfile> {
        val scenes = listOf("email", "im", "document", "general")
        val profiles = mutableMapOf<String, SceneStyleProfile>()

        for (scene in scenes) {
            val sceneRecords = records.filter { it.sceneType == scene || (scene == "general" && it.sceneType !in listOf("email", "im", "document")) }
            if (sceneRecords.isEmpty()) {
                profiles[scene] = SceneStyleProfile(
                    scene = scene,
                    formalityScore = when (scene) {
                        "email" -> 0.8f
                        "im" -> 0.2f
                        "document" -> 0.6f
                        else -> 0.5f
                    },
                    commonPhrases = emptyList(),
                    avgResponseLength = 20f
                )
                continue
            }

            val texts = sceneRecords.mapNotNull {
                when (it.userAction) {
                    UserActionType.ACCEPTED -> it.originalReply
                    UserActionType.MODIFIED -> it.modifiedReply
                    UserActionType.SELF_WRITTEN -> it.selfWrittenReply
                    else -> null
                }
            }

            if (texts.isEmpty()) {
                profiles[scene] = SceneStyleProfile(scene, 0.5f, emptyList(), 20f)
                continue
            }

            val formality = analyzeFormalityNormalized(texts)
            val phrases = extractCommonPhrases(texts).take(15)
            val avgLen = texts.map { it.length }.average().toFloat().coerceIn(1f, 200f)

            profiles[scene] = SceneStyleProfile(
                scene = scene,
                formalityScore = formality,
                commonPhrases = phrases,
                avgResponseLength = avgLen
            )
        }

        return profiles
    }

    // ========== Emoji Usage Analysis ==========

    private fun analyzeEmojiUsage(texts: List<String>): EmojiUsage {
        if (texts.isEmpty()) return EmojiUsage()

        val emojiPattern = Regex(
            "[🌀-🗿]|[😀-🙏]|[🚀-🛿]|[☀-⛿]|[✀-➿]"
        )
        val emoticonPattern = Regex("""[:;=8][\-^]?[)(\/\\DpP\[\]{}|@><]|[)(\/\\DpP\[\]{}|@><][\-^]?[:;=8]|<3|^\^|T_T|>_<|O_O|o_o|-_-|\^_\^""")

        val allText = texts.joinToString("")
        val emojiCount = emojiPattern.findAll(allText).count()
        val emoticonCount = emoticonPattern.findAll(allText).count()
        val totalChars = allText.length.coerceAtLeast(1)

        val emojisFound = emojiPattern.findAll(allText).map { it.value }.toList()
        val emoticonsFound = emoticonPattern.findAll(allText).map { it.value }.toList()

        return EmojiUsage(
            usesEmoji = emojiCount > 0,
            emojiRatio = (emojiCount.toFloat() / totalChars).coerceIn(0f, 1f),
            commonEmojis = emojisFound.groupBy { it }.entries
                .sortedByDescending { it.value.size }
                .take(10)
                .map { it.key },
            usesEmoticons = emoticonCount > 0,
            commonEmoticons = emoticonsFound.groupBy { it }.entries
                .sortedByDescending { it.value.size }
                .take(10)
                .map { it.key }
        )
    }

    // ========== Time-Based Pattern Analysis ==========

    private fun analyzeTimeBasedPatterns(records: List<BehaviorRecordEntity>): Map<Int, Float> {
        val hourFormalityMap = mutableMapOf<Int, MutableList<Float>>()

        for (record in records) {
            val hour = java.util.Calendar.getInstance().apply {
                timeInMillis = record.timestamp
            }.get(java.util.Calendar.HOUR_OF_DAY)

            val text = when (record.userAction) {
                UserActionType.ACCEPTED -> record.originalReply
                UserActionType.MODIFIED -> record.modifiedReply
                UserActionType.SELF_WRITTEN -> record.selfWrittenReply
                else -> null
            } ?: continue

            val formality = analyzeFormalityNormalized(listOf(text))
            hourFormalityMap.getOrPut(hour) { mutableListOf() }.add(formality)
        }

        return hourFormalityMap.mapValues { (_, scores) ->
            scores.average().toFloat().coerceIn(0f, 1f)
        }
    }

    // ========== Helper Methods ==========

    private fun mergeLists(existing: List<String>, newItems: List<String>): List<String> {
        return (existing + newItems).toSet().toMutableList()
    }

    private fun parseSceneFormality(style: String): Float = when (style) {
        "formal" -> 0.8f
        "casual" -> 0.2f
        "neutral" -> 0.5f
        else -> 0.5f
    }

    private fun parsePunctuationStyle(habits: List<String>): PunctuationStyle {
        return PunctuationStyle(
            frequentExclamation = habits.contains("frequent_exclamation"),
            frequentQuestion = habits.contains("frequent_question"),
            frequentEllipsis = habits.contains("frequent_ellipsis"),
            frequentComma = habits.contains("frequent_comma"),
            usesPeriod = !habits.contains("no_period")
        )
    }

    private fun analyzeAvgSentenceLengthFromPhrases(phrases: List<String>): Float {
        if (phrases.isEmpty()) return 20f
        return phrases.map { it.length }.average().toFloat().coerceIn(1f, 200f)
    }

    private fun calculateVocabularyRichnessFromPhrases(phrases: List<String>): Float {
        if (phrases.isEmpty()) return 0.5f
        val allChars = phrases.joinToString("")
        if (allChars.isEmpty()) return 0.5f
        val uniqueChars = allChars.toSet().size.toFloat()
        val totalChars = allChars.length.toFloat()
        return (uniqueChars / totalChars).coerceIn(0f, 1f)
    }
}