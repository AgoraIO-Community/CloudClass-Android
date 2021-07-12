package io.agora.education.api.manager.listener

import io.agora.education.api.message.EduActionMessage
import io.agora.education.api.message.EduChatMsg
import io.agora.education.api.message.EduMsg
import io.agora.education.api.message.EduPeerChatMsg

interface EduManagerEventListener {

    fun onUserMessageReceived(message: EduMsg)

    fun onUserChatMessageReceived(chatMsg: EduPeerChatMsg)

    fun onUserActionMessageReceived(actionMessage: EduActionMessage)
}