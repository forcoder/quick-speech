package com.quickspeech.input

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.quickspeech.input.ai.data.AiReply
import com.quickspeech.input.ai.data.ReplyMode
import com.quickspeech.input.ai.data.ReplySource
import com.quickspeech.input.ai.network.AiReplyRepository
import com.quickspeech.input.ai.network.AiReplyResult
import com.quickspeech.input.di.ImeEntryPoint
import com.quickspeech.input.viewmodel.InputMethodViewModel
import com.quickspeech.wubi.engine.WubiEngine
import dagger.hilt.EntryPoints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class QuickSpeechInputMethodService : InputMethodService() {

    companion object {
        private const val TAG = "QuickSpeechIME"
    }

    private lateinit var viewModel: InputMethodViewModel
    private lateinit var aiRepository: AiReplyRepository
    private lateinit var apiService: com.quickspeech.common.network.ApiService
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var isEnglishMode = false
    private var isSymbolMode = false
    private var isAiPanelVisible = false
    private var currentAiMode = ReplyMode.HYBRID
    private var currentInputText = ""
    private var inputView: View? = null

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "onCreate entered")

        try {
            // Get Hilt dependencies via EntryPoint
            val entryPoint = EntryPoints.get(applicationContext, ImeEntryPoint::class.java)
            aiRepository = entryPoint.aiReplyRepository()
            apiService = entryPoint.apiService()

            val wubiEngine = WubiEngine()
            Log.e(TAG, "WubiEngine created, native loaded: ${WubiEngine.isNativeLoaded}")
            viewModel = InputMethodViewModel(wubiEngine)
            Log.e(TAG, "ViewModel created")
        } catch (e: Throwable) {
            Log.e(TAG, "Error in onCreate", e)
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
                    val ic = currentInputConnection ?: return@setOnClickListener
                    ic.commitText(key, 1)
                    currentInputText += key
                    triggerAiSuggestions()
                } else {
                    viewModel.onKeyInput(key.lowercase())
                    updateCandidates(view)
                    updateInputTextFromCandidates(view)
                }
            }
        }

        // Number keys
        val numKeyIds = listOf(
            R.id.key_1, R.id.key_2, R.id.key_3, R.id.key_4, R.id.key_5,
            R.id.key_6, R.id.key_7, R.id.key_8, R.id.key_9, R.id.key_0
        )
        for (keyId in numKeyIds) {
            view.findViewById<TextView>(keyId)?.setOnClickListener { v ->
                val num = (v as TextView).text.toString()
                val ic = currentInputConnection ?: return@setOnClickListener
                if (isSymbolMode) {
                    val symbols = listOf("!", "@", "#", "$", "%", "^", "&", "*", "(", ")")
                    val idx = num.toIntOrNull() ?: return@setOnClickListener
                    if (idx in 0..9) ic.commitText(symbols[idx], 1)
                } else {
                    ic.commitText(num, 1)
                }
            }
        }

        // Backspace
        view.findViewById<TextView>(R.id.key_backspace)?.setOnClickListener {
            if (!isEnglishMode && viewModel.uiState.value.inputCode.isNotEmpty()) {
                viewModel.onDelete()
                updateCandidates(view)
                updateInputTextFromCandidates(view)
            } else {
                val ic = currentInputConnection ?: return@setOnClickListener
                ic.deleteSurroundingText(1, 0)
                if (currentInputText.isNotEmpty()) {
                    currentInputText = currentInputText.dropLast(1)
                }
            }
        }

        // Enter
        view.findViewById<TextView>(R.id.key_enter)?.setOnClickListener {
            if (!isEnglishMode && viewModel.uiState.value.candidates.isNotEmpty()) {
                val candidate = viewModel.uiState.value.candidates.first()
                viewModel.onCandidateSelected(candidate)
                val ic = currentInputConnection ?: return@setOnClickListener
                ic.commitText(candidate, 1)
                currentInputText += candidate
                updateCandidates(view)
                triggerAiSuggestions()
            } else {
                val ic = currentInputConnection ?: return@setOnClickListener
                ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))
                // Trigger AI suggestion on enter (sentence completed)
                triggerAiSuggestions()
                currentInputText = ""
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

        // AI panel toggle
        view.findViewById<TextView>(R.id.key_ai_toggle)?.setOnClickListener {
            isAiPanelVisible = !isAiPanelVisible
            val aiPanel = view.findViewById<LinearLayout>(R.id.ai_panel)
            aiPanel?.visibility = if (isAiPanelVisible) View.VISIBLE else View.GONE
            if (isAiPanelVisible) {
                triggerAiSuggestions()
            }
        }

        // Language toggle
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
                // Check if speech recognition is available
                val pm = packageManager
                val activities = pm.queryIntentActivities(
                    Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0
                )
                if (activities.isEmpty()) {
                    // Fallback: try to open any voice input method
                    val intent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        startActivity(intent)
                    } catch (e2: Exception) {
                        Toast.makeText(this, "语音输入不可用，请安装语音识别应用", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                        putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "请说话...")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Voice input error", e)
                Toast.makeText(this, "语音输入不可用: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // AI panel collapse button
        view.findViewById<TextView>(R.id.btn_ai_collapse)?.setOnClickListener {
            isAiPanelVisible = false
            view.findViewById<LinearLayout>(R.id.ai_panel)?.visibility = View.GONE
        }

        // AI mode switch
        view.findViewById<TextView>(R.id.btn_ai_mode)?.setOnClickListener {
            currentAiMode = when (currentAiMode) {
                ReplyMode.KNOWLEDGE_BASE -> ReplyMode.AI_AGENT
                ReplyMode.AI_AGENT -> ReplyMode.HYBRID
                ReplyMode.HYBRID -> ReplyMode.KNOWLEDGE_BASE
            }
            val label = when (currentAiMode) {
                ReplyMode.KNOWLEDGE_BASE -> "📚"
                ReplyMode.AI_AGENT -> "🤖"
                ReplyMode.HYBRID -> "🔀"
            }
            view.findViewById<TextView>(R.id.btn_ai_mode)?.text = label
            Toast.makeText(this, "模式: ${currentAiMode.displayName}", Toast.LENGTH_SHORT).show()
            if (isAiPanelVisible) triggerAiSuggestions()
        }

        // Knowledge search button
        view.findViewById<TextView>(R.id.btn_knowledge_search)?.setOnClickListener {
            val knowledgePanel = view.findViewById<LinearLayout>(R.id.knowledge_panel)
            if (knowledgePanel?.visibility == View.VISIBLE) {
                knowledgePanel.visibility = View.GONE
            } else {
                knowledgePanel?.visibility = View.VISIBLE
                performKnowledgeSearch()
            }
        }

        inputView = view
        return view
    }

    private fun updateInputTextFromCandidates(view: View) {
        // Build current input text from candidates for AI context
        val state = viewModel.uiState.value
        if (state.candidates.isNotEmpty()) {
            currentInputText = state.candidates.first()
        }
    }

    private fun triggerAiSuggestions() {
        if (!::aiRepository.isInitialized) return
        if (currentInputText.isBlank()) return

        scope.launch {
            try {
                val result = aiRepository.fetchReplies(
                    inputContext = currentInputText,
                    appPackage = "",
                    appCategory = com.quickspeech.input.ai.data.AppCategory.OTHER
                )
                when (result) {
                    is AiReplyResult.Success -> {
                        Log.e(TAG, "AI replies received: ${result.replies.size}")
                        // Store replies for UI update
                        pendingReplies = result.replies
                        runOnUiThread { updateAiRepliesView() }
                    }
                    is AiReplyResult.Error -> {
                        Log.e(TAG, "AI reply error: ${result.message}")
                    }
                    is AiReplyResult.Loading -> { /* do nothing */ }
                }
            } catch (e: Exception) {
                Log.e(TAG, "AI suggestion error", e)
            }
        }
    }

    private var pendingReplies: List<AiReply> = emptyList()

    private fun runOnUiThread(action: () -> Unit) {
        android.os.Handler(android.os.Looper.getMainLooper()).post(action)
    }

    private fun updateAiRepliesView() {
        try {
            val view = inputView ?: return
            val container = view.findViewById<LinearLayout>(R.id.ai_replies_container) ?: return
            container.removeAllViews()

            for (reply in pendingReplies.take(5)) {
                val tv = TextView(this).apply {
                    text = reply.text
                    textSize = 13f
                    setPadding(12, 8, 12, 8)
                    setBackgroundColor(0xFFFFFFFF.toInt())
                    setTextColor(0xFF333333.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginEnd = 8
                        topMargin = 2
                        bottomMargin = 2
                    }
                    setOnClickListener {
                        val ic = currentInputConnection ?: return@setOnClickListener
                        ic.deleteSurroundingText(currentInputText.length, 0)
                        ic.commitText(reply.text, 1)
                        currentInputText = reply.text
                    }
                }
                container.addView(tv)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating AI replies view", e)
        }
    }

    private fun performKnowledgeSearch() {
        val view = inputView ?: return
        if (!::apiService.isInitialized) {
            Toast.makeText(this, "知识库服务未就绪", Toast.LENGTH_SHORT).show()
            return
        }

        val query = currentInputText
        if (query.isBlank()) {
            Toast.makeText(this, "请先输入内容再搜索知识库", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            try {
                val request = com.quickspeech.common.network.model.KnowledgeSearchRequest(
                    query = query,
                    limit = 5
                )
                val response = apiService.searchKnowledge(request)
                runOnUiThread {
                    val container = view.findViewById<LinearLayout>(R.id.knowledge_results_container)
                    container?.removeAllViews()

                    if (response.isSuccessful && response.body() != null) {
                        val results = response.body()!!.results
                        if (results.isEmpty()) {
                            val tv = TextView(this@QuickSpeechInputMethodService).apply {
                                text = "未找到相关知识"
                                textSize = 12f
                                setTextColor(0xFF999999.toInt())
                                setPadding(8, 8, 8, 8)
                            }
                            container?.addView(tv)
                        } else {
                            for (result in results) {
                                val tv = TextView(this@QuickSpeechInputMethodService).apply {
                                    text = "• ${result.content}"
                                    textSize = 12f
                                    setTextColor(0xFF333333.toInt())
                                    setPadding(8, 6, 8, 6)
                                    setOnClickListener {
                                        val ic = currentInputConnection ?: return@setOnClickListener
                                        ic.commitText(result.content, 1)
                                    }
                                }
                                container?.addView(tv)
                            }
                        }
                    } else {
                        val tv = TextView(this@QuickSpeechInputMethodService).apply {
                            text = "搜索失败: ${response.message()}"
                            textSize = 12f
                            setTextColor(0xFFCC0000.toInt())
                            setPadding(8, 8, 8, 8)
                        }
                        container?.addView(tv)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Knowledge search error", e)
                runOnUiThread {
                    Toast.makeText(this@QuickSpeechInputMethodService, "搜索出错: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
                    currentInputText += candidate
                    updateCandidates(view)
                    triggerAiSuggestions()
                }
            }
            container?.addView(tv)
        }
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
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

    override fun onEvaluateInputViewShown(): Boolean {
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        Log.e(TAG, "onDestroy")
    }
}
