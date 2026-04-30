package com.quickspeech.input.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quickspeech.input.viewmodel.AiMode
import com.quickspeech.input.viewmodel.InputMethodViewModel

@Composable
fun InputMethodKeyboardView(
    viewModel: InputMethodViewModel,
    onCommitText: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        CandidateBar(
            inputCode = uiState.inputCode,
            candidates = uiState.candidates,
            onCandidateSelected = { candidate ->
                viewModel.onCandidateSelected(candidate)
                onCommitText(candidate)
            }
        )

        if (uiState.isAiPanelVisible) {
            AiReplyPanel(
                replies = uiState.aiReplies,
                currentMode = uiState.aiMode,
                isLoading = uiState.isLoading,
                onReplySelected = { reply ->
                    viewModel.onAiReplySelected(reply)
                    onCommitText(reply.text)
                },
                onModeChange = { viewModel.setAiMode(it) }
            )
        }

        WubiKeyboard(
            onKeyInput = { viewModel.onKeyInput(it) },
            onDelete = { viewModel.onDelete() },
            onToggleAi = { viewModel.toggleAiPanel() },
            isAiActive = uiState.isAiPanelVisible
        )
    }
}

@Composable
fun CandidateBar(
    inputCode: String,
    candidates: List<String>,
    onCandidateSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (inputCode.isNotEmpty()) {
                Text(
                    text = inputCode,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(candidates.take(10)) { candidate ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.clickable { onCandidateSelected(candidate) }
                    ) {
                        Text(
                            text = candidate,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AiReplyPanel(
    replies: List<com.quickspeech.input.viewmodel.AiReplyUiItem>,
    currentMode: AiMode,
    isLoading: Boolean,
    onReplySelected: (com.quickspeech.input.viewmodel.AiReplyUiItem) -> Unit,
    onModeChange: (AiMode) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                AiModeChip("📚 知识库", currentMode == AiMode.KNOWLEDGE) {
                    onModeChange(AiMode.KNOWLEDGE)
                }
                AiModeChip("🤖 AI", currentMode == AiMode.AGENT) {
                    onModeChange(AiMode.AGENT)
                }
                AiModeChip("🔀 混合", currentMode == AiMode.HYBRID) {
                    onModeChange(AiMode.HYBRID)
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("AI 生成中…", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(replies) { reply ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .width(200.dp)
                                .clickable { onReplySelected(reply) }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = reply.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = reply.source,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun WubiKeyboard(
    onKeyInput: (String) -> Unit,
    onDelete: () -> Unit,
    onToggleAi: () -> Unit,
    isAiActive: Boolean
) {
    val row1 = listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
    val row2 = listOf("A", "S", "D", "F", "G", "H", "J", "K", "L")
    val row3 = listOf("Z", "X", "C", "V", "B", "N", "M")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onToggleAi) {
                Icon(
                    Icons.Default.ThumbUp,
                    contentDescription = "AI 回复",
                    tint = if (isAiActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        KeyboardRow(row1, onKeyInput)
        KeyboardRow(row2, onKeyInput)
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "删除"
                )
            }
            KeyboardRowContent(row3, onKeyInput)
            IconButton(onClick = { /* Enter action */ }) {
                Icon(
                    Icons.Default.Keyboard,
                    contentDescription = "回车"
                )
            }
        }
    }
}

@Composable
fun KeyboardRow(keys: List<String>, onKeyInput: (String) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        KeyboardRowContent(keys, onKeyInput)
    }
}

@Composable
fun KeyboardRowContent(keys: List<String>, onKeyInput: (String) -> Unit) {
    keys.forEach { key ->
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .width(32.dp)
                .height(40.dp)
                .clickable { onKeyInput(key.lowercase()) }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
