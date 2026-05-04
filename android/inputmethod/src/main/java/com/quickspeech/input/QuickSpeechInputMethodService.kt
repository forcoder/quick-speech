package com.quickspeech.input

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.ComposeView
import com.quickspeech.input.ui.InputMethodKeyboardView
import com.quickspeech.input.viewmodel.InputMethodViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class QuickSpeechInputMethodService : InputMethodService() {

    private val TAG = "QuickSpeechIME"

    @Inject
    lateinit var viewModel: InputMethodViewModel

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "onCreate")
    }

    override fun onCreateInputView(): View {
        Log.e(TAG, "onCreateInputView")
        return ComposeView(this).apply {
            setContent {
                InputMethodKeyboardView(
                    viewModel = viewModel,
                    onCommitText = { text -> insertText(text) }
                )
            }
        }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        Log.e(TAG, "onStartInput restarting=$restarting")
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        Log.e(TAG, "onStartInputView restarting=$restarting")
        viewModel.onInputStarted()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        Log.e(TAG, "onFinishInput")
        viewModel.onInputFinished()
    }

    override fun onEvaluateInputViewShown(): Boolean {
        Log.e(TAG, "onEvaluateInputViewShown")
        return super.onEvaluateInputViewShown()
    }

    override fun onBindInput() {
        super.onBindInput()
        Log.e(TAG, "onBindInput")
    }

    override fun onUnbindInput() {
        super.onUnbindInput()
        Log.e(TAG, "onUnbindInput")
    }

    private fun insertText(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "onDestroy")
    }
}
