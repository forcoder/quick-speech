package com.quickspeech.wubi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quickspeech.wubi.engine.InputMode
import com.quickspeech.wubi.ui.WubiViewModel

/**
 * 五笔输入面板 - 主UI组件
 * 整合候选词、联想词、模式指示器、设置等
 */
@Composable
fun WubiInputPanel(
    viewModel: WubiViewModel,
    modifier: Modifier = Modifier
) {
    val candidates by viewModel.candidates.collectAsState()
    val composingCode by viewModel.composingCode.collectAsState()
    val inputMode by viewModel.inputMode.collectAsState()
    val associatedWords by viewModel.associatedWords.collectAsState()
    val outputText by viewModel.outputText.collectAsState()
    val settingsVisible by viewModel.settingsVisible.collectAsState()
    val fuzzyMatchEnabled by viewModel.fuzzyMatchEnabled.collectAsState()
    val associativeEnabled by viewModel.associativeEnabled.collectAsState()
    val frequencyLearningEnabled by viewModel.frequencyLearningEnabled.collectAsState()

    Column(modifier = modifier.fillMaxWidth()) {
        // 输出文本显示区
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = outputText.ifEmpty { "五笔输入演示..." },
                style = MaterialTheme.typography.bodyLarge,
                color = if (outputText.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 工具栏：模式指示器 + 设置按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 模式指示器
            InputModeIndicator(
                mode = inputMode,
                onClick = { viewModel.toggleInputMode() }
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 退格键
                IconButton(onClick = { viewModel.deleteLast() }) {
                    Icon(
                        imageVector = Icons.Default.Backspace,
                        contentDescription = "退格",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 设置按钮
                IconButton(onClick = { viewModel.toggleSettings() }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "设置",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 编码显示
        if (composingCode.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CodeDisplayPanel(code = composingCode)
            }
        }

        // 候选词列表
        CandidateBar(
            candidates = candidates,
            composingCode = composingCode,
            onCandidateClick = { index -> viewModel.onCandidateSelected(index) },
            modifier = Modifier.padding(vertical = 2.dp)
        )

        // 词组联想面板
        if (associativeEnabled && associatedWords.isNotEmpty()) {
            AssociativeWordPanel(
                associatedWords = associatedWords,
                onWordClick = { word -> viewModel.onAssociatedWordSelected(word) }
            )
        }

        // 设置面板
        WubiSettingsPanel(
            visible = settingsVisible,
            onDismiss = { viewModel.dismissSettings() },
            fuzzyMatchEnabled = fuzzyMatchEnabled,
            onFuzzyMatchToggle = { viewModel.setFuzzyMatchEnabled(it) },
            associativeEnabled = associativeEnabled,
            onAssociativeToggle = { viewModel.setAssociativeEnabled(it) },
            frequencyLearningEnabled = frequencyLearningEnabled,
            onFrequencyLearningToggle = { viewModel.setFrequencyLearningEnabled(it) },
            onResetLearning = { viewModel.resetLearning() }
        )
    }
}
