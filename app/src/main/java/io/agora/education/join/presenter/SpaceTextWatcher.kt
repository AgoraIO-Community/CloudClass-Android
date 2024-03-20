package io.agora.education.join.presenter

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

/**
 * author : felix
 * date : 2023/8/11
 * description :
 */
class SpaceTextWatcher(private val editText: EditText, var count: Int = 3) : TextWatcher {
    private var isFormatting: Boolean = false // 标记是否正在进行格式化操作

    override fun afterTextChanged(s: Editable?) {
        if (isFormatting) {
            return
        }

        isFormatting = true

        // 清除所有空格并重新格式化
        val trimmedText = s?.toString()?.replace("\\s".toRegex(), "") ?: ""
        var formattedText = formatNumber(trimmedText)
        formattedText = formattedText.trim()

        // 重置 EditText 的文本和光标位置
        editText.setText(formattedText)
        editText.setSelection(formattedText.length)

        isFormatting = false
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    private fun formatNumber(number: String): String {
        val numberLength = number.length
        val formattedNumber = StringBuilder()

        for (i in 0 until numberLength) {
            // 每3个数字添加一个空格
            if (i > 0 && i % count == 0) {
                formattedNumber.append(" ")
            }

            formattedNumber.append(number[i])
        }

        return formattedNumber.toString()
    }
}