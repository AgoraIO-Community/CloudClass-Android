package io.agora.edu.uikit.impl.chat.tabs

import io.agora.edu.uikit.impl.chat.AgoraUIChatItem

interface IChatTab {
    fun isActive(active: Boolean)

    fun addMessage(item: AgoraUIChatItem)

    fun addMessageList(list: List<AgoraUIChatItem>, front: Boolean)

    fun onSendLocalChat(item: AgoraUIChatItem)

    fun onSendResult(userId: String, messageId: String, timestamp: Long, success: Boolean)

    /**
     * Receives the text chat permission change from teacher.
     * @param group whether all the students are allowed to chat or not, null means not involved
     * @param local whether only local user is allowed to chat or not, null means not involved
     */
    fun allowChat(group: Boolean?, local: Boolean?)

    fun getType(): TabType
}