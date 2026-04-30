package com.quickspeech.wubi.engine

import com.quickspeech.wubi.data.WubiDao
import com.quickspeech.wubi.data.WubiWordEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 五笔输入引擎 - 核心协调器
 * 整合解码、匹配、排序、联想、学习等所有功能
 */
class WubiInputEngine(
    private val dao: WubiDao
) {
    private val decoder = WubiInputDecoder()
    private val matcher = WubiMatcher(dao)
    private val sorter = CandidateSorter()
    private val associativeEngine = AssociativeEngine(dao)
    private val learner = FrequencyLearner(dao)

    // ========== 状态 ==========

    private val _candidates = MutableStateFlow<List<RankedCandidate>>(emptyList())
    val candidates: StateFlow<List<RankedCandidate>> = _candidates.asStateFlow()

    private val _composingCode = MutableStateFlow("")
    val composingCode: StateFlow<String> = _composingCode.asStateFlow()

    private val _inputMode = MutableStateFlow(InputMode.CHINESE)
    val inputMode: StateFlow<InputMode> = _inputMode.asStateFlow()

    private val _selectedText = MutableStateFlow("")
    val selectedText: StateFlow<String> = _selectedText.asStateFlow()

    private val _associatedWords = MutableStateFlow<List<WubiWordEntry>>(emptyList())
    val associatedWords: StateFlow<List<WubiWordEntry>> = _associatedWords.asStateFlow()

    // 用户数据缓存
    private var userFrequencies: Map<String, com.quickspeech.wubi.data.UserFrequencyEntry> = emptyMap()
    private var recentWords: Set<String> = emptySet()

    /**
     * 刷新用户数据缓存
     */
    suspend fun refreshUserData() {
        userFrequencies = learner.getUserFrequencies()
        recentWords = learner.getRecentWords()
    }

    /**
     * 处理按键输入（主入口）
     */
    suspend fun processKey(key: Char): EngineResult {
        val result = decoder.processKey(key)

        return when (result) {
            is InputResult.Composing -> {
                _composingCode.value = result.code
                // 实时查询候选词
                val matchResult = matcher.smartMatch(result.code)
                val ranked = sorter.sort(
                    matchResult.candidates,
                    userFrequencies,
                    recentWords,
                    result.code
                )
                _candidates.value = ranked

                // 清空联想词
                _associatedWords.value = emptyList()

                EngineResult.Composing(result.code, ranked)
            }

            is InputResult.Confirmed -> {
                // 空格确认：选择第一个候选词
                val currentCandidates = _candidates.value
                if (currentCandidates.isNotEmpty()) {
                    selectCandidate(0)
                } else {
                    // 无候选词，直接输出编码对应的字符
                    _selectedText.value = result.code
                    _composingCode.value = ""
                    _candidates.value = emptyList()
                    _associatedWords.value = emptyList()
                    EngineResult.TextSelected(result.code)
                }
            }

            is InputResult.SelectCandidate -> {
                selectCandidate(result.index)
            }

            is InputResult.DirectText -> {
                _selectedText.value = result.text
                EngineResult.DirectOutput(result.text)
            }

            is InputResult.Backspace -> {
                EngineResult.Backspace
            }

            is InputResult.Cleared -> {
                _composingCode.value = ""
                _candidates.value = emptyList()
                _associatedWords.value = emptyList()
                EngineResult.Cleared
            }

            is InputResult.Ignored -> {
                EngineResult.Ignored
            }
        }
    }

    /**
     * 选择候选词
     */
    private suspend fun selectCandidate(index: Int): EngineResult {
        val currentCandidates = _candidates.value
        if (index < 0 || index >= currentCandidates.size) {
            return EngineResult.Ignored
        }

        val selected = currentCandidates[index]
        val word = selected.entry.word
        val code = selected.entry.code

        // 记录用户选择（词频学习）
        learner.recordSelection(word, code)

        // 输出选中的词
        _selectedText.value = word

        // 如果是单字，触发词组联想
        if (word.length == 1) {
            val associated = associativeEngine.associate(word)
            _associatedWords.value = associated
        } else {
            _associatedWords.value = emptyList()
        }

        // 清空编码缓冲区
        decoder.clear()
        _composingCode.value = ""
        _candidates.value = emptyList()

        return EngineResult.TextSelected(word)
    }

    /**
     * 手动选择联想词
     */
    suspend fun selectAssociatedWord(word: String) {
        val associated = _associatedWords.value
        val entry = associated.find { it.word == word }
        if (entry != null) {
            learner.recordSelection(word, entry.code)
            _selectedText.value = word
            _associatedWords.value = emptyList()
        }
    }

    /**
     * 切换输入模式
     */
    fun toggleInputMode(): InputMode {
        val newMode = decoder.toggleMode()
        _inputMode.value = newMode
        _composingCode.value = ""
        _candidates.value = emptyList()
        _associatedWords.value = emptyList()
        return newMode
    }

    /**
     * 获取当前输入模式
     */
    fun getInputMode(): InputMode = decoder.inputMode

    /**
     * 重置引擎状态
     */
    fun reset() {
        decoder.reset()
        _candidates.value = emptyList()
        _composingCode.value = ""
        _selectedText.value = ""
        _associatedWords.value = emptyList()
    }
}

/** 引擎处理结果 */
sealed class EngineResult {
    data class Composing(val code: String, val candidates: List<RankedCandidate>) : EngineResult()
    data class TextSelected(val word: String) : EngineResult()
    data class DirectOutput(val text: String) : EngineResult()
    object Backspace : EngineResult()
    object Cleared : EngineResult()
    object Ignored : EngineResult()
}
