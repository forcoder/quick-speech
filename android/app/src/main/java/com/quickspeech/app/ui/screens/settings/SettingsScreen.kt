package com.quickspeech.app.ui.screens.settings

import android.content.Context
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.quickspeech.common.ui.components.SettingsGroup
import com.quickspeech.common.ui.components.SettingsSwitch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val ds = context.settingsDataStore
    val scope = rememberCoroutineScope()

    val wubiEnabled by ds.data.map { it[booleanPreferencesKey("wubi_enabled")] ?: true }.collectAsState(initial = true)
    val aiReplyEnabled by ds.data.map { it[booleanPreferencesKey("ai_reply_enabled")] ?: true }.collectAsState(initial = true)
    val ragModeEnabled by ds.data.map { it[booleanPreferencesKey("rag_mode_enabled")] ?: true }.collectAsState(initial = true)
    val aiAgentModeEnabled by ds.data.map { it[booleanPreferencesKey("ai_agent_mode_enabled")] ?: true }.collectAsState(initial = true)
    val hybridModeEnabled by ds.data.map { it[booleanPreferencesKey("hybrid_mode_enabled")] ?: true }.collectAsState(initial = true)
    val styleLearningEnabled by ds.data.map { it[booleanPreferencesKey("style_learning_enabled")] ?: true }.collectAsState(initial = true)
    val localLearningEnabled by ds.data.map { it[booleanPreferencesKey("local_learning_enabled")] ?: false }.collectAsState(initial = false)

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
                    onCheckedChange = { v -> scope.launch { ds.edit { it[booleanPreferencesKey("wubi_enabled")] = v } } }
                )
                SettingsSwitch(
                    title = "AI 智能回复",
                    checked = aiReplyEnabled,
                    onCheckedChange = { v -> scope.launch { ds.edit { it[booleanPreferencesKey("ai_reply_enabled")] = v } } }
                )
            }
            SettingsGroup(title = "AI 模式") {
                SettingsSwitch(
                    title = "知识库模式 (RAG)",
                    checked = ragModeEnabled,
                    onCheckedChange = { v -> scope.launch { ds.edit { it[booleanPreferencesKey("rag_mode_enabled")] = v } } }
                )
                SettingsSwitch(
                    title = "AI 智能体模式",
                    checked = aiAgentModeEnabled,
                    onCheckedChange = { v -> scope.launch { ds.edit { it[booleanPreferencesKey("ai_agent_mode_enabled")] = v } } }
                )
                SettingsSwitch(
                    title = "混合模式",
                    checked = hybridModeEnabled,
                    onCheckedChange = { v -> scope.launch { ds.edit { it[booleanPreferencesKey("hybrid_mode_enabled")] = v } } }
                )
            }
            SettingsGroup(title = "自进化") {
                SettingsSwitch(
                    title = "回复风格学习",
                    checked = styleLearningEnabled,
                    onCheckedChange = { v -> scope.launch { ds.edit { it[booleanPreferencesKey("style_learning_enabled")] = v } } }
                )
                SettingsSwitch(
                    title = "本地学习模式",
                    checked = localLearningEnabled,
                    onCheckedChange = { v -> scope.launch { ds.edit { it[booleanPreferencesKey("local_learning_enabled")] = v } } }
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
