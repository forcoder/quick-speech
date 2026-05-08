package com.quickspeech.input.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.quickspeech.wubi.engine.WubiEngine
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
    val aiReplies: List<AiReplyUiItem> = emptyList(),
    val aiMode: AiMode = AiMode.HYBRID,
    val isAiPanelVisible: Boolean = false,
    val isLoading: Boolean = false,
    val appType: String = "unknown"
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
    private val wubiEngine: WubiEngine
) : ViewModel() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _uiState = MutableStateFlow(InputMethodUiState())
    val uiState: StateFlow<InputMethodUiState> = _uiState.asStateFlow()

    init {
        Log.e("QuickSpeech", "InputMethodViewModel created, nativeLoaded=${WubiEngine.isNativeLoaded}")
    }

    fun onKeyInput(key: String) {
        val newCode = _uiState.value.inputCode + key
        Log.e("QuickSpeech", "onKeyInput: key=$key, newCode=$newCode")
        _uiState.value = _uiState.value.copy(inputCode = newCode)
        searchCandidates(newCode)
        Log.e("QuickSpeech", "onKeyInput result: code=${_uiState.value.inputCode}, candidates=${_uiState.value.candidates}")
    }

    fun onDelete() {
        val currentCode = _uiState.value.inputCode
        if (currentCode.isNotEmpty()) {
            val newCode = currentCode.dropLast(1)
            _uiState.value = _uiState.value.copy(inputCode = newCode)
            if (newCode.isNotEmpty()) {
                searchCandidates(newCode)
            } else {
                _uiState.value = _uiState.value.copy(candidates = emptyList())
            }
        }
    }

    fun onCandidateSelected(candidate: String) {
        _uiState.value = _uiState.value.copy(inputCode = "", candidates = emptyList())
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
        _uiState.value = _uiState.value.copy(
            inputCode = "",
            candidates = emptyList(),
            aiReplies = emptyList(),
            isAiPanelVisible = false
        )
    }

    private fun searchCandidates(code: String) {
        val results = wubiEngine.search(code)
        _uiState.value = _uiState.value.copy(candidates = results)
    }

    override fun onCleared() {
        super.onCleared()
        scope.cancel()
    }
}
