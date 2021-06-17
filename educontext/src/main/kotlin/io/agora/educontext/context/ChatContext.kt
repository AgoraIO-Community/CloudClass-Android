package io.agora.educontext.context

import io.agora.educontext.AbsHandlerPool
import io.agora.educontext.EduContextChatItem
import io.agora.educontext.EduContextChatItemSendResult
import io.agora.educontext.EduContextCallback
import io.agora.educontext.eventHandler.IChatHandler

abstract class ChatContext : AbsHandlerPool<IChatHandler>() {
    /**
     * @param message the string text message
     * @param timestamp
     * @param callback result of sending this message, the server id and
     * timestamp of this message will be returned if success
     */
    abstract fun sendLocalChannelMessage(message: String, timestamp: Long,
                                         callback: EduContextCallback<EduContextChatItemSendResult>) : EduContextChatItem

    /**
     * @param startId the start message id (exclusive) to search the
     * message history from reversely
     */
    abstract fun fetchChannelHistory(startId: String?, count: Int? = 50, callback: EduContextCallback<List<EduContextChatItem>>)

    /**
     * Conversations are currently taken as Q&A sessions,
     * and the messages are sent to the so-call "groups" that
     * are named with the local users' ids internally by aPaaS server.
     * The messages are sent to the group that both the teacher and TA
     * can see and reply
     */
    abstract fun sendConversationMessage(message: String, timestamp: Long,
                                         callback: EduContextCallback<EduContextChatItemSendResult>) : EduContextChatItem

    abstract fun fetchConversationHistory(startId: String?, callback: EduContextCallback<List<EduContextChatItem>>)
}