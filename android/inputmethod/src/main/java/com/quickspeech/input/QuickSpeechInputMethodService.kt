package com.quickspeech.input

import android.inputmethodservice.InputMethodService
import android.view.View
import androidx.compose.ui.platform.ComposeView
import com.quickspeech.input.ui.InputMethodKeyboardView
import com.quickspeech.input.viewmodel.InputMethodViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class QuickSpeechInputMethodService : InputMethodService() {

    @Inject
    lateinit var viewModel: InputMethodViewModel

    override fun onCreateInputView(): View {
        return ComposeView(this).apply {
            setContent {
                InputMethodKeyboardView(
                    viewModel = viewModel,
                    onCommitText = { text -> insertText(text) }
                )
            }
        }
    }

    private fun insertText(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        viewModel.onInputStarted()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        viewModel.onInputFinished()
    }
}
