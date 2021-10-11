package io.agora.edu.uikit.handlers

import io.agora.edu.core.context.EduContextChatItem
import io.agora.edu.core.context.EduContextUserInfo
import io.agora.edu.core.context.IChatHandler

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