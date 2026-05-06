package com.quickspeech.input

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.quickspeech.input.viewmodel.InputMethodViewModel
import com.quickspeech.wubi.engine.WubiEngine

class QuickSpeechInputMethodService : InputMethodService() {

    companion object {
        private const val TAG = "QuickSpeechIME"
    }

    private lateinit var viewModel: InputMethodViewModel
    private var isEnglishMode = false
    private var isSymbolMode = false

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
                val key = (v as TextView).text.toString()
                if (isEnglishMode) {
                    // English mode: commit letter directly
                    val ic = currentInputConnection ?: return@setOnClickListener
                    ic.commitText(key, 1)
                } else {
                    // Wubi mode: process through engine
                    viewModel.onKeyInput(key.lowercase())
                    updateCandidates(view)
                }
            }
        }

        // Number keys - commit directly
        val numKeyIds = listOf(
            R.id.key_1, R.id.key_2, R.id.key_3, R.id.key_4, R.id.key_5,
            R.id.key_6, R.id.key_7, R.id.key_8, R.id.key_9, R.id.key_0
        )
        for (keyId in numKeyIds) {
            view.findViewById<TextView>(keyId)?.setOnClickListener { v ->
                if (isSymbolMode) {
                    // Symbol mode: map numbers to common symbols
                    val symbols = listOf("!", "@", "#", "$", "%", "^", "&", "*", "(", ")")
                    val idx = (v as TextView).text.toString().toIntOrNull() ?: return@setOnClickListener
                    if (idx in 0..9) {
                        val ic = currentInputConnection ?: return@setOnClickListener
                        ic.commitText(symbols[idx], 1)
                    }
                } else {
                    val num = (v as TextView).text.toString()
                    val ic = currentInputConnection ?: return@setOnClickListener
                    ic.commitText(num, 1)
                }
            }
        }

        // Backspace
        view.findViewById<TextView>(R.id.key_backspace)?.setOnClickListener {
            if (!isEnglishMode && viewModel.uiState.value.inputCode.isNotEmpty()) {
                viewModel.onDelete()
                updateCandidates(view)
            } else {
                val ic = currentInputConnection ?: return@setOnClickListener
                ic.deleteSurroundingText(1, 0)
            }
        }

        // Enter
        view.findViewById<TextView>(R.id.key_enter)?.setOnClickListener {
            if (!isEnglishMode && viewModel.uiState.value.candidates.isNotEmpty()) {
                // Commit first candidate
                val candidate = viewModel.uiState.value.candidates.first()
                viewModel.onCandidateSelected(candidate)
                val ic = currentInputConnection ?: return@setOnClickListener
                ic.commitText(candidate, 1)
                updateCandidates(view)
            } else {
                val ic = currentInputConnection ?: return@setOnClickListener
                ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))
            }
        }

        // Symbol toggle
        view.findViewById<TextView>(R.id.key_symbol)?.setOnClickListener {
            isSymbolMode = !isSymbolMode
            val key = view.findViewById<TextView>(R.id.key_symbol)
            if (isSymbolMode) {
                key?.text = "ABC"
                key?.setBackgroundColor(0xFF4A90D9.toInt())
                key?.setTextColor(0xFFFFFFFF.toInt())
            } else {
                key?.text = "符"
                key?.setBackgroundColor(0xFFC8CACC.toInt())
                key?.setTextColor(0xFF555555.toInt())
            }
        }

        // Number toggle (switch between number row and letter row)
        view.findViewById<TextView>(R.id.key_toggle_num)?.setOnClickListener {
            // Toggle number row visibility is handled by always showing numbers
            // This key can be used to switch to pure number pad in future
            Toast.makeText(this, "数字模式", Toast.LENGTH_SHORT).show()
        }

        // Language toggle (Chinese/English)
        view.findViewById<TextView>(R.id.key_toggle_lang)?.setOnClickListener {
            isEnglishMode = !isEnglishMode
            val key = view.findViewById<TextView>(R.id.key_toggle_lang)
            if (isEnglishMode) {
                key?.text = "英"
                key?.setBackgroundColor(0xFF4A90D9.toInt())
                key?.setTextColor(0xFFFFFFFF.toInt())
            } else {
                key?.text = "中/英"
                key?.setBackgroundColor(0xFFC8CACC.toInt())
                key?.setTextColor(0xFF555555.toInt())
            }
        }

        // Voice input
        view.findViewById<TextView>(R.id.key_voice)?.setOnClickListener {
            try {
                val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                    putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "请说话...")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "语音输入不可用", Toast.LENGTH_SHORT).show()
            }
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
        Log.e(TAG, "onStartInput restarting=$restarting")
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
