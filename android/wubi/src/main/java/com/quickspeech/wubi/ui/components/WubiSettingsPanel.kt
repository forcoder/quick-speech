package com.quickspeech.wubi.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * 五笔设置面板
 */
@Composable
fun WubiSettingsPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    fuzzyMatchEnabled: Boolean,
    onFuzzyMatchToggle: (Boolean) -> Unit,
    associativeEnabled: Boolean,
    onAssociativeToggle: (Boolean) -> Unit,
    frequencyLearningEnabled: Boolean,
    onFrequencyLearningToggle: (Boolean) -> Unit,
    onResetLearning: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "五笔输入设置",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 设置项
                SettingItem(
                    title = "容错输入",
                    description = "启用z键通配和常见错误编码自动修正",
                    checked = fuzzyMatchEnabled,
                    onCheckedChange = onFuzzyMatchToggle
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingItem(
                    title = "词组联想",
                    description = "输入单字后自动联想常用词组",
                    checked = associativeEnabled,
                    onCheckedChange = onAssociativeToggle
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingItem(
                    title = "词频学习",
                    description = "记录您的输入习惯，优先显示常用词",
                    checked = frequencyLearningEnabled,
                    onCheckedChange = onFrequencyLearningToggle
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 重置按钮
                androidx.compose.material3.OutlinedButton(
                    onClick = onResetLearning,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("重置学习数据")
                }
            }
        }
    }
}

/**
 * 设置项组件
 */
@Composable
private fun SettingItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
