package com.quickspeech.wubi.engine

/**
 * 五笔输入解码器
 * 将按键序列解析为五笔编码
 */
class WubiInputDecoder {

    /** 当前输入的编码序列 */
    private val codeBuffer = StringBuilder()

    /** 当前输入模式 */
    var inputMode: InputMode = InputMode.CHINESE
        private set

    /** 中文/英文模式切换 */
    fun toggleMode(): InputMode {
        inputMode = if (inputMode == InputMode.CHINESE) InputMode.ENGLISH else InputMode.CHINESE
        if (inputMode == InputMode.ENGLISH) {
            codeBuffer.clear()
        }
        return inputMode
    }

    fun setMode(mode: InputMode) {
        inputMode = mode
        if (mode == InputMode.ENGLISH) {
            codeBuffer.clear()
        }
    }

    /**
     * 处理按键输入
     * @return 解码结果
     */
    fun processKey(key: Char): InputResult {
        if (inputMode == InputMode.ENGLISH) {
            return InputResult.DirectText(key.toString())
        }

        val lowerKey = key.lowercaseChar()

        // 只接受 a-z 字母键作为五笔编码
        if (lowerKey in 'a'..'z') {
            if (codeBuffer.length < MAX_CODE_LENGTH) {
                codeBuffer.append(lowerKey)
            }
            return InputResult.Composing(codeBuffer.toString())
        }

        // 空格键：确认输入
        if (key == ' ') {
            val code = codeBuffer.toString()
            return if (code.isEmpty()) {
                InputResult.DirectText(" ")
            } else {
                InputResult.Confirmed(code)
            }
        }

        // 退格键
        if (key == '\b' || key.code == 67) { // KEYCODE_DEL
            return if (codeBuffer.isNotEmpty()) {
                codeBuffer.deleteCharAt(codeBuffer.length - 1)
                if (codeBuffer.isEmpty()) {
                    InputResult.Cleared
                } else {
                    InputResult.Composing(codeBuffer.toString())
                }
            } else {
                InputResult.Backspace
            }
        }

        // ESC 清除
        if (key.code == 27) {
            codeBuffer.clear()
            return InputResult.Cleared
        }

        // 数字键：选择候选词
        if (key in '1'..'9') {
            return InputResult.SelectCandidate(key - '1')
        }

        // 标点符号直接输出
        if (isPunctuation(key)) {
            return InputResult.DirectText(key.toString())
        }

        return InputResult.Ignored
    }

    /** 获取当前编码 */
    fun getCurrentCode(): String = codeBuffer.toString()

    /** 清除缓冲区 */
    fun clear() {
        codeBuffer.clear()
    }

    /** 重置状态 */
    fun reset() {
        codeBuffer.clear()
        inputMode = InputMode.CHINESE
    }

    private fun isPunctuation(key: Char): Boolean {
        return key in "，。！？、；：""''（）【】《》"
    }

    companion object {
        const val MAX_CODE_LENGTH = 4
    }
}

/** 输入模式 */
enum class InputMode {
    CHINESE,    // 中文五笔模式
    ENGLISH     // 英文直接输出模式
}

/** 输入处理结果 */
sealed class InputResult {
    /** 直接输出的文本（英文模式/标点） */
    data class DirectText(val text: String) : InputResult()

    /** 正在组字中（显示编码） */
    data class Composing(val code: String) : InputResult()

    /** 确认输入（空格触发，需要查词库） */
    data class Confirmed(val code: String) : InputResult()

    /** 选择候选词（数字键） */
    data class SelectCandidate(val index: Int) : InputResult()

    /** 退格（编码区为空时） */
    object Backspace : InputResult()

    /** 已清除编码区 */
    object Cleared : InputResult()

    /** 忽略的按键 */
    object Ignored : InputResult()
}
