package com.quickspeech.wubi.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickspeech.wubi.engine.InputMode

/**
 * 中文/英文切换指示器
 */
@Composable
fun InputModeIndicator(
    mode: InputMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = mode,
        transitionSpec = {
            (slideInHorizontally { it } + fadeIn())
                .togetherWith(slideOutHorizontally { -it } + fadeOut())
        },
        label = "mode_indicator",
        modifier = modifier
    ) { currentMode ->
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (currentMode == InputMode.CHINESE)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.tertiary
                )
                .clickable { onClick() }
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (currentMode == InputMode.CHINESE) "中" else "EN",
                color = if (currentMode == InputMode.CHINESE)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onTertiary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * 编码显示面板
 * 显示当前输入的五笔编码
 */
@Composable
fun CodeDisplayPanel(
    code: String,
    modifier: Modifier = Modifier
) {
    if (code.isEmpty()) return

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = code.uppercase(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp
        )
    }
}
