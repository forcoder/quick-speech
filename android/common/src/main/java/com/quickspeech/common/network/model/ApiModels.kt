package com.quickspeech.common.network.model

data class AiReplyRequest(
    val context: String,
    val appType: String,
    val mode: String = "hybrid",
    val knowledgeBaseIds: List<String>? = null,
    val agentId: String? = null
)

data class AiReplyResponse(
    val replies: List<AiReplyItem>,
    val mode: String
)

data class AiReplyItem(
    val text: String,
    val source: String,
    val score: Float
)

data class KnowledgeSearchRequest(
    val query: String,
    val knowledgeBaseIds: List<String>? = null,
    val limit: Int = 10
)

data class KnowledgeSearchResponse(
    val results: List<KnowledgeResult>
)

data class KnowledgeResult(
    val content: String,
    val source: String,
    val score: Float,
    val knowledgeBaseId: String
)
