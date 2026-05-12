package com.quickspeech.wubi.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickspeech.wubi.engine.EngineResult
import com.quickspeech.wubi.engine.InputMode
import com.quickspeech.wubi.engine.RankedCandidate
import com.quickspeech.wubi.engine.WubiInputEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 五笔输入 ViewModel
 * 管理输入状态、候选词、设置项等
 */
class WubiViewModel(
    application: Application,
    private val engine: WubiInputEngine
) : AndroidViewModel(application) {

    // ========== 公开状态 ==========

    val candidates: StateFlow<List<RankedCandidate>> = engine.candidates
    val composingCode: StateFlow<String> = engine.composingCode
    val inputMode: StateFlow<InputMode> = engine.inputMode
    val selectedText: StateFlow<String> = engine.selectedText
    val associatedWords: StateFlow<List<com.quickspeech.wubi.data.WubiWordEntry>> = engine.associatedWords

    // ========== 设置状态 ==========

    private val _fuzzyMatchEnabled = MutableStateFlow(true)
    val fuzzyMatchEnabled: StateFlow<Boolean> = _fuzzyMatchEnabled.asStateFlow()

    private val _associativeEnabled = MutableStateFlow(true)
    val associativeEnabled: StateFlow<Boolean> = _associativeEnabled.asStateFlow()

    private val _frequencyLearningEnabled = MutableStateFlow(true)
    val frequencyLearningEnabled: StateFlow<Boolean> = _frequencyLearningEnabled.asStateFlow()

    private val _settingsVisible = MutableStateFlow(false)
    val settingsVisible: StateFlow<Boolean> = _settingsVisible.asStateFlow()

    // 累计输出的文本
    val outputText = MutableStateFlow("")

    init {
        viewModelScope.launch {
            engine.refreshUserData()
        }
    }

    /**
     * 处理按键输入
     */
    fun onKeyPressed(key: Char) {
        viewModelScope.launch {
            try {
                val result = engine.processKey(key)
                when (result) {
                    is EngineResult.TextSelected -> {
                        outputText.value += result.word
                    }
                    is EngineResult.DirectOutput -> {
                        outputText.value += result.text
                    }
                    is EngineResult.Backspace -> {
                        val current = outputText.value
                        if (current.isNotEmpty()) {
                            outputText.value = current.dropLast(1)
                        }
                    }
                    is EngineResult.Cleared -> { /* no output change needed */ }
                    is EngineResult.Ignored -> { /* no-op */ }
                    is EngineResult.Composing -> { /* candidates/code updated via StateFlow */ }
                }
            } catch (e: Throwable) {
                // Engine failure should not crash the UI
            }
        }
    }

    /**
     * 选择候选词（点击）
     * 通过索引选择对应候选词
     */
    fun onCandidateSelected(index: Int) {
        viewModelScope.launch {
            val currentCandidates = candidates.value
            if (index in currentCandidates.indices) {
                // Use number key '1'+index to select the candidate at the given index
                // WubiInputDecoder maps '1'..'9' to SelectCandidate(0..8)
                val numKey = ('1' + index)
                val result = engine.processKey(numKey)
                when (result) {
                    is com.quickspeech.wubi.engine.EngineResult.TextSelected -> {
                        outputText.value += result.word
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * 选择联想词
     */
    fun onAssociatedWordSelected(word: String) {
        viewModelScope.launch {
            engine.selectAssociatedWord(word)
            outputText.value += word
        }
    }

    /**
     * 切换输入模式
     */
    fun toggleInputMode() {
        engine.toggleInputMode()
    }

    /**
     * 切换设置面板
     */
    fun toggleSettings() {
        _settingsVisible.value = !_settingsVisible.value
    }

    fun dismissSettings() {
        _settingsVisible.value = false
    }

    /**
     * 设置项切换
     */
    fun setFuzzyMatchEnabled(enabled: Boolean) {
        _fuzzyMatchEnabled.value = enabled
    }

    fun setAssociativeEnabled(enabled: Boolean) {
        _associativeEnabled.value = enabled
    }

    fun setFrequencyLearningEnabled(enabled: Boolean) {
        _frequencyLearningEnabled.value = enabled
    }

    /**
     * 清除输出文本
     */
    fun clearOutput() {
        outputText.value = ""
    }

    /**
     * 重置学习数据并清空输出
     */
    fun resetLearning() {
        engine.reset()
        outputText.value = ""
    }

    /**
     * 删除最后一个字符（退格）
     * 委托给引擎处理，保持引擎状态与输出同步
     */
    fun deleteLast() {
        viewModelScope.launch {
            val result = engine.processKey('\b')
            when (result) {
                is EngineResult.TextSelected -> {
                    // Auto-committed on backspace with full code, add word to output
                    outputText.value += result.word
                }
                is EngineResult.Backspace -> {
                    // Buffer was empty, remove last output char
                    val current = outputText.value
                    if (current.isNotEmpty()) {
                        outputText.value = current.dropLast(1)
                    }
                }
                is EngineResult.DirectOutput -> {
                    outputText.value += result.text
                }
                else -> { /* Composing/Cleared/Ignored - state updated via StateFlow */ }
            }
        }
    }

    /**
     * 重置引擎
     */
    fun reset() {
        engine.reset()
        outputText.value = ""
    }
}
