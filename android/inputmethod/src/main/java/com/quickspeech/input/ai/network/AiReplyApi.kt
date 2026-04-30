package com.quickspeech.input.ai.network

import com.quickspeech.input.ai.data.AiReplyRequest
import com.quickspeech.input.ai.data.AiReplyResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AiReplyApi {

    @POST("/api/v1/ai/replies")
    suspend fun getReplies(@Body request: AiReplyRequest): AiReplyResponse

    companion object {
        const val BASE_URL = "https://api.quickspeech.com/"
    }
}
