package com.quickspeech.input.ai.engine

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StyleAdapterTest {

    private lateinit var styleAdapter: StyleAdapter

    @Before
    fun setup() {
        styleAdapter = StyleAdapter()
    }

    // ========== Formality Tests ==========

    @Test
    fun adaptReply_formalProfile_expandsShortText() {
        val profile = UserStyleProfile(
            formalityScore = 0.9f,
            perSceneProfiles = mapOf("email" to SceneStyleProfile("email", 0.9f))
        )
        val result = styleAdapter.adaptReply("好", profile, "email")
        assertTrue(result.length > 1, "Formal style should expand short text")
    }

    @Test
    fun adaptReply_casualProfile_shortensText() {
        val profile = UserStyleProfile(
            formalityScore = 0.1f,
            perSceneProfiles = mapOf("im" to SceneStyleProfile("im", 0.1f))
        )
        val result = styleAdapter.adaptReply("已收到您的来信，我们会尽快处理。", profile, "im")
        assertTrue(result.length <= 20, "Casual style should shorten formal text")
    }

    @Test
    fun adaptReply_formalProfile_replacesCasualWords() {
        val profile = UserStyleProfile(
            formalityScore = 0.9f,
            perSceneProfiles = mapOf("email" to SceneStyleProfile("email", 0.9f))
        )
        val result = styleAdapter.adaptReply("好的", profile, "email")
        assertTrue(result.contains("收到") || result.contains("了解"), "Should expand '好的' in formal mode")
    }

    @Test
    fun adaptReply_casualProfile_replacesFormalWords() {
        val profile = UserStyleProfile(
            formalityScore = 0.1f,
            perSceneProfiles = mapOf("im" to SceneStyleProfile("im", 0.1f))
        )
        val result = styleAdapter.adaptReply("已收到", profile, "im")
        assertTrue(result.contains("收到啦") || result.contains("收到"), "Should make formal text casual")
    }

    // ========== Punctuation Tests ==========

    @Test
    fun adaptReply_frequentExclamation_addsExclamation() {
        val profile = UserStyleProfile(
            punctuationStyle = PunctuationStyle(frequentExclamation = true)
        )
        val result = styleAdapter.adaptReply("好的。", profile, "general")
        assertTrue(result.contains("！"), "Should add exclamation for frequent exclamation users")
    }

    @Test
    fun adaptReply_noExclamation_removesExclamation() {
        val profile = UserStyleProfile(
            punctuationStyle = PunctuationStyle(frequentExclamation = false)
        )
        val result = styleAdapter.adaptReply("好的！", profile, "general")
        assertFalse(result.contains("！"), "Should remove exclamation for non-exclamation users")
    }

    @Test
    fun adaptReply_frequentEllipsis_addsEllipsis() {
        val profile = UserStyleProfile(
            punctuationStyle = PunctuationStyle(frequentEllipsis = true)
        )
        val result = styleAdapter.adaptReply("好的。", profile, "general")
        assertTrue(result.contains("……"), "Should add ellipsis for frequent ellipsis users")
    }

    // ========== Emoji Tests ==========

    @Test
    fun adaptReply_emojiUser_addsEmoji() {
        val profile = UserStyleProfile(
            emojiUsage = EmojiUsage(
                usesEmoji = true,
                commonEmojis = listOf("👍", "😊")
            )
        )
        val result = styleAdapter.adaptReply("好的", profile, "general")
        assertTrue(result.contains("👍") || result.contains("😊"), "Should add emoji for emoji users")
    }

    @Test
    fun adaptReply_noEmojiUser_noEmojiAdded() {
        val profile = UserStyleProfile(
            emojiUsage = EmojiUsage(usesEmoji = false)
        )
        val result = styleAdapter.adaptReply("好的", profile, "general")
        assertFalse(result.contains("👍"), "Should not add emoji for non-emoji users")
    }

    // ========== Sentence Length Tests ==========

    @Test
    fun adaptReply_shortPreference_simplifiesLongSentence() {
        val profile = UserStyleProfile(
            avgSentenceLength = 5f,
            perSceneProfiles = mapOf("im" to SceneStyleProfile("im", 0.2f, avgResponseLength = 5f))
        )
        val result = styleAdapter.adaptReply("这是一个非常长的句子，用来测试句子长度调整功能是否正常工作。", profile, "im")
        assertTrue(result.length <= 20, "Should simplify long sentences for short-preference users")
    }

    // ========== Batch Adaptation Tests ==========

    @Test
    fun adaptReplies_multipleReplies_adaptsAll() {
        val profile = UserStyleProfile(
            formalityScore = 0.9f,
            perSceneProfiles = mapOf("email" to SceneStyleProfile("email", 0.9f))
        )
        val replies = listOf("好", "收到", "谢谢")
        val results = styleAdapter.adaptReplies(replies, profile, "email")
        assertEquals(3, results.size, "Should return same number of replies")
    }

    @Test
    fun adaptReplies_emptyList_returnsEmpty() {
        val profile = UserStyleProfile()
        val results = styleAdapter.adaptReplies(emptyList(), profile, "general")
        assertTrue(results.isEmpty(), "Should return empty list for empty input")
    }

    @Test
    fun adaptReply_blankReply_returnsBlank() {
        val profile = UserStyleProfile()
        val result = styleAdapter.adaptReply("", profile, "general")
        assertEquals("", result, "Should return blank for blank input")
    }

    // ========== Scene-Specific Tests ==========

    @Test
    fun adaptReply_emailScene_usesEmailFormality() {
        val profile = UserStyleProfile(
            formalityScore = 0.5f,
            perSceneProfiles = mapOf(
                "email" to SceneStyleProfile("email", 0.9f),
                "im" to SceneStyleProfile("im", 0.1f)
            )
        )
        val emailResult = styleAdapter.adaptReply("好", profile, "email")
        val imResult = styleAdapter.adaptReply("好", profile, "im")
        // Email should be more formal (longer) than IM
        assertTrue(emailResult.length >= imResult.length,
            "Email result should be more formal than IM result")
    }

    @Test
    fun adaptReply_unknownScene_usesDefaultFormality() {
        val profile = UserStyleProfile(formalityScore = 0.5f)
        val result = styleAdapter.adaptReply("好的", profile, "unknown_scene")
        assertEquals("好的", result, "Unknown scene should use default formality without changes")
    }
}
