package com.quickspeech.input

import android.inputmethodservice.InputMethodService
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.quickspeech.input.ui.InputMethodKeyboardView
import com.quickspeech.input.viewmodel.InputMethodViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

class QuickSpeechViewModelStoreOwner : ViewModelStoreOwner {
    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = store
}

@AndroidEntryPoint
class QuickSpeechInputMethodService : InputMethodService() {

    @Inject
    lateinit var defaultViewModelFactory: ViewModelProvider.Factory

    private val viewModelStoreOwner = QuickSpeechViewModelStoreOwner()
    private var viewModel: InputMethodViewModel? = null

    override fun onCreateInputView(): View {
        return ComposeView(this).apply {
            setContent {
                if (viewModel == null) {
                    val provider = ViewModelProvider(viewModelStoreOwner, defaultViewModelFactory)
                    viewModel = provider.get(InputMethodViewModel::class.java)
                }
                InputMethodKeyboardView(
                    viewModel = viewModel!!,
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
        viewModel?.onInputStarted()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        viewModel?.onInputFinished()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModelStoreOwner.viewModelStore.clear()
    }
}
