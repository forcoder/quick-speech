package com.quickspeech.input

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import com.quickspeech.input.ui.InputMethodKeyboardView
import com.quickspeech.input.viewmodel.InputMethodViewModel
import com.quickspeech.wubi.engine.WubiEngine

class QuickSpeechInputMethodService : InputMethodService() {

    companion object {
        private const val TAG = "QuickSpeechIME"
    }

    private lateinit var viewModel: InputMethodViewModel
    private val serviceLifecycleOwner = object : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }

    override fun onCreate() {
        super.onCreate()
        serviceLifecycleOwner.registry.currentState = Lifecycle.State.RESUMED
        Log.e(TAG, "onCreate")

        val wubiEngine = WubiEngine()
        Log.e(TAG, "WubiEngine native loaded: ${WubiEngine.isNativeLoaded}")

        viewModel = InputMethodViewModel(wubiEngine)
    }

    override fun onCreateInputView(): View {
        Log.e(TAG, "onCreateInputView")
        return ComposeView(this).apply {
            setViewTreeLifecycleOwner(serviceLifecycleOwner)
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
    }

    override fun onFinishInput() {
        super.onFinishInput()
        Log.e(TAG, "onFinishInput")
    }

    override fun onEvaluateInputViewShown(): Boolean {
        return super.onEvaluateInputViewShown()
    }

    private fun insertText(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceLifecycleOwner.registry.currentState = Lifecycle.State.DESTROYED
        Log.e(TAG, "onDestroy")
    }
}
