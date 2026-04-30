package com.quickspeech.wubi.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickspeech.wubi.data.WubiDao
import com.quickspeech.wubi.data.WubiDatabase
import androidx.lifecycle.viewModelScope
import com.quickspeech.wubi.data.WubiDao
import com.quickspeech.wubi.data.WubiDatabase
import com.quickspeech.wubi.engine.EngineResult
import com.quickspeech.wubi.engine.FrequencyLearner
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
class WubiViewModel(application: Application) : AndroidViewModel(application) {

    private val dao: WubiDao = WubiDatabase.create(application).wubiDao()
    private val engine = WubiInputEngine(dao)

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
            val result = engine.processKey(key)
            when (result) {
                is com.quickspeech.wubi.engine.EngineResult.TextSelected -> {
                    outputText.value += result.word
                }
                is com.quickspeech.wubi.engine.EngineResult.DirectOutput -> {
                    outputText.value += result.text
                }
                else -> { /* 其他状态通过StateFlow自动更新 */ }
            }
        }
    }

    /**
     * 选择候选词（点击）
     */
    fun onCandidateSelected(index: Int) {
        viewModelScope.launch {
            val currentCandidates = candidates.value
            if (index in currentCandidates.indices) {
                val word = currentCandidates[index].entry.word
                val code = currentCandidates[index].entry.code
                // 模拟选择
                val result = engine.processKey(' ') // 空格确认
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
     * 重置学习数据
     */
    fun resetLearning() {
        viewModelScope.launch {
            FrequencyLearner(dao).resetLearning()
            engine.refreshUserData()
        }
    }

    /**
     * 清除输出文本
     */
    fun clearOutput() {
        outputText.value = ""
    }

    /**
     * 删除最后一个字符/词
     */
    fun deleteLast() {
        val current = outputText.value
        if (current.isNotEmpty()) {
            outputText.value = current.dropLast(1)
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
