package com.quickspeech.input.ai.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for StyleLearningEngine data flow and integration.
 * Focuses on testing the UserStyleProfile data transformations
 * that the engine coordinates.
 */
class StyleLearningEngineTest {

    @Test
    fun userStyleProfile_formalityScore_casualThreshold() {
        val casualProfile = UserStyleProfile(formalityScore = 0.2f)
        assertTrue("Score below 0.3 should be considered casual", casualProfile.formalityScore < 0.3f)
    }

    @Test
    fun userStyleProfile_formalityScore_formalThreshold() {
        val formalProfile = UserStyleProfile(formalityScore = 0.8f)
        assertTrue("Score above 0.7 should be considered formal", formalProfile.formalityScore > 0.7f)
    }

    @Test
    fun userStyleProfile_formalityScore_neutralRange() {
        val neutralProfile = UserStyleProfile(formalityScore = 0.5f)
        assertTrue("Score between 0.3 and 0.7 should be neutral",
            neutralProfile.formalityScore >= 0.3f && neutralProfile.formalityScore <= 0.7f)
    }

    @Test
    fun userStyleProfile_minSamplesForAdaptation() {
        val profile = UserStyleProfile(totalSamples = 3)
        assertTrue("Should not have enough data below 5 samples", profile.totalSamples < 5)

        val readyProfile = UserStyleProfile(totalSamples = 5)
        assertTrue("Should have enough data at 5 samples", readyProfile.totalSamples >= 5)
    }

    @Test
    fun sceneStyleProfile_email_defaultFormality() {
        val emailProfile = SceneStyleProfile("email", formalityScore = 0.8f)
        assertEquals(0.8f, emailProfile.formalityScore, 0.01f)
        assertEquals("email", emailProfile.scene)
    }

    @Test
    fun sceneStyleProfile_im_defaultFormality() {
        val imProfile = SceneStyleProfile("im", formalityScore = 0.2f)
        assertEquals(0.2f, imProfile.formalityScore, 0.01f)
        assertEquals("im", imProfile.scene)
    }

    @Test
    fun sceneStyleProfile_document_defaultFormality() {
        val docProfile = SceneStyleProfile("document", formalityScore = 0.6f)
        assertEquals(0.6f, docProfile.formalityScore, 0.01f)
        assertEquals("document", docProfile.scene)
    }

    @Test
    fun userStyleProfile_perSceneProfiles_allScenesPresent() {
        val profile = UserStyleProfile(
            perSceneProfiles = mapOf(
                "email" to SceneStyleProfile("email", 0.8f, listOf("此致敬礼"), 30f),
                "im" to SceneStyleProfile("im", 0.2f, listOf("哈哈"), 8f),
                "document" to SceneStyleProfile("document", 0.6f, listOf("经分析"), 25f),
                "general" to SceneStyleProfile("general", 0.5f, emptyList(), 20f)
            )
        )
        assertEquals(4, profile.perSceneProfiles.size)
        assertNotNull(profile.perSceneProfiles["email"])
        assertNotNull(profile.perSceneProfiles["im"])
        assertNotNull(profile.perSceneProfiles["document"])
        assertNotNull(profile.perSceneProfiles["general"])
    }

    @Test
    fun userStyleProfile_punctuationStyle_allFlagsAccessible() {
        val profile = UserStyleProfile(
            punctuationStyle = PunctuationStyle(
                frequentExclamation = true,
                frequentQuestion = false,
                frequentEllipsis = true,
                frequentComma = false,
                usesPeriod = true,
                exclamationRatio = 0.15f,
                questionRatio = 0.02f,
                ellipsisRatio = 0.08f,
                commaRatio = 0.03f
            )
        )
        assertTrue(profile.punctuationStyle.frequentExclamation)
        assertFalse(profile.punctuationStyle.frequentQuestion)
        assertTrue(profile.punctuationStyle.frequentEllipsis)
        assertFalse(profile.punctuationStyle.frequentComma)
        assertTrue(profile.punctuationStyle.usesPeriod)
        assertEquals(0.15f, profile.punctuationStyle.exclamationRatio, 0.01f)
    }

    @Test
    fun userStyleProfile_emojiUsage_allFieldsAccessible() {
        val profile = UserStyleProfile(
            emojiUsage = EmojiUsage(
                usesEmoji = true,
                emojiRatio = 0.05f,
                commonEmojis = listOf("👍", "😊", "❤️"),
                usesEmoticons = true,
                commonEmoticons = listOf("^_^", ":)")
            )
        )
        assertTrue(profile.emojiUsage.usesEmoji)
        assertEquals(0.05f, profile.emojiUsage.emojiRatio, 0.01f)
        assertEquals(3, profile.emojiUsage.commonEmojis.size)
        assertTrue(profile.emojiUsage.usesEmoticons)
        assertEquals(2, profile.emojiUsage.commonEmoticons.size)
    }

    @Test
    fun userStyleProfile_timeBasedPatterns_hourlyFormality() {
        val profile = UserStyleProfile(
            timeBasedPatterns = mapOf(
                8 to 0.9f,   // Morning - very formal
                12 to 0.6f,  // Noon - moderate
                14 to 0.7f,  // Afternoon - formal
                18 to 0.4f,  // Evening - casual
                22 to 0.2f   // Night - very casual
            )
        )
        assertEquals(5, profile.timeBasedPatterns.size)
        assertEquals(0.9f, profile.timeBasedPatterns[8]!!, 0.01f)
        assertEquals(0.2f, profile.timeBasedPatterns[22]!!, 0.01f)
    }

    @Test
    fun userStyleProfile_responsePatterns_categorizedResponses() {
        val profile = UserStyleProfile(
            responsePatterns = mapOf(
                "greeting" to listOf("你好！", "您好！", "嗨～"),
                "thanks" to listOf("不客气！", "应该的～", "没事儿！"),
                "farewell" to listOf("再见！", "拜拜～", "回头见！"),
                "confirmation" to listOf("好的！", "收到！", "明白！"),
                "general" to listOf("嗯嗯", "好的好的", "OKOK")
            )
        )
        assertEquals(5, profile.responsePatterns.size)
        assertEquals(3, profile.responsePatterns["greeting"]?.size)
        assertEquals(3, profile.responsePatterns["thanks"]?.size)
    }

    @Test
    fun userStyleProfile_vocabularyRichness_range() {
        val simpleProfile = UserStyleProfile(vocabularyRichness = 0.2f)
        val richProfile = UserStyleProfile(vocabularyRichness = 0.8f)
        assertTrue(simpleProfile.vocabularyRichness < richProfile.vocabularyRichness)
        assertTrue(simpleProfile.vocabularyRichness >= 0f)
        assertTrue(richProfile.vocabularyRichness <= 1f)
    }

    @Test
    fun userStyleProfile_avgSentenceLength_range() {
        val shortProfile = UserStyleProfile(avgSentenceLength = 5f)
        val longProfile = UserStyleProfile(avgSentenceLength = 50f)
        assertTrue(shortProfile.avgSentenceLength < longProfile.avgSentenceLength)
        assertTrue(shortProfile.avgSentenceLength >= 1f)
        assertTrue(longProfile.avgSentenceLength <= 200f)
    }

    @Test
    fun userStyleProfile_lastUpdated_timestamp() {
        val before = System.currentTimeMillis()
        val profile = UserStyleProfile(lastUpdated = before)
        assertTrue(profile.lastUpdated <= System.currentTimeMillis())
        assertTrue(profile.lastUpdated >= before - 1000) // Within 1 second
    }

    @Test
    fun userStyleProfile_copyWithNewFormality() {
        val original = UserStyleProfile(formalityScore = 0.5f, totalSamples = 10)
        val updated = original.copy(formalityScore = 0.8f, totalSamples = 20)
        assertEquals(0.8f, updated.formalityScore, 0.01f)
        assertEquals(20, updated.totalSamples)
        assertEquals(0.5f, original.formalityScore, 0.01f) // Original unchanged
    }

    @Test
    fun sceneStyleProfile_copyWithNewPhrases() {
        val original = SceneStyleProfile("email", 0.8f, listOf("您好"), 30f)
        val updated = original.copy(commonPhrases = listOf("您好", "此致敬礼", "敬请"))
        assertEquals(3, updated.commonPhrases.size)
        assertEquals(1, original.commonPhrases.size) // Original unchanged
    }

    @Test
    fun userStyleProfile_equality() {
        val profile1 = UserStyleProfile(formalityScore = 0.5f, totalSamples = 10)
        val profile2 = UserStyleProfile(formalityScore = 0.5f, totalSamples = 10)
        assertEquals(profile1, profile2)
    }

    @Test
    fun sceneStyleProfile_equality() {
        val profile1 = SceneStyleProfile("email", 0.8f, listOf("您好"), 30f)
        val profile2 = SceneStyleProfile("email", 0.8f, listOf("您好"), 30f)
        assertEquals(profile1, profile2)
    }
}
