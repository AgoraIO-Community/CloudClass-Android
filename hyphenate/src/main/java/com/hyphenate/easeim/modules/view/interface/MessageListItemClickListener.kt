package com.hyphenate.easeim.modules.view.`interface`

import android.view.View
import com.hyphenate.chat.EMMessage

interface MessageListItemClickListener {

    /**
     * 重发点击
     */
    fun onResendClick(message: EMMessage): Boolean

    /**
     * 消息失败
     */
    fun onMessageError(message: EMMessage, code: Int, error: String?)

    /**
     * 单击item
     */
    fun onItemClick(v: View, message: EMMessage)
}