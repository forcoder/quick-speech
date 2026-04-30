package com.quickspeech.input.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickspeech.input.ai.data.*
import com.quickspeech.input.ai.network.AiReplyRepository
import com.quickspeech.input.ai.network.AiReplyResult
import com.quickspeech.input.context.AppContextDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiReplyUiState(
    val replies: List<AiReply> = emptyList(),
    val currentMode: ReplyMode = ReplyMode.HYBRID,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isPanelExpanded: Boolean = false,
    val currentRequestId: String = "",
    val currentAppPackage: String = "",
    val currentAppCategory: AppCategory = AppCategory.OTHER
)

@HiltViewModel
class AiReplyViewModel @Inject constructor(
    private val repository: AiReplyRepository,
    private val preferencesRepository: PreferencesRepository,
    private val appContextDetector: AppContextDetector
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiReplyUiState())
    val uiState: StateFlow<AiReplyUiState> = _uiState.asStateFlow()

    private val _inputContext = MutableStateFlow("")

    init {
        viewModelScope.launch {
            preferencesRepository.replyModeFlow.collect { mode ->
                _uiState.update { it.copy(currentMode = mode) }
            }
        }
    }

    fun updateInputContext(text: String) {
        _inputContext.value = text
    }

    fun generateReplies() {
        viewModelScope.launch {
            val context = _inputContext.value
            if (context.isBlank()) return@launch

            val foregroundApp = appContextDetector.getCurrentForegroundApp() ?: ""
            val category = appContextDetector.getAppCategory(foregroundApp)

            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    currentAppPackage = foregroundApp,
                    currentAppCategory = category
                )
            }

            when (val result = repository.fetchReplies(context, foregroundApp, category)) {
                is AiReplyResult.Success -> {
                    _uiState.update {
                        it.copy(
                            replies = result.replies,
                            currentRequestId = result.requestId,
                            isLoading = false,
                            isPanelExpanded = true
                        )
                    }
                }
                is AiReplyResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                is AiReplyResult.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun switchMode(mode: ReplyMode) {
        viewModelScope.launch {
            preferencesRepository.setReplyMode(mode)
            _uiState.update { it.copy(currentMode = mode) }
            generateReplies()
        }
    }

    fun togglePanel() {
        _uiState.update { it.copy(isPanelExpanded = !it.isPanelExpanded) }
    }

    fun collapsePanel() {
        _uiState.update { it.copy(isPanelExpanded = false) }
    }

    fun onReplyAdopted(reply: AiReply) {
        viewModelScope.launch {
            repository.saveFeedback(
                UserFeedback(
                    replyId = reply.id,
                    requestId = _uiState.value.currentRequestId,
                    feedbackType = FeedbackType.ADOPTED,
                    originalText = reply.text,
                    appPackage = _uiState.value.currentAppPackage,
                    appCategory = _uiState.value.currentAppCategory
                )
            )
        }
    }

    fun onReplyModified(reply: AiReply, modifiedText: String) {
        viewModelScope.launch {
            repository.saveFeedback(
                UserFeedback(
                    replyId = reply.id,
                    requestId = _uiState.value.currentRequestId,
                    feedbackType = FeedbackType.MODIFIED,
                    originalText = reply.text,
                    modifiedText = modifiedText,
                    appPackage = _uiState.value.currentAppPackage,
                    appCategory = _uiState.value.currentAppCategory
                )
            )
        }
    }

    fun onThumbsUp(reply: AiReply) {
        viewModelScope.launch {
            repository.saveFeedback(
                UserFeedback(
                    replyId = reply.id,
                    requestId = _uiState.value.currentRequestId,
                    feedbackType = FeedbackType.THUMBS_UP,
                    originalText = reply.text,
                    appPackage = _uiState.value.currentAppPackage,
                    appCategory = _uiState.value.currentAppCategory
                )
            )
        }
    }

    fun onThumbsDown(reply: AiReply) {
        viewModelScope.launch {
            repository.saveFeedback(
                UserFeedback(
                    replyId = reply.id,
                    requestId = _uiState.value.currentRequestId,
                    feedbackType = FeedbackType.THUMBS_DOWN,
                    originalText = reply.text,
                    appPackage = _uiState.value.currentAppPackage,
                    appCategory = _uiState.value.currentAppCategory
                )
            )
        }
    }

    fun refreshReplies() {
        generateReplies()
    }
}
