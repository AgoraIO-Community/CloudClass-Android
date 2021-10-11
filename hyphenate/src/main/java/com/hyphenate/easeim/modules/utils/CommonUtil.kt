package com.hyphenate.easeim.modules.utils

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

const val TAG = "CommonUtil"

object CommonUtil {
    /**
     * 隐藏软键盘
     */
    fun hideSoftKeyboard(et: EditText) {
        val inputManager: InputMethodManager =
                et.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(
                et.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
        )


}

/**
 * 显示软键盘
 */
fun showSoftKeyboard(et: EditText) {
    et.requestFocus()
    val inputManager: InputMethodManager =
            et.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputManager.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
}
}