package io.agora.agoraeducore.core.internal.server.requests.http.retrofit.dispatch

import io.agora.agoraeducore.core.internal.server.requests.Request
import io.agora.agoraeducore.core.internal.server.requests.RequestCallback
import io.agora.agoraeducore.core.internal.server.requests.RequestConfig
import io.agora.agoraeducore.core.internal.server.requests.http.retrofit.services.MessageService
import io.agora.agoraeducore.core.internal.server.struct.request.*

class MessageServiceDispatcher(private val messageService: MessageService) : AbsServiceDispatcher()  {
    override fun dispatch(config: RequestConfig, callback: RequestCallback<Any>?, vararg args: Any) {
        if (!Request.isValidArguments(config, args)) {
            return
        }

        when (config.request) {
            Request.SendRoomMessage -> {
                messageService.sendRoomMessage(args[0] as String, args[1] as String,
                        args[2] as String, args[3] as EduRoomChatMsgReq).enqueue(ServiceRespCallback(callback))
            }
            Request.GetRoomMessage -> {
                messageService.retrieveRoomMessages(args[0] as String, args[1] as String,
                        args[2] as Int, args[3] as String, args[4] as Int).enqueue(ServiceRespCallbackWithDataBody(callback))
            }
            Request.SendRoomCustomMessage -> {
                messageService.sendRoomCustomMessage(args[0] as String, args[1] as String, args[2] as EduRoomMsgReq)
                        .enqueue(ServiceRespCallbackWithDataBody(callback))
            }
            Request.SendPeerMessage -> {
                messageService.sendPeerMessage(args[0] as String, args[1] as String, args[2] as String, args[3] as EduUserChatMsgReq)
                        .enqueue(ServiceRespCallbackWithDataBody(callback))
            }
            Request.SendPeerCustomMessage -> {
                messageService.sendPeerCustomMessage(args[0] as String, args[1] as String, args[2] as String, args[3] as EduUserMsgReq)
                        .enqueue(ServiceRespCallbackWithDataBody(callback))
            }
            Request.SendConversationMessage -> {
                messageService.sendConversationMessage(args[0] as String, args[1] as String, args[2] as String,
                        args[3] as EduRoomChatMsgReq).enqueue(ServiceRespCallback(callback))
            }
            Request.GetConversationMessage -> {
                messageService.retrieveConversationMessages(args[0] as String, args[1] as String, args[2] as String,
                    args[3] as String, args[4] as Int).enqueue(ServiceRespCallbackWithDataBody(callback))
            }
            Request.Translate -> {

            }
            Request.SetUserChatMuteState -> {
                messageService.setUserChatMuteState(args[0] as String, args[1] as String, args[2] as String,
                    args[3] as EduUserRoomChatMuteReq).enqueue(ServiceRespCallbackWithDataBody(callback))
            }
            else -> {}
        }
    }
}