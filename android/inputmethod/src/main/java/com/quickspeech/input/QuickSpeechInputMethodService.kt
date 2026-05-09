package com.quickspeech.input

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.KeyEvent
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
    private var isShiftOn = false          // Temporary uppercase (single tap Shift)
    private var isCapsLock = false         // Caps lock (double tap Shift)
    private var isAiPanelVisible = false
    private var currentAiMode = ReplyMode.HYBRID
    private var currentInputText = ""
    private var inputView: View? = null
    private var lastShiftTapTime = 0L


    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "onCreate entered")
        try {
            val entryPoint = EntryPoints.get(applicationContext, ImeEntryPoint::class.java)
            aiRepository = entryPoint.aiReplyRepository()
            apiService = entryPoint.apiService()
            val wubiInputEngine = entryPoint.wubiInputEngine()
            Log.e(TAG, "WubiInputEngine obtained from DI")
            viewModel = InputMethodViewModel(wubiInputEngine)
            Log.e(TAG, "ViewModel created with WubiInputEngine")
        } catch (e: Throwable) {
            Log.e(TAG, "Error in onCreate", e)
        }
        Log.e(TAG, "onCreate finished")
    }

    override fun onCreateInputView(): View {
        Log.e(TAG, "onCreateInputView")
        val view = LayoutInflater.from(this).inflate(R.layout.input_method_view, null)

        // ===== Letter keys =====
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
                handleLetterKey(key)
            }
        }

        // ===== Number keys =====
        val numKeyIds = listOf(
            R.id.key_1, R.id.key_2, R.id.key_3, R.id.key_4, R.id.key_5,
            R.id.key_6, R.id.key_7, R.id.key_8, R.id.key_9, R.id.key_0
        )
        for (keyId in numKeyIds) {
            view.findViewById<TextView>(keyId)?.setOnClickListener { v ->
                val num = (v as TextView).text.toString()
                handleNumberKey(num)
            }
        }

        // ===== Shift (case toggle) =====
        view.findViewById<TextView>(R.id.key_shift)?.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastShiftTapTime < 400) {
                // Double tap: Caps Lock toggle
                isCapsLock = !isCapsLock
                isShiftOn = false
            } else {
                // Single tap: temporary uppercase
                if (isCapsLock) {
                    isCapsLock = false
                    isShiftOn = false
                } else {
                    isShiftOn = !isShiftOn
                }
            }
            lastShiftTapTime = now
            updateShiftKeyVisual(view)
        }

        // ===== Backspace =====
        view.findViewById<TextView>(R.id.key_backspace)?.setOnClickListener {
            if (!isEnglishMode && !isSymbolMode && viewModel.uiState.value.inputCode.isNotEmpty()) {
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

        // ===== Enter key =====
        view.findViewById<TextView>(R.id.key_enter)?.setOnClickListener {
            handleEnterKey()
        }

        // ===== Symbol toggle =====
        view.findViewById<TextView>(R.id.key_symbol)?.setOnClickListener {
            toggleSymbolMode(view)
        }

        // ===== Symbol keyboard back button =====
        view.findViewById<TextView>(R.id.key_symbol_back)?.setOnClickListener {
            toggleSymbolMode(view)
        }

        // ===== Symbol keyboard keys =====
        setupSymbolKeys(view)

        // ===== AI panel toggle with animation =====
        view.findViewById<TextView>(R.id.key_ai_toggle)?.setOnClickListener {
            isAiPanelVisible = !isAiPanelVisible
            val aiPanel = view.findViewById<LinearLayout>(R.id.ai_panel)
            if (isAiPanelVisible) {
                aiPanel?.visibility = View.VISIBLE
                // Slide-down animation
                val slideDown = android.view.animation.TranslateAnimation(
                    0f, 0f,
                    -(aiPanel?.height?.toFloat() ?: 200f), 0f
                )
                slideDown.duration = 200
                slideDown.interpolator = android.view.animation.DecelerateInterpolator()
                aiPanel?.startAnimation(slideDown)
                triggerAiSuggestions()
            } else {
                // Slide-up animation
                val slideUp = android.view.animation.TranslateAnimation(
                    0f, 0f,
                    0f, -(aiPanel?.height?.toFloat() ?: 200f)
                )
                slideUp.duration = 150
                slideUp.interpolator = android.view.animation.AccelerateInterpolator()
                slideUp.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationStart(a: android.view.animation.Animation?) {}
                    override fun onAnimationRepeat(a: android.view.animation.Animation?) {}
                    override fun onAnimationEnd(a: android.view.animation.Animation?) {
                        aiPanel?.visibility = View.GONE
                        aiPanel?.clearAnimation()
                    }
                })
                aiPanel?.startAnimation(slideUp)
            }
        }

        // ===== Language toggle (Chinese/English) =====
        view.findViewById<TextView>(R.id.key_toggle_lang)?.setOnClickListener {
            isEnglishMode = !isEnglishMode
            view.findViewById<TextView>(R.id.key_toggle_lang)?.apply {
                if (isEnglishMode) {
                    text = "英"
                    setBackgroundColor(0xFF1976D2.toInt())
                    setTextColor(0xFFFFFFFF.toInt())
                } else {
                    text = "中"
                    setBackgroundColor(0xFFD0D0D0.toInt())
                    setTextColor(0xFF555555.toInt())
                }
            }
            // Auto-enable Shift when switching to English
            if (isEnglishMode && !isShiftOn && !isCapsLock) {
                isShiftOn = true
                updateShiftKeyVisual(view)
            }
            // Disable Shift when switching back to Chinese
            if (!isEnglishMode) {
                isShiftOn = false
                isCapsLock = false
                updateShiftKeyVisual(view)
            }
        }

        // ===== Voice input =====
        view.findViewById<TextView>(R.id.key_voice)?.setOnClickListener {
            try {
                val pm = packageManager
                val activities = pm.queryIntentActivities(
                    Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0
                )
                if (activities.isEmpty()) {
                    val intent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        startActivity(intent)
                    } catch (e2: Exception) {
                        Toast.makeText(this, "Voice input unavailable. Please install a speech recognition app.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                        putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Please speak...")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Voice input error", e)
                Toast.makeText(this, "Voice input unavailable: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // ===== Punctuation keys =====
        view.findViewWithTag<TextView>("comma")?.setOnClickListener {
            commitPunctuation("，")
        }
        view.findViewWithTag<TextView>("period")?.setOnClickListener {
            commitPunctuation("。")
        }

        // ===== Space key =====
        view.findViewWithTag<TextView>("space")?.setOnClickListener {
            if (!isEnglishMode && !isSymbolMode && viewModel.uiState.value.candidates.isNotEmpty()) {
                // Wubi mode: select first candidate with space
                val candidate = viewModel.uiState.value.candidates.first()
                viewModel.onCandidateSelected(candidate)
                val ic = currentInputConnection ?: return@setOnClickListener
                ic.commitText(candidate, 1)
                currentInputText += candidate
                updateCandidates(view)
                triggerAiSuggestions()
            } else {
                val ic = currentInputConnection ?: return@setOnClickListener
                ic.commitText(" ", 1)
                currentInputText += " "
            }
            // Auto-disable temporary Shift after use
            if (isShiftOn && !isCapsLock) {
                isShiftOn = false
                updateShiftKeyVisual(view)
            }
        }

        // ===== AI panel collapse =====
        view.findViewById<TextView>(R.id.btn_ai_collapse)?.setOnClickListener {
            isAiPanelVisible = false
            view.findViewById<LinearLayout>(R.id.ai_panel)?.visibility = View.GONE
        }

        // ===== AI mode toggle =====
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
            Toast.makeText(this, "Mode: ${currentAiMode.displayName}", Toast.LENGTH_SHORT).show()
            if (isAiPanelVisible) triggerAiSuggestions()
        }

        // ===== Knowledge base search =====
        view.findViewById<TextView>(R.id.btn_knowledge_search)?.setOnClickListener {
            val knowledgePanel = view.findViewById<LinearLayout>(R.id.knowledge_panel)
            if (knowledgePanel?.visibility == View.VISIBLE) {
                knowledgePanel.visibility = View.GONE
            } else {
                knowledgePanel?.visibility = View.VISIBLE
                performKnowledgeSearch()
            }
        }

        // Bind Wubi radicals to letter keys
        com.quickspeech.input.ui.WubiKeyBinder.bindAllKeys(view)

        inputView = view
        updateShiftKeyVisual(view)
        return view
    }

    // ===== Letter key handling =====
    private fun handleLetterKey(key: String) {
        val ic = currentInputConnection ?: return
        when {
            isSymbolMode -> {
                // Symbol mode: commit letter directly
                ic.commitText(key, 1)
                currentInputText += key
            }
            isEnglishMode -> {
                // English mode: case depends on Shift state
                val out = if (isShiftOn || isCapsLock) key.uppercase() else key.lowercase()
                ic.commitText(out, 1)
                currentInputText += out
                triggerAiSuggestions()
                // Auto-disable temporary Shift
                if (isShiftOn && !isCapsLock && inputView != null) {
                    isShiftOn = false
                    updateShiftKeyVisual(inputView!!)
                }
            }
            else -> {
                // Wubi mode: send key to engine
                viewModel.onKeyInput(key.lowercase())
                inputView?.let { updateCandidates(it); updateInputTextFromCandidates(it) }
            }
        }
    }

    // ===== Number key handling =====
    private fun handleNumberKey(num: String) {
        val ic = currentInputConnection ?: return
        if (isSymbolMode) {
            // Symbol mode: map numbers to symbols
            val symbols = listOf("!", "@", "#", "$", "%", "^", "&", "*", "(", ")")
            val idx = num.toIntOrNull() ?: return
            if (idx in 0..9) {
                ic.commitText(symbols[idx], 1)
                currentInputText += symbols[idx]
            }
        } else if (!isEnglishMode && !isSymbolMode && viewModel.uiState.value.candidates.isNotEmpty()) {
            // Wubi mode: select candidate by number
            val idx = num.toIntOrNull() ?: return
            val candidates = viewModel.uiState.value.candidates
            if (idx >= 1 && idx <= candidates.size) {
                val candidate = candidates[idx - 1]
                viewModel.onCandidateSelected(candidate)
                ic.commitText(candidate, 1)
                currentInputText += candidate
                inputView?.let { updateCandidates(it); triggerAiSuggestions() }
            }
        } else {
            ic.commitText(num, 1)
            currentInputText += num
        }
    }

    // ===== Enter key handling =====
    private fun handleEnterKey() {
        val ic = currentInputConnection ?: return
        if (!isEnglishMode && !isSymbolMode && viewModel.uiState.value.candidates.isNotEmpty()) {
            val candidate = viewModel.uiState.value.candidates.first()
            viewModel.onCandidateSelected(candidate)
            ic.commitText(candidate, 1)
            currentInputText += candidate
            inputView?.let { updateCandidates(it); triggerAiSuggestions() }
        } else {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            triggerAiSuggestions()
            currentInputText = ""
        }
        // Auto-disable temporary Shift
        if (isShiftOn && !isCapsLock && inputView != null) {
            isShiftOn = false
            updateShiftKeyVisual(inputView!!)
        }
    }

    // ===== Commit punctuation =====
    private fun commitPunctuation(punct: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(punct, 1)
        currentInputText += punct
    }

    // ===== Symbol keyboard toggle =====
    private fun toggleSymbolMode(view: View) {
        isSymbolMode = !isSymbolMode
        val mainKeyboard = view.findViewById<LinearLayout>(R.id.keyboard_main)
        val symbolKeyboard = view.findViewById<LinearLayout>(R.id.keyboard_symbol)
        val symbolKey = view.findViewById<TextView>(R.id.key_symbol)

        if (isSymbolMode) {
            mainKeyboard?.visibility = View.GONE
            symbolKeyboard?.visibility = View.VISIBLE
            symbolKey?.text = "ABC"
            symbolKey?.setBackgroundColor(0xFF1976D2.toInt())
            symbolKey?.setTextColor(0xFFFFFFFF.toInt())
        } else {
            mainKeyboard?.visibility = View.VISIBLE
            symbolKeyboard?.visibility = View.GONE
            symbolKey?.text = "符"
            symbolKey?.setBackgroundColor(0xFFD0D0D0.toInt())
            symbolKey?.setTextColor(0xFF555555.toInt())
        }
    }

    // ===== Symbol keyboard key setup =====
    private fun setupSymbolKeys(view: View) {
        val symbolPairs = mapOf(
            "s_exclaim" to "!", "s_at" to "@", "s_hash" to "#", "s_dollar" to "$",
            "s_percent" to "%", "s_caret" to "^", "s_amp" to "&", "s_star" to "*",
            "s_lparen" to "(", "s_rparen" to ")",
            "s_tilde" to "~", "s_backtick" to "`", "s_minus" to "-", "s_equals" to "=",
            "s_plus" to "+", "s_lbracket" to "[", "s_rbracket" to "]",
            "s_lbrace" to "{", "s_rbrace" to "}",
            "s_pipe" to "|", "s_bslash" to "\\", "s_slash" to "/",
            "s_colon" to ":", "s_semicolon" to ";", "s_quote" to "\"", "s_apos" to "'",
            "s_lt" to "<", "s_gt" to ">",
            "s_comma" to "，", "s_period" to "。"
        )

        for ((tag, symbol) in symbolPairs) {
            view.findViewWithTag<TextView>(tag)?.setOnClickListener {
                commitPunctuation(symbol)
            }
        }

        // Symbol keyboard number keys
        for (i in 0..9) {
            view.findViewWithTag<TextView>("s$i")?.setOnClickListener {
                commitPunctuation(i.toString())
            }
        }

        // Symbol keyboard backspace
        view.findViewWithTag<TextView>("symbol_backspace")?.setOnClickListener {
            val ic = currentInputConnection ?: return@setOnClickListener
            ic.deleteSurroundingText(1, 0)
            if (currentInputText.isNotEmpty()) currentInputText = currentInputText.dropLast(1)
        }

        // Symbol keyboard space
        view.findViewWithTag<TextView>("s_space")?.setOnClickListener {
            commitPunctuation(" ")
        }

        // Symbol keyboard enter
        view.findViewWithTag<TextView>("s_enter")?.setOnClickListener {
            handleEnterKey()
        }
    }

    // ===== Shift key visual update =====
    private fun updateShiftKeyVisual(view: View) {
        val shiftKey = view.findViewById<TextView>(R.id.key_shift) ?: return
        when {
            isCapsLock -> {
                shiftKey.text = "⇪"
                shiftKey.setBackgroundResource(R.drawable.btn_shift_active_bg)
                shiftKey.setTextColor(0xFFFFFFFF.toInt())
            }
            isShiftOn -> {
                shiftKey.text = "⇧"
                shiftKey.setBackgroundResource(R.drawable.btn_shift_active_bg)
                shiftKey.setTextColor(0xFFFFFFFF.toInt())
            }
            else -> {
                shiftKey.text = "⇧"
                shiftKey.setBackgroundResource(R.drawable.btn_func_key_bg)
                shiftKey.setTextColor(0xFF555555.toInt())
            }
        }
    }

    // ===== Candidate display update =====
    private fun updateCandidates(view: View) {
        val state = viewModel.uiState.value
        view.findViewById<TextView>(R.id.input_code)?.text = state.inputCode
        val container = view.findViewById<LinearLayout>(R.id.candidates_container)
        container?.removeAllViews()

        // Show Wubi candidates
        for ((index, candidate) in state.candidates.take(10).withIndex()) {
            val tv = TextView(this).apply {
                text = if (index < 9) "${index + 1}.$candidate" else candidate
                textSize = 14f
                setPadding(14, 6, 14, 6)
                setTextColor(0xFF333333.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply { marginEnd = 4; topMargin = 4; bottomMargin = 4 }
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

        // Show associated words with separator
        if (state.associatedWords.isNotEmpty()) {
            // Add separator
            val separator = TextView(this).apply {
                text = " | "
                textSize = 14f
                setPadding(8, 6, 8, 6)
                setTextColor(0xFF999999.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply { topMargin = 4; bottomMargin = 4 }
            }
            container?.addView(separator)

            for (word in state.associatedWords.take(10)) {
                val tv = TextView(this).apply {
                    text = word
                    textSize = 14f
                    setPadding(14, 6, 14, 6)
                    setTextColor(0xFF1976D2.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    ).apply { marginEnd = 4; topMargin = 4; bottomMargin = 4 }
                    setOnClickListener {
                        viewModel.onAssociatedWordSelected(word)
                        val ic = currentInputConnection ?: return@setOnClickListener
                        ic.commitText(word, 1)
                        currentInputText += word
                        updateCandidates(view)
                        triggerAiSuggestions()
                    }
                }
                container?.addView(tv)
            }
        }
    }

    private fun updateInputTextFromCandidates(view: View) {
        val state = viewModel.uiState.value
        if (state.candidates.isNotEmpty()) {
            currentInputText = state.candidates.first()
        }
    }

    // ===== AI suggestions =====
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
                        pendingReplies = result.replies
                        runOnUiThread { updateAiRepliesView() }
                    }
                    is AiReplyResult.Error -> Log.e(TAG, "AI error: ${result.message}")
                    is AiReplyResult.Loading -> {}
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
                val sourceLabel = when (reply.source) {
                    ReplySource.KNOWLEDGE_BASE -> "📚"
                    ReplySource.AI_AGENT -> "🤖"
                    ReplySource.HYBRID -> "🔀"
                    else -> ""
                }
                val tv = TextView(this).apply {
                    text = if (sourceLabel.isNotEmpty()) "$sourceLabel ${reply.text}" else reply.text
                    textSize = 13f
                    setPadding(12, 8, 12, 8)
                    setBackgroundColor(0xFFFFFFFF.toInt())
                    setTextColor(0xFF333333.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { marginEnd = 8; topMargin = 2; bottomMargin = 2 }
                    // Click: replace current input with reply
                    setOnClickListener {
                        val ic = currentInputConnection ?: return@setOnClickListener
                        ic.deleteSurroundingText(currentInputText.length, 0)
                        ic.commitText(reply.text, 1)
                        currentInputText = reply.text
                    }
                    // Long press: show context menu (copy / favorite)
                    setOnLongClickListener {
                        showReplyContextMenu(reply.text, this@apply)
                        true
                    }
                }
                container.addView(tv)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating AI view", e)
        }
    }

    /**
     * 显示回复卡片长按菜单
     */
    private fun showReplyContextMenu(text: String, anchor: View) {
        val popup = android.widget.PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "复制")
        popup.menu.add(0, 2, 1, "收藏")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("reply", text))
                    Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
                    true
                }
                2 -> {
                    Toast.makeText(this, "已收藏", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    // ===== Knowledge base search =====
    private fun performKnowledgeSearch() {
        val view = inputView ?: return
        if (!::apiService.isInitialized) {
            Toast.makeText(this, "Knowledge base service not ready", Toast.LENGTH_SHORT).show()
            return
        }
        val query = currentInputText
        if (query.isBlank()) {
            Toast.makeText(this, "Please enter content before searching knowledge base", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            try {
                val request = com.quickspeech.common.network.model.KnowledgeSearchRequest(query = query, limit = 5)
                val response = apiService.searchKnowledge(request)
                runOnUiThread {
                    val container = view.findViewById<LinearLayout>(R.id.knowledge_results_container)
                    container?.removeAllViews()
                    if (response.isSuccessful && response.body() != null) {
                        val results = response.body()!!.results
                        if (results.isEmpty()) {
                            container?.addView(TextView(this@QuickSpeechInputMethodService).apply {
                                text = "No relevant knowledge found"; textSize = 12f
                                setTextColor(0xFF999999.toInt()); setPadding(8, 8, 8, 8)
                            })
                        } else {
                            for (result in results) {
                                container?.addView(TextView(this@QuickSpeechInputMethodService).apply {
                                    text = "• ${result.content}"; textSize = 12f
                                    setTextColor(0xFF333333.toInt()); setPadding(8, 6, 8, 6)
                                    setOnClickListener {
                                        val ic = currentInputConnection ?: return@setOnClickListener
                                        ic.commitText(result.content, 1)
                                    }
                                })
                            }
                        }
                    } else {
                        container?.addView(TextView(this@QuickSpeechInputMethodService).apply {
                            text = "Search failed: ${response.message()}"; textSize = 12f
                            setTextColor(0xFFCC0000.toInt()); setPadding(8, 8, 8, 8)
                        })
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Knowledge search error", e)
                runOnUiThread {
                    Toast.makeText(this@QuickSpeechInputMethodService, "Search error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
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

    override fun onEvaluateInputViewShown(): Boolean = true

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        Log.e(TAG, "onDestroy")
    }
}
