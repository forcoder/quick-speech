package com.quickspeech.input.ai.network

import com.quickspeech.input.ai.data.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

sealed class AiReplyResult {
    data class Success(val replies: List<AiReply>, val requestId: String) : AiReplyResult()
    data class Error(val message: String) : AiReplyResult()
    data object Loading : AiReplyResult()
}

@Singleton
class AiReplyRepository @Inject constructor(
    private val api: AiReplyApi,
    private val preferencesRepository: PreferencesRepository,
    private val feedbackDao: UserFeedbackDao
) {
    suspend fun fetchReplies(
        inputContext: String,
        appPackage: String,
        appCategory: AppCategory
    ): AiReplyResult {
        return try {
            val userId = preferencesRepository.userIdFlow.first()
            val mode = preferencesRepository.replyModeFlow.first()

            val request = AiReplyRequest(
                inputContext = inputContext,
                appPackage = appPackage,
                appCategory = appCategory.name,
                replyMode = mode.name,
                userId = userId
            )

            val response = api.getReplies(request)
            val replies = response.replies.map { item ->
                AiReply(
                    id = item.id,
                    text = item.text,
                    source = when (item.source) {
                        "KNOWLEDGE_BASE" -> ReplySource.KNOWLEDGE_BASE
                        "AI_AGENT" -> ReplySource.AI_AGENT
                        else -> ReplySource.HYBRID
                    },
                    confidence = item.confidence,
                    requestId = response.requestId
                )
            }

            AiReplyResult.Success(replies, response.requestId)
        } catch (e: Exception) {
            AiReplyResult.Error(e.message ?: "网络请求失败")
        }
    }

    suspend fun saveFeedback(feedback: UserFeedback) {
        feedbackDao.insertFeedback(
            UserFeedbackEntity(
                replyId = feedback.replyId,
                requestId = feedback.requestId,
                feedbackType = feedback.feedbackType.name,
                originalText = feedback.originalText,
                modifiedText = feedback.modifiedText,
                timestamp = feedback.timestamp,
                appPackage = feedback.appPackage,
                appCategory = feedback.appCategory.name
            )
        )
    }
}
