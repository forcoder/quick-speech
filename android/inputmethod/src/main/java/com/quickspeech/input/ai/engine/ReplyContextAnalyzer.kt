package com.quickspeech.input.ai.engine

import com.quickspeech.input.ai.data.AppCategory

class ReplyContextAnalyzer {

    enum class Intent {
        GREETING, FAREWELL, QUESTION, STATEMENT,
        REQUEST, CONFIRMATION, APOLOGY, COMPLAINT,
        SUGGESTION, AGREEMENT, DISAGREEMENT, UNKNOWN
    }

    enum class Sentiment {
        POSITIVE, NEGATIVE, NEUTRAL, MIXED
    }

    enum class Urgency {
        URGENT, NORMAL, LOW
    }

    enum class ResponseType {
        ACKNOWLEDGE, ANSWER, DEFLECT, CLOSE,
        EMPATHIZE, CLARIFY, ESCALATE, DEFER
    }

    data class ContextAnalysis(
        val intent: Intent,
        val sentiment: Sentiment,
        val urgency: Urgency,
        val suggestedResponseType: ResponseType,
        val keywords: List<String> = emptyList(),
        val confidence: Float = 0.5f
    )

    fun analyze(text: String, appCategory: AppCategory): ContextAnalysis {
        val normalizedText = text.trim()
        if (normalizedText.isBlank()) {
            return ContextAnalysis(
                intent = Intent.UNKNOWN,
                sentiment = Sentiment.NEUTRAL,
                urgency = Urgency.NORMAL,
                suggestedResponseType = ResponseType.ACKNOWLEDGE,
                confidence = 0.1f
            )
        }

        val intent = detectIntent(normalizedText)
        val sentiment = detectSentiment(normalizedText)
        val urgency = detectUrgency(normalizedText, appCategory)
        val responseType = determineResponseType(intent, sentiment, urgency, appCategory)
        val keywords = extractKeywords(normalizedText)
        val confidence = calculateAnalysisConfidence(normalizedText, intent, sentiment)

        return ContextAnalysis(
            intent = intent,
            sentiment = sentiment,
            urgency = urgency,
            suggestedResponseType = responseType,
            keywords = keywords,
            confidence = confidence
        )
    }

    private fun detectIntent(text: String): Intent {
        val lowerText = text.lowercase()

        // Greeting patterns
        if (matchesAny(lowerText, listOf(
                "你好", "您好", "hello", "hi", "嗨", "哈喽",
                "早上好", "下午好", "晚上好", "午安",
                "good morning", "good afternoon", "good evening"
            ))) {
            return Intent.GREETING
        }

        // Farewell patterns
        if (matchesAny(lowerText, listOf(
                "再见", "拜拜", "bye", "goodbye", "告辞",
                "回头见", "下次见", "see you", "later",
                "先走了", "撤了", "溜了"
            ))) {
            return Intent.FAREWELL
        }

        // Question patterns
        if (matchesAny(lowerText, listOf(
                "吗", "什么", "怎么", "如何", "哪里", "哪儿",
                "为什么", "何时", "谁", "多少", "几个",
                "?", "？", "what", "how", "where", "when",
                "why", "who", "which", "能否", "是否", "能不能"
            ))) {
            return Intent.QUESTION
        }

        // Request patterns
        if (matchesAny(lowerText, listOf(
                "请", "麻烦", "帮我", "能不能", "可以吗",
                "希望", "需要", "想要", "please", "could you",
                "would you", "can you", "帮我", "劳驾"
            ))) {
            return Intent.REQUEST
        }

        // Confirmation patterns
        if (matchesAny(lowerText, listOf(
                "好的", "可以", "同意", "没问题", "行", "ok",
                "确认", "收到", "明白", "了解", "sure",
                "okay", "fine", "agreed", "没问题", "妥妥的"
            ))) {
            return Intent.CONFIRMATION
        }

        // Apology patterns
        if (matchesAny(lowerText, listOf(
                "对不起", "抱歉", "不好意思", "sorry", "apologize",
                "请原谅", "失礼了", "my bad", "excuse me"
            ))) {
            return Intent.APOLOGY
        }

        // Complaint patterns
        if (matchesAny(lowerText, listOf(
                "太差", "不行", "有问题", "投诉", "不满",
                "失望", "糟糕", "terrible", "awful", "bad",
                "hate", "disappointed", "unacceptable"
            ))) {
            return Intent.COMPLAINT
        }

        // Suggestion patterns
        if (matchesAny(lowerText, listOf(
                "建议", "不如", "要不要", "试试", "可以考虑",
                "我建议", "maybe", "perhaps", "suggest",
                "how about", "what if", "不如", "要不"
            ))) {
            return Intent.SUGGESTION
        }

        // Agreement patterns
        if (matchesAny(lowerText, listOf(
                "赞同", "支持", "赞成", "同意", "说得对",
                "有道理", "没错", "对对对", "agree", "correct",
                "right", "exactly", "absolutely"
            ))) {
            return Intent.AGREEMENT
        }

        // Disagreement patterns
        if (matchesAny(lowerText, listOf(
                "不同意", "反对", "不对", "不是", "不行",
                "不可以", "disagree", "wrong", "incorrect",
                "not right", "不行", "不可以"
            ))) {
            return Intent.DISAGREEMENT
        }

        // Default to statement
        return Intent.STATEMENT
    }

    private fun detectSentiment(text: String): Sentiment {
        val lowerText = text.lowercase()

        var positiveScore = 0
        var negativeScore = 0

        val positiveIndicators = listOf(
            "好", "棒", "赞", "优秀", "出色", "完美", "喜欢",
            "感谢", "谢谢", "开心", "高兴", "满意", "期待",
            "good", "great", "excellent", "amazing", "wonderful",
            "love", "like", "happy", "glad", "thanks", "thank",
            "nice", "awesome", "perfect", "beautiful", "哈哈", "嘿嘿",
            "不错", "厉害", "强", "牛", "太好了", "太棒了"
        )

        val negativeIndicators = listOf(
            "差", "糟", "烂", "坏", "讨厌", "烦", "失望",
            "难过", "伤心", "生气", "愤怒", "不满", "抱怨",
            "bad", "terrible", "awful", "horrible", "hate",
            "angry", "sad", "disappointed", "frustrated", "annoying",
            "worst", "ugly", "stupid", "useless", "无语", "郁闷",
            "烦死了", "受不了", "太差了", "不行", "不可以"
        )

        for (indicator in positiveIndicators) {
            if (lowerText.contains(indicator)) positiveScore++
        }

        for (indicator in negativeIndicators) {
            if (lowerText.contains(indicator)) negativeScore++
        }

        return when {
            positiveScore > 0 && negativeScore > 0 -> Sentiment.MIXED
            positiveScore > negativeScore -> Sentiment.POSITIVE
            negativeScore > positiveScore -> Sentiment.NEGATIVE
            else -> Sentiment.NEUTRAL
        }
    }

    private fun detectUrgency(text: String, appCategory: AppCategory): Urgency {
        val lowerText = text.lowercase()

        // Urgent indicators
        if (matchesAny(lowerText, listOf(
                "紧急", "急", "马上", "立刻", "尽快", "urgent",
                "asap", "immediately", "emergency", "critical",
                "火急", "刻不容缓", "十万火急", "急事"
            ))) {
            return Urgency.URGENT
        }

        // Low urgency indicators
        if (matchesAny(lowerText, listOf(
                "有空", "不急", "慢慢", "随时", "方便时",
                "no rush", "take your time", "whenever",
                "不急", "慢慢来", "有空时"
            ))) {
            return Urgency.LOW
        }

        // App category based urgency
        return when (appCategory) {
            AppCategory.EMAIL -> {
                if (matchesAny(lowerText, listOf("urgent", "asap", "紧急", "急"))) {
                    Urgency.URGENT
                } else {
                    Urgency.NORMAL
                }
            }
            AppCategory.INSTANT_MESSAGING -> Urgency.NORMAL
            AppCategory.DOCUMENT -> Urgency.LOW
            AppCategory.OTHER -> Urgency.NORMAL
        }
    }

    private fun determineResponseType(
        intent: Intent,
        sentiment: Sentiment,
        urgency: Urgency,
        appCategory: AppCategory
    ): ResponseType {
        return when (intent) {
            Intent.GREETING -> ResponseType.ACKNOWLEDGE
            Intent.FAREWELL -> ResponseType.CLOSE
            Intent.QUESTION -> ResponseType.ANSWER
            Intent.REQUEST -> {
                if (urgency == Urgency.URGENT) ResponseType.ACKNOWLEDGE
                else ResponseType.DEFER
            }
            Intent.CONFIRMATION -> ResponseType.ACKNOWLEDGE
            Intent.APOLOGY -> ResponseType.EMPATHIZE
            Intent.COMPLAINT -> {
                if (sentiment == Sentiment.NEGATIVE) ResponseType.EMPATHIZE
                else ResponseType.ACKNOWLEDGE
            }
            Intent.SUGGESTION -> ResponseType.ACKNOWLEDGE
            Intent.AGREEMENT -> ResponseType.ACKNOWLEDGE
            Intent.DISAGREEMENT -> ResponseType.DEFLECT
            Intent.STATEMENT -> {
                when (appCategory) {
                    AppCategory.EMAIL -> ResponseType.ACKNOWLEDGE
                    AppCategory.INSTANT_MESSAGING -> ResponseType.ACKNOWLEDGE
                    AppCategory.DOCUMENT -> ResponseType.ACKNOWLEDGE
                    AppCategory.OTHER -> ResponseType.ACKNOWLEDGE
                }
            }
            Intent.UNKNOWN -> ResponseType.CLARIFY
        }
    }

    private fun extractKeywords(text: String): List<String> {
        val keywords = mutableListOf<String>()

        // Extract Chinese keywords (2-4 character phrases)
        for (len in 2..4) {
            for (i in 0..(text.length - len)) {
                val phrase = text.substring(i, i + len)
                if (isSignificantPhrase(phrase)) {
                    keywords.add(phrase)
                }
            }
        }

        // Extract English words
        val englishWords = Regex("[a-zA-Z]+").findAll(text).map { it.value }.toList()
        keywords.addAll(englishWords.filter { it.length > 2 })

        return keywords.distinct().take(10)
    }

    private fun isSignificantPhrase(phrase: String): Boolean {
        // Filter out common stop words and punctuation
        val stopWords = setOf("的", "了", "是", "在", "有", "和", "与", "或",
            "这", "那", "我", "你", "他", "她", "它", "们",
            "个", "些", "什么", "怎么", "如何", "可以", "能",
            "不", "没", "很", "非常", "太", "比较", "更", "最")
        return !stopWords.contains(phrase) && !phrase.all { it.isWhitespace() || "，。！？、；：\"\"''（）【】《》".contains(it) }
    }

    private fun calculateAnalysisConfidence(text: String, intent: Intent, sentiment: Sentiment): Float {
        var confidence = 0.5f

        // Longer text generally gives more confidence
        confidence += (text.length.coerceIn(0, 100) / 100f) * 0.2f

        // Known intents boost confidence
        if (intent != Intent.UNKNOWN && intent != Intent.STATEMENT) {
            confidence += 0.2f
        }

        // Clear sentiment boosts confidence
        if (sentiment != Sentiment.NEUTRAL) {
            confidence += 0.1f
        }

        return confidence.coerceIn(0.1f, 1.0f)
    }

    private fun matchesAny(text: String, patterns: List<String>): Boolean {
        return patterns.any { text.contains(it) }
    }
}