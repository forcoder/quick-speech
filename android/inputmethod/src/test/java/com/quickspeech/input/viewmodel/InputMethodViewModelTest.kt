package com.quickspeech.input.viewmodel

import com.quickspeech.wubi.data.UserRuleEntity
import com.quickspeech.wubi.engine.EngineResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for InputMethodViewModel UI state and data classes.
 * Focuses on state transitions and data class correctness.
 */
class InputMethodViewModelTest {

    @Test
    fun inputMethodUiState_defaultValues_areCorrect() {
        val state = InputMethodUiState()
        assertEquals("", state.inputCode)
        assertEquals(emptyList<String>(), state.candidates)
        assertEquals(emptyList<String>(), state.associatedWords)
        assertEquals(emptyList<AiReplyUiItem>(), state.aiReplies)
        assertEquals(AiMode.HYBRID, state.aiMode)
        assertEquals(false, state.isAiPanelVisible)
        assertEquals(false, state.isLoading)
        assertEquals("unknown", state.appType)
        assertNull(state.userRuleMatch)
        assertTrue(state.userRulePrefixMatches.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun inputMethodUiState_copy_preservesUnchangedFields() {
        val original = InputMethodUiState(
            inputCode = "ab",
            candidates = listOf("工", "式"),
            associatedWords = listOf("工作", "工具"),
            error = null
        )
        val copied = original.copy(inputCode = "abc")
        assertEquals("abc", copied.inputCode)
        assertEquals(original.candidates, copied.candidates)
        assertEquals(original.associatedWords, copied.associatedWords)
    }

    @Test
    fun userRuleMatch_canBeCreatedWithAllFields() {
        val match = UserRuleMatch(
            ruleId = 42,
            shortcut = "addr",
            expansion = "123 Main St",
            category = "address",
            description = "Home address"
        )
        assertEquals(42, match.ruleId)
        assertEquals("addr", match.shortcut)
        assertEquals("123 Main St", match.expansion)
        assertEquals("address", match.category)
        assertEquals("Home address", match.description)
    }

    @Test
    fun userRuleMatch_equality() {
        val a = UserRuleMatch(1, "sig", "signature", "email", "sig")
        val b = UserRuleMatch(1, "sig", "signature", "email", "sig")
        assertEquals(a, b)
    }

    @Test
    fun aiReplyUiItem_canBeCreated() {
        val item = AiReplyUiItem(text = "reply text", source = "local", score = 0.9f)
        assertEquals("reply text", item.text)
        assertEquals("local", item.source)
        assertEquals(0.9f, item.score, 0.01f)
    }

    @Test
    fun aiReplyUiItem_equality() {
        val a = AiReplyUiItem("text", "source", 0.5f)
        val b = AiReplyUiItem("text", "source", 0.5f)
        assertEquals(a, b)
    }

    @Test
    fun aiMode_enumValues_areCorrect() {
        assertEquals("knowledge", AiMode.KNOWLEDGE.label)
        assertEquals("agent", AiMode.AGENT.label)
        assertEquals("hybrid", AiMode.HYBRID.label)
    }

    @Test
    fun aiMode_enumValues_count() {
        assertEquals(3, AiMode.entries.size)
    }

    @Test
    fun engineResult_composing_hasCorrectProperties() {
        val candidates = listOf(
            com.quickspeech.wubi.engine.RankedCandidate(
                com.quickspeech.wubi.data.WubiWordEntry(code = "aa", word = "工", frequency = 5000),
                100L, 1
            )
        )
        val composing = EngineResult.Composing("ab", candidates)
        assertEquals("ab", composing.code)
        assertEquals(1, composing.candidates.size)
    }

    @Test
    fun engineResult_textSelected_hasCorrectWord() {
        val selected = EngineResult.TextSelected("测试词")
        assertEquals("测试词", selected.word)
    }

    @Test
    fun engineResult_directOutput_hasCorrectText() {
        val direct = EngineResult.DirectOutput("，")
        assertEquals("，", direct.text)
    }

    @Test
    fun engineResult_singletonObjects_areSingletons() {
        assertNotNull(EngineResult.Backspace)
        assertNotNull(EngineResult.Cleared)
        assertNotNull(EngineResult.Ignored)
    }

    @Test
    fun engineResult_sealedClass_exhaustive() {
        val results = listOf<EngineResult>(
            EngineResult.Composing("a", emptyList()),
            EngineResult.TextSelected("word"),
            EngineResult.DirectOutput("text"),
            EngineResult.Backspace,
            EngineResult.Cleared,
            EngineResult.Ignored
        )
        assertEquals(6, results.size)
    }

    @Test
    fun inputMethodUiState_withError_preservesOtherFields() {
        val state = InputMethodUiState(
            inputCode = "abc",
            candidates = listOf("工"),
            error = "输入处理失败"
        )
        assertEquals("abc", state.inputCode)
        assertEquals(listOf("工"), state.candidates)
        assertEquals("输入处理失败", state.error)
    }

    @Test
    fun inputMethodUiState_withUserRuleMatch() {
        val match = UserRuleMatch(1, "addr", "address", "general", "desc")
        val state = InputMethodUiState(
            inputCode = "addr",
            userRuleMatch = match
        )
        assertNotNull(state.userRuleMatch)
        assertEquals("addr", state.userRuleMatch!!.shortcut)
        assertEquals("address", state.userRuleMatch!!.expansion)
    }

    @Test
    fun inputMethodUiState_withPrefixMatches() {
        val rules = listOf(
            UserRuleEntity(shortcut = "addr", expansion = "address"),
            UserRuleEntity(shortcut = "add", expansion = "addition")
        )
        val state = InputMethodUiState(
            inputCode = "add",
            userRulePrefixMatches = rules
        )
        assertEquals(2, state.userRulePrefixMatches.size)
    }
}
