package com.quickspeech.input

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.quickspeech.input.ai.data.AiReply
import com.quickspeech.input.ai.data.AppCategory
import com.quickspeech.input.ai.data.ReplyMode
import com.quickspeech.input.ai.data.ReplySource
import com.quickspeech.input.ai.engine.LocalReplyGenerator
import com.quickspeech.input.ai.network.AiReplyRepository
import com.quickspeech.input.ai.network.AiReplyResult
import com.quickspeech.input.di.ImeEntryPoint
import com.quickspeech.input.ui.UserRuleManager
import com.quickspeech.input.viewmodel.InputMethodViewModel
import com.quickspeech.input.viewmodel.UserRuleMatch
import com.quickspeech.wubi.engine.UserRuleEngine
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
    private lateinit var userRuleEngine: UserRuleEngine
    private lateinit var userRuleManager: UserRuleManager
    private var scope: CoroutineScope? = null

    @Volatile private var isEnglishMode = false
    @Volatile private var isSymbolMode = false
    @Volatile private var isShiftOn = false          // Temporary uppercase (single tap Shift)
    @Volatile private var isCapsLock = false         // Caps lock (double tap Shift)
    @Volatile private var isAiPanelVisible = false
    private var currentAiMode = ReplyMode.HYBRID
    private var currentAiStyle = LocalReplyGenerator.ReplyStyle.CASUAL
    private var currentInputText = ""
    private var inputView: View? = null
    @Volatile private var lastShiftTapTime = 0L
    @Volatile private var showAssociatedWords = false


    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate entered")
        try {
            val entryPoint = EntryPoints.get(applicationContext, ImeEntryPoint::class.java)
            aiRepository = entryPoint.aiReplyRepository()
            apiService = entryPoint.apiService()
            val wubiInputEngine = entryPoint.wubiInputEngine()
            userRuleEngine = entryPoint.userRuleEngine()
            Log.d(TAG, "WubiInputEngine + UserRuleEngine obtained from DI")
            viewModel = InputMethodViewModel(wubiInputEngine, userRuleEngine)
            userRuleManager = UserRuleManager(applicationContext, entryPoint.userRuleDao(), userRuleEngine)
            Log.d(TAG, "ViewModel + UserRuleManager created")
        } catch (e: Throwable) {
            Log.d(TAG, "Fatal error in onCreate - IME may not function", e)
            throw e
        }
        Log.d(TAG, "onCreate finished")
    }

    override fun onCreateInputView(): View {
        // Create a fresh scope for each input view session to avoid leaks
        scope?.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        Log.d(TAG, "onCreateInputView")
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
                // Use tag to get the single-letter key, avoiding radical text in toString()
                val key = v.tag?.toString() ?: (v as TextView).text.toString().takeLast(1)
                handleLetterKey(key)
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

        // ===== Symbol toggle (123 mode) =====
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
                aiPanel?.clearAnimation()
                // Ensure layout pass before reading height
                aiPanel?.post {
                    val panelHeight = aiPanel?.height?.toFloat() ?: 200f
                    val slideDown = android.view.animation.TranslateAnimation(
                        0f, 0f, -panelHeight, 0f
                    )
                    slideDown.duration = 200
                    slideDown.interpolator = android.view.animation.DecelerateInterpolator()
                    aiPanel?.startAnimation(slideDown)
                }
                triggerAiSuggestions()
            } else {
                aiPanel?.clearAnimation()
                aiPanel?.post {
                    val panelHeight = aiPanel?.height?.toFloat() ?: 200f
                    val slideUp = android.view.animation.TranslateAnimation(
                        0f, 0f, 0f, -panelHeight
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
        }

        // ===== Language toggle (Chinese/English) =====
        view.findViewById<TextView>(R.id.key_toggle_lang)?.setOnClickListener {
            isEnglishMode = !isEnglishMode
            view.findViewById<TextView>(R.id.key_toggle_lang)?.apply {
                if (isEnglishMode) {
                    text = "英"
                    setBackgroundColor(0xFF1E88E5.toInt())
                    setTextColor(0xFFFFFFFF.toInt())
                } else {
                    text = "中"
                    setBackgroundColor(0xFFFFFFFF.toInt())
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
                Log.d(TAG, "Voice input error", e)
                Toast.makeText(this, "Voice input unavailable: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Long press voice key: open rule management hint
        view.findViewById<TextView>(R.id.key_voice)?.setOnLongClickListener {
            showRuleManagementHint()
            true
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
            if (!isEnglishMode && !isSymbolMode) {
                val state = viewModel.uiState.value
                // Priority 1: user rule match
                if (state.userRuleMatch != null) {
                    val match = state.userRuleMatch
                    if (match.expansion.isNotEmpty()) {
                        viewModel.onUserRuleSelected(match)
                        val ic = currentInputConnection ?: return@setOnClickListener
                        ic.commitText(match.expansion, 1)
                        currentInputText += match.expansion
                        updateCandidates(view)
                        triggerAiSuggestions()
                    } else {
                        val ic = currentInputConnection ?: return@setOnClickListener
                        ic.commitText(" ", 1)
                        currentInputText += " "
                    }
                }
                // Priority 2: Wubi candidates
                else if (state.candidates.isNotEmpty()) {
                    val candidate = state.candidates.first()
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

        // ===== AI style toggle (new: formal/casual/brief) =====
        view.findViewById<TextView>(R.id.btn_ai_style)?.setOnClickListener {
            currentAiStyle = when (currentAiStyle) {
                LocalReplyGenerator.ReplyStyle.FORMAL -> LocalReplyGenerator.ReplyStyle.CASUAL
                LocalReplyGenerator.ReplyStyle.CASUAL -> LocalReplyGenerator.ReplyStyle.BRIEF
                LocalReplyGenerator.ReplyStyle.BRIEF -> LocalReplyGenerator.ReplyStyle.FORMAL
            }
            val label = when (currentAiStyle) {
                LocalReplyGenerator.ReplyStyle.FORMAL -> "👔正式"
                LocalReplyGenerator.ReplyStyle.CASUAL -> "😊随意"
                LocalReplyGenerator.ReplyStyle.BRIEF -> "⚡简洁"
            }
            view.findViewById<TextView>(R.id.btn_ai_style)?.text = label
            Toast.makeText(this, "风格: $label", Toast.LENGTH_SHORT).show()
            if (isAiPanelVisible) triggerAiSuggestions()
        }

        // ===== AI refresh button (new: clear cache and regenerate) =====
        view.findViewById<TextView>(R.id.btn_ai_refresh)?.setOnClickListener {
            aiRepository.clearCache()
            triggerAiSuggestions()
            Toast.makeText(this, "已刷新", Toast.LENGTH_SHORT).show()
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

        // ===== Associated words expand/collapse toggle =====
        view.findViewById<TextView>(R.id.btn_candidates_more)?.setOnClickListener {
            showAssociatedWords = !showAssociatedWords
            updateCandidates(view)
            // Update button visual
            view.findViewById<TextView>(R.id.btn_candidates_more)?.text =
                if (showAssociatedWords) "▾" else "▸"
        }

        // Bind Wubi radicals to letter keys (small radical text above letter)
        val radicalMap = mapOf(
            R.id.key_q to "金", R.id.key_w to "人", R.id.key_e to "月",
            R.id.key_r to "白", R.id.key_t to "禾", R.id.key_y to "言",
            R.id.key_u to "立", R.id.key_i to "水", R.id.key_o to "火",
            R.id.key_p to "之",
            R.id.key_a to "工", R.id.key_s to "木", R.id.key_d to "大",
            R.id.key_f to "土", R.id.key_g to "一", R.id.key_h to "目",
            R.id.key_j to "日", R.id.key_k to "口", R.id.key_l to "田",
            R.id.key_z to "纟", R.id.key_x to "幺", R.id.key_c to "又",
            R.id.key_v to "女", R.id.key_b to "子", R.id.key_n to "已",
            R.id.key_m to "山"
        )
        for ((keyId, radical) in radicalMap) {
            view.findViewById<TextView>(keyId)?.apply {
                val letter = text.toString().takeLast(1)
                // Store the single-letter key as tag for reliable click handling
                tag = letter
                val spannable = android.text.SpannableString("$radical\n$letter")
                spannable.setSpan(
                    android.text.style.ForegroundColorSpan(0xFF999999.toInt()),
                    0, radical.length,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    android.text.style.AbsoluteSizeSpan(9, true),
                    0, radical.length,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    android.text.style.ForegroundColorSpan(0xFF333333.toInt()),
                    radical.length + 1, spannable.length,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    android.text.style.AbsoluteSizeSpan(15, true),
                    radical.length + 1, spannable.length,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                setText(spannable)
                gravity = android.view.Gravity.CENTER
            }
        }

        // ===== Keyboard height proportional to screen width =====
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val keyboardHeight = (screenWidth * 0.55f).toInt()
        view.post {
            val params = view.layoutParams ?: return@post
            params.height = keyboardHeight
            view.layoutParams = params
        }
        // Adjust key heights proportionally
        val keyHeight = ((keyboardHeight - dpToPx(44) - dpToPx(16)) / 5).coerceAtLeast(dpToPx(36))
        adjustKeyHeights(view, keyHeight)

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
                inputView?.let { updateCandidates(it) }
            }
        }
    }

    // ===== Enter key handling =====
    private fun handleEnterKey() {
        val ic = currentInputConnection ?: return
        val state = viewModel.uiState.value
        if (!isEnglishMode && !isSymbolMode && state.userRuleMatch != null) {
            val match = state.userRuleMatch
            if (match.expansion.isNotEmpty()) {
                viewModel.onUserRuleSelected(match)
                ic.commitText(match.expansion, 1)
                currentInputText += match.expansion
                inputView?.let { updateCandidates(it); triggerAiSuggestions() }
            } else {
                sendEnterKey()
                triggerAiSuggestions()
                currentInputText = ""
            }
        } else if (!isEnglishMode && !isSymbolMode && state.candidates.isNotEmpty()) {
            val candidate = state.candidates.first()
            viewModel.onCandidateSelected(candidate)
            ic.commitText(candidate, 1)
            currentInputText += candidate
            inputView?.let { updateCandidates(it); triggerAiSuggestions() }
        } else {
            sendEnterKey()
            triggerAiSuggestions()
            currentInputText = ""
        }
        // Auto-disable temporary Shift
        if (isShiftOn && !isCapsLock && inputView != null) {
            isShiftOn = false
            updateShiftKeyVisual(inputView!!)
        }
    }

    // ===== Send Enter key respecting EditorInfo action =====
    private fun sendEnterKey() {
        val ic = currentInputConnection ?: return
        val editorInfo = currentInputEditorInfo
        if (editorInfo == null) {
            // No editor info available, default to newline
            ic.commitText("\n", 1)
            return
        }
        val actionId = editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
        when (actionId) {
            EditorInfo.IME_ACTION_DONE,
            EditorInfo.IME_ACTION_GO,
            EditorInfo.IME_ACTION_SEARCH,
            EditorInfo.IME_ACTION_SEND -> {
                ic.performEditorAction(actionId)
            }
            EditorInfo.IME_ACTION_NEXT -> {
                ic.performEditorAction(EditorInfo.IME_ACTION_NEXT)
            }
            else -> {
                ic.commitText("\n", 1)
            }
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
            symbolKey?.setBackgroundColor(0xFF1E88E5.toInt())
            symbolKey?.setTextColor(0xFFFFFFFF.toInt())
            // Clear Wubi state when entering symbol mode
            viewModel.clearCandidates()
            updateCandidates(view)
        } else {
            mainKeyboard?.visibility = View.VISIBLE
            symbolKeyboard?.visibility = View.GONE
            symbolKey?.text = "符"
            symbolKey?.setBackgroundColor(0xFFB0B2B8.toInt())
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

        // Show user rule match (highest priority, with special styling)
        val ruleMatch = state.userRuleMatch
        if (ruleMatch != null && ruleMatch.expansion.isNotEmpty()) {
            val ruleTv = TextView(this).apply {
                text = "📋 ${ruleMatch.expansion}"
                textSize = 15f
                setPadding(16, 8, 16, 8)
                setTextColor(0xFF1565C0.toInt())
                setBackgroundColor(0xFFE3F2FD.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply { marginEnd = 6; topMargin = 5; bottomMargin = 5 }
                setOnClickListener {
                    viewModel.onUserRuleSelected(ruleMatch)
                    val ic = currentInputConnection ?: return@setOnClickListener
                    ic.commitText(ruleMatch.expansion, 1)
                    currentInputText += ruleMatch.expansion
                    updateCandidates(view)
                    triggerAiSuggestions()
                }
            }
            container?.addView(ruleTv)
        }

        // Show prefix-matching rule hints
        for (rule in state.userRulePrefixMatches.take(3)) {
            val hintTv = TextView(this).apply {
                text = "📋 ${rule.shortcut}"
                textSize = 13f
                setPadding(12, 8, 12, 8)
                setTextColor(0xFF7B1FA2.toInt())
                setBackgroundColor(0xFFF3E5F5.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply { marginEnd = 6; topMargin = 5; bottomMargin = 5 }
                setOnClickListener {
                    val match = UserRuleMatch(
                        ruleId = rule.id,
                        shortcut = rule.shortcut,
                        expansion = rule.expansion,
                        category = rule.category,
                        description = rule.description
                    )
                    viewModel.onUserRuleSelected(match)
                    val ic = currentInputConnection ?: return@setOnClickListener
                    ic.commitText(rule.expansion, 1)
                    currentInputText += rule.expansion
                    updateCandidates(view)
                    triggerAiSuggestions()
                }
            }
            container?.addView(hintTv)
        }

        // Show Wubi candidates (up to 7, card style)
        for ((index, candidate) in state.candidates.take(7).withIndex()) {
            val isFirst = index == 0
            val tv = TextView(this).apply {
                text = if (index < 6) "${index + 1}.$candidate" else candidate
                textSize = if (isFirst) 16f else 15f
                setPadding(
                    if (isFirst) 18 else 14,
                    8,
                    if (isFirst) 18 else 14,
                    8
                )
                setTextColor(
                    if (isFirst) 0xFF1E88E5.toInt() else 0xFF333333.toInt()
                )
                if (isFirst) {
                    setBackgroundColor(0xFFE3F2FD.toInt())
                } else {
                    setBackgroundColor(0xFFFFFFFF.toInt())
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    marginEnd = 6
                    topMargin = 5
                    bottomMargin = 5
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

        // Show associated words with separator (expandable via ▸ button)
        if (state.associatedWords.isNotEmpty() && showAssociatedWords) {
            // Add separator
            val separator = TextView(this).apply {
                text = " │ "
                textSize = 14f
                setPadding(6, 6, 6, 6)
                setTextColor(0xFFBBBBBB.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply { topMargin = 5; bottomMargin = 5 }
            }
            container?.addView(separator)

            for (word in state.associatedWords.take(10)) {
                val tv = TextView(this).apply {
                    text = word
                    textSize = 14f
                    setPadding(14, 8, 14, 8)
                    setTextColor(0xFF1E88E5.toInt())
                    setBackgroundColor(0xFFF5F5F5.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    ).apply { marginEnd = 6; topMargin = 5; bottomMargin = 5 }
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

    /**
     * Adjust all key heights proportionally
     */
    private fun adjustKeyHeights(view: View, heightPx: Int) {
        val keyIds = listOf(
            R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t,
            R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p,
            R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g,
            R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l,
            R.id.key_z, R.id.key_x, R.id.key_c, R.id.key_v, R.id.key_b,
            R.id.key_n, R.id.key_m,
            R.id.key_shift, R.id.key_enter, R.id.key_symbol, R.id.key_backspace,
            R.id.key_toggle_lang, R.id.key_voice, R.id.key_ai_toggle
        )
        for (keyId in keyIds) {
            view.findViewById<View>(keyId)?.layoutParams?.height = heightPx
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    /**
     * Detect current app category (context-aware)
     */
    private fun detectAppCategory(): AppCategory {
        val pkg = currentInputEditorInfo?.packageName ?: return AppCategory.OTHER
        return when {
            pkg.contains("mail") || pkg.contains("outlook") || pkg.contains("gmail") || pkg.contains("email") ->
                AppCategory.EMAIL
            pkg.contains("whatsapp") || pkg.contains("telegram") || pkg.contains("wechat") ||
            pkg.contains("qq") || pkg.contains("messenger") || pkg.contains("slack") || pkg.contains("dingtalk") ->
                AppCategory.INSTANT_MESSAGING
            pkg.contains("docs") || pkg.contains("word") || pkg.contains("notion") || pkg.contains("evernote") ->
                AppCategory.DOCUMENT
            else -> AppCategory.OTHER
        }
    }

    // ===== AI suggestions (enhanced with local fallback and style support) =====
    private fun triggerAiSuggestions() {
        if (!::aiRepository.isInitialized) return
        if (currentInputText.isBlank()) return
        val appCategory = detectAppCategory()
        scope?.launch {
            try {
                val result = aiRepository.fetchRepliesWithFallback(
                    inputContext = currentInputText,
                    appPackage = currentInputEditorInfo?.packageName ?: "",
                    appCategory = appCategory,
                    style = currentAiStyle
                )
                when (result) {
                    is AiReplyResult.Success -> {
                        pendingReplies = result.replies
                        runOnUiThread { updateAiRepliesView() }
                    }
                    is AiReplyResult.Error -> {
                        Log.d(TAG, "AI error: ${result.message}")
                        // Try local-only fallback when network fails
                        val localResult = aiRepository.generateLocalRepliesOnly(
                            inputContext = currentInputText,
                            appCategory = appCategory,
                            style = currentAiStyle
                        )
                        if (localResult is AiReplyResult.Success) {
                            pendingReplies = localResult.replies
                            runOnUiThread { updateAiRepliesView() }
                        }
                    }
                    is AiReplyResult.Loading -> {}
                }
            } catch (e: Exception) {
                Log.d(TAG, "AI suggestion error", e)
            }
        }
    }

    @Volatile
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

                    // Different background colors for AI-generated vs other sources
                    val bgColor = when (reply.source) {
                        ReplySource.KNOWLEDGE_BASE -> 0xFFE8F5E9.toInt() // Light green
                        ReplySource.AI_AGENT -> 0xFFE3F2FD.toInt()      // Light blue
                        ReplySource.HYBRID -> 0xFFF3E5F5.toInt()         // Light purple
                        else -> 0xFFFFFFFF.toInt()
                    }
                    setBackgroundColor(bgColor)
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
                        // Record positive feedback for adopted reply
                        aiRepository.recordReplyPreference(reply.text, true)
                    }
                    // Long press: show context menu (copy / thumbs down)
                    setOnLongClickListener {
                        showReplyContextMenu(reply.text, this@apply)
                        true
                    }
                }
                container.addView(tv)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error updating AI view", e)
        }
    }

    /**
     * Show reply card long-press context menu
     */
    private fun showReplyContextMenu(text: String, anchor: View) {
        val popup = android.widget.PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "复制")
        popup.menu.add(0, 2, 1, "不喜欢")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("reply", text))
                    Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
                    true
                }
                2 -> {
                    aiRepository.recordReplyPreference(text, false)
                    Toast.makeText(this, "已记录反馈", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    // ===== Rule management hint =====
    private fun showRuleManagementHint() {
        scope?.launch {
            try {
                val ruleCount = userRuleEngine.getRuleCount()
                val message = if (ruleCount == 0) {
                    "💡 Tip: Create custom rules for quick text expansion\n\n" +
                    "• Example: Type \"addr\" to insert your address\n" +
                    "• Example: Type \"sig1\" to insert email signature\n" +
                    "• Long-press voice key to manage rules"
                } else {
                    "💡 You have $ruleCount custom rule${if (ruleCount != 1) "s" else ""}. \n" +
                    "Long-press voice key to view/edit rules."
                }
                runOnUiThread {
                    Toast.makeText(this@QuickSpeechInputMethodService, message, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error showing rule hint", e)
            }
        }
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
        scope?.launch {
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
                Log.d(TAG, "Knowledge search error", e)
                runOnUiThread {
                    Toast.makeText(this@QuickSpeechInputMethodService, "Search error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        Log.d(TAG, "onStartInputView restarting=$restarting")
        try {
            @Suppress("DEPRECATION")
            window?.window?.setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        } catch (e: Exception) {
            Log.d(TAG, "Error setting window layout", e)
        }
    }

    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        return true
    }

    override fun onFinishInput() {
        super.onFinishInput()
        // Clear composing state when switching to a different input field
        viewModel.onInputFinished()
        currentInputText = ""
        isAiPanelVisible = false
        Log.d(TAG, "onFinishInput - cleared composing state")
    }

    override fun onStartInput(attribute: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        // Reset per-input-session state
        isEnglishMode = false
        isSymbolMode = false
        isShiftOn = false
        isCapsLock = false
        currentInputText = ""
        Log.d(TAG, "onStartInput restarting=$restarting")
    }

    override fun onDestroy() {
        pendingReplies = emptyList()
        scope?.cancel()
        scope = null
        viewModel.clear()
        Log.d(TAG, "onDestroy - cleaned up resources")
        super.onDestroy()
    }
}