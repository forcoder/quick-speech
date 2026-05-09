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
import androidx.compose.material.icons.outlined.CloudOff
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
import com.quickspeech.input.ai.engine.LocalReplyGenerator

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
            onStyleSelected = { viewModel.switchStyle(it) },
            onAdoptReply = { reply ->
                onInsertText(reply.text)
                viewModel.onReplyAdopted(reply)
            },
            onThumbsUp = { viewModel.onThumbsUp(it) },
            onThumbsDown = { viewModel.onThumbsDown(it) },
            onRefresh = { viewModel.refreshReplies() },
            onCollapse = { viewModel.collapsePanel() },
            onToggleStyleBar = { viewModel.toggleStyleBar() },
            modifier = modifier
        )
    }
}

@Composable
fun AiReplyPanelContent(
    uiState: com.quickspeech.input.ai.AiReplyUiState,
    onModeSelected: (ReplyMode) -> Unit,
    onStyleSelected: (LocalReplyGenerator.ReplyStyle) -> Unit,
    onAdoptReply: (AiReply) -> Unit,
    onThumbsUp: (AiReply) -> Unit,
    onThumbsDown: (AiReply) -> Unit,
    onRefresh: () -> Unit,
    onCollapse: () -> Unit,
    onToggleStyleBar: () -> Unit,
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
            // Drag indicator
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

            // Title bar
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

                // Offline mode indicator
                if (uiState.isOfflineMode) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Outlined.CloudOff,
                        contentDescription = "离线模式",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "离线",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

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

            // Mode switch bar
            ModeSwitchBar(
                currentMode = uiState.currentMode,
                onModeSelected = onModeSelected
            )

            // Style toggle bar
            StyleSwitchBar(
                currentStyle = uiState.currentStyle,
                onStyleSelected = onStyleSelected,
                onToggle = onToggleStyleBar,
                showStyleBar = uiState.showStyleBar
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Reply candidate list (horizontal scroll)
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
 * Style switch bar for reply style selection (正式/随意/简洁)
 */
@Composable
fun StyleSwitchBar(
    currentStyle: LocalReplyGenerator.ReplyStyle,
    onStyleSelected: (LocalReplyGenerator.ReplyStyle) -> Unit,
    onToggle: () -> Unit,
    showStyleBar: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Toggle button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "风格:",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = getStyleDisplayName(currentStyle),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (showStyleBar) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "展开风格选项",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        // Style options
        AnimatedVisibility(visible = showStyleBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LocalReplyGenerator.ReplyStyle.entries.forEach { style ->
                    val isSelected = style == currentStyle
                    val backgroundColor = if (isSelected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    }
                    val textColor = if (isSelected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = backgroundColor,
                        onClick = { onStyleSelected(style) }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = getStyleEmoji(style),
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = getStyleDisplayName(style),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getStyleDisplayName(style: LocalReplyGenerator.ReplyStyle): String {
    return when (style) {
        LocalReplyGenerator.ReplyStyle.FORMAL -> "正式"
        LocalReplyGenerator.ReplyStyle.CASUAL -> "随意"
        LocalReplyGenerator.ReplyStyle.BRIEF -> "简洁"
    }
}

private fun getStyleEmoji(style: LocalReplyGenerator.ReplyStyle): String {
    return when (style) {
        LocalReplyGenerator.ReplyStyle.FORMAL -> "👔"
        LocalReplyGenerator.ReplyStyle.CASUAL -> "😊"
        LocalReplyGenerator.ReplyStyle.BRIEF -> "⚡"
    }
}

/**
 * AI reply panel trigger button (floating button style)
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