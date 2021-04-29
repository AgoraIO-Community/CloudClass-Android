package io.agora.educontext.eventHandler

import io.agora.educontext.EduContextChatItem

interface IChatHandler {
    fun onReceiveMessage(item: EduContextChatItem)

    fun onReceiveChatHistory(history: List<EduContextChatItem>)

    fun onChatAllowed(allowed: Boolean)

    fun onChatTips(tip: String)
}

