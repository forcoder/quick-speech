package com.quickspeech.input.ai.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the style profile data classes to ensure correct defaults and construction.
 */
class StyleProfileDataClassesTest {

    @Test
    fun userStyleProfile_defaultValues() {
        val profile = UserStyleProfile()
        assertEquals(0.5f, profile.formalityScore, 0.01f)
        assertEquals(20f, profile.avgSentenceLength, 0.01f)
        assertEquals(0.5f, profile.vocabularyRichness, 0.01f)
        assertTrue(profile.commonPhrases.isEmpty())
        assertTrue(profile.responsePatterns.isEmpty())
        assertTrue(profile.perSceneProfiles.isEmpty())
        assertTrue(profile.timeBasedPatterns.isEmpty())
        assertEquals(0, profile.totalSamples)
    }

    @Test
    fun userStyleProfile_customValues() {
        val profile = UserStyleProfile(
            formalityScore = 0.8f,
            avgSentenceLength = 30f,
            vocabularyRichness = 0.7f,
            commonPhrases = listOf("您好", "此致敬礼"),
            totalSamples = 100
        )
        assertEquals(0.8f, profile.formalityScore, 0.01f)
        assertEquals(30f, profile.avgSentenceLength, 0.01f)
        assertEquals(0.7f, profile.vocabularyRichness, 0.01f)
        assertEquals(2, profile.commonPhrases.size)
        assertEquals(100, profile.totalSamples)
    }

    @Test
    fun sceneStyleProfile_defaultValues() {
        val profile = SceneStyleProfile(scene = "email")
        assertEquals("email", profile.scene)
        assertEquals(0.5f, profile.formalityScore, 0.01f)
        assertTrue(profile.commonPhrases.isEmpty())
        assertEquals(20f, profile.avgResponseLength, 0.01f)
    }

    @Test
    fun sceneStyleProfile_customValues() {
        val profile = SceneStyleProfile(
            scene = "im",
            formalityScore = 0.2f,
            commonPhrases = listOf("哈哈", "嗯嗯"),
            avgResponseLength = 8f
        )
        assertEquals("im", profile.scene)
        assertEquals(0.2f, profile.formalityScore, 0.01f)
        assertEquals(2, profile.commonPhrases.size)
        assertEquals(8f, profile.avgResponseLength, 0.01f)
    }

    @Test
    fun punctuationStyle_defaultValues() {
        val style = PunctuationStyle()
        assertFalse(style.frequentExclamation)
        assertFalse(style.frequentQuestion)
        assertFalse(style.frequentEllipsis)
        assertFalse(style.frequentComma)
        assertTrue(style.usesPeriod)
        assertEquals(0f, style.exclamationRatio, 0.01f)
        assertEquals(0f, style.questionRatio, 0.01f)
        assertEquals(0f, style.ellipsisRatio, 0.01f)
        assertEquals(0f, style.commaRatio, 0.01f)
    }

    @Test
    fun punctuationStyle_customValues() {
        val style = PunctuationStyle(
            frequentExclamation = true,
            frequentQuestion = true,
            exclamationRatio = 0.15f,
            questionRatio = 0.08f
        )
        assertTrue(style.frequentExclamation)
        assertTrue(style.frequentQuestion)
        assertFalse(style.frequentEllipsis)
        assertEquals(0.15f, style.exclamationRatio, 0.01f)
        assertEquals(0.08f, style.questionRatio, 0.01f)
    }

    @Test
    fun emojiUsage_defaultValues() {
        val usage = EmojiUsage()
        assertFalse(usage.usesEmoji)
        assertEquals(0f, usage.emojiRatio, 0.01f)
        assertTrue(usage.commonEmojis.isEmpty())
        assertFalse(usage.usesEmoticons)
        assertTrue(usage.commonEmoticons.isEmpty())
    }

    @Test
    fun emojiUsage_customValues() {
        val usage = EmojiUsage(
            usesEmoji = true,
            emojiRatio = 0.05f,
            commonEmojis = listOf("👍", "😊", "❤️"),
            usesEmoticons = true,
            commonEmoticons = listOf("^_^", ":)")
        )
        assertTrue(usage.usesEmoji)
        assertEquals(0.05f, usage.emojiRatio, 0.01f)
        assertEquals(3, usage.commonEmojis.size)
        assertTrue(usage.usesEmoticons)
        assertEquals(2, usage.commonEmoticons.size)
    }

    @Test
    fun userStyleProfile_perSceneProfiles_accessible() {
        val profile = UserStyleProfile(
            perSceneProfiles = mapOf(
                "email" to SceneStyleProfile("email", 0.8f),
                "im" to SceneStyleProfile("im", 0.2f),
                "document" to SceneStyleProfile("document", 0.6f)
            )
        )
        assertEquals(3, profile.perSceneProfiles.size)
        assertEquals(0.8f, profile.perSceneProfiles["email"]!!.formalityScore, 0.01f)
        assertEquals(0.2f, profile.perSceneProfiles["im"]!!.formalityScore, 0.01f)
        assertEquals(0.6f, profile.perSceneProfiles["document"]!!.formalityScore, 0.01f)
    }

    @Test
    fun userStyleProfile_timeBasedPatterns_accessible() {
        val profile = UserStyleProfile(
            timeBasedPatterns = mapOf(
                9 to 0.8f,
                12 to 0.5f,
                20 to 0.3f
            )
        )
        assertEquals(3, profile.timeBasedPatterns.size)
        assertEquals(0.8f, profile.timeBasedPatterns[9]!!, 0.01f)
        assertEquals(0.5f, profile.timeBasedPatterns[12]!!, 0.01f)
        assertEquals(0.3f, profile.timeBasedPatterns[20]!!, 0.01f)
    }

    @Test
    fun userStyleProfile_responsePatterns_accessible() {
        val profile = UserStyleProfile(
            responsePatterns = mapOf(
                "greeting" to listOf("你好", "您好"),
                "thanks" to listOf("不客气", "应该的")
            )
        )
        assertEquals(2, profile.responsePatterns.size)
        assertEquals(2, profile.responsePatterns["greeting"]?.size)
        assertEquals(2, profile.responsePatterns["thanks"]?.size)
    }
}
