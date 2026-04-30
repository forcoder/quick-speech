package com.quickspeech.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.quickspeech.common.ui.theme.QuickSpeechTheme
import com.quickspeech.app.ui.screens.settings.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuickSpeechTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(onNavigateBack = { finish() })
                }
            }
        }
    }
}
