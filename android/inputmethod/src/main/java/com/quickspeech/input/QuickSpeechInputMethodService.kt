package com.quickspeech.input

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
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

        // Letter keys
        val letterKeyIds = listOf(
            R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t,
            R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p,
            R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g,
            R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l,
            R.id.key_z, R.id.key_x, R.id.key_c, R.id.key_v, R.id.key_b,
            R.id.key_n, R.id.key_m
        )

        for (keyId in letterKeyIds) {
            view.findViewById<TextView>(keyId)?.setOnClickListener { v ->
                val key = (v as TextView).text.toString().lowercase()
                viewModel.onKeyInput(key)
                updateCandidates(view)
            }
        }

        // Number keys - commit directly
        val numKeyIds = listOf(
            R.id.key_1, R.id.key_2, R.id.key_3, R.id.key_4, R.id.key_5,
            R.id.key_6, R.id.key_7, R.id.key_8, R.id.key_9, R.id.key_0
        )
        for (keyId in numKeyIds) {
            view.findViewById<TextView>(keyId)?.setOnClickListener { v ->
                val num = (v as TextView).text.toString()
                val ic = currentInputConnection ?: return@setOnClickListener
                ic.commitText(num, 1)
            }
        }

        // Backspace
        view.findViewById<TextView>(R.id.key_backspace)?.setOnClickListener {
            viewModel.onDelete()
            updateCandidates(view)
        }

        // Enter
        view.findViewById<TextView>(R.id.key_enter)?.setOnClickListener {
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

        for ((index, candidate) in state.candidates.take(10).withIndex()) {
            val tv = TextView(this).apply {
                text = if (index < 9) "${index + 1}.$candidate" else candidate
                textSize = 15f
                setPadding(16, 8, 16, 8)
                setBackgroundColor(0xFFFFFFFF.toInt())
                setTextColor(0xFF333333.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = 6
                }
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
        Log.e(TAG, "onStartInput restarting=$restarting attribute=$attribute")
        if (attribute != null) {
            Log.e(TAG, "onStartInput inputType=${attribute.inputType} imeOptions=${attribute.imeOptions}")
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        Log.e(TAG, "onStartInputView restarting=$restarting")
        try {
            @Suppress("DEPRECATION")
            window?.window?.setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error setting window layout", e)
        }
    }

    override fun onFinishInput() {
        super.onFinishInput()
        Log.e(TAG, "onFinishInput")
    }

    override fun onEvaluateInputViewShown(): Boolean {
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "onDestroy")
    }
}
