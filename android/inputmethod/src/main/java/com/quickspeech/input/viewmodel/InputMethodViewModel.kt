package com.quickspeech.input.viewmodel

import android.util.Log
import com.quickspeech.wubi.data.UserRuleEntity
import com.quickspeech.wubi.engine.EngineResult
import com.quickspeech.wubi.engine.UserRuleEngine
import com.quickspeech.wubi.engine.WubiInputEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    val userRulePrefixMatches: List<UserRuleEntity> = emptyList(),
    val error: String? = null
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
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(InputMethodUiState())
    val uiState: StateFlow<InputMethodUiState> = _uiState

    init {
        Log.d("QuickSpeech", "InputMethodViewModel created")
        scope.launch {
            try { wubiInputEngine.refreshUserData() } catch (e: Throwable) { Log.e("QuickSpeech", "refresh failed", e) }
        }
        scope.launch {
            wubiInputEngine.candidates.collectLatest { ranked ->
                _uiState.value = _uiState.value.copy(candidates = ranked.map { rc -> rc.entry.word })
            }
        }
        scope.launch {
            wubiInputEngine.composingCode.collectLatest { code ->
                _uiState.value = _uiState.value.copy(inputCode = code)
                if (code.isNotEmpty()) {
                    val exact = userRuleEngine.matchRule(code)
                    if (exact != null) {
                        _uiState.value = _uiState.value.copy(
                            userRuleMatch = UserRuleMatch(exact.id, exact.shortcut, exact.expansion, exact.category, exact.description),
                            userRulePrefixMatches = emptyList()
                        )
                        return@collectLatest
                    }
                    _uiState.value = _uiState.value.copy(
                        userRuleMatch = null,
                        userRulePrefixMatches = userRuleEngine.matchRulesPrefix(code)
                    )
                } else {
                    clearUserRuleMatches()
                }
            }
        }
        scope.launch {
            wubiInputEngine.associatedWords.collectLatest { words ->
                _uiState.value = _uiState.value.copy(associatedWords = words.map { it.word })
            }
        }
    }

    /** Cancel all coroutines when the IME is destroyed */
    fun clear() {
        scope.cancel()
    }

    private fun clearUserRuleMatches() {
        _uiState.value = _uiState.value.copy(userRuleMatch = null, userRulePrefixMatches = emptyList())
    }

    fun onUserRuleSelected(match: UserRuleMatch) {
        scope.launch {
            try { userRuleEngine.recordUsage(match.ruleId) } catch (e: Throwable) { Log.d("QuickSpeech", "recordUsage failed", e) }
            wubiInputEngine.reset()
            _uiState.value = _uiState.value.copy(
                inputCode = "", candidates = emptyList(), associatedWords = emptyList(),
                userRuleMatch = null, userRulePrefixMatches = emptyList()
            )
        }
    }

    fun onKeyInput(key: String) {
        if (key.isEmpty()) return
        scope.launch {
            try {
                handleEngineResult(wubiInputEngine.processKey(key[0]))
            } catch (e: Throwable) {
                Log.e("QuickSpeech", "key error", e)
                _uiState.value = _uiState.value.copy(error = "输入处理失败")
            }
        }
    }

    fun onDelete() {
        scope.launch {
            try {
                handleEngineResult(wubiInputEngine.processKey('\b'))
            } catch (e: Throwable) {
                Log.e("QuickSpeech", "del error", e)
                _uiState.value = _uiState.value.copy(error = "删除失败")
            }
        }
    }

    fun onCandidateSelected(candidate: String) {
        scope.launch {
            try {
                val idx = _uiState.value.candidates.indexOf(candidate)
                if (idx in 0..6) handleEngineResult(wubiInputEngine.processKey('1' + idx))
            } catch (e: Throwable) {
                Log.e("QuickSpeech", "sel error", e)
                _uiState.value = _uiState.value.copy(error = "选词失败")
            }
        }
    }

    fun onAssociatedWordSelected(word: String) {
        scope.launch { try { wubiInputEngine.selectAssociatedWord(word) } catch (e: Throwable) { Log.d("QuickSpeech", "selectAssociatedWord failed", e) } }
    }

    fun onAiReplySelected(reply: AiReplyUiItem) { _uiState.value = _uiState.value.copy(aiReplies = emptyList(), isAiPanelVisible = false) }
    fun toggleAiPanel() { _uiState.value = _uiState.value.copy(isAiPanelVisible = !_uiState.value.isAiPanelVisible) }
    fun setAiMode(mode: AiMode) { _uiState.value = _uiState.value.copy(aiMode = mode) }
    fun onInputStarted() {}

    fun onInputFinished() {
        scope.launch {
            wubiInputEngine.reset()
            _uiState.value = InputMethodUiState()
        }
    }

    fun clearCandidates() {
        _uiState.value = _uiState.value.copy(inputCode = "", candidates = emptyList(), associatedWords = emptyList(), error = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun handleEngineResult(result: EngineResult) {
        when (result) {
            // Composing candidates are already handled by the candidates collector
            is EngineResult.Composing -> { /* candidates collected from engine flow */ }
            is EngineResult.TextSelected -> _uiState.value = _uiState.value.copy(
                inputCode = "", candidates = emptyList(), associatedWords = emptyList(),
                userRuleMatch = null, userRulePrefixMatches = emptyList()
            )
            is EngineResult.DirectOutput -> _uiState.value = _uiState.value.copy(
                inputCode = "", candidates = emptyList(), associatedWords = emptyList(),
                userRuleMatch = null, userRulePrefixMatches = emptyList()
            )
            is EngineResult.Backspace -> { /* Engine buffer empty; IME service handles input connection backspace directly */ }
            is EngineResult.Cleared -> _uiState.value = _uiState.value.copy(
                inputCode = "", candidates = emptyList(), associatedWords = emptyList(),
                userRuleMatch = null, userRulePrefixMatches = emptyList()
            )
            is EngineResult.Ignored -> {}
        }
    }
}
