package com.hyphenate.easeim.modules.view.`interface`

import android.text.Editable

interface InputMsgListener {
    /**
     * 发送点击
     */
    fun onSendMsg(content: String)

    /**
     * 点击输入框以外区域
     */
    fun onOutsideClick()

    /**
     * 输入内容变化
     */
    fun onContentChange(content: String)

    /**
     * 选择图片
     */
    fun onSelectImage()

}