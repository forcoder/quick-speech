package com.quickspeech.input

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.quickspeech.input.ai.AiReplyViewModel
import com.quickspeech.input.ai.ui.AiReplyPanel
import dagger.hilt.android.AndroidEntryPoint

class QuickSpeechViewModelStoreOwner : ViewModelStoreOwner {
    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = store
}

@AndroidEntryPoint
class QuickSpeechInputMethodService : InputMethodService() {

    private var aiReplyViewModel: AiReplyViewModel? = null
    private val viewModelStoreOwner = QuickSpeechViewModelStoreOwner()

    override fun onCreate() {
        super.onCreate()
        aiReplyViewModel = ViewModelProvider(viewModelStoreOwner)[AiReplyViewModel::class.java]
    }

    override fun onCreateInputView(): View {
        return ComposeView(this).apply {
            setContent {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            aiReplyViewModel?.let { viewModel ->
                                AiReplyPanel(
                                    viewModel = viewModel,
                                    onInsertText = { text -> insertText(text) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        aiReplyViewModel?.let { vm ->
            val ic = currentInputConnection
            if (ic != null) {
                val textBeforeCursor = ic.getTextBeforeCursor(200, 0) ?: ""
                val textAfterCursor = ic.getTextAfterCursor(200, 0) ?: ""
                vm.updateInputContext("$textBeforeCursor$textAfterCursor")
            }
        }
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(
            oldSelStart, oldSelEnd,
            newSelStart, newSelEnd,
            candidatesStart, candidatesEnd
        )
        aiReplyViewModel?.let { vm ->
            val ic = currentInputConnection
            if (ic != null) {
                val textBeforeCursor = ic.getTextBeforeCursor(200, 0) ?: ""
                val textAfterCursor = ic.getTextAfterCursor(200, 0) ?: ""
                vm.updateInputContext("$textBeforeCursor$textAfterCursor")
            }
        }
    }

    private fun insertText(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModelStoreOwner.viewModelStore.clear()
    }
}
