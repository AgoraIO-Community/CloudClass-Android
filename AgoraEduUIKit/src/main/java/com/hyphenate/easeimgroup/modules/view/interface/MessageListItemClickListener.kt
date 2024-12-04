package com.hyphenate.easeimgroup.modules.view.`interface`

import android.view.View
import io.agora.chat.ChatMessage

interface MessageListItemClickListener {

    /**
     * 重发点击
     */
    fun onResendClick(message: ChatMessage): Boolean

    /**
     * 消息失败
     */
    fun onMessageError(message: ChatMessage, code: Int, error: String?)

    /**
     * 单击item
     */
    fun onItemClick(v: View, message: ChatMessage)
}