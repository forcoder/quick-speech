package com.quickspeech.input.ai.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quickspeech.input.ai.EvolutionUiState
import com.quickspeech.input.ai.EvolutionViewModel
import com.quickspeech.input.ai.KnowledgeItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvolutionScreen(
    viewModel: EvolutionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("自进化中心") },
                actions = {
                    IconButton(onClick = { viewModel.triggerStyleAnalysis() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "分析风格")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    label = { Text("知识库") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Tune, contentDescription = null) },
                    label = { Text("风格") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("设置") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> KnowledgeTab(viewModel, uiState)
                1 -> StyleTab(viewModel, uiState)
                2 -> SettingsTab(viewModel, uiState)
            }
        }
    }

    if (uiState.showExportDialog) {
        ExportDialog(
            onDismiss = { viewModel.dismissExportDialog() },
            onConfirm = { viewModel.dismissExportDialog() }
        )
    }

    if (uiState.showDeleteDialog) {
        DeleteDataDialog(
            onDismiss = { viewModel.dismissDeleteDialog() },
            onConfirm = { viewModel.deleteAllData() }
        )
    }
}

@Composable
private fun KnowledgeTab(viewModel: EvolutionViewModel, uiState: EvolutionUiState) {
    if (uiState.selectedKnowledge != null) {
        KnowledgeEditScreen(viewModel, uiState)
    } else {
        KnowledgeSearchScreen(viewModel, uiState)
    }
}

@Composable
private fun KnowledgeSearchScreen(viewModel: EvolutionViewModel, uiState: EvolutionUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索知识库...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        viewModel.searchKnowledge(uiState.searchQuery)
                    }) {
                        Icon(Icons.Filled.Send, contentDescription = "搜索")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(onClick = { viewModel.searchKnowledge(uiState.searchQuery) }) {
                Text("关键词搜索")
            }
            FilledTonalButton(onClick = { viewModel.searchKnowledge(uiState.searchQuery) }) {
                Text("语义搜索")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isSearching) {
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.searchResults.isEmpty() && uiState.searchQuery.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "未找到相关内容",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.searchResults) { item ->
                    KnowledgeItemCard(
                        item = item,
                        query = uiState.searchQuery,
                        onClick = { viewModel.selectKnowledge(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun KnowledgeItemCard(
    item: KnowledgeItem,
    query: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            HighlightedText(
                text = item.content,
                highlightRanges = item.highlightRanges,
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "相关度: ${(item.relevanceScore * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "编辑",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun HighlightedText(
    text: String,
    highlightRanges: List<IntRange>,
    maxLines: Int = Int.MAX_VALUE
) {
    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        val sortedRanges = highlightRanges.sortedBy { it.first }
        for (range in sortedRanges) {
            if (range.first < lastIndex || range.first >= text.length) continue
            val end = (range.last + 1).coerceAtMost(text.length)
            append(text.substring(lastIndex, range.first))
            withStyle(
                SpanStyle(
                    background = Color.Yellow.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(text.substring(range.first, end))
            }
            lastIndex = end
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KnowledgeEditScreen(viewModel: EvolutionViewModel, uiState: EvolutionUiState) {
    val knowledge = uiState.selectedKnowledge ?: return
    var editReason by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    val editHistory by viewModel.allEditHistory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.selectKnowledge(KnowledgeItem("__deselect__", "", "", "")) }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
            }
            Text(
                text = knowledge.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { showHistory = !showHistory }) {
                Icon(Icons.Filled.Edit, contentDescription = "编辑历史")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("原始内容", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(knowledge.content, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("编辑内容", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.editedContent,
            onValueChange = { viewModel.updateEditedContent(it) },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            placeholder = { Text("在此编辑内容...") },
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = editReason,
            onValueChange = { editReason = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("编辑原因（可选）") },
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    viewModel.saveEdit(
                        knowledge.id,
                        knowledge.content,
                        uiState.editedContent,
                        editReason.ifBlank { null }
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("保存编辑")
            }
            FilledTonalButton(
                onClick = {
                    viewModel.submitForReview(knowledge.id, knowledge.content, uiState.editedContent)
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("提交审核")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val relatedHistory = editHistory.filter { it.knowledgeId == knowledge.id }
        AnimatedVisibility(visible = showHistory && relatedHistory.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("编辑历史", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    relatedHistory.take(5).forEach { history ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = java.text.SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm",
                                    java.util.Locale.getDefault()
                                ).format(java.util.Date(history.editTimestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = history.editedContent,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (history.editReason != null) {
                                Text(
                                    text = "原因: ${history.editReason}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontStyle = FontStyle.Italic
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
private fun StyleTab(viewModel: EvolutionViewModel, uiState: EvolutionUiState) {
    val styleProfile by viewModel.styleProfileFlow.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("风格化", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = uiState.isStyleEnabled,
                        onCheckedChange = { viewModel.toggleStyleEnabled(it) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("风格强度: ${uiState.styleIntensity.toInt()}", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = uiState.styleIntensity,
                    onValueChange = { viewModel.updateStyleIntensity(it) },
                    valueRange = 1f..10f,
                    steps = 8,
                    enabled = uiState.isStyleEnabled
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("风格画像", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                if (styleProfile != null) {
                    StyleProfileBar("正式程度", styleProfile!!.formalityLevel)
                    StyleProfileBar("简洁程度", styleProfile!!.concisenessLevel)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("场景风格", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    SceneStyleRow("邮件", styleProfile!!.emailStyle)
                    SceneStyleRow("即时通讯", styleProfile!!.imStyle)
                    SceneStyleRow("文档", styleProfile!!.documentStyle)

                    if (styleProfile!!.commonPhrases.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "常用词汇/短语",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRowLayout {
                            styleProfile!!.commonPhrases.take(10).forEach { phrase ->
                                Box(
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(phrase, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "暂无风格数据，使用越多学习越精准",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("学习统计", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                StatRow("总行为记录", uiState.totalBehaviorRecords.toString())
                StatRow("采纳回复", uiState.acceptedCount.toString())
                StatRow("跳过回复", uiState.skippedCount.toString())
                StatRow("修改回复", uiState.modifiedCount.toString())
                StatRow("自输入回复", uiState.selfWrittenCount.toString())
                StatRow("待审核修正", uiState.pendingCorrections.toString())
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("反馈", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.ThumbUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("更像这样")
                    }
                    OutlinedButton(onClick = { }, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Filled.ThumbDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("不要这样")
                    }
                }
            }
        }

        FilledTonalButton(
            onClick = { viewModel.triggerStyleAnalysis() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("重新分析风格")
        }

        OutlinedButton(
            onClick = { viewModel.resetStyleLearning() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("重置风格学习")
        }
    }
}

@Composable
private fun FlowRowLayout(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        content()
    }
}

@Composable
private fun StyleProfileBar(label: String, value: Int) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                "$value/10",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { value / 10f },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
        )
    }
}

@Composable
private fun SceneStyleRow(scene: String, style: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(scene, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = when (style) {
                "formal" -> "正式"
                "casual" -> "随意"
                else -> "中性"
            },
            style = MaterialTheme.typography.bodySmall,
            color = when (style) {
                "formal" -> Color(0xFF1565C0)
                "casual" -> Color(0xFF2E7D32)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsTab(viewModel: EvolutionViewModel, uiState: EvolutionUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("数据安全", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "所有学习数据均加密存储在本地设备中，不会上传到服务器。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                FilledTonalButton(
                    onClick = { viewModel.exportData() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("导出个人数据")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { viewModel.showDeleteDialog() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("删除所有个人数据", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        val styleProfile by viewModel.styleProfileFlow.collectAsState()
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("关于", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("QuickSpeech 自进化模块", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "版本: 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "通过记录您的交互行为，学习您的回复风格，让AI越来越懂您。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExportDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出数据") },
        text = { Text("您的个人数据已加密导出，可通过分享功能保存到安全位置。") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun DeleteDataDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除所有数据") },
        text = {
            Text(
                "此操作将永久删除所有学习数据、风格画像和编辑历史，且无法恢复。" +
                    "确定要继续吗？"
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
