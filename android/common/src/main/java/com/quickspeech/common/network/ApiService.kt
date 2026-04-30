package com.quickspeech.common.network

import com.quickspeech.common.network.model.AiReplyRequest
import com.quickspeech.common.network.model.AiReplyResponse
import com.quickspeech.common.network.model.KnowledgeSearchRequest
import com.quickspeech.common.network.model.KnowledgeSearchResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("/api/v1/ai/reply")
    suspend fun generateAiReply(@Body request: AiRequestWrapper): Response<AiReplyResponse>

    @POST("/api/v1/knowledge/search")
    suspend fun searchKnowledge(@Body request: KnowledgeSearchRequest): Response<KnowledgeSearchResponse>
}

data class AiRequestWrapper(
    val context: String,
    val appType: String,
    val mode: String,
    val knowledgeBaseIds: List<String>? = null,
    val agentId: String? = null
)
