package com.quickspeech.input.ui

import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import com.quickspeech.input.R

/**
 * 五笔字根按键绑定工具
 * 将字根数据显示在字母按键上（左上角小字 + 居中大字）
 */
object WubiKeyBinder {

    // 每个字母键对应的主字根（键名字）- 五笔86版
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

    // 每个字母键对应的全部字根（用于 tooltip）
    val keyAllRoots: Map<Int, String> = mapOf(
        R.id.key_q to "金儿勹夕",
        R.id.key_w to "人八登",
        R.id.key_e to "月乃用豕",
        R.id.key_r to "白手斤",
        R.id.key_t to "禾竹彳",
        R.id.key_y to "言讠文方广",
        R.id.key_u to "立辛冫丬",
        R.id.key_i to "水氵小",
        R.id.key_o to "火米业",
        R.id.key_p to "之宀辶廴",
        R.id.key_a to "工戈匚艹廾",
        R.id.key_s to "木西丁",
        R.id.key_d to "大犬三古石厂",
        R.id.key_f to "土士二干十寸雨",
        R.id.key_g to "一王戋五",
        R.id.key_h to "目丨卜上止",
        R.id.key_j to "日曰早虫",
        R.id.key_k to "口川",
        R.id.key_l to "田甲口四皿车",
        R.id.key_z to "纟幺",
        R.id.key_x to "幺弓匕",
        R.id.key_c to "又巴马厶",
        R.id.key_v to "女刀九臼",
        R.id.key_b to "子耳阝了也凵",
        R.id.key_n to "已己巳乙尸忄羽",
        R.id.key_m to "山由贝门"
    )

    /**
     * 为按键设置字根显示
     * 使用 SpannableString 在字母上方显示字根
     */
    fun bindKey(textView: TextView) {
        val radical = keyRoots[textView.id] ?: return
        val letter = textView.text.toString()
        if (letter.isBlank()) return

        val displayText = "$radical\n$letter"
        val spannable = SpannableString(displayText)

        // 字根样式：小字、灰色
        spannable.setSpan(
            AbsoluteSizeSpan(8, true),
            0, radical.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#AAAAAA")),
            0, radical.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // 字母样式：大字、深色
        spannable.setSpan(
            AbsoluteSizeSpan(15, true),
            radical.length + 1, displayText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#333333")),
            radical.length + 1, displayText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        textView.text = spannable
        textView.gravity = android.view.Gravity.CENTER
        textView.setTypeface(null, Typeface.BOLD)
    }

    /**
     * 为所有字母键批量绑定字根
     */
    fun bindAllKeys(view: android.view.View) {
        for (keyId in keyRoots.keys) {
            view.findViewById<TextView>(keyId)?.let { bindKey(it) }
        }
    }
}
