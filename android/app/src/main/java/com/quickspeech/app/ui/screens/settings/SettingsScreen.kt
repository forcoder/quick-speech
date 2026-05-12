package com.quickspeech.app.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quickspeech.common.ui.components.SettingsGroup
import com.quickspeech.common.ui.components.SettingsSwitch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    // Settings state
    var wubiEnabled by remember { mutableStateOf(true) }
    var aiReplyEnabled by remember { mutableStateOf(true) }
    var ragModeEnabled by remember { mutableStateOf(true) }
    var aiAgentModeEnabled by remember { mutableStateOf(true) }
    var hybridModeEnabled by remember { mutableStateOf(true) }
    var styleLearningEnabled by remember { mutableStateOf(true) }
    var localLearningEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            SettingsGroup(title = "输入设置") {
                SettingsSwitch(
                    title = "五笔输入",
                    checked = wubiEnabled,
                    onCheckedChange = { wubiEnabled = it }
                )
                SettingsSwitch(
                    title = "AI 智能回复",
                    checked = aiReplyEnabled,
                    onCheckedChange = { aiReplyEnabled = it }
                )
            }
            SettingsGroup(title = "AI 模式") {
                SettingsSwitch(
                    title = "知识库模式 (RAG)",
                    checked = ragModeEnabled,
                    onCheckedChange = { ragModeEnabled = it }
                )
                SettingsSwitch(
                    title = "AI 智能体模式",
                    checked = aiAgentModeEnabled,
                    onCheckedChange = { aiAgentModeEnabled = it }
                )
                SettingsSwitch(
                    title = "混合模式",
                    checked = hybridModeEnabled,
                    onCheckedChange = { hybridModeEnabled = it }
                )
            }
            SettingsGroup(title = "自进化") {
                SettingsSwitch(
                    title = "回复风格学习",
                    checked = styleLearningEnabled,
                    onCheckedChange = { styleLearningEnabled = it }
                )
                SettingsSwitch(
                    title = "本地学习模式",
                    checked = localLearningEnabled,
                    onCheckedChange = { localLearningEnabled = it }
                )
            }
            SettingsGroup(title = "关于") {
                Text(
                    text = "QuickSpeech v1.0.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}
