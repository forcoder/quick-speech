package com.quickspeech.input.ai.engine

import com.quickspeech.input.ai.data.AppCategory
import kotlin.random.Random

class LocalReplyGenerator {

    enum class ReplyStyle { FORMAL, CASUAL, BRIEF }

    fun generateReplies(
        context: String,
        appCategory: AppCategory,
        style: ReplyStyle = ReplyStyle.CASUAL,
        count: Int = 3
    ): List<GeneratedReply> {
        val normalizedContext = context.trim()
        if (normalizedContext.isBlank()) return emptyList()

        val templates = getTemplatesForCategory(appCategory, style)
        val replies = mutableListOf<GeneratedReply>()

        // Generate different replies based on intent analysis
        when (analyzeIntent(normalizedContext)) {
            IntentType.GREETING -> {
                val greetings = templates["greeting"] ?: listOf()
                replies.addAll(generateFromTemplates(greetings, normalizedContext, style, count))
            }
            IntentType.CONFIRMATION -> {
                val confirmations = templates["confirmation"] ?: listOf()
                replies.addAll(generateFromTemplates(confirmations, normalizedContext, style, count))
            }
            IntentType.QUESTION -> {
                val questions = templates["question"] ?: listOf()
                replies.addAll(generateFromTemplates(questions, normalizedContext, style, count))
            }
            IntentType.FAREWELL -> {
                val farewells = templates["farewell"] ?: listOf()
                replies.addAll(generateFromTemplates(farewells, normalizedContext, style, count))
            }
            IntentType.THANKS -> {
                val thanks = templates["thanks"] ?: listOf()
                replies.addAll(generateFromTemplates(thanks, normalizedContext, style, count))
            }
            IntentType.AGREEMENT -> {
                val agreements = templates["agreement"] ?: listOf()
                replies.addAll(generateFromTemplates(agreements, normalizedContext, style, count))
            }
            else -> {
                // Generic responses
                val generics = templates["generic"] ?: listOf()
                replies.addAll(generateFromTemplates(generics, normalizedContext, style, count))
            }
        }

        // Fill with fallback templates if needed
        while (replies.size < count) {
            val fallback = getFallbackTemplates(style).random()
            replies.add(GeneratedReply(
                text = fallback,
                style = style,
                confidence = Random.nextFloat() * 0.6f + 0.2f,
                source = "local"
            ))
        }

        // Deduplicate and limit
        return replies.distinctBy { it.text.lowercase() }.take(count)
            .sortedByDescending { it.confidence }
    }

    private fun analyzeIntent(text: String): IntentType {
        val lowerText = text.lowercase()

        return when {
            lowerText.contains("你好") || lowerText.contains("hello") || lowerText.contains("hi") ||
            lowerText.contains("早上好") || lowerText.contains("晚上好") || lowerText.contains("午安") ->
                IntentType.GREETING

            lowerText.contains("好的") || lowerText.contains("可以") || lowerText.contains("同意") ||
            lowerText.contains("没问题") || lowerText.contains("行") || lowerText.contains("ok") ->
                IntentType.CONFIRMATION

            lowerText.contains("吗") || lowerText.contains("?")
                    || lowerText.contains("什么") || lowerText.contains("如何") ||
                    lowerText.contains("哪里") || lowerText.contains("为什么") ||
                    lowerText.contains("how") || lowerText.contains("what") ||
                    lowerText.contains("where") || lowerText.contains("why") ->
                IntentType.QUESTION

            lowerText.contains("再见") || lowerText.contains("bye") || lowerText.contains("拜拜") ||
            lowerText.contains("告辞") || lowerText.contains("回头见") ||
            lowerText.contains("goodbye") || lowerText.contains("see you") ->
                IntentType.FAREWELL

            lowerText.contains("谢谢") || lowerText.contains("感谢") ||
            lowerText.contains("thank") || lowerText.contains("thx") ||
            lowerText.contains("thanks") ->
                IntentType.THANKS

            lowerText.contains("赞同") || lowerText.contains("同意") ||
            lowerText.contains("支持") || lowerText.contains("赞成") ||
            lowerText.contains("agree") || lowerText.contains("support") ->
                IntentType.AGREEMENT

            else -> IntentType.GENERIC
        }
    }

    private fun getTemplatesForCategory(appCategory: AppCategory, style: ReplyStyle): Map<String, List<String>> {
        val templates = mutableMapOf<String, MutableList<String>>()

        val baseTemplates = when (appCategory) {
            AppCategory.EMAIL -> getEmailTemplates(style)
            AppCategory.INSTANT_MESSAGING -> getImTemplates(style)
            AppCategory.DOCUMENT -> getDocumentTemplates(style)
            AppCategory.OTHER -> getDefaultTemplates(style)
        }

        baseTemplates.forEach { (category, list) ->
            templates.getOrPut(category) { mutableListOf() }.addAll(list)
        }

        return templates
    }

    private fun getEmailTemplates(style: ReplyStyle): Map<String, List<String>> {
        val formal = listOf(
            "感谢您的来信。",
            "已收到您的邮件，我们会尽快处理。",
            "谨此回复，请查收。",
            "感谢您的关注与支持。",
            "期待您的进一步沟通。",
            "此致敬礼。",
            "如有疑问，请随时联系。",
            "祝工作顺利。",
            "期待与您合作。",
            "敬请回复。"
        )

        val casual = listOf(
            "收到！谢谢你的邮件。",
            "好的，收到啦～",
            "明白，稍后给你回复。",
            "没问题，放心交给我。",
            "收到，辛苦啦！",
            "好的，马上处理。",
            "明白，这就去办。",
            "收到，谢谢！",
            "OK，没问题。",
            "好的，我知道了。"
        )

        val brief = listOf(
            "收到",
            "谢谢",
            "OK",
            "明白",
            "好的",
            "收到",
            "行",
            "可以",
            "没问题",
            "收到"
        )

        return mapOf(
            "generic" to when (style) {
                ReplyStyle.FORMAL -> formal
                ReplyStyle.CASUAL -> casual
                ReplyStyle.BRIEF -> brief
            }
        )
    }

    private fun getImTemplates(style: ReplyStyle): Map<String, List<String>> {
        val greeting = listOf(
            "你好！",
            "嗨～",
            "哈喽！",
            "在呢～",
            "我在这儿！",
            "收到！",
            "在的！"
        )

        val confirmation = listOf(
            "好的！",
            "收到～",
            "明白！",
            "OK！",
            "没问题！",
            "行行行！",
            "妥妥的！",
            "收到，安排！"
        )

        val question = listOf(
            "嗯？",
            "啥情况？",
            "怎么了？",
            "你说说看？",
            "具体说说？",
            "啥意思？",
            "详细讲讲呗？"
        )

        val thanks = listOf(
            "不客气！",
            "应该的～",
            "小事一桩！",
            "客气啥！",
            "没事儿！",
            "哈哈，别客气！"
        )

        val agreement = listOf(
            "完全同意！",
            "我也是这么想的～",
            "对对对！",
            "没错没错！",
            "完全赞同！",
            "说得对！",
            "就是这个理！"
        )

        val generic = listOf(
            "哈哈～",
            "嗯嗯！",
            "好的好的！",
            "收到！",
            "明白～",
            "行行行！",
            "妥妥的！",
            "没问题！",
            "OKOK！",
            "收到收到！"
        )

        return mapOf(
            "greeting" to greeting,
            "confirmation" to confirmation,
            "question" to question,
            "thanks" to thanks,
            "agreement" to agreement,
            "generic" to generic
        )
    }

    private fun getDocumentTemplates(style: ReplyStyle): Map<String, List<String>> {
        val formal = listOf(
            "已审阅相关内容。",
            "根据文档内容，建议如下：",
            "经分析，提出以下建议：",
            "综合考量后认为：",
            "基于现有资料，得出以下结论：",
            "经过仔细研究，发现：",
            "分析结果表明：",
            "综合各方面因素，建议：",
            "基于专业判断，认为：",
            "经过系统分析，得出："
        )

        val casual = listOf(
            "看起来不错！",
            "这个想法挺好的～",
            "我觉得可行。",
            "有道理！",
            "这个观点很中肯。",
            "确实如此！",
            "分析得很到位！",
            "总结得很棒！",
            "思路清晰！",
            "见解独到！"
        )

        val brief = listOf(
            "同意",
            "可行",
            "不错",
            "很好",
            "同意",
            "OK",
            "可以",
            "同意",
            "很好",
            "不错"
        )

        return mapOf(
            "generic" to when (style) {
                ReplyStyle.FORMAL -> formal
                ReplyStyle.CASUAL -> casual
                ReplyStyle.BRIEF -> brief
            }
        )
    }

    private fun getDefaultTemplates(style: ReplyStyle): Map<String, List<String>> {
        return getImTemplates(style) // Default to IM style
    }

    private fun getFallbackTemplates(style: ReplyStyle): List<String> {
        val fallbacks = listOf(
            "好的",
            "收到",
            "明白",
            "OK",
            "可以",
            "没问题",
            "行",
            "收到",
            "好的好的",
            "明白明白"
        )
        return when (style) {
            ReplyStyle.FORMAL -> fallbacks.map { "此致：$it" }
            ReplyStyle.CASUAL -> fallbacks.map { "$it！" }
            ReplyStyle.BRIEF -> fallbacks.take(5)
        }
    }

    private fun generateFromTemplates(
        templates: List<String>,
        context: String,
        style: ReplyStyle,
        maxCount: Int
    ): List<GeneratedReply> {
        if (templates.isEmpty()) return emptyList()

        val replies = templates.shuffled().take(maxCount).mapIndexed { index, template ->
            var reply = template

            // Simple context insertion for some styles
            if (style == ReplyStyle.CASUAL && Random.nextBoolean()) {
                val insertPoint = Random.nextInt(reply.length)
                reply = reply.substring(0, insertPoint) +
                        "[关于${context.take(5)}...]" +
                        reply.substring(insertPoint)
            }

            GeneratedReply(
                text = reply,
                style = style,
                confidence = calculateConfidence(template, context, style),
                source = "local"
            )
        }

        return replies
    }

    private fun calculateConfidence(template: String, context: String, style: ReplyStyle): Float {
        var confidence = 0.7f

        // Adjust based on template relevance (simple heuristic)
        if (template.length > 2) {
            confidence += 0.1f
        }
        if (template.length < 10) {
            confidence += 0.1f // Prefer concise responses
        }

        // Style-based adjustments
        confidence += when (style) {
            ReplyStyle.BRIEF -> 0.1f
            ReplyStyle.CASUAL -> 0.1f
            ReplyStyle.FORMAL -> 0.0f
        }

        return (confidence + Random.nextFloat() * 0.2f).coerceIn(0.1f, 1.0f)
    }

    data class GeneratedReply(
        val text: String,
        val style: ReplyStyle,
        val confidence: Float,
        val source: String = "local"
    )

    private enum class IntentType {
        GREETING, CONFIRMATION, QUESTION, FAREWELL, THANKS, AGREEMENT, GENERIC
    }
}