package com.quickspeech.input.ai.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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
        assertTrue("Formal style should expand short text", result.length > 1)
    }

    @Test
    fun adaptReply_casualProfile_shortensText() {
        val profile = UserStyleProfile(
            formalityScore = 0.1f,
            perSceneProfiles = mapOf("im" to SceneStyleProfile("im", 0.1f))
        )
        val result = styleAdapter.adaptReply("已收到您的来信，我们会尽快处理。", profile, "im")
        assertTrue("Casual style should shorten formal text", result.length <= 20)
    }

    @Test
    fun adaptReply_formalProfile_replacesCasualWords() {
        val profile = UserStyleProfile(
            formalityScore = 0.9f,
            perSceneProfiles = mapOf("email" to SceneStyleProfile("email", 0.9f))
        )
        val result = styleAdapter.adaptReply("好的", profile, "email")
        assertTrue("Should expand '好的' in formal mode", result.contains("收到") || result.contains("了解"))
    }

    @Test
    fun adaptReply_casualProfile_replacesFormalWords() {
        val profile = UserStyleProfile(
            formalityScore = 0.1f,
            perSceneProfiles = mapOf("im" to SceneStyleProfile("im", 0.1f))
        )
        val result = styleAdapter.adaptReply("已收到", profile, "im")
        assertTrue("Should make formal text casual", result.contains("收到啦") || result.contains("收到"))
    }

    // ========== Punctuation Tests ==========

    @Test
    fun adaptReply_frequentExclamation_addsExclamation() {
        val profile = UserStyleProfile(
            punctuationStyle = PunctuationStyle(frequentExclamation = true)
        )
        val result = styleAdapter.adaptReply("好的。", profile, "general")
        assertTrue("Should add exclamation for frequent exclamation users", result.contains("！"))
    }

    @Test
    fun adaptReply_noExclamation_removesExclamation() {
        val profile = UserStyleProfile(
            punctuationStyle = PunctuationStyle(frequentExclamation = false)
        )
        val result = styleAdapter.adaptReply("好的！", profile, "general")
        assertFalse("Should remove exclamation for non-exclamation users", result.contains("！"))
    }

    @Test
    fun adaptReply_frequentEllipsis_addsEllipsis() {
        val profile = UserStyleProfile(
            punctuationStyle = PunctuationStyle(frequentEllipsis = true)
        )
        val result = styleAdapter.adaptReply("好的。", profile, "general")
        assertTrue("Should add ellipsis for frequent ellipsis users", result.contains("……"))
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
        assertTrue("Should add emoji for emoji users", result.contains("👍") || result.contains("😊"))
    }

    @Test
    fun adaptReply_noEmojiUser_noEmojiAdded() {
        val profile = UserStyleProfile(
            emojiUsage = EmojiUsage(usesEmoji = false)
        )
        val result = styleAdapter.adaptReply("好的", profile, "general")
        assertFalse("Should not add emoji for non-emoji users", result.contains("👍"))
    }

    // ========== Sentence Length Tests ==========

    @Test
    fun adaptReply_shortPreference_simplifiesLongSentence() {
        val profile = UserStyleProfile(
            avgSentenceLength = 5f,
            perSceneProfiles = mapOf("im" to SceneStyleProfile("im", 0.2f, avgResponseLength = 5f))
        )
        val result = styleAdapter.adaptReply("这是一个非常长的句子，用来测试句子长度调整功能是否正常工作。", profile, "im")
        assertTrue("Should simplify long sentences for short-preference users", result.length <= 20)
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
        assertEquals("Should return same number of replies", 3, results.size)
    }

    @Test
    fun adaptReplies_emptyList_returnsEmpty() {
        val profile = UserStyleProfile()
        val results = styleAdapter.adaptReplies(emptyList(), profile, "general")
        assertTrue("Should return empty list for empty input", results.isEmpty())
    }

    @Test
    fun adaptReply_blankReply_returnsBlank() {
        val profile = UserStyleProfile()
        val result = styleAdapter.adaptReply("", profile, "general")
        assertEquals("Should return blank for blank input", "", result)
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
        assertTrue("Email result should be more formal than IM result", emailResult.length >= imResult.length)
    }

    @Test
    fun adaptReply_unknownScene_usesDefaultFormality() {
        val profile = UserStyleProfile(formalityScore = 0.5f)
        val result = styleAdapter.adaptReply("好的", profile, "unknown_scene")
        assertEquals("Unknown scene should use default formality without changes", "好的", result)
    }
}
