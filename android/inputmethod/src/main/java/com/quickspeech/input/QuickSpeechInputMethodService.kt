package com.quickspeech.input

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.ComposeView
import com.quickspeech.input.ui.InputMethodKeyboardView
import com.quickspeech.input.viewmodel.InputMethodViewModel
import com.quickspeech.wubi.engine.WubiEngine
import dagger.hilt.android.EntryPoint
import dagger.hilt.android.EntryPointAccessors
import javax.inject.Inject

@EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface ImeEntryPoint {
    fun wubiEngine(): WubiEngine
}

class QuickSpeechInputMethodService : InputMethodService() {

    companion object {
        private const val TAG = "QuickSpeechIME"
    }

    private lateinit var viewModel: InputMethodViewModel

    override fun onCreate() {
        super.onCreate(savedInstanceState)
        Log.e(TAG, "onCreate")

        // 手动从 Hilt 获取依赖（InputMethodService 不支持 @AndroidEntryPoint）
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ImeEntryPoint::class.java
        )
        val wubiEngine = entryPoint.wubiEngine()
        Log.e(TAG, "WubiEngine native loaded: ${WubiEngine.isNativeLoaded}")

        viewModel = InputMethodViewModel(wubiEngine)
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
        Log.e(TAG, "onDestroy")
        if (::viewModel.isInitialized) {
            viewModel.onCleared()
        }
    }
}
