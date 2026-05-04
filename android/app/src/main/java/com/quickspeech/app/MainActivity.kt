package com.quickspeech.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.quickspeech.common.ui.theme.QuickSpeechTheme
import com.quickspeech.app.navigation.QuickSpeechNavHost
import com.quickspeech.wubi.engine.WubiEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var wubiEngine: WubiEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("QuickSpeech", "WubiEngine native loaded: ${WubiEngine.isNativeLoaded}")
        enableEdgeToEdge()
        setContent {
            QuickSpeechTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        QuickSpeechNavHost(
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
