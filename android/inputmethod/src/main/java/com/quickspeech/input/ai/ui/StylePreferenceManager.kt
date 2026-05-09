package com.quickspeech.input.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickspeech.input.ai.engine.SceneStyleProfile
import com.quickspeech.input.ai.engine.StyleLearningEngine
import com.quickspeech.input.ai.engine.UserStyleProfile

/**
 * UI component for managing style preferences.
 * Shows current style analysis results and allows manual adjustment.
 */
@Composable
fun StylePreferencePanel(
    styleEngine: StyleLearningEngine,
    onResetLearning: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by styleEngine.currentProfile.collectAsState()
    val isLearning by styleEngine.isLearning.collectAsState()
    val styleLabel = styleEngine.getStyleLabel()
    val sampleCount = styleEngine.getSampleCount()
    val hasEnoughData = styleEngine.hasEnoughData()

    var showResetDialog by remember { mutableStateOf(false) }
    var selectedScene by remember { mutableStateOf("general") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "风格偏好设置",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "关闭")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Current style indicator
        StyleIndicatorCard(
            styleLabel = styleLabel,
            sampleCount = sampleCount,
            hasEnoughData = hasEnoughData,
            isLearning = isLearning
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Formality slider
        FormalitySliderCard(profile = profile)

        Spacer(modifier = Modifier.height(12.dp))

        // Scene selector
        SceneSelectorCard(
            profile = profile,
            selectedScene = selectedScene,
            onSceneSelected = { selectedScene = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Vocabulary & Punctuation stats
        StyleStatsCard(profile = profile)

        Spacer(modifier = Modifier.height(12.dp))

        // Common phrases
        if (profile.commonPhrases.isNotEmpty()) {
            CommonPhrasesCard(phrases = profile.commonPhrases)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Emoji usage
        EmojiUsageCard(profile = profile)

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { styleEngine.triggerFullAnalysis() },
                modifier = Modifier.weight(1f),
                enabled = !isLearning
            ) {
                if (isLearning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text("重新分析", fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("重置学习", fontSize = 13.sp)
            }
        }

        // Privacy notice
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "隐私说明：所有风格学习数据仅存储在本地设备上，不会上传到服务器。",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Reset confirmation dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("重置风格学习") },
            text = { Text("确定要清除所有风格学习数据吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetLearning()
                        styleEngine.resetLearning()
                        showResetDialog = false
                    }
                ) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun StyleIndicatorCard(
    styleLabel: String,
    sampleCount: Int,
    hasEnoughData: Boolean,
    isLearning: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = styleLabel,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (hasEnoughData) "已基于 $sampleCount 条记录学习" else "数据不足（需至少5条）",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            if (isLearning) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun FormalitySliderCard(profile: UserStyleProfile) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "正式程度",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("随意", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = profile.formalityScore,
                    onValueChange = { /* Read-only display */ },
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    enabled = false
                )
                Text("正式", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = "平均句长: ${profile.avgSentenceLength.toInt()} 字 | 词汇丰富度: ${(profile.vocabularyRichness * 100).toInt()}%",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SceneSelectorCard(
    profile: UserStyleProfile,
    selectedScene: String,
    onSceneSelected: (String) -> Unit
) {
    val scenes = listOf(
        "email" to "邮件",
        "im" to "即时通讯",
        "document" to "文档",
        "general" to "通用"
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "场景风格",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for ((sceneKey, sceneName) in scenes) {
                    val isSelected = selectedScene == sceneKey
                    val sceneProfile = profile.perSceneProfiles[sceneKey]
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSceneSelected(sceneKey) },
                        label = {
                            Text(
                                text = sceneName,
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null
                    )
                }
            }

            // Show selected scene details
            val selectedProfile = profile.perSceneProfiles[selectedScene]
            if (selectedProfile != null) {
                Spacer(modifier = Modifier.height(8.dp))
                SceneProfileDetails(selectedProfile)
            }
        }
    }
}

@Composable
private fun SceneProfileDetails(profile: SceneStyleProfile) {
    val formalityLabel = when {
        profile.formalityScore >= 0.7f -> "正式"
        profile.formalityScore >= 0.4f -> "中性"
        else -> "随意"
    }
    val formalityColor = when {
        profile.formalityScore >= 0.7f -> Color(0xFF1976D2)
        profile.formalityScore >= 0.4f -> Color(0xFF757575)
        else -> Color(0xFF4CAF50)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AssistChip(
            onClick = {},
            label = { Text(formalityLabel, fontSize = 11.sp) },
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(formalityColor)
                )
            }
        )
        AssistChip(
            onClick = {},
            label = { Text("平均 ${profile.avgResponseLength.toInt()} 字", fontSize = 11.sp) }
        )
        if (profile.commonPhrases.isNotEmpty()) {
            AssistChip(
                onClick = {},
                label = { Text("${profile.commonPhrases.size} 个常用语", fontSize = 11.sp) }
            )
        }
    }
}

@Composable
private fun StyleStatsCard(profile: UserStyleProfile) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "标点习惯",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (profile.punctuationStyle.frequentExclamation) {
                    StatChip("频繁感叹", Color(0xFFFF9800))
                }
                if (profile.punctuationStyle.frequentQuestion) {
                    StatChip("频繁问句", Color(0xFF2196F3))
                }
                if (profile.punctuationStyle.frequentEllipsis) {
                    StatChip("频繁省略", Color(0xFF9C27B0))
                }
                if (profile.punctuationStyle.frequentComma) {
                    StatChip("频繁逗号", Color(0xFF4CAF50))
                }
                if (!profile.punctuationStyle.frequentExclamation &&
                    !profile.punctuationStyle.frequentQuestion &&
                    !profile.punctuationStyle.frequentEllipsis &&
                    !profile.punctuationStyle.frequentComma
                ) {
                    StatChip("标准标点", Color(0xFF757575))
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommonPhrasesCard(phrases: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "常用语",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (phrase in phrases.take(15)) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(phrase, fontSize = 11.sp) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmojiUsageCard(profile: UserStyleProfile) {
    val emojiUsage = profile.emojiUsage
    if (!emojiUsage.usesEmoji && !emojiUsage.usesEmoticons) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "表情使用",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (emojiUsage.usesEmoji && emojiUsage.commonEmojis.isNotEmpty()) {
                Text(
                    text = "常用表情: ${emojiUsage.commonEmojis.take(5).joinToString(" ")}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (emojiUsage.usesEmoticons && emojiUsage.commonEmoticons.isNotEmpty()) {
                Text(
                    text = "常用颜文字: ${emojiUsage.commonEmoticons.take(5).joinToString(" ")}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
