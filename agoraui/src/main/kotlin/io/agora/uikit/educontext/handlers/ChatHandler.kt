package io.agora.uikit.educontext.handlers

import io.agora.educontext.EduContextChatItem
import io.agora.educontext.eventHandler.IChatHandler

open class ChatHandler : IChatHandler {
    override fun onReceiveMessage(item: EduContextChatItem) {

    }

    override fun onReceiveChatHistory(history: List<EduContextChatItem>) {

    }

    override fun onChatAllowed(allowed: Boolean) {

    }

    override fun onChatTips(tip: String) {

    }
}