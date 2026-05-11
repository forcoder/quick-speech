package com.quickspeech.input.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickspeech.wubi.data.UserRuleEntity
import com.quickspeech.wubi.engine.EngineResult
import com.quickspeech.wubi.engine.UserRuleEngine
import com.quickspeech.wubi.engine.WubiInputEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class InputMethodUiState(
    val inputCode: String = "",
    val candidates: List<String> = emptyList(),
    val associatedWords: List<String> = emptyList(),
    val aiReplies: List<AiReplyUiItem> = emptyList(),
    val aiMode: AiMode = AiMode.HYBRID,
    val isAiPanelVisible: Boolean = false,
    val isLoading: Boolean = false,
    val appType: String = "unknown",
    val userRuleMatch: UserRuleMatch? = null,
    val userRulePrefixMatches: List<UserRuleEntity> = emptyList()
)

data class UserRuleMatch(
    val ruleId: Long,
    val shortcut: String,
    val expansion: String,
    val category: String,
    val description: String
)

data class AiReplyUiItem(
    val text: String,
    val source: String,
    val score: Float
)

enum class AiMode(val label: String) {
    KNOWLEDGE("knowledge"),
    AGENT("agent"),
    HYBRID("hybrid")
}

class InputMethodViewModel(
    private val wubiInputEngine: WubiInputEngine,
    private val userRuleEngine: UserRuleEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(InputMethodUiState())
    val uiState: StateFlow<InputMethodUiState> = _uiState

    init {
        Log.e("QuickSpeech", "InputMethodViewModel created")
        viewModelScope.launch {
            try { wubiInputEngine.refreshUserData() } catch (e: Throwable) { Log.e("QuickSpeech", "refresh failed", e) }
        }
        viewModelScope.launch {
            wubiInputEngine.candidates.collectLatest { ranked ->
                _uiState.value = _uiState.value.copy(candidates = ranked.map { rc -> rc.entry.word })
            }
        }
        viewModelScope.launch {
            wubiInputEngine.composingCode.collectLatest { code ->
                _uiState.value = _uiState.value.copy(inputCode = code)
                if (code.isNotEmpty()) checkUserRules(code) else clearUserRuleMatches()
            }
        }
        viewModelScope.launch {
            wubiInputEngine.associatedWords.collectLatest { words ->
                _uiState.value = _uiState.value.copy(associatedWords = words.map { it.word })
            }
        }
    }

    private fun checkUserRules(input: String) {
        viewModelScope.launch {
            try {
                val exact = userRuleEngine.matchRule(input)
                if (exact != null) {
                    _uiState.value = _uiState.value.copy(
                        userRuleMatch = UserRuleMatch(exact.id, exact.shortcut, exact.expansion, exact.category, exact.description),
                        userRulePrefixMatches = emptyList()
                    )
                    return@launch
                }
                _uiState.value = _uiState.value.copy(userRuleMatch = null, userRulePrefixMatches = userRuleEngine.matchRulesPrefix(input))
            } catch (e: Throwable) { Log.e("QuickSpeech", "checkUserRules error", e) }
        }
    }

    private fun clearUserRuleMatches() {
        _uiState.value = _uiState.value.copy(userRuleMatch = null, userRulePrefixMatches = emptyList())
    }

    fun onUserRuleSelected(match: UserRuleMatch) {
        viewModelScope.launch { try { userRuleEngine.recordUsage(match.ruleId) } catch (e: Throwable) {} }
        wubiInputEngine.reset()
        _uiState.value = _uiState.value.copy(
            inputCode = "", candidates = emptyList(), associatedWords = emptyList(),
            userRuleMatch = null, userRulePrefixMatches = emptyList()
        )
    }

    fun onKeyInput(key: String) {
        if (key.isEmpty()) return
        viewModelScope.launch { try { handleEngineResult(wubiInputEngine.processKey(key[0])) } catch (e: Throwable) { Log.e("QuickSpeech", "key error", e) } }
    }

    fun onDelete() {
        viewModelScope.launch { try { handleEngineResult(wubiInputEngine.processKey('\b')) } catch (e: Throwable) { Log.e("QuickSpeech", "del error", e) } }
    }

    fun onCandidateSelected(candidate: String) {
        viewModelScope.launch {
            try {
                val idx = _uiState.value.candidates.indexOf(candidate)
                if (idx in 0..8) handleEngineResult(wubiInputEngine.processKey('1' + idx))
            } catch (e: Throwable) { Log.e("QuickSpeech", "sel error", e) }
        }
    }

    fun onAssociatedWordSelected(word: String) {
        viewModelScope.launch { try { wubiInputEngine.selectAssociatedWord(word) } catch (e: Throwable) {} }
    }

    fun onAiReplySelected(reply: AiReplyUiItem) { _uiState.value = _uiState.value.copy(aiReplies = emptyList(), isAiPanelVisible = false) }
    fun toggleAiPanel() { _uiState.value = _uiState.value.copy(isAiPanelVisible = !_uiState.value.isAiPanelVisible) }
    fun setAiMode(mode: AiMode) { _uiState.value = _uiState.value.copy(aiMode = mode) }
    fun onInputStarted() {}

    fun onInputFinished() {
        _uiState.value = _uiState.value.copy(
            inputCode = "", candidates = emptyList(), associatedWords = emptyList(),
            aiReplies = emptyList(), isAiPanelVisible = false,
            userRuleMatch = null, userRulePrefixMatches = emptyList()
        )
        wubiInputEngine.reset()
    }

    fun clearCandidates() {
        _uiState.value = _uiState.value.copy(inputCode = "", candidates = emptyList(), associatedWords = emptyList())
    }

    private fun handleEngineResult(result: EngineResult) {
        when (result) {
            is EngineResult.Composing -> _uiState.value = _uiState.value.copy(
                inputCode = result.code,
                candidates = result.candidates.map { it.entry.word }
            )
            is EngineResult.TextSelected -> _uiState.value = _uiState.value.copy(
                inputCode = "", candidates = emptyList(), associatedWords = emptyList(),
                userRuleMatch = null, userRulePrefixMatches = emptyList()
            )
            is EngineResult.DirectOutput -> _uiState.value = _uiState.value.copy(
                inputCode = "", candidates = emptyList(), associatedWords = emptyList(),
                userRuleMatch = null, userRulePrefixMatches = emptyList()
            )
            is EngineResult.Backspace -> {}
            is EngineResult.Cleared -> _uiState.value = _uiState.value.copy(
                inputCode = "", candidates = emptyList(), associatedWords = emptyList(),
                userRuleMatch = null, userRulePrefixMatches = emptyList()
            )
            is EngineResult.Ignored -> {}
        }
    }
}
