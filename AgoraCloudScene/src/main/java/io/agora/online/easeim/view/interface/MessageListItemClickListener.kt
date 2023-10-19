package io.agora.online.easeim.view.`interface`

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
     * 单击 item
     */
    fun onItemClick(v: View, message: ChatMessage)

    fun onPrivateChatViewDisplayed(message: ChatMessage)
}