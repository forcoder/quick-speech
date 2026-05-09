package com.quickspeech.input.ai.engine

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adapts generated replies to match the user's learned style profile.
 * All processing is on-device and privacy-preserving.
 */
@Singleton
class StyleAdapter @Inject constructor() {

    /**
     * Adapt a single reply to match the user's style for the given scene.
     */
    fun adaptReply(
        reply: String,
        userStyle: UserStyleProfile,
        scene: String
    ): String {
        if (reply.isBlank()) return reply

        var adapted = reply

        // 1. Adjust formality
        adapted = adjustFormality(adapted, userStyle, scene)

        // 2. Adjust sentence length
        adapted = adjustSentenceLength(adapted, userStyle, scene)

        // 3. Adjust punctuation
        adapted = adjustPunctuation(adapted, userStyle.punctuationStyle)

        // 4. Add emoji if user commonly uses them
        adapted = adjustEmojiUsage(adapted, userStyle.emojiUsage)

        return adapted
    }

    /**
     * Adapt a list of replies to match the user's style.
     */
    fun adaptReplies(
        replies: List<String>,
        userStyle: UserStyleProfile,
        scene: String
    ): List<String> {
        if (replies.isEmpty()) return replies
        return replies.map { adaptReply(it, userStyle, scene) }
    }

    // ========== Formality Adjustment ==========

    private fun adjustFormality(text: String, userStyle: UserStyleProfile, scene: String): String {
        val sceneProfile = userStyle.perSceneProfiles[scene]
        val targetFormality = sceneProfile?.formalityScore ?: userStyle.formalityScore

        var result = text

        if (targetFormality > 0.7f) {
            // User prefers formal style - make text more formal
            result = makeFormal(result)
        } else if (targetFormality < 0.3f) {
            // User prefers casual style - make text more casual
            result = makeCasual(result)
        }

        return result
    }

    private fun makeFormal(text: String): String {
        var result = text
        // Replace casual expressions with formal ones
        val casualToFormal = mapOf(
            "好的" to "好的，收到",
            "收到" to "已收到",
            "谢谢" to "感谢",
            "明白" to "明白，已了解",
            "OK" to "好的",
            "ok" to "好的",
            "没问题" to "没有问题",
            "行" to "可以",
            "好的好的" to "好的，已了解",
            "嗯嗯" to "好的",
            "哈哈" to "",
            "嘿" to "",
            "嗨" to "",
            "拜拜" to "再见",
            "bye" to "再见",
            "嗯" to "好的",
            "哦" to "了解",
            "呀" to "",
            "嘛" to "",
            "吧" to "",
            "呢" to ""
        )

        for ((casual, formal) in casualToFormal) {
            if (result == casual) {
                result = formal
                break
            }
        }

        // If the text is very short and casual, expand it slightly
        if (result.length <= 2 && result.isNotBlank()) {
            result = when (result) {
                "好" -> "好的，收到"
                "行" -> "可以"
                "嗯" -> "好的"
                else -> result
            }
        }

        return result
    }

    private fun makeCasual(text: String): String {
        var result = text
        // Replace formal expressions with casual ones
        val formalToCasual = mapOf(
            "已收到" to "收到啦",
            "感谢" to "谢谢",
            "已了解" to "明白",
            "没有问题" to "没问题",
            "已审阅" to "看了",
            "此致敬礼" to "谢谢",
            "敬请" to "请",
            "贵" to "您的",
            "谨此" -> "这里",
            "顺颂" to "",
            "为盼" to "",
            "台鉴" to "",
            "惠赐" to "给",
            "承蒙" to "谢谢",
            "烦请" to "麻烦",
            "恳请" to "请"
        )

        for ((formal, casual) in formalToCasual) {
            result = result.replace(formal, casual)
        }

        // Remove trailing formal punctuation patterns
        if (result.endsWith("。") && result.length > 5) {
            // Keep period for longer formal sentences, remove for short ones
        }

        return result
    }

    // ========== Sentence Length Adjustment ==========

    private fun adjustSentenceLength(text: String, userStyle: UserStyleProfile, scene: String): String {
        val sceneProfile = userStyle.perSceneProfiles[scene]
        val targetLength = sceneProfile?.avgResponseLength ?: userStyle.avgSentenceLength

        // If user prefers short sentences and text is long, try to simplify
        if (targetLength < 10f && text.length > 20) {
            return simplifySentence(text)
        }

        // If user prefers longer sentences and text is very short, consider expanding
        if (targetLength > 40f && text.length < 5) {
            return expandSentence(text)
        }

        return text
    }

    private fun simplifySentence(text: String): String {
        // Split long sentences at natural break points
        val breakPoints = listOf("，", ",", "。", "；", ";")
        for (bp in breakPoints) {
            val idx = text.indexOf(bp)
            if (idx in 3..15) {
                return text.substring(0, idx)
            }
        }
        return text
    }

    private fun expandSentence(text: String): String {
        // Add context to very short responses
        return when (text) {
            "好" -> "好的"
            "收到" -> "收到，谢谢"
            "明白" -> "明白了"
            "OK" -> "OK，没问题"
            "行" -> "行，没问题"
            "可以" -> "可以，没问题"
            "谢谢" -> "谢谢！"
            else -> text
        }
    }

    // ========== Punctuation Adjustment ==========

    private fun adjustPunctuation(text: String, style: PunctuationStyle): String {
        var result = text

        // If user frequently uses exclamation marks, add them to enthusiastic responses
        if (style.frequentExclamation && !result.contains("！") && !result.contains("!")) {
            if (result.endsWith("。") || result.endsWith(".")) {
                result = result.dropLast(1) + "！"
            } else if (!result.endsWith("？") && !result.endsWith("?") && !result.endsWith("…")) {
                result = result + "！"
            }
        }

        // If user rarely uses exclamation, remove them
        if (!style.frequentExclamation && (result.contains("！") || result.contains("!"))) {
            result = result.replace("！", "。").replace("!", "。")
        }

        // If user frequently uses ellipsis, add them to trailing-off sentences
        if (style.frequentEllipsis && result.endsWith("。")) {
            result = result.dropLast(1) + "……"
        }

        // If user doesn't use periods (common in IM), remove trailing period
        if (!style.usesPeriod && result.endsWith("。")) {
            result = result.dropLast(1)
        }

        return result
    }

    // ========== Emoji Adjustment ==========

    private fun adjustEmojiUsage(text: String, emojiUsage: EmojiUsage): String {
        var result = text

        // If user commonly uses emojis, add appropriate ones
        if (emojiUsage.usesEmoji && emojiUsage.commonEmojis.isNotEmpty()) {
            // Only add emoji if the text doesn't already contain one
            val hasEmoji = emojiUsage.commonEmojis.any { text.contains(it) }
            if (!hasEmoji) {
                val emoji = selectEmojiForContext(text, emojiUsage.commonEmojis)
                if (emoji != null && result.length < 50) {
                    result = result + emoji
                }
            }
        }

        // If user uses emoticons, consider adding them
        if (emojiUsage.usesEmoticons && emojiUsage.commonEmoticons.isNotEmpty()) {
            val hasEmoticon = emojiUsage.commonEmoticons.any { text.contains(it) }
            if (!hasEmoticon && result.length < 30) {
                val emoticon = selectEmoticonForContext(text, emojiUsage.commonEmoticons)
                if (emoticon != null) {
                    result = result + emoticon
                }
            }
        }

        return result
    }

    private fun selectEmojiForContext(text: String, availableEmojis: List<String>): String? {
        // Simple context-based emoji selection
        return when {
            text.contains("谢谢") || text.contains("感谢") -> {
                availableEmojis.firstOrNull { it in listOf("🙏", "😊", "👍", "❤️", "🥰") }
            }
            text.contains("好的") || text.contains("收到") || text.contains("明白") -> {
                availableEmojis.firstOrNull { it in listOf("👍", "👌", "✅", "😊", "🙂") }
            }
            text.contains("再见") || text.contains("拜拜") -> {
                availableEmojis.firstOrNull { it in listOf("👋", "😊", "🙋", "✌️") }
            }
            text.contains("哈哈") || text.contains("嘿嘿") -> {
                availableEmojis.firstOrNull { it in listOf("😄", "😆", "😂", "🤣", "😊") }
            }
            else -> availableEmojis.firstOrNull()
        }
    }

    private fun selectEmoticonForContext(text: String, availableEmoticons: List<String>): String? {
        return when {
            text.contains("谢谢") || text.contains("感谢") -> {
                availableEmoticons.firstOrNull { it in listOf("^_^", "^^", ":)") }
            }
            text.contains("好的") || text.contains("收到") -> {
                availableEmoticons.firstOrNull { it in listOf("^_^", ":)", ":D") }
            }
            text.contains("再见") || text.contains("拜拜") -> {
                availableEmoticons.firstOrNull { it in listOf("^_^", ":)", "T_T") }
            }
            else -> availableEmoticons.firstOrNull()
        }
    }
}
