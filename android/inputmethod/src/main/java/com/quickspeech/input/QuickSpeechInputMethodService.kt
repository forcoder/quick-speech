package com.quickspeech.input

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.quickspeech.input.viewmodel.InputMethodViewModel
import com.quickspeech.wubi.engine.WubiEngine

class QuickSpeechInputMethodService : InputMethodService() {

    companion object {
        private const val TAG = "QuickSpeechIME"
    }

    private lateinit var viewModel: InputMethodViewModel

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "onCreate entered")

        try {
            val wubiEngine = WubiEngine()
            Log.e(TAG, "WubiEngine created, native loaded: ${WubiEngine.isNativeLoaded}")
            viewModel = InputMethodViewModel(wubiEngine)
            Log.e(TAG, "ViewModel created")
        } catch (e: Throwable) {
            Log.e(TAG, "Error in onCreate", e)
            throw e
        }

        Log.e(TAG, "onCreate finished")
    }

    override fun onCreateInputView(): View {
        Log.e(TAG, "onCreateInputView")

        val view = LayoutInflater.from(this).inflate(R.layout.input_method_view, null)

        // Set up key listeners
        val keyIds = listOf(
            R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t,
            R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p,
            R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g,
            R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l,
            R.id.key_z, R.id.key_x, R.id.key_c, R.id.key_v, R.id.key_b,
            R.id.key_n, R.id.key_m
        )

        for (keyId in keyIds) {
            view.findViewById<Button>(keyId)?.setOnClickListener { btn ->
                val key = (btn as Button).text.toString().lowercase()
                viewModel.onKeyInput(key)
                updateCandidates(view)
            }
        }

        view.findViewById<Button>(R.id.key_backspace)?.setOnClickListener {
            viewModel.onDelete()
            updateCandidates(view)
        }

        view.findViewById<Button>(R.id.key_enter)?.setOnClickListener {
            val ic = currentInputConnection ?: return@setOnClickListener
            ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
            ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))
        }

        return view
    }

    private fun updateCandidates(view: View) {
        val state = viewModel.uiState.value
        view.findViewById<TextView>(R.id.input_code)?.text = state.inputCode

        val container = view.findViewById<LinearLayout>(R.id.candidates_container)
        container?.removeAllViews()

        for (candidate in state.candidates.take(10)) {
            val tv = TextView(this).apply {
                text = candidate
                textSize = 14f
                setPadding(12, 6, 12, 6)
                setBackgroundResource(android.R.drawable.btn_default)
                setOnClickListener {
                    viewModel.onCandidateSelected(candidate)
                    val ic = currentInputConnection ?: return@setOnClickListener
                    ic.commitText(candidate, 1)
                    updateCandidates(view)
                }
            }
            container?.addView(tv)
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
    }
}
