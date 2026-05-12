package com.quickspeech.input.ai.network

import com.quickspeech.input.ai.data.*
import com.quickspeech.input.ai.engine.LocalReplyGenerator
import com.quickspeech.input.ai.engine.ReplyContextAnalyzer
import kotlinx.coroutines.flow.first
import java.util.Collections
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
    private val feedbackDao: UserFeedbackDao,
    private val localReplyGenerator: LocalReplyGenerator,
    private val contextAnalyzer: ReplyContextAnalyzer
) {
    // LRU cache for recent replies (synchronized for thread safety)
    private val replyCache = Collections.synchronizedMap(
        object : LinkedHashMap<String, CachedReply>(MAX_CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedReply>?): Boolean {
                return size > MAX_CACHE_SIZE
            }
        }
    )

    // User preference scores for reply ranking (synchronized for thread safety)
    private val replyPreferenceScores = Collections.synchronizedMap(mutableMapOf<String, Float>())

    companion object {
        private const val MAX_CACHE_SIZE = 100
        private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes
        private const val LOCAL_SOURCE = "local"
        private const val REMOTE_SOURCE = "remote"
    }

    data class CachedReply(
        val replies: List<AiReply>,
        val timestamp: Long,
        val requestId: String
    )

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
            val remoteReplies = response.replies.map { item ->
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

            // Cache the remote replies
            val cacheKey = buildCacheKey(inputContext, appCategory)
            replyCache[cacheKey] = CachedReply(
                replies = remoteReplies,
                timestamp = System.currentTimeMillis(),
                requestId = response.requestId
            )

            // Merge with local replies for enhanced results
            val mergedReplies = mergeAndRankReplies(remoteReplies, inputContext, appCategory)

            AiReplyResult.Success(mergedReplies, response.requestId)
        } catch (e: Exception) {
            // On network error, try cache first, then fall back to local generation
            val cachedResult = getCachedReplies(inputContext, appCategory)
            if (cachedResult != null) {
                AiReplyResult.Success(cachedResult.replies, cachedResult.requestId)
            } else {
                // Generate local replies as fallback
                generateLocalReplies(inputContext, appCategory)
            }
        }
    }

    /**
     * Fetch replies with local fallback - tries network first, then cache, then local generation
     */
    suspend fun fetchRepliesWithFallback(
        inputContext: String,
        appPackage: String,
        appCategory: AppCategory,
        style: LocalReplyGenerator.ReplyStyle = LocalReplyGenerator.ReplyStyle.CASUAL
    ): AiReplyResult {
        // Try network first
        val networkResult = try {
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
            val remoteReplies = response.replies.map { item ->
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

            // Cache the results
            val cacheKey = buildCacheKey(inputContext, appCategory)
            replyCache[cacheKey] = CachedReply(
                replies = remoteReplies,
                timestamp = System.currentTimeMillis(),
                requestId = response.requestId
            )

            remoteReplies
        } catch (e: Exception) {
            null
        }

        val remoteReplies = networkResult

        // Generate local replies
        val localReplies = generateLocalReplyList(inputContext, appCategory, style)

        // Merge, deduplicate, and rank
        val allReplies = if (remoteReplies != null) {
            mergeAndRankReplies(remoteReplies + localReplies, inputContext, appCategory)
        } else {
            // Try cache if network failed
            val cached = getCachedReplies(inputContext, appCategory)
            if (cached != null) {
                mergeAndRankReplies(cached.replies + localReplies, inputContext, appCategory)
            } else {
                localReplies
            }
        }

        val requestId = java.util.UUID.randomUUID().toString()
        return AiReplyResult.Success(allReplies, requestId)
    }

    /**
     * Generate local replies only (offline mode)
     */
    suspend fun generateLocalRepliesOnly(
        inputContext: String,
        appCategory: AppCategory,
        style: LocalReplyGenerator.ReplyStyle = LocalReplyGenerator.ReplyStyle.CASUAL,
        count: Int = 3
    ): AiReplyResult {
        val localReplies = generateLocalReplyList(inputContext, appCategory, style, count)
        val requestId = java.util.UUID.randomUUID().toString()
        return AiReplyResult.Success(localReplies, requestId)
    }

    private fun generateLocalReplies(
        inputContext: String,
        appCategory: AppCategory
    ): AiReplyResult {
        val localReplies = generateLocalReplyList(inputContext, appCategory)
        val requestId = java.util.UUID.randomUUID().toString()
        return AiReplyResult.Success(localReplies, requestId)
    }

    private fun generateLocalReplyList(
        inputContext: String,
        appCategory: AppCategory,
        style: LocalReplyGenerator.ReplyStyle = LocalReplyGenerator.ReplyStyle.CASUAL,
        count: Int = 3
    ): List<AiReply> {
        // Analyze context for better reply generation
        val analysis = contextAnalyzer.analyze(inputContext, appCategory)

        // Adjust style based on app category if not explicitly set
        val effectiveStyle = when {
            appCategory == AppCategory.EMAIL && style == LocalReplyGenerator.ReplyStyle.CASUAL ->
                LocalReplyGenerator.ReplyStyle.FORMAL
            appCategory == AppCategory.DOCUMENT && style == LocalReplyGenerator.ReplyStyle.CASUAL ->
                LocalReplyGenerator.ReplyStyle.FORMAL
            else -> style
        }

        val generatedReplies = localReplyGenerator.generateReplies(
            context = inputContext,
            appCategory = appCategory,
            style = effectiveStyle,
            count = count
        )

        return generatedReplies.mapIndexed { index, reply ->
            AiReply(
                id = "local_${System.currentTimeMillis()}_$index",
                text = reply.text,
                source = ReplySource.LOCAL,
                confidence = reply.confidence * 0.8f,
                requestId = "local_${System.currentTimeMillis()}"
            )
        }
    }

    private fun mergeAndRankReplies(
        remoteReplies: List<AiReply>,
        inputContext: String,
        appCategory: AppCategory
    ): List<AiReply> {
        // Deduplicate by text content
        val seen = mutableSetOf<String>()
        val deduplicated = mutableListOf<AiReply>()

        for (reply in remoteReplies) {
            val key = reply.text.trim().lowercase()
            if (!seen.contains(key)) {
                seen.add(key)
                deduplicated.add(reply)
            }
        }

        // Rank based on user preferences and confidence
        val analysis = contextAnalyzer.analyze(inputContext, appCategory)
        return deduplicated
            .map { reply ->
                val preferenceScore = replyPreferenceScores[reply.text] ?: 0.5f
                val rankedConfidence = (reply.confidence * 0.7f) + (preferenceScore * 0.3f)
                reply.copy(confidence = rankedConfidence.coerceIn(0f, 1f))
            }
            .sortedByDescending { it.confidence }
    }

    private fun getCachedReplies(inputContext: String, appCategory: AppCategory): CachedReply? {
        val cacheKey = buildCacheKey(inputContext, appCategory)
        val cached = replyCache[cacheKey] ?: return null

        // Check TTL
        if (System.currentTimeMillis() - cached.timestamp > CACHE_TTL_MS) {
            replyCache.remove(cacheKey)
            return null
        }

        return cached
    }

    private fun buildCacheKey(inputContext: String, appCategory: AppCategory): String {
        return "${appCategory.name}_${inputContext.trim().lowercase().hashCode()}"
    }

    /**
     * Record user feedback for reply ranking improvement
     */
    fun recordReplyPreference(replyText: String, isPositive: Boolean) {
        synchronized(replyPreferenceScores) {
            val currentScore = replyPreferenceScores[replyText] ?: 0.5f
            val adjustment = if (isPositive) 0.1f else -0.1f
            replyPreferenceScores[replyText] = (currentScore + adjustment).coerceIn(0f, 1f)
        }
    }

    /**
     * Clear the reply cache
     */
    fun clearCache() {
        replyCache.clear()
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

        // Update preference scores based on feedback
        when (feedback.feedbackType) {
            FeedbackType.THUMBS_UP, FeedbackType.ADOPTED ->
                recordReplyPreference(feedback.originalText, true)
            FeedbackType.THUMBS_DOWN, FeedbackType.REJECTED ->
                recordReplyPreference(feedback.originalText, false)
            FeedbackType.MODIFIED ->
                // Slight positive for modified (user found it useful enough to edit)
                recordReplyPreference(feedback.originalText, true)
        }
    }
}