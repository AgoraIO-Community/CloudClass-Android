package io.agora.educontext.eventHandler

import io.agora.educontext.EduContextChatItem
import io.agora.educontext.EduContextUserInfo

interface IChatHandler {
    fun onReceiveMessage(item: EduContextChatItem)

    fun onReceiveChatHistory(history: List<EduContextChatItem>)

    fun onReceiveConversationMessage(item: EduContextChatItem)

    fun onReceiveConversationHistory(history: List<EduContextChatItem>)

    fun onChatAllowed(allowed: Boolean)

    /**
     * @param allowed
     * @param userInfo related user info
     * @param operator who caused this change
     * @param local whether the related user is local user
     */
    fun onChatAllowed(allowed: Boolean, userInfo: EduContextUserInfo, operator: EduContextUserInfo?, local: Boolean)

    fun onChatTips(tip: String)
}

