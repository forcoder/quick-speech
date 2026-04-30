package com.quickspeech.input.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickspeech.input.ai.data.AiReply
import com.quickspeech.input.ai.data.ReplySource

@Composable
fun ReplyCandidateCard(
    reply: AiReply,
    onAdopt: () -> Unit,
    onThumbsUp: () -> Unit,
    onThumbsDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    var thumbsUpSelected by remember { mutableStateOf(false) }
    var thumbsDownSelected by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onAdopt),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (reply.source) {
                        ReplySource.KNOWNLEDGE_BASE -> MaterialTheme.colorScheme.tertiaryContainer
                        ReplySource.AI_AGENT -> MaterialTheme.colorScheme.secondaryContainer
                        ReplySource.HYBRID -> MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Text(
                        text = "${reply.source.emoji} ${reply.source.name.let {
                            when(it) {
                                "KNOWLEDGE_BASE" -> "知识库"
                                "AI_AGENT" -> "AI智能体"
                                "HYBRID" -> "混合"
                                else -> it
                            }
                        }}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = when (reply.source) {
                            ReplySource.KNOWNLEDGE_BASE -> MaterialTheme.colorScheme.onTertiaryContainer
                            ReplySource.AI_AGENT -> MaterialTheme.colorScheme.onSecondaryContainer
                            ReplySource.HYBRID -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "${(reply.confidence * 100).toInt()}%",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = reply.text,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        thumbsUpSelected = !thumbsUpSelected
                        if (thumbsUpSelected) thumbsDownSelected = false
                        onThumbsUp()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (thumbsUpSelected) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "点赞",
                        tint = if (thumbsUpSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = {
                        thumbsDownSelected = !thumbsDownSelected
                        if (thumbsDownSelected) thumbsUpSelected = false
                        onThumbsDown()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (thumbsDownSelected) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                        contentDescription = "踩",
                        tint = if (thumbsDownSelected) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
