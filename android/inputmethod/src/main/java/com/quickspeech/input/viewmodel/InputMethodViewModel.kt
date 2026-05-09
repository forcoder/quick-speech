package com.quickspeech.input.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
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
import kotlinx.coroutines.flow.asStateFlow
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
    /** 用户规则匹配结果 - 当输入匹配到规则时显示 */
    val userRuleMatch: UserRuleMatch? = null,
    /** 所有匹配前缀的规则（用于提示） */
    val userRulePrefixMatches: List<UserRuleEntity> = emptyList()
)

/**
 * 用户规则匹配结果
 * 当输入完全匹配某个规则时，提供展开文本
 */
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _uiState = MutableStateFlow(InputMethodUiState())
    val uiState: StateFlow<InputMethodUiState> = _uiState.asStateFlow()

    init {
        Log.e("QuickSpeech", "InputMethodViewModel created with WubiInputEngine + UserRuleEngine")
        scope.launch {
            try {
                wubiInputEngine.refreshUserData()
            } catch (e: Throwable) {
                Log.e("QuickSpeech", "Failed to refresh user data", e)
            }
        }
        // Collect candidates from engine
        scope.launch {
            wubiInputEngine.candidates.collect { ranked ->
                _uiState.update { it.copy(
                    candidates = ranked.map { rc -> rc.entry.word }
                )}
            }
        }
        // Collect composing code
        scope.launch {
            wubiInputEngine.composingCode.collect { code ->
                _uiState.update { it.copy(inputCode = code) }
                // Check user rules when composing code changes
                if (code.isNotEmpty()) {
                    checkUserRules(code)
                } else {
                    clearUserRuleMatches()
                }
            }
        }
        // Collect associated words
        scope.launch {
            wubiInputEngine.associatedWords.collect { words ->
                _uiState.update { it.copy(
                    associatedWords = words.map { it.word }
                )}
            }
        }
    }

    /**
     * 检查用户输入是否匹配任何自定义规则
     */
    private fun checkUserRules(input: String) {
        scope.launch {
            try {
                // 1. 精确匹配
                val exactMatch = userRuleEngine.matchRule(input)
                if (exactMatch != null) {
                    _uiState.update {
                        it.copy(
                            userRuleMatch = UserRuleMatch(
                                ruleId = exactMatch.id,
                                shortcut = exactMatch.shortcut,
                                expansion = exactMatch.expansion,
                                category = exactMatch.category,
                                description = exactMatch.description
                            ),
                            userRulePrefixMatches = emptyList()
                        )
                    }
                    return@launch
                }

                // 2. 前缀匹配（提示可用规则）
                val prefixMatches = userRuleEngine.matchRulesPrefix(input)
                _uiState.update {
                    it.copy(
                        userRuleMatch = null,
                        userRulePrefixMatches = prefixMatches
                    )
                }
            } catch (e: Throwable) {
                Log.e("QuickSpeech", "Error checking user rules", e)
            }
        }
    }

    /**
     * 清除规则匹配状态
     */
    private fun clearUserRuleMatches() {
        _uiState.update {
            it.copy(
                userRuleMatch = null,
                userRulePrefixMatches = emptyList()
            )
        }
    }

    /**
     * 选择使用规则展开
     * 当用户选择规则匹配的候选时调用
     */
    fun onUserRuleSelected(match: UserRuleMatch) {
        scope.launch {
            try {
                // 记录使用
                userRuleEngine.recordUsage(match.ruleId)
                Log.e("QuickSpeech", "User rule used: ${match.shortcut} -> ${match.expansion}")
            } catch (e: Throwable) {
                Log.e("QuickSpeech", "Error recording rule usage", e)
            }
        }
        // 清除输入状态
        wubiInputEngine.reset()
        _uiState.update {
            it.copy(
                inputCode = "",
                candidates = emptyList(),
                associatedWords = emptyList(),
                userRuleMatch = null,
                userRulePrefixMatches = emptyList()
            )
        }
    }

    fun onKeyInput(key: String) {
        scope.launch {
            try {
                val result = wubiInputEngine.processKey(key[0])
                handleEngineResult(result)
            } catch (e: Throwable) {
                Log.e("QuickSpeech", "Error processing key: $key", e)
            }
        }
    }

    fun onDelete() {
        scope.launch {
            try {
                val result = wubiInputEngine.processKey('\b')
                handleEngineResult(result)
            } catch (e: Throwable) {
                Log.e("QuickSpeech", "Error processing delete", e)
            }
        }
    }

    fun onCandidateSelected(candidate: String) {
        scope.launch {
            try {
                // Find the candidate word in current candidates and confirm it
                val currentCandidates = _uiState.value.candidates
                val index = currentCandidates.indexOf(candidate)
                if (index >= 0 && index < 9) {
                    // Use number key selection via processKey
                    val result = wubiInputEngine.processKey('1' + index)
                    handleEngineResult(result)
                }
            } catch (e: Throwable) {
                Log.e("QuickSpeech", "Error selecting candidate: $candidate", e)
            }
        }
        _uiState.update { it.copy(inputCode = "", candidates = emptyList()) }
    }

    fun onAssociatedWordSelected(word: String) {
        scope.launch {
            try {
                wubiInputEngine.selectAssociatedWord(word)
            } catch (e: Throwable) {
                Log.e("QuickSpeech", "Error selecting associated word: $word", e)
            }
        }
    }

    fun onAiReplySelected(reply: AiReplyUiItem) {
        _uiState.value = _uiState.value.copy(aiReplies = emptyList(), isAiPanelVisible = false)
    }

    fun toggleAiPanel() {
        _uiState.value = _uiState.value.copy(isAiPanelVisible = !_uiState.value.isAiPanelVisible)
    }

    fun setAiMode(mode: AiMode) {
        _uiState.value = _uiState.value.copy(aiMode = mode)
    }

    fun onInputStarted() {}

    fun onInputFinished() {
        _uiState.update { it.copy(
            inputCode = "",
            candidates = emptyList(),
            associatedWords = emptyList(),
            aiReplies = emptyList(),
            isAiPanelVisible = false,
            userRuleMatch = null,
            userRulePrefixMatches = emptyList()
        )}
        wubiInputEngine.reset()
    }

    private fun handleEngineResult(result: EngineResult) {
        when (result) {
            is EngineResult.Composing -> {
                _uiState.update { it.copy(
                    inputCode = result.code,
                    candidates = result.candidates.map { it.entry.word }
                )}
            }
            is EngineResult.TextSelected -> {
                _uiState.update { it.copy(
                    inputCode = "",
                    candidates = emptyList(),
                    userRuleMatch = null,
                    userRulePrefixMatches = emptyList()
                )}
            }
            is EngineResult.DirectOutput -> {
                _uiState.update { it.copy(
                    inputCode = "",
                    candidates = emptyList(),
                    userRuleMatch = null,
                    userRulePrefixMatches = emptyList()
                )}
            }
            is EngineResult.Backspace -> {
                // Backspace handled by engine state flows
            }
            is EngineResult.Cleared -> {
                _uiState.update { it.copy(
                    inputCode = "",
                    candidates = emptyList(),
                    associatedWords = emptyList(),
                    userRuleMatch = null,
                    userRulePrefixMatches = emptyList()
                )}
            }
            is EngineResult.Ignored -> {
                // No-op
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        scope.cancel()
    }
}
