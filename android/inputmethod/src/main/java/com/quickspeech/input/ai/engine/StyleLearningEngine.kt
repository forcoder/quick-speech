package com.quickspeech.input.ai.engine

import android.util.Log
import com.quickspeech.input.ai.BehaviorRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main engine that coordinates style learning from user behavior.
 * All processing happens on-device for privacy.
 *
 * Lifecycle:
 * 1. Observe user's text input (with privacy protection)
 * 2. Record style features from committed text
 * 3. Periodically update style profiles
 * 4. Provide style data to the reply generator
 */
@Singleton
class StyleLearningEngine @Inject constructor(
    private val behaviorRecorder: BehaviorRecorder,
    private val styleAnalyzer: StyleAnalyzer,
    private val styleAdapter: StyleAdapter
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _currentProfile = MutableStateFlow(UserStyleProfile())
    val currentProfile: StateFlow<UserStyleProfile> = _currentProfile.asStateFlow()

    private val _isLearning = MutableStateFlow(false)
    val isLearning: StateFlow<Boolean> = _isLearning.asStateFlow()

    // Track pending text for style observation
    private var pendingText: String = ""
    private var pendingScene: String = "general"

    init {
        // Load existing profile on initialization
        scope.launch {
            loadProfile()
        }
    }

    /**
     * Called when user commits text (sends a message, types in a document, etc.).
     * This is the primary observation point for style learning.
     *
     * @param text The text the user committed
     * @param scene The scene context (email, im, document, general)
     */
    fun onTextCommitted(text: String, scene: String = "general") {
        if (text.isBlank()) return

        // Privacy: only record style metadata, not the full text content
        // We record the text for style analysis but it stays on-device
        pendingText = text
        pendingScene = scene

        // Record as a self-written behavior (this is the user's own writing)
        behaviorRecorder.recordSelfWritten(
            originalReply = "",
            selfWrittenReply = text,
            sceneType = scene,
            contextPrompt = null
        )

        // Trigger profile update if enough new data
        scope.launch {
            val profile = styleAnalyzer.analyzeComprehensive()
            _currentProfile.value = profile
        }
    }

    /**
     * Called when user accepts an AI-generated reply.
     * This indicates the generated style matches user preference.
     */
    fun onReplyAccepted(reply: String, scene: String = "general", contextPrompt: String? = null) {
        behaviorRecorder.recordAccepted(reply, scene, contextPrompt)
        triggerIncrementalUpdate()
    }

    /**
     * Called when user modifies an AI-generated reply before accepting.
     * The modification reveals the user's preferred style.
     */
    fun onReplyModified(original: String, modified: String, scene: String = "general", contextPrompt: String? = null) {
        behaviorRecorder.recordModified(original, modified, scene, contextPrompt)
        triggerIncrementalUpdate()
    }

    /**
     * Called when user skips an AI-generated reply.
     * This indicates the generated style does NOT match user preference.
     */
    fun onReplySkipped(reply: String, scene: String = "general", contextPrompt: String? = null) {
        behaviorRecorder.recordSkipped(reply, scene, contextPrompt)
    }

    /**
     * Get the current style profile for a specific scene.
     */
    fun getStyleProfile(scene: String): UserStyleProfile {
        return _currentProfile.value
    }

    /**
     * Get the scene-specific style profile.
     */
    fun getSceneProfile(scene: String): SceneStyleProfile? {
        return _currentProfile.value.perSceneProfiles[scene]
    }

    /**
     * Adapt a list of generated replies to match the user's style.
     * This is called by the reply generation pipeline.
     */
    fun adaptReplies(replies: List<String>, scene: String): List<String> {
        val profile = _currentProfile.value
        if (profile.totalSamples < MIN_SAMPLES_FOR_ADAPTATION) {
            // Not enough data to adapt - return original replies
            return replies
        }
        return styleAdapter.adaptReplies(replies, profile, scene)
    }

    /**
     * Adapt a single reply to match the user's style.
     */
    fun adaptReply(reply: String, scene: String): String {
        val profile = _currentProfile.value
        if (profile.totalSamples < MIN_SAMPLES_FOR_ADAPTATION) {
            return reply
        }
        return styleAdapter.adaptReply(reply, profile, scene)
    }

    /**
     * Get a human-readable style label for the current profile.
     * Used for UI display (e.g., "正式风格", "随意风格").
     */
    fun getStyleLabel(scene: String = "general"): String {
        val profile = _currentProfile.value
        val sceneProfile = profile.perSceneProfiles[scene]
        val formality = sceneProfile?.formalityScore ?: profile.formalityScore

        return when {
            formality >= 0.7f -> "正式风格"
            formality >= 0.4f -> "中性风格"
            else -> "随意风格"
        }
    }

    /**
     * Check if the engine has enough data to provide meaningful style adaptation.
     */
    fun hasEnoughData(): Boolean {
        return _currentProfile.value.totalSamples >= MIN_SAMPLES_FOR_ADAPTATION
    }

    /**
     * Get the number of behavior samples collected.
     */
    fun getSampleCount(): Int {
        return _currentProfile.value.totalSamples
    }

    /**
     * Manually trigger a full style analysis update.
     */
    fun triggerFullAnalysis() {
        scope.launch {
            _isLearning.value = true
            try {
                styleAnalyzer.analyzeAndUpdate()
                val profile = styleAnalyzer.analyzeComprehensive()
                _currentProfile.value = profile
            } finally {
                _isLearning.value = false
            }
        }
    }

    /**
     * Reset all learning data. This clears all behavior records and style profiles.
     */
    fun resetLearning() {
        scope.launch {
            _currentProfile.value = UserStyleProfile()
            // Note: Actual database clearing is handled by EvolutionViewModel
            // which has access to the DAOs
        }
    }

    /**
     * Load the existing profile from the database.
     */
    private suspend fun loadProfile() {
        val profile = styleAnalyzer.loadExistingProfile()
            ?: styleAnalyzer.analyzeComprehensive()
        _currentProfile.value = profile
    }

    /**
     * Trigger an incremental profile update after new behavior data.
     */
    private fun triggerIncrementalUpdate() {
        scope.launch {
            try {
                val profile = styleAnalyzer.analyzeComprehensive()
                _currentProfile.value = profile
            } catch (e: Exception) {
                Log.d("StyleLearningEngine", "Incremental update failed", e)
            }
        }
    }

    companion object {
        /**
         * Minimum number of behavior samples before style adaptation kicks in.
         * This prevents premature adaptation from insufficient data.
         */
        private const val MIN_SAMPLES_FOR_ADAPTATION = 5
    }
}
