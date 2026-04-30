package com.quickspeech.wubi.engine

import com.quickspeech.wubi.data.WubiDao
import com.quickspeech.wubi.data.WubiWordEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 五笔输入引擎 - 核心协调器
 * 整合 JNI 底层引擎 + Room 数据库 + 解码 + 匹配 + 排序 + 联想 + 学习
 */
@Singleton
class WubiInputEngine @Inject constructor(
    private val dao: WubiDao,
    private val wubiEngine: WubiEngine
) {
    private val decoder = WubiInputDecoder()
    private val matcher = WubiMatcher(dao)
    private val sorter = CandidateSorter()
    private val associativeEngine = AssociativeEngine(dao)
    private val learner = FrequencyLearner(dao)

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

    private var userFrequencies: Map<String, com.quickspeech.wubi.data.UserFrequencyEntry> = emptyMap()
    private var recentWords: Set<String> = emptySet()

    init {
        // 设置默认86版方案
        wubiEngine.setScheme(WubiScheme.WUBI_86)
        wubiEngine.enable纠错(true)
    }

    suspend fun refreshUserData() {
        userFrequencies = learner.getUserFrequencies()
        recentWords = learner.getRecentWords()
    }

    suspend fun processKey(key: Char): EngineResult {
        val result = decoder.processKey(key)

        return when (result) {
            is InputResult.Composing -> {
                _composingCode.value = result.code
                val matchResult = matcher.smartMatch(result.code)
                val ranked = sorter.sort(matchResult.candidates, userFrequencies, recentWords, result.code)
                _candidates.value = ranked
                _associatedWords.value = emptyList()
                EngineResult.Composing(result.code, ranked)
            }
            is InputResult.Confirmed -> {
                val currentCandidates = _candidates.value
                if (currentCandidates.isNotEmpty()) {
                    selectCandidate(0)
                } else {
                    // 尝试用 JNI 引擎搜索
                    val nativeResults = wubiEngine.search(result.code)
                    if (nativeResults.isNotEmpty()) {
                        _selectedText.value = nativeResults.first()
                        decoder.clear()
                        _composingCode.value = ""
                        _candidates.value = emptyList()
                        _associatedWords.value = emptyList()
                        EngineResult.TextSelected(nativeResults.first())
                    } else {
                        _selectedText.value = result.code
                        decoder.clear()
                        _composingCode.value = ""
                        _candidates.value = emptyList()
                        _associatedWords.value = emptyList()
                        EngineResult.TextSelected(result.code)
                    }
                }
            }
            is InputResult.SelectCandidate -> selectCandidate(result.index)
            is InputResult.DirectText -> {
                _selectedText.value = result.text
                decoder.clear()
                EngineResult.DirectOutput(result.text)
            }
            is InputResult.Backspace -> EngineResult.Backspace
            is InputResult.Cleared -> {
                _composingCode.value = ""
                _candidates.value = emptyList()
                _associatedWords.value = emptyList()
                EngineResult.Cleared
            }
            is InputResult.Ignored -> EngineResult.Ignored
        }
    }

    private suspend fun selectCandidate(index: Int): EngineResult {
        val currentCandidates = _candidates.value
        if (index < 0 || index >= currentCandidates.size) return EngineResult.Ignored

        val selected = currentCandidates[index]
        val word = selected.entry.word
        val code = selected.entry.code

        learner.recordSelection(word, code)
        _selectedText.value = word

        if (word.length == 1) {
            _associatedWords.value = associativeEngine.associate(word)
        } else {
            _associatedWords.value = emptyList()
        }

        decoder.clear()
        _composingCode.value = ""
        _candidates.value = emptyList()

        return EngineResult.TextSelected(word)
    }

    suspend fun selectAssociatedWord(word: String) {
        val associated = _associatedWords.value
        val entry = associated.find { it.word == word }
        if (entry != null) {
            learner.recordSelection(word, entry.code)
            _selectedText.value = word
            _associatedWords.value = emptyList()
        }
    }

    fun toggleInputMode(): InputMode {
        val newMode = decoder.toggleMode()
        _inputMode.value = newMode
        _composingCode.value = ""
        _candidates.value = emptyList()
        _associatedWords.value = emptyList()
        return newMode
    }

    fun getInputMode(): InputMode = decoder.inputMode

    fun reset() {
        decoder.reset()
        _candidates.value = emptyList()
        _composingCode.value = ""
        _selectedText.value = ""
        _associatedWords.value = emptyList()
    }
}

sealed class EngineResult {
    data class Composing(val code: String, val candidates: List<RankedCandidate>) : EngineResult()
    data class TextSelected(val word: String) : EngineResult()
    data class DirectOutput(val text: String) : EngineResult()
    object Backspace : EngineResult()
    object Cleared : EngineResult()
    object Ignored : EngineResult()
}
