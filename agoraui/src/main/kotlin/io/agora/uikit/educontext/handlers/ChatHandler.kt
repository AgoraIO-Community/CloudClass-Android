package io.agora.uikit.educontext.handlers

import io.agora.educontext.EduContextChatItem
import io.agora.educontext.EduContextUserInfo
import io.agora.educontext.eventHandler.IChatHandler

open class ChatHandler : IChatHandler {
    override fun onReceiveMessage(item: EduContextChatItem) {

    }

    override fun onReceiveConversationMessage(item: EduContextChatItem) {

    }

    override fun onReceiveConversationHistory(history: List<EduContextChatItem>) {

    }

    override fun onReceiveChatHistory(history: List<EduContextChatItem>) {

    }

    override fun onChatAllowed(allowed: Boolean) {

    }

    override fun onChatAllowed(allowed: Boolean, userInfo: EduContextUserInfo, operator: EduContextUserInfo?, local: Boolean) {

    }

    override fun onChatTips(tip: String) {

    }
}