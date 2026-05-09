package com.quickspeech.input.ai.engine

import com.quickspeech.input.ai.BehaviorRecorder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StyleLearningEngineTest {

    @Mock
    private lateinit var behaviorRecorder: BehaviorRecorder

    @Mock
    private lateinit var styleAnalyzer: StyleAnalyzer

    @Mock
    private lateinit var styleAdapter: StyleAdapter

    private lateinit var engine: StyleLearningEngine

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        engine = StyleLearningEngine(behaviorRecorder, styleAnalyzer, styleAdapter)
    }

    @Test
    fun onTextCommitted_blankText_doesNothing() = runTest {
        engine.onTextCommitted("", "general")
        verify(behaviorRecorder, never()).recordSelfWritten(any(), any(), any(), any())
    }

    @Test
    fun onTextCommitted_validText_recordsBehavior() = runTest {
        engine.onTextCommitted("你好世界", "im")
        verify(behaviorRecorder).recordSelfWritten(
            originalReply = "",
            selfWrittenReply = "你好世界",
            sceneType = "im",
            contextPrompt = null
        )
    }

    @Test
    fun getStyleProfile_returnsCurrentProfile() = runTest {
        val profile = engine.getStyleProfile("general")
        assertEquals(0.5f, profile.formalityScore, 0.01f)
    }

    @Test
    fun getSceneProfile_unknownScene_returnsNull() = runTest {
        val profile = engine.getSceneProfile("unknown")
        assertEquals(null, profile)
    }

    @Test
    fun adaptReplies_notEnoughData_returnsOriginalReplies() = runTest {
        val replies = listOf("好的", "收到", "谢谢")
        val result = engine.adaptReplies(replies, "general")
        assertEquals(replies, result, "Should return original replies when not enough data")
    }

    @Test
    fun adaptReply_notEnoughData_returnsOriginalReply() = runTest {
        val reply = "好的"
        val result = engine.adaptReply(reply, "general")
        assertEquals(reply, result, "Should return original reply when not enough data")
    }

    @Test
    fun hasEnoughData_newEngine_returnsFalse() = runTest {
        assertFalse(engine.hasEnoughData(), "New engine should not have enough data")
    }

    @Test
    fun getSampleCount_newEngine_returnsZero() = runTest {
        assertEquals(0, engine.getSampleCount(), "New engine should have 0 samples")
    }

    @Test
    fun getStyleLabel_formalProfile_returnsFormalLabel() = runTest {
        // Default profile has formality 0.5, which is "中性风格"
        val label = engine.getStyleLabel("general")
        assertEquals("中性风格", label)
    }

    @Test
    fun onReplyAccepted_recordsBehavior() = runTest {
        engine.onReplyAccepted("好的", "im", "context")
        verify(behaviorRecorder).recordAccepted("好的", "im", "context")
    }

    @Test
    fun onReplyModified_recordsBehavior() = runTest {
        engine.onReplyModified("好的", "好的谢谢", "im", "context")
        verify(behaviorRecorder).recordModified("好的", "好的谢谢", "im", "context")
    }

    @Test
    fun onReplySkipped_recordsBehavior() = runTest {
        engine.onReplySkipped("好的", "im", "context")
        verify(behaviorRecorder).recordSkipped("好的", "im", "context")
    }

    @Test
    fun resetLearning_clearsProfile() = runTest {
        engine.resetLearning()
        val profile = engine.getStyleProfile("general")
        assertEquals(0, profile.totalSamples, "Reset should clear sample count")
    }

    @Test
    fun currentProfile_initialState_hasDefaultValues() = runTest {
        val profile = engine.currentProfile.first()
        assertEquals(0.5f, profile.formalityScore, 0.01f)
        assertEquals(20f, profile.avgSentenceLength, 0.01f)
        assertEquals(0.5f, profile.vocabularyRichness, 0.01f)
        assertTrue(profile.commonPhrases.isEmpty())
        assertTrue(profile.perSceneProfiles.isEmpty())
    }

    @Test
    fun isLearning_initialState_isFalse() = runTest {
        assertFalse(engine.isLearning.first(), "Initial learning state should be false")
    }
}
