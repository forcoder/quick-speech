package com.quickspeech.input.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickspeech.common.db.BehaviorRecordDao
import com.quickspeech.common.db.CorrectionDao
import com.quickspeech.common.db.CorrectionEntity
import com.quickspeech.common.db.CorrectionStatus
import com.quickspeech.common.db.EditHistoryDao
import com.quickspeech.common.db.EditHistoryEntity
import com.quickspeech.common.db.StyleProfileDao
import com.quickspeech.common.db.StyleProfileEntity
import com.quickspeech.common.db.UserActionType
import com.quickspeech.common.util.DataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KnowledgeItem(
    val id: String,
    val title: String,
    val content: String,
    val category: String,
    val relevanceScore: Float = 0f,
    val highlightRanges: List<IntRange> = emptyList()
)

data class EvolutionUiState(
    val searchQuery: String = "",
    val searchResults: List<KnowledgeItem> = emptyList(),
    val isSearching: Boolean = false,
    val selectedKnowledge: KnowledgeItem? = null,
    val editedContent: String = "",
    val styleProfile: StyleProfileEntity? = null,
    val styleIntensity: Float = 5f,
    val isStyleEnabled: Boolean = true,
    val totalBehaviorRecords: Int = 0,
    val acceptedCount: Int = 0,
    val skippedCount: Int = 0,
    val modifiedCount: Int = 0,
    val selfWrittenCount: Int = 0,
    val pendingCorrections: Int = 0,
    val showExportDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val exportFile: java.io.File? = null,
    val message: String? = null
)

@HiltViewModel
class EvolutionViewModel @Inject constructor(
    private val editHistoryDao: EditHistoryDao,
    private val correctionDao: CorrectionDao,
    private val behaviorRecordDao: BehaviorRecordDao,
    private val styleProfileDao: StyleProfileDao,
    private val behaviorRecorder: BehaviorRecorder,
    private val styleAnalyzer: StyleAnalyzer,
    private val dataManager: DataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(EvolutionUiState())
    val uiState: StateFlow<EvolutionUiState> = _uiState.asStateFlow()

    val allEditHistory: StateFlow<List<EditHistoryEntity>> = editHistoryDao
        .getAllHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allCorrections: StateFlow<List<CorrectionEntity>> = correctionDao
        .getAllCorrections()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val styleProfileFlow: StateFlow<StyleProfileEntity?> = styleProfileDao
        .getProfile()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        loadStatistics()
        loadStyleProfile()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            val total = behaviorRecordDao.getRecordCount()
            val accepted = behaviorRecordDao.getCountByAction(UserActionType.ACCEPTED)
            val skipped = behaviorRecordDao.getCountByAction(UserActionType.SKIPPED)
            val modified = behaviorRecordDao.getCountByAction(UserActionType.MODIFIED)
            val selfWritten = behaviorRecordDao.getCountByAction(UserActionType.SELF_WRITTEN)
            val pending = correctionDao.getPendingCorrections().size
            _uiState.value = _uiState.value.copy(
                totalBehaviorRecords = total,
                acceptedCount = accepted,
                skippedCount = skipped,
                modifiedCount = modified,
                selfWrittenCount = selfWritten,
                pendingCorrections = pending
            )
        }
    }

    private fun loadStyleProfile() {
        viewModelScope.launch {
            val profile = styleProfileDao.getProfileSync()
            _uiState.value = _uiState.value.copy(styleProfile = profile)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun searchKnowledge(query: String) {
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearching = false)
            return
        }
        _uiState.value = _uiState.value.copy(isSearching = true)
        viewModelScope.launch {
            val results = performSearch(query)
            _uiState.value = _uiState.value.copy(
                searchResults = results,
                isSearching = false
            )
        }
    }

    private fun performSearch(query: String): List<KnowledgeItem> {
        val mockItems = listOf(
            KnowledgeItem("kb_001", "常用礼貌用语", "您好，请，谢谢，对不起，再见", "礼貌用语"),
            KnowledgeItem("kb_002", "邮件开头模板", "尊敬的xxx：您好！非常荣幸能够与您沟通。", "邮件模板"),
            KnowledgeItem("kb_003", "会议记录格式", "会议主题：\n参会人员：\n会议时间：\n会议内容：\n行动计划：", "文档模板"),
            KnowledgeItem("kb_004", "回复确认用语", "收到，好的，明白了，已阅，同意", "沟通用语")
        )
        return mockItems
            .filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.content.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true)
            }
            .map { item ->
                item.copy(
                    relevanceScore = calculateRelevance(item, query),
                    highlightRanges = findHighlightRanges(item.content, query)
                )
            }
            .sortedByDescending { it.relevanceScore }
    }

    private fun findHighlightRanges(content: String, query: String): List<IntRange> {
        if (query.isBlank()) return emptyList()
        val ranges = mutableListOf<IntRange>()
        var startIndex = 0
        while (true) {
            val index = content.indexOf(query, startIndex, ignoreCase = true)
            if (index < 0) break
            ranges.add(index until index + query.length)
            startIndex = index + 1
        }
        return ranges
    }

    private fun calculateRelevance(item: KnowledgeItem, query: String): Float {
        var score = 0f
        if (item.title.contains(query, ignoreCase = true)) score += 0.5f
        if (item.content.contains(query, ignoreCase = true)) score += 0.3f
        if (item.category.contains(query, ignoreCase = true)) score += 0.2f
        return score.coerceIn(0f, 1f)
    }

    fun selectKnowledge(item: KnowledgeItem) {
        if (item.id == "__deselect__") {
            _uiState.value = _uiState.value.copy(selectedKnowledge = null, editedContent = "")
        } else {
            _uiState.value = _uiState.value.copy(
                selectedKnowledge = item,
                editedContent = item.content
            )
        }
    }

    fun updateEditedContent(content: String) {
        _uiState.value = _uiState.value.copy(editedContent = content)
    }

    fun saveEdit(knowledgeId: String, originalContent: String, editedContent: String, reason: String?) {
        viewModelScope.launch {
            editHistoryDao.insert(
                EditHistoryEntity(
                    knowledgeId = knowledgeId,
                    originalContent = originalContent,
                    editedContent = editedContent,
                    editReason = reason
                )
            )
            _uiState.value = _uiState.value.copy(message = "编辑已保存")
        }
    }

    fun submitForReview(knowledgeId: String, originalContent: String, correctedContent: String) {
        viewModelScope.launch {
            correctionDao.insert(
                CorrectionEntity(
                    knowledgeId = knowledgeId,
                    originalContent = originalContent,
                    correctedContent = correctedContent,
                    status = CorrectionStatus.PENDING
                )
            )
            _uiState.value = _uiState.value.copy(message = "已提交审核")
            loadStatistics()
        }
    }

    fun updateStyleIntensity(intensity: Float) {
        _uiState.value = _uiState.value.copy(styleIntensity = intensity)
    }

    fun toggleStyleEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isStyleEnabled = enabled)
    }

    fun triggerStyleAnalysis() {
        viewModelScope.launch {
            styleAnalyzer.analyzeAndUpdate()
            loadStyleProfile()
            _uiState.value = _uiState.value.copy(message = "风格分析完成")
        }
    }

    fun resetStyleLearning() {
        viewModelScope.launch {
            behaviorRecordDao.deleteAll()
            styleProfileDao.deleteAll()
            _uiState.value = _uiState.value.copy(
                styleProfile = null,
                message = "风格学习数据已重置"
            )
            loadStatistics()
        }
    }

    fun acceptReply(reply: String, sceneType: String = "general", contextPrompt: String? = null) {
        behaviorRecorder.recordAccepted(reply, sceneType, contextPrompt)
        loadStatistics()
    }

    fun skipReply(reply: String, sceneType: String = "general", contextPrompt: String? = null) {
        behaviorRecorder.recordSkipped(reply, sceneType, contextPrompt)
        loadStatistics()
    }

    fun modifyReply(original: String, modified: String, sceneType: String = "general", contextPrompt: String? = null) {
        behaviorRecorder.recordModified(original, modified, sceneType, contextPrompt)
        loadStatistics()
    }

    fun selfWriteReply(original: String, selfWritten: String, sceneType: String = "general", contextPrompt: String? = null) {
        behaviorRecorder.recordSelfWritten(original, selfWritten, sceneType, contextPrompt)
        loadStatistics()
    }

    fun exportData() {
        viewModelScope.launch {
            try {
                val file = dataManager.exportUserData()
                _uiState.value = _uiState.value.copy(
                    exportFile = file,
                    showExportDialog = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = "导出失败: ${e.message}")
            }
        }
    }

    fun dismissExportDialog() {
        _uiState.value = _uiState.value.copy(showExportDialog = false)
    }

    fun showDeleteDialog() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = true)
    }

    fun dismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = false)
    }

    fun deleteAllData() {
        viewModelScope.launch {
            val success = dataManager.deleteAllUserData()
            if (success) {
                _uiState.value = _uiState.value.copy(
                    showDeleteDialog = false,
                    message = "所有数据已删除"
                )
                loadStatistics()
                loadStyleProfile()
            } else {
                _uiState.value = _uiState.value.copy(
                    showDeleteDialog = false,
                    message = "删除失败"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
