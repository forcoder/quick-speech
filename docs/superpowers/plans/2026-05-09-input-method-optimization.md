# QuickSpeech 输入法全面优化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 分五个阶段优化 QuickSpeech 输入法：词库扩充 + 智能纠错 + 词频排序 + 联想输入 + UI/布局优化 + AI 面板打磨

**Architecture:** 现有代码已有完整引擎层（WubiInputEngine、CandidateSorter、FrequencyLearner、AssociativeEngine、WubiMatcher + Room DB），但 IME 服务层（QuickSpeechInputMethodService）仍在使用旧的内嵌词典 WubiEngine。核心工作是：(1) 将 IME 层迁移到新的 WubiInputEngine；(2) 扩充词库数据；(3) 集成纠错和词频排序到候选词显示；(4) 添加联想输入 UI；(5) 优化键盘布局和视觉。

**Tech Stack:** Kotlin, Android XML Layout, Room Database, Hilt DI, Coroutines

---

## Phase 1: 五笔体验增强

### Task 1.1: 扩充词库预置数据

**Files:**
- Modify: `android/wubi/src/main/java/com/quickspeech/wubi/data/WubiPrepopulateData.kt`
- Create: `android/wubi/src/main/assets/wubi_dict_init.sql`

- [ ] **Step 1: 扩充 CHAR_CODE_MAP 到 600+ 常用字**

修改 `WubiPrepopulateData.kt`，在现有 ~150 字基础上，将 `CHAR_CODE_MAP` 扩充至 600+ 覆盖 GB2312 一级常用字。现有代码已有 150 个条目，在其后追加约 450 个常用字编码映射。

```kotlin
// 在现有 CHAR_CODE_MAP 末尾追加（在最后一个 "筋" to "t" 条目之后，mapOf 闭合之前）:
"脉" to "eyni", "脑" to "eyb", "神" to "pyj", "记" to "yn", "让" to "yh",
"给" to "xw", "向" to "tm", "由" to "mh", "听" to "kr", "见" to "mq",
"好" to "vb", "少" to "it", "太" to "dy", "老" to "ftx", "少" to "it",
"已" to "nnn", "经" to "x", "动" to "fcl", "起" to "fhn", "放" to "yt",
"进" to "fj", "去" to "fcu", "回" to "lkd", "出" to "bm", "来" to "goi",
"到" to "gcf", "开" to "gak", "关" to "udu", "走" to "fhu", "跑" to "khhi",
"坐" to "wwff", "站" to "uhkg", "睡" to "htip", "吃" to "ktnn",
// ... 继续追加至 600+ 字
```

- [ ] **Step 2: 扩充 PHRASE_CODE_MAP 到 2000+ 词组**

修改 `WubiPrepopulateData.kt`，将 `PHRASE_CODE_MAP` 从 ~230 条扩充到 2000+ 条常用二字词。

```kotlin
// 在 PHRASE_CODE_MAP 末尾追加：
"发展" to "ntna", "改革" to "afyg", "开放" to "gamy", "创新" to "wjuq",
"合作" to "wgaa", "努力" to "vcll", "工作" to "aawt", "学习" to "ipnu",
"生活" to "tgiy", "社会" to "pywf", "经济" to "xciy", "文化" to "yywx",
"教育" to "fytw", "科学" to "tufh", "技术" to "rsyw", "管理" to "tpgj",
"服务" to "etyn", "组织" to "xewf", "制度" to "rmak", "政策" to "gigh",
// ... 继续追加至 2000+ 词组
```

- [ ] **Step 3: 修改 CHAR_CODE_MAP 中频率值分配**

修改 `generateSingleCharEntries()` 方法，使用更合理的频率梯度：

```kotlin
fun generateSingleCharEntries(): List<WubiWordEntry> {
    val entries = mutableListOf<WubiWordEntry>()
    // 按使用频率分档：最高频 5000，最低频 500
    var freq = 5000
    val totalChars = CHAR_CODE_MAP.size
    val freqStep = 4500 / totalChars  // 线性递减
    CHAR_CODE_MAP.forEach { (char, code) ->
        entries.add(WubiWordEntry(code = code, word = char, frequency = freq.coerceAtLeast(500), type = 0))
        freq -= freqStep
    }
    return entries
}
```

- [ ] **Step 4: 添加数据库预填充逻辑**

修改 `WubiDatabase.kt`，添加 `createFromAsset` 支持：

```kotlin
fun create(context: Context): WubiDatabase {
    return Room.databaseBuilder(
        context.applicationContext,
        WubiDatabase::class.java,
        DATABASE_NAME
    )
        .createFromAsset("database/wubi_dict.db")  // 预填充数据库
        .fallbackToDestructiveMigration()
        .build()
}
```

- [ ] **Step 5: 提交**

```bash
git add android/wubi/src/main/java/com/quickspeech/wubi/data/WubiPrepopulateData.kt \
        android/wubi/src/main/java/com/quickspeech/wubi/data/WubiDatabase.kt
git commit -m "feat(P1): 扩充词库数据至 600+ 单字 + 2000+ 词组，添加数据库预填充支持"
```

---

### Task 1.2: 将 IME 服务迁移到 WubiInputEngine

**Files:**
- Modify: `android/inputmethod/src/main/java/com/quickspeech/input/QuickSpeechInputMethodService.kt`
- Modify: `android/inputmethod/src/main/java/com/quickspeech/input/di/ImeEntryPoint.kt`
- Modify: `android/inputmethod/src/main/java/com/quickspeech/input/viewmodel/InputMethodViewModel.kt`

- [ ] **Step 1: 修改 ImeEntryPoint 暴露 WubiInputEngine**

修改 `ImeEntryPoint.kt`，增加 `wubiInputEngine()` 方法：

```kotlin
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ImeEntryPoint {
    fun aiReplyRepository(): AiReplyRepository
    fun apiService(): com.quickspeech.common.network.ApiService
    fun wubiInputEngine(): com.quickspeech.wubi.engine.WubiInputEngine
}
```

- [ ] **Step 2: 修改 InputMethodViewModel 使用 WubiInputEngine**

修改 `InputMethodViewModel.kt`，将 `WubiEngine` 替换为 `WubiInputEngine`：

```kotlin
package com.quickspeech.input.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.quickspeech.wubi.engine.WubiInputEngine
import com.quickspeech.wubi.engine.EngineResult
import com.quickspeech.wubi.engine.RankedCandidate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InputMethodUiState(
    val inputCode: String = "",
    val candidates: List<String> = emptyList(),
    val associatedWords: List<String> = emptyList(),
    val aiReplies: List<AiReplyUiItem> = emptyList(),
    val aiMode: AiMode = AiMode.HYBRID,
    val isAiPanelVisible: Boolean = false,
    val isLoading: Boolean = false,
    val appType: String = "unknown"
)

data class AiReplyUiItem(
    val text: String,
    val source: String,
    val score: Float
)

enum class AiMode(val label: String) {
    KNOWLEDGE("knowledge"),
    AGENT("agent"),
    HYBRID("hybrid")
}

class InputMethodViewModel(
    private val wubiInputEngine: WubiInputEngine
) : ViewModel() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _uiState = MutableStateFlow(InputMethodUiState())
    val uiState: StateFlow<InputMethodUiState> = _uiState.asStateFlow()

    init {
        Log.e("QuickSpeech", "InputMethodViewModel created")
        scope.launch {
            wubiInputEngine.refreshUserData()
        }
    }

    fun onKeyInput(key: String) {
        scope.launch {
            val result = wubiInputEngine.processKey(key[0])
            when (result) {
                is EngineResult.Composing -> {
                    _uiState.value = _uiState.value.copy(
                        inputCode = result.code,
                        candidates = result.candidates.map { it.entry.word },
                        associatedWords = emptyList()
                    )
                }
                is EngineResult.TextSelected -> {
                    _uiState.value = _uiState.value.copy(
                        inputCode = "",
                        candidates = emptyList(),
                        associatedWords = wubiInputEngine.associatedWords.value.map { it.word }
                    )
                }
                is EngineResult.Cleared -> {
                    _uiState.value = _uiState.value.copy(
                        inputCode = "",
                        candidates = emptyList(),
                        associatedWords = emptyList()
                    )
                }
                else -> {}
            }
        }
    }

    fun onDelete() {
        scope.launch {
            val result = wubiInputEngine.processKey('\b')
            when (result) {
                is EngineResult.Composing -> {
                    _uiState.value = _uiState.value.copy(
                        inputCode = result.code,
                        candidates = result.candidates.map { it.entry.word }
                    )
                }
                is EngineResult.Cleared -> {
                    _uiState.value = _uiState.value.copy(
                        inputCode = "",
                        candidates = emptyList()
                    )
                }
                else -> {}
            }
        }
    }

    fun onCandidateSelected(candidate: String) {
        val ranked = wubiInputEngine.candidates.value
        val index = ranked.indexOfFirst { it.entry.word == candidate }
        if (index >= 0) {
            scope.launch {
                wubiInputEngine.selectAssociatedWord(candidate)
            }
        }
        _uiState.value = _uiState.value.copy(inputCode = "", candidates = emptyList())
    }

    fun onAiReplySelected(reply: AiReplyUiItem) {
        _uiState.value = _uiState.value.copy(aiReplies = emptyList(), isAiPanelVisible = false)
    }

    fun toggleAiPanel() {
        _uiState.value = _uiState.value.copy(isAiPanelVisible = !_uiState.value.isAiPanelVisible)
    }

    fun setAiMode(mode: AiMode) {
        _uiState.value = _uiState.value.copy(aiMode = mode)
    }

    fun onInputFinished() {
        _uiState.value = _uiState.value.copy(
            inputCode = "",
            candidates = emptyList(),
            aiReplies = emptyList(),
            isAiPanelVisible = false
        )
        wubiInputEngine.reset()
    }

    override fun onCleared() {
        super.onCleared()
        scope.cancel()
    }
}
```

- [ ] **Step 3: 修改 QuickSpeechInputMethodService 使用新 ViewModel**

修改 `QuickSpeechInputMethodService.kt` 中的 `onCreate()` 方法：

```kotlin
override fun onCreate() {
    super.onCreate()
    Log.e(TAG, "onCreate entered")
    try {
        val entryPoint = EntryPoints.get(applicationContext, ImeEntryPoint::class.java)
        aiRepository = entryPoint.aiReplyRepository()
        apiService = entryPoint.apiService()
        val wubiInputEngine = entryPoint.wubiInputEngine()
        viewModel = InputMethodViewModel(wubiInputEngine)
        Log.e(TAG, "WubiInputEngine created, native loaded: ${com.quickspeech.wubi.engine.WubiEngine.isNativeLoaded}")
    } catch (e: Throwable) {
        Log.e(TAG, "Error in onCreate", e)
    }
    Log.e(TAG, "onCreate finished")
}
```

- [ ] **Step 4: 更新 updateCandidates 方法使用 RankedCandidate**

修改 `QuickSpeechInputMethodService.kt` 中的 `updateCandidates()` 方法，从 ViewModel 的 uiState 获取候选词列表并显示：

```kotlin
private fun updateCandidates(view: View) {
    val state = viewModel.uiState.value
    view.findViewById<TextView>(R.id.input_code)?.text = state.inputCode
    val container = view.findViewById<LinearLayout>(R.id.candidates_container)
    container?.removeAllViews()

    // 显示主候选词
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

    // 显示联想词（用分隔符隔开）
    if (state.associatedWords.isNotEmpty()) {
        val sep = TextView(this).apply {
            text = "|"
            textSize = 14f
            setTextColor(0xFFAAAAAA.toInt())
            setPadding(4, 6, 4, 6)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply { topMargin = 4; bottomMargin = 4 }
        }
        container?.addView(sep)

        for (word in state.associatedWords.take(5)) {
            val tv = TextView(this).apply {
                text = word
                textSize = 13f
                setPadding(10, 6, 10, 6)
                setTextColor(0xFF1976D2.toInt())
                setBackgroundColor(0xFFE3F2FD.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply { marginEnd = 4; topMargin = 4; bottomMargin = 4 }
                setOnClickListener {
                    viewModel.onCandidateSelected(word)
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
```

- [ ] **Step 5: 提交**

```bash
git add android/inputmethod/src/main/java/com/quickspeech/input/QuickSpeechInputMethodService.kt \
        android/inputmethod/src/main/java/com/quickspeech/input/di/ImeEntryPoint.kt \
        android/inputmethod/src/main/java/com/quickspeech/input/viewmodel/InputMethodViewModel.kt
git commit -m "feat(P1): 将 IME 服务迁移到 WubiInputEngine，集成词频排序 + 联想词显示"
```

---

### Task 1.3: 相邻键位纠错

**Files:**
- Modify: `android/wubi/src/main/java/com/quickspeech/wubi/engine/WubiMatcher.kt`

- [ ] **Step 1: 在 WubiMatcher 中添加相邻键位纠错方法**

修改 `WubiMatcher.kt`，在 `smartMatch` 方法中，当精确匹配和前缀匹配都无结果时，增加相邻键位纠错层：

```kotlin
/**
 * 相邻键位纠错匹配
 * 将编码中每个字符替换为物理相邻键位，生成候选编码后查询
 */
suspend fun adjacentKeyMatch(code: String, limit: Int = 20): MatchResult = withContext(Dispatchers.IO) {
    if (code.length > 4) return@withContext MatchResult(code, emptyList(), MatchType.NONE)

    val adjacentMap = mapOf(
        'q' to "wa", 'w' to "qeas", 'e' to "wrsd", 'r' to "etdf",
        't' to "ryfg", 'y' to "tugh", 'u' to "yijh", 'i' to "ujko",
        'o' to "iklp", 'p' to "ol",
        'a' to "qwsz", 's' to "awedx", 'd' to "serfcx",
        'f' to "drtgvc", 'g' to "ftyhbv", 'h' to "gyujnb",
        'j' to "huiknm", 'k' to "jiolm", 'l' to "kop",
        'z' to "asx", 'x' to "zsdc", 'c' to "xdfv",
        'v' to "cfgb", 'b' to "vghn", 'n' to "bhjm", 'm' to "njk"
    )

    val candidates = mutableListOf<WubiWordEntry>()

    // 对编码中每一位尝试替换为相邻键
    for (i in code.indices) {
        val originalChar = code[i]
        val adjacentChars = adjacentMap[originalChar] ?: continue
        for (replacement in adjacentChars) {
            val correctedCode = code.substring(0, i) + replacement + code.substring(i + 1)
            val results = try { dao.exactMatch(correctedCode) } catch (e: Exception) { emptyList() }
            candidates.addAll(results)
        }
    }

    // 去重并按词频排序
    val deduped = candidates.distinctBy { it.word }.sortedByDescending { it.frequency }.take(limit)
    if (deduped.isNotEmpty()) {
        MatchResult(code, deduped, MatchType.CORRECTED)
    } else {
        MatchResult(code, emptyList(), MatchType.NONE)
    }
}
```

- [ ] **Step 2: 在 smartMatch 中集成纠错层**

修改 `smartMatch` 方法，在现有匹配逻辑末尾增加纠错层：

```kotlin
// 在 smartMatch 方法最后（前缀匹配之后）增加：
// 第五优先级：相邻键位纠错
if (prefixResults.isEmpty()) {
    val correctedResult = adjacentKeyMatch(lowerCode, limit)
    if (correctedResult.candidates.isNotEmpty()) {
        return@withContext correctedResult
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add android/wubi/src/main/java/com/quickspeech/wubi/engine/WubiMatcher.kt
git commit -m "feat(P1): 添加相邻键位纠错功能"
```

---

## Phase 2: 联想输入

### Task 2.1: 整词联想 UI 集成

**Files:**
- Modify: `android/inputmethod/src/main/java/com/quickspeech/input/QuickSpeechInputMethodService.kt`
- Modify: `android/inputmethod/src/main/res/layout/input_method_view.xml`

- [ ] **Step 1: 在候选词栏添加联想词展开功能**

修改 `input_method_view.xml`，将 ▸ 按钮改为可展开联想词的触发器（已有 `btn_candidates_more`，保持不变）。

候选词栏中的联想词展示区域已在 Task 1.2 的 `updateCandidates()` 中实现（通过 `state.associatedWords` 追加到 candidates_container）。

- [ ] **Step 2: 添加联想词展开/收起逻辑**

在 `QuickSpeechInputMethodService.kt` 中，为 `btn_candidates_more` 添加展开联想词功能：

```kotlin
// 在 onCreateInputView 中替换原有的 btn_candidates_more 点击逻辑
view.findViewById<TextView>(R.id.btn_candidates_more)?.setOnClickListener {
    val state = viewModel.uiState.value
    if (state.associatedWords.isNotEmpty()) {
        // 切换联想词显示
        showAssociatedWords = !showAssociatedWords
        updateCandidates(view)
    }
}
```

添加 `showAssociatedWords` 成员变量：

```kotlin
private var showAssociatedWords = false
```

修改 `updateCandidates()` 方法，当 `showAssociatedWords` 为 false 时不显示联想词区域。

- [ ] **Step 3: 提交**

```bash
git add android/inputmethod/src/main/java/com/quickspeech/input/QuickSpeechInputMethodService.kt \
        android/inputmethod/src/main/res/layout/input_method_view.xml
git commit -m "feat(P2): 添加整词联想 UI 展开/收起功能"
```

---

## Phase 3: UI/布局优化

### Task 3.1: 按键字根显示

**Files:**
- Modify: `android/inputmethod/src/main/res/layout/input_method_view.xml`
- Create: `android/inputmethod/src/main/java/com/quickspeech/input/ui/WubiKeyBinder.kt`

- [ ] **Step 1: 创建 WubiKeyBinder 工具类**

新建 `WubiKeyBinder.kt`，负责将字根数据绑定到按键：

```kotlin
package com.quickspeech.input.ui

import android.widget.TextView
import com.quickspeech.input.R

object WubiKeyBinder {
    // 每个字母键对应的主字根（键名字）
    val keyRoots: Map<Int, String> = mapOf(
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

    fun bindKey(textView: TextView) {
        val root = keyRoots[textView.id] ?: return
        // 设置字根为 tag，由 IME 服务层处理显示
        textView.tag = root
    }
}
```

- [ ] **Step 2: 修改字母按键布局使用 key_wubi.xml 样式**

修改 `input_method_view.xml`，将每个字母按键从简单的 `TextView` 改为使用 `include` 引用 `key_wubi.xml`：

每个字母键从：
```xml
<TextView android:id="@+id/key_q" style="@style/WubiKey" android:text="Q" />
```

改为：
```xml
<include
    android:id="@+id/key_q"
    layout="@layout/key_wubi"
    style="@style/WubiKey"
    android:layout_width="0dp"
    android:layout_height="44dp"
    android:layout_weight="1"
    android:layout_margin="1.5dp" />
```

注意：由于 `include` 不能直接应用 style，需要在代码中动态设置布局参数。更好的方案是保持 TextView 但在代码中设置字根。

**替代方案（更简洁）**：在 `QuickSpeechInputMethodService.kt` 的 `onCreateInputView()` 中，为每个字母键设置字根提示：

```kotlin
// 在 onCreateInputView 中，字母键设置完成后
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
        // 使用 SpannableString 在上方显示字根
        val spannable = SpannableString("$radical\n$text")
        spannable.setSpan(
            ForegroundColorSpan(0xFFAAAAAA.toInt()),
            0, radical.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            AbsoluteSizeSpan(8, true),
            0, radical.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        setText(spannable)
        gravity = android.view.Gravity.CENTER
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add android/inputmethod/src/main/java/com/quickspeech/input/ui/WubiKeyBinder.kt \
        android/inputmethod/src/main/java/com/quickspeech/input/QuickSpeechInputMethodService.kt
git commit -m "feat(P3): 按键显示五笔字根"
```

---

### Task 3.2: 布局比例适配

**Files:**
- Modify: `android/inputmethod/src/main/java/com/quickspeech/input/QuickSpeechInputMethodService.kt`
- Modify: `android/inputmethod/src/main/res/layout/input_method_view.xml`
- Modify: `android/inputmethod/src/main/res/values/styles.xml`

- [ ] **Step 1: 修改键盘高度为比例计算**

在 `QuickSpeechInputMethodService.kt` 的 `onCreateInputView()` 末尾添加：

```kotlin
// 根据屏幕宽度计算键盘高度
val displayMetrics = resources.displayMetrics
val screenWidth = displayMetrics.widthPixels
val keyboardHeight = (screenWidth * 0.55f).toInt()

view.post {
    val params = view.layoutParams ?: return@post
    params.height = keyboardHeight
    view.layoutParams = params
}

// 调整按键高度
val keyHeight = ((keyboardHeight - 44 - 16) / 5).coerceAtLeast(36)
adjustKeyHeights(view, keyHeight)
```

添加辅助方法：

```kotlin
private fun adjustKeyHeights(view: View, heightDp: Int) {
    val heightPx = (heightDp * resources.displayMetrics.density).toInt()
    val keyIds = listOf(
        R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t,
        R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p,
        R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g,
        R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l,
        R.id.key_z, R.id.key_x, R.id.key_c, R.id.key_v, R.id.key_b,
        R.id.key_n, R.id.key_m,
        R.id.key_1, R.id.key_2, R.id.key_3, R.id.key_4, R.id.key_5,
        R.id.key_6, R.id.key_7, R.id.key_8, R.id.key_9, R.id.key_0,
        R.id.key_shift, R.id.key_enter, R.id.key_symbol, R.id.key_backspace,
        R.id.key_toggle_lang, R.id.key_voice, R.id.key_ai_toggle
    )
    for (keyId in keyIds) {
        view.findViewById<View>(keyId)?.layoutParams?.height = heightPx
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add android/inputmethod/src/main/java/com/quickspeech/input/QuickSpeechInputMethodService.kt \
        android/inputmethod/src/main/res/layout/input_method_view.xml \
        android/inputmethod/src/main/res/values/styles.xml
git commit -m "feat(P3): 键盘高度按比例适配屏幕宽度"
```

---

### Task 3.3: 深色主题支持

**Files:**
- Create: `android/inputmethod/src/main/res/values-night/styles.xml`
- Create: `android/inputmethod/src/main/res/drawable/btn_key_bg_dark.xml`
- Create: `android/inputmethod/src/main/res/drawable/btn_func_key_bg_dark.xml`
- Create: `android/inputmethod/src/main/res/drawable/btn_space_bg_dark.xml`
- Create: `android/inputmethod/src/main/res/drawable/btn_enter_key_bg_dark.xml`

- [ ] **Step 1: 创建深色主题 styles**

新建 `android/inputmethod/src/main/res/values-night/styles.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="WubiKey.Dark">
        <item name="android:layout_width">0dp</item>
        <item name="android:layout_height">44dp</item>
        <item name="android:layout_weight">1</item>
        <item name="android:layout_margin">1.5dp</item>
        <item name="android:background">@drawable/btn_key_bg_dark</item>
        <item name="android:textSize">16sp</item>
        <item name="android:textColor">#E0E0E0</item>
        <item name="android:textStyle">bold</item>
        <item name="android:gravity">center</item>
        <item name="android:minWidth">0dp</item>
        <item name="android:minHeight">0dp</item>
        <item name="android:padding">0dp</item>
        <item name="android:clickable">true</item>
        <item name="android:focusable">true</item>
        <item name="android:fontFamily">sans-serif</item>
    </style>
    <style name="WubiKey.Func.Dark">
        <item name="android:layout_width">0dp</item>
        <item name="android:layout_height">44dp</item>
        <item name="android:layout_weight">1</item>
        <item name="android:layout_margin">1.5dp</item>
        <item name="android:background">@drawable/btn_func_key_bg_dark</item>
        <item name="android:textSize">11sp</item>
        <item name="android:textColor">#AAAAAA</item>
        <item name="android:textStyle">normal</item>
        <item name="android:gravity">center</item>
        <item name="android:fontFamily">sans-serif</item>
    </style>
    <style name="WubiKey.Space.Dark">
        <item name="android:layout_width">0dp</item>
        <item name="android:layout_height">44dp</item>
        <item name="android:layout_weight">4</item>
        <item name="android:layout_margin">1.5dp</item>
        <item name="android:background">@drawable/btn_space_bg_dark</item>
        <item name="android:textSize">12sp</item>
        <item name="android:textColor">#888888</item>
        <item name="android:fontFamily">sans-serif</item>
    </style>
    <style name="WubiKey.Enter.Dark">
        <item name="android:layout_width">0dp</item>
        <item name="android:layout_height">44dp</item>
        <item name="android:layout_weight">1.5</item>
        <item name="android:layout_margin">1.5dp</item>
        <item name="android:background">@drawable/btn_enter_key_bg_dark</item>
        <item name="android:textSize">13sp</item>
        <item name="android:textColor">#FFFFFF</item>
        <item name="android:fontFamily">sans-serif</item>
    </style>
</resources>
```

- [ ] **Step 2: 创建深色主题 drawable 文件**

`btn_key_bg_dark.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#2D2D2D" />
    <corners android:radius="4dp" />
    <stroke android:width="0.5dp" android:color="#3A3A3A" />
</shape>
```

`btn_func_key_bg_dark.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#3A3A3A" />
    <corners android:radius="4dp" />
</shape>
```

`btn_space_bg_dark.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#2D2D2D" />
    <corners android:radius="4dp" />
    <stroke android:width="0.5dp" android:color="#3A3A3A" />
</shape>
```

`btn_enter_key_bg_dark.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#1565C0" />
    <corners android:radius="4dp" />
</shape>
```

- [ ] **Step 3: 提交**

```bash
git add android/inputmethod/src/main/res/values-night/styles.xml \
        android/inputmethod/src/main/res/drawable/btn_key_bg_dark.xml \
        android/inputmethod/src/main/res/drawable/btn_func_key_bg_dark.xml \
        android/inputmethod/src/main/res/drawable/btn_space_bg_dark.xml \
        android/inputmethod/src/main/res/drawable/btn_enter_key_bg_dark.xml
git commit -m "feat(P3): 添加深色主题样式和 drawable"
```

---

## Phase 4: AI 功能打磨

### Task 4.1: AI 面板动画

**Files:**
- Modify: `android/inputmethod/src/main/java/com/quickspeech/input/QuickSpeechInputMethodService.kt`

- [ ] **Step 1: 为 AI 面板添加展开/收起动画**

```kotlin
view.findViewById<TextView>(R.id.key_ai_toggle)?.setOnClickListener {
    isAiPanelVisible = !isAiPanelVisible
    val aiPanel = view.findViewById<LinearLayout>(R.id.ai_panel)
    if (isAiPanelVisible) {
        aiPanel?.visibility = View.VISIBLE
        val slideDown = TranslateAnimation(0f, 0f, -aiPanel?.height?.toFloat() ?: -200f, 0f)
        slideDown.duration = 200
        slideDown.interpolator = android.view.animation.DecelerateInterpolator()
        aiPanel?.startAnimation(slideDown)
        triggerAiSuggestions()
    } else {
        val slideUp = TranslateAnimation(0f, 0f, 0f, -aiPanel?.height?.toFloat() ?: -200f)
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
```

- [ ] **Step 2: 提交**

```bash
git add android/inputmethod/src/main/java/com/quickspeech/input/QuickSpeechInputMethodService.kt
git commit -m "feat(P4): AI 面板展开/收起动画"
```

---

### Task 4.2: 回复卡片增强

**Files:**
- Modify: `android/inputmethod/src/main/java/com/quickspeech/input/QuickSpeechInputMethodService.kt`

- [ ] **Step 1: 添加长按菜单**

在回复卡片 TextView 上添加 setOnLongClickListener：

```kotlin
setOnLongClickListener {
    showReplyContextMenu(reply.text, this)
    true
}
```

添加方法：

```kotlin
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
            2 -> { Toast.makeText(this, "已收藏", Toast.LENGTH_SHORT).show(); true }
            else -> false
        }
    }
    popup.show()
}
```

- [ ] **Step 2: 提交**

```bash
git add android/inputmethod/src/main/java/com/quickspeech/input/QuickSpeechInputMethodService.kt
git commit -m "feat(P4): AI 回复卡片长按菜单（复制/收藏）"
```

---

## Phase 5: 高级 AI

### Task 5.1: 上下文感知回复

**Files:**
- Modify: `android/inputmethod/src/main/java/com/quickspeech/input/QuickSpeechInputMethodService.kt`

- [ ] **Step 1: 添加应用类型检测**

```kotlin
private fun detectAppCategory(): String {
    val pkg = currentInputEditorInfo?.packageName ?: return "other"
    return when {
        pkg.contains("mail") || pkg.contains("outlook") || pkg.contains("gmail") -> "email"
        pkg.contains("whatsapp") || pkg.contains("telegram") || pkg.contains("wechat") || pkg.contains("qq") -> "im"
        pkg.contains("docs") || pkg.contains("word") || pkg.contains("notion") -> "document"
        else -> "other"
    }
}
```

修改 triggerAiSuggestions() 传入 detectAppCategory() 结果作为 appCategory 参数。

- [ ] **Step 2: 提交**

```bash
git add android/inputmethod/src/main/java/com/quickspeech/input/QuickSpeechInputMethodService.kt
git commit -m "feat(P5): 上下文感知 AI 回复（邮件/IM/文档场景检测）"
```

---

## 验收检查清单

### P1 验收
- [ ] 600+ 常用字可通过编码打出
- [ ] 2000+ 二字词可通过编码打出
- [ ] 相邻键位误触（如 a→s）能返回正确候选词
- [ ] 用户选词后，下次该词排名上升
- [ ] 候选词排序响应时间 < 50ms

### P2 验收
- [ ] 选中候选词后自动显示联想词
- [ ] 联想词可继续选择上屏
- [ ] 点击 ▸ 展开整词联想

### P3 验收
- [ ] 按键同时显示字母和字根
- [ ] 深色主题在系统深色模式下自动生效
- [ ] 键盘布局在不同屏幕尺寸下正常显示

### P4 验收
- [ ] AI 面板展开/收起有动画
- [ ] 长按回复卡片弹出操作菜单

### P5 验收
- [ ] 邮件/IM/文档场景下传递给 AI 的 category 参数不同
