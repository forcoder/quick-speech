package com.quickspeech.app

import android.app.Activity
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class ImeTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ctx = this
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        layout.addView(TextView(ctx).apply {
            text = "IME 测试页面"
            textSize = 24f
            setPadding(0, 0, 0, 32)
        })

        layout.addView(TextView(ctx).apply {
            text = "点击输入框测试 QuickSpeech 输入法："
            textSize = 14f
            setPadding(0, 0, 0, 16)
        })

        layout.addView(EditText(ctx).apply {
            hint = "在此输入..."
            textSize = 18f
            minHeight = 120
            setPadding(24, 24, 24, 24)
        })

        layout.addView(EditText(ctx).apply {
            hint = "多行输入测试..."
            textSize = 16f
            minHeight = 200
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setPadding(24, 24, 24, 24)
        })

        val scrollView = ScrollView(ctx)
        scrollView.addView(layout)
        setContentView(scrollView)
    }
}
