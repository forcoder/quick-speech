package com.quickspeech.input.ai.data

import com.google.gson.annotations.SerializedName

/**
 * AI回复模式枚举
 */
enum class ReplyMode(val displayName: String, val emoji: String) {
    KNOWLEDGE_BASE("知识库", "📚"),
    AI_AGENT("AI智能体", "🤖"),
    HYBRID("混合", "🔀");

    companion object {
        fun fromOrdinal(ordinal: Int): ReplyMode = entries.getOrElse(ordinal) { HYBRID }
    }
}

/**
 * 回复来源标签
 */
enum class ReplySource(val emoji: String) {
    KNOWLEDGE_BASE("📚"),
    AI_AGENT("🤖"),
    HYBRID("🔀")
}

/**
 * 应用类型分类
 */
enum class AppCategory {
    EMAIL,
    INSTANT_MESSAGING,
    DOCUMENT,
    OTHER
}

/**
 * 用户反馈类型
 */
enum class FeedbackType {
    THUMBS_UP,
    THUMBS_DOWN,
    ADOPTED,
    MODIFIED,
    REJECTED
}

// ========== API 请求/响应模型 ==========

data class AiReplyRequest(
    @SerializedName("input_context") val inputContext: String,
    @SerializedName("app_package") val appPackage: String,
    @SerializedName("app_category") val appCategory: String,
    @SerializedName("reply_mode") val replyMode: String,
    @SerializedName("user_id") val userId: String
)

data class AiReplyResponse(
    @SerializedName("replies") val replies: List<ReplyItem>,
    @SerializedName("request_id") val requestId: String
)

data class ReplyItem(
    @SerializedName("id") val id: String,
    @SerializedName("text") val text: String,
    @SerializedName("source") val source: String,
    @SerializedName("confidence") val confidence: Float
)

// ========== 本地数据模型 ==========

data class AiReply(
    val id: String,
    val text: String,
    val source: ReplySource,
    val confidence: Float,
    val requestId: String
)

data class UserFeedback(
    val replyId: String,
    val requestId: String,
    val feedbackType: FeedbackType,
    val originalText: String,
    val modifiedText: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val appPackage: String,
    val appCategory: AppCategory
)

data class AppCategoryMapping(
    val packageName: String,
    val category: AppCategory
)
