# QuickSpeech 输入法全面优化设计文档

**日期**: 2026-05-09
**版本**: v1.0
**定位**: 五笔输入体验增强 + UI 优化 + AI 功能打磨

---

## 1. 现状分析

### 1.1 已有基础
- 完整的 QWERTY 键盘布局（XML 实现）
- 五笔引擎 `WubiEngine`（Kotlin 内置词典 + JNI native 框架）
- Room 数据库 `WubiDatabase`（词库表、用户词频表、最近使用表）
- 候选词显示/选择/翻页
- AI 面板（可折叠）、知识库搜索面板
- 中英文切换、符号键盘、Shift/CapsLock、语音输入
- Hilt DI 框架

### 1.2 当前不足
| 模块 | 问题 |
|------|------|
| 词库 | 内置词典仅覆盖一级/二级简码 + 少量二字词，大量常用字缺失 |
| 纠错 | 无模糊匹配，打错编码只能删除重来 |
| 词频 | `UserFrequencyEntry` 表已建但未与候选词排序联动 |
| 联想 | `associateWords` DAO 方法已定义但未在 ViewModel 中调用 |
| UI | 按键仅显示字母，未显示五笔字根；布局未适配不同屏幕 |
| AI | AI 面板交互生硬（无动画），回复卡片功能单一 |

---

## 2. 优化目标（分阶段）

| 阶段 | 内容 | 核心目标 |
|------|------|----------|
| **P1** | 五笔体验增强 | 词库扩充 + 智能纠错 + 词频排序 |
| **P2** | 联想输入 | 单字联想 + 整词联想 |
| **P3** | UI/布局优化 | 布局调整 + 字根显示 + 主题支持 |
| **P4** | AI 功能打磨 | AI 面板交互优化 + 上下文感知 |
| **P5** | 高级 AI | 风格学习 + AI 预测输入 |

---

## 3. P1：五笔体验增强

### 3.1 词库扩充（混合方案）

**本地词库（优先）**
- 扩充 `WubiDatabase` 中的 `wubi_words` 表数据，覆盖：
  - GB2312 全部 6763 个常用单字（编码 + 频率值）
  - 5000+ 常用二字词
  - 1000+ 常用三字词及多字词
- 数据来源：从开源五笔词库（如 ibus-wubi / Rime 词表）转换导入
- 在 `WubiEngine` 初始化时，若 `wubi_words` 表为空，从 assets 预置 SQL 文件批量导入
- 预置数据打包在 `inputmethod/src/main/assets/wubi_dict.sql`

**云端同步（后续）**
- 后端 API：`GET /api/wubi/dict/updates?since={timestamp}` 返回增量更新
- 客户端启动时检查更新，增量写入本地数据库
- 用户可手动触发「同步词库」

### 3.2 智能纠错

**实现方案**：在 `WubiEngine.search()` 中增加模糊匹配层

```kotlin
// 纠错策略（按优先级）
fun searchWithCorrection(code: String): List<ScoredCandidate> {
    // 1. 精确匹配
    val exact = dictionary[code]?.map { ScoredCandidate(it, 1.0f) } ?: emptyList()
    if (exact.isNotEmpty()) return exact

    // 2. 相邻键位纠错（单次替换）
    val adjacentMap = mapOf(
        'a' to "qs", 's' to "awedx", 'd' to "serfcx", ...
    )
    val corrections = generateCorrections(code, adjacentMap)
        .mapNotNull { dictionary[it] }
        .flatten()
        .map { ScoredCandidate(it, 0.7f) }

    // 3. 漏码纠错（逐位删除）
    val deletions = code.indices.map { i ->
        code.removeRange(i, i + 1)
    }.mapNotNull { dictionary[it] }
     .flatten()
     .map { ScoredCandidate(it, 0.5f) }

    return (exact + corrections + dedup(deletions))
        .sortedByDescending { it.score }
}
```

**相邻键位映射表**（QWERTY 布局物理相邻）：

```
q → wa    w → qeas   e → wrsd   r → etdf   t → ryfg
y → tugh  u → yijh   i → ujko   o → iklp   p → ol
a → qwsz  s → awedx  d → serfcx f → drtgvc g → ftyhbv
h → gyujnb j → huiknm k → jiolm  l → kop
z → asx   x → zsdc   c → xdfv   v → cfgb   b → vghn
n → bhjm  m → njk
```

### 3.3 词频排序

当前 `WubiDao.exactMatch()` 已按 `frequency` 排序，但未结合用户个人词频。

**排序算法**：
```kotlin
data class ScoredCandidate(
    val word: String,
    val baseFrequency: Int,      // 基础词频（来自 wubi_words.frequency）
    val userCount: Int,          // 用户选择次数（来自 user_frequency.count）
    val recencyScore: Float,     // 时间衰减因子
    val correctionPenalty: Float // 纠错惩罚（精确匹配=1.0，纠错=0.7/0.5）
) {
    val finalScore: Float
        get() = (baseFrequency * 0.3f +
                 userCount * 10f +
                 recencyScore * 0.2f) * correctionPenalty
}
```

**时间衰减因子**：
```kotlin
fun calcRecency(lastUsed: Long): Float {
    val daysSinceUse = (System.currentTimeMillis() - lastUsed) / (1000 * 86400)
    return 1f / (1f + daysSinceUse * 0.1f)  // 半衰期约 7 天
}
```

**用户词频记录时机**：
- 用户通过候选词点击选词 → `incrementFrequency(word)`
- 用户按空格选第一个候选词 → `incrementFrequency(word)`
- 用户手动输入（非候选词选择）→ 不记录

---

## 4. P2：联想输入

### 4.1 单字联想

**场景**：用户输入编码并选中第一个字后，自动联想下一个可能的字。

**实现方案**：
1. `WubiEngine` 中新增 `nextCharAssociates: Map<String, List<String>>` 映射表
   - 数据来源：从大规模语料库统计的「字→下一字」频率表
   - 初始内置 Top 1000 常用汉字的联想数据
2. 用户在候选词栏选中一个字后，`InputMethodViewModel.onCandidateSelected()` 触发联想查询
3. 联想结果追加到候选词栏末尾（用分隔符 `|` 隔开），用户可继续选择

**示例**：
```
输入: w → 候选: [1.人 2.个 3.们 ...]
选中: 人 → 候选变为: [1.人] | [1.们 2.生 3.家 4.民 ...]
```

### 4.2 整词联想

**场景**：用户选中一个字/词后，自动联想后续常用搭配。

**实现方案**：
1. 利用已有的 `WubiDao.associateWords()` DAO 方法
2. 查询 `wubi_words` 表中包含当前字的词组（type > 0）
3. 联想词组显示在候选词栏的「展开」区域（点击 ▸ 按钮后展示）

**示例**：
```
选中: "工" → ▸ 展开: [工作 工人 工程 工厂 工业 工具 ...]
选中: "工作" → ▸ 展开: [工作人员 工作方法 工作效率 工作地点 ...]
```

---

## 5. P3：UI/布局优化

### 5.1 布局调整

**键盘尺寸**：
- 键盘总高度 = 屏幕宽度 × 0.55（当前为固定 dp，改为比例）
- 按键高度 = (键盘高度 - 候选词栏高度 - padding) / 5 行
- 按键 margin 从 1.5dp 改为 1dp，增加可用空间

**候选词栏**：
- 高度从 38dp 增加到 44dp
- 增加候选词字号（13sp → 14sp）
- 选中的候选词高亮显示（蓝色背景）

**功能键区**：
- Shift/符号/中英 键使用更大的视觉区分
- 空格键增加当前模式提示文字（五笔模式下显示"QuickSpeech"）

### 5.2 按键字根显示

**方案**：将当前纯字母按键改为「字母 + 字根」双行显示

**实现**：
1. 修改 `input_method_view.xml` 中的字母按键，使用 `key_wubi.xml` 作为 item template
2. `key_wubi.xml` 中已有的 `key_radical`（左上角小字）+ `key_letter`（居中大字）布局
3. 在 `QuickSpeechInputMethodService.setupWubiKeys()` 中绑定字根数据
4. 字根颜色使用 `#AAAAAA`（浅灰），不干扰字母识别

**字根数据**：每个字母键对应的五笔字根表（内置在 `WubiRadicals.kt` 中，已有基础数据）

```
Q(金) W(人) E(月) R(白) T(禾) Y(言) U(立) I(水) O(火) P(之)
A(工) S(木) D(大) F(土) G(一) H(目) J(日) K(口) L(田)
Z(纟) X(幺) C(又) V(女) B(子) N(已) M(山)
```

### 5.3 主题支持

**浅色主题**（当前默认，微调优化）：
- 背景：`#E8E8E8` → `#F0F0F0`
- 按键背景：`#FFFFFF` → `#FFFFFF`
- 按键文字：`#222222` → `#333333`
- 功能键背景：`#C8CACC` → `#D8DADC`

**深色主题**：
- 背景：`#1A1A1A`
- 按键背景：`#2D2D2D`
- 按键文字：`#E0E0E0`
- 功能键背景：`#3A3A3A`
- 候选词栏：`#252525`

**实现方式**：
- 新增 `ThemeManager.kt`，管理主题配置
- `styles.xml` 中新增 `WubiKey.Dark`、`WubiKey.Func.Dark` 等深色样式
- 主题切换入口：长按符号键弹出主题选择菜单
- 用户偏好存入 `SharedPreferences`

---

## 6. P4：AI 功能打磨

### 6.1 AI 面板交互优化

**动画效果**：
- 面板展开：从上方滑入（`TranslateAnimation`，200ms，`DecelerateInterpolator`）
- 面板收起：向上滑出（反向，150ms）
- 使用 `android:animateLayoutChanges="true"` 简化布局动画

**回复卡片增强**：
- 点击回复卡片 → 替换当前输入文本（先删除已输入内容，再插入回复）
- 长按回复卡片 → 弹出操作菜单（复制 / 编辑后插入 / 收藏）
- 卡片右上角显示来源标签（📚 知识库 / 🤖 AI智能体）

**面板工具栏**：
- 增加「刷新」按钮，重新获取 AI 建议
- 模式切换按钮增加 tooltip 文字说明（长按显示）

### 6.2 上下文感知

**应用类型检测**：
```kotlin
fun detectAppCategory(packageName: String): AppCategory {
    return when {
        packageName.contains("mail") || packageName.contains("outlook") -> AppCategory.EMAIL
        packageName.contains("whatsapp") || packageName.contains("telegram") -> AppCategory.IM
        packageName.contains("docs") || packageName.contains("word") -> AppCategory.DOCUMENT
        else -> AppCategory.OTHER
    }
}
```

**回复策略调整**：
- 邮件场景：正式语气、完整句式、包含敬语
- IM 场景：轻松语气、可省略标点、支持网络用语
- 文档场景：中性语气、专业术语、结构化表达

**实现**：`AiReplyRepository.fetchReplies()` 增加 `appCategory` 参数，传递给后端 API。

---

## 7. P5：高级 AI（后续迭代）

### 7.1 风格学习

- 隐式学习：记录用户采纳/拒绝/修改的回复
- 显式学习：用户配置风格偏好（正式/简洁/详细）
- 风格画像存储在 `AppDatabase.user_style_profile` 表中（已有表结构）

### 7.2 AI 预测输入

- 在候选词栏中混入 AI 预测的词/短语（用不同颜色标记）
- 不等用户打开 AI 面板，实时预测
- 需要优化延迟（本地缓存 + 预加载）

---

## 8. 数据模型变更

### 8.1 数据库变更

**`WubiDatabase` 版本升级（v1 → v2）**：
```sql
-- 已有表结构不变，新增索引
CREATE INDEX IF NOT EXISTS idx_code_prefix ON wubi_words(code);

-- 新增纠错缓存表（可选，用于记录常见纠错模式）
CREATE TABLE IF NOT EXISTS error_cache (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    wrong_code TEXT NOT NULL,
    correct_code TEXT NOT NULL,
    word TEXT NOT NULL,
    count INTEGER DEFAULT 1,
    last_used INTEGER NOT NULL
);
```

### 8.2 新增文件

| 文件 | 用途 |
|------|------|
| `inputmethod/src/main/assets/wubi_dict.sql` | 预置词库数据 |
| `inputmethod/src/main/java/.../theme/ThemeManager.kt` | 主题管理 |
| `inputmethod/src/main/java/.../theme/ThemePreferences.kt` | 主题偏好存储 |
| `wubi/src/main/java/.../engine/AssociateEngine.kt` | 联想引擎 |
| `wubi/src/main/java/.../engine/CorrectionEngine.kt` | 纠错引擎 |
| `wubi/src/main/java/.../data/WubiRadicals.kt` | 字根映射表（扩充） |

### 8.3 修改文件

| 文件 | 改动 |
|------|------|
| `WubiEngine.kt` | 集成纠错 + 联想 + 词频排序 |
| `WubiDao.kt` | 新增排序查询方法 |
| `InputMethodViewModel.kt` | 联想逻辑 + 词频记录 |
| `QuickSpeechInputMethodService.kt` | UI 绑定 + 主题切换 + AI 面板动画 |
| `input_method_view.xml` | 布局调整 + 字根按键 |
| `styles.xml` | 深色主题样式 |
| `key_wubi.xml` | 字根数据绑定 |

---

## 9. 技术风险与应对

| 风险 | 影响 | 应对 |
|------|------|------|
| 词库数据量大导致首次加载慢 | 首次启动卡顿 | 使用 `createFromAsset` 预置数据库，避免运行时导入 |
| 纠错算法增加响应延迟 | 输入卡顿 | 限制纠错候选数量（≤20），使用协程异步查询 |
| 深色主题适配工作量大 | 开发时间增加 | 先定义颜色资源引用，再逐步替换硬编码颜色值 |
| 数据库版本升级导致数据丢失 | 用户词频丢失 | 使用 `addMigrations` 而非 `fallbackToDestructiveMigration` |

---

## 10. 验收标准

### P1 验收
- [ ] GB2312 全部 6763 个字可通过编码打出
- [ ] 5000+ 二字词可通过编码打出
- [ ] 相邻键位误触（如 a→s）能返回正确候选词
- [ ] 用户选词后，下次该词排名上升
- [ ] 候选词排序响应时间 < 50ms

### P2 验收
- [ ] 选中候选词后自动显示联想字
- [ ] 联想字可继续选择上屏
- [ ] 点击 ▸ 展开整词联想

### P3 验收
- [ ] 按键同时显示字母和字根
- [ ] 深色/浅色主题可切换
- [ ] 键盘布局在不同屏幕尺寸下正常显示

### P4 验收
- [ ] AI 面板展开/收起有动画
- [ ] 长按回复卡片弹出操作菜单
- [ ] 邮件/IM/文档场景下回复风格有差异

---

*本文档为 v1.0 版本，后续根据开发进展持续更新。*
