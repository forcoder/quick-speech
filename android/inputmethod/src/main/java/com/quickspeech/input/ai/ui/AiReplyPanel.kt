package com.quickspeech.input.ai.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickspeech.input.ai.AiReplyViewModel
import com.quickspeech.input.ai.data.AiReply
import com.quickspeech.input.ai.data.ReplyMode

@Composable
fun AiReplyPanel(
    viewModel: AiReplyViewModel,
    onInsertText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    AnimatedVisibility(
        visible = uiState.isPanelExpanded,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        AiReplyPanelContent(
            uiState = uiState,
            onModeSelected = { viewModel.switchMode(it) },
            onAdoptReply = { reply ->
                onInsertText(reply.text)
                viewModel.onReplyAdopted(reply)
            },
            onThumbsUp = { viewModel.onThumbsUp(it) },
            onThumbsDown = { viewModel.onThumbsDown(it) },
            onRefresh = { viewModel.refreshReplies() },
            onCollapse = { viewModel.collapsePanel() },
            modifier = modifier
        )
    }
}

@Composable
fun AiReplyPanelContent(
    uiState: com.quickspeech.input.ai.AiReplyUiState,
    onModeSelected: (ReplyMode) -> Unit,
    onAdoptReply: (AiReply) -> Unit,
    onThumbsUp: (AiReply) -> Unit,
    onThumbsDown: (AiReply) -> Unit,
    onRefresh: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            // 拖拽指示条
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "AI 智能回复",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.weight(1f))

                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "收起",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // 模式切换栏
            ModeSwitchBar(
                currentMode = uiState.currentMode,
                onModeSelected = onModeSelected
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 回复候选列表（横向滑动）
            if (uiState.replies.isNotEmpty()) {
                val listState = rememberLazyListState()
                val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

                LazyRow(
                    state = listState,
                    flingBehavior = flingBehavior,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(
                        items = uiState.replies,
                        key = { _, reply -> reply.id }
                    ) { _, reply ->
                        ReplyCandidateCard(
                            reply = reply,
                            onAdopt = { onAdoptReply(reply) },
                            onThumbsUp = { onThumbsUp(reply) },
                            onThumbsDown = { onThumbsDown(reply) },
                            modifier = Modifier.width(260.dp)
                        )
                    }
                }
            } else if (!uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error ?: "暂无候选回复",
                        fontSize = 13.sp,
                        color = if (uiState.error != null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * AI回复面板触发按钮（浮动按钮形式）
 */
@Composable
fun AiReplyFab(
    viewModel: AiReplyViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    FloatingActionButton(
        onClick = {
            if (uiState.isPanelExpanded) {
                viewModel.collapsePanel()
            } else {
                viewModel.generateReplies()
            }
        },
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Icon(
            imageVector = if (uiState.isPanelExpanded) Icons.Default.Close else Icons.Default.AutoAwesome,
            contentDescription = "AI回复"
        )
    }
}
