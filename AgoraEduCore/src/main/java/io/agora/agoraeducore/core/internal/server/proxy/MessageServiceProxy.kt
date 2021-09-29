package io.agora.agoraeducore.core.internal.server.proxy

import io.agora.agoraeducore.core.internal.framework.data.EduChatMessageType
import io.agora.agoraeducore.core.internal.server.struct.request.EduRoomChatMsgReq
import io.agora.agoraeducore.core.internal.server.struct.request.EduRoomMsgReq
import io.agora.agoraeducore.core.internal.server.struct.request.EduUserChatMsgReq
import io.agora.agoraeducore.core.internal.server.struct.request.EduUserMsgReq
import io.agora.agoraeducore.core.internal.server.requests.AgoraRequestClient
import io.agora.agoraeducore.core.internal.server.requests.Request
import io.agora.agoraeducore.core.internal.server.requests.RequestCallback

/**
 * Service proxy is the direct interface between the app level users and the aPaaS services
 */
class MessageServiceProxy : IServiceProxy {
    fun sendRoomMessage(appId: String, roomUuid: String, fromUserId: String,
                        message: String, region: String, callback: RequestCallback<Any>) {
        AgoraRequestClient.send(
                request = Request.SendRoomMessage,
                region = region,
                callback = callback,
                args = *arrayOf(appId, roomUuid, fromUserId,
                        EduRoomChatMsgReq(message, EduChatMessageType.Text.value))
        )
    }

    fun retrieveRoomMessages(appId: String, roomUuid: String, nextId: String?,
                             count: Int, reverse: Boolean, region: String, callback: RequestCallback<Any>) {
        AgoraRequestClient.send(
                request = Request.GetRoomMessage,
                region = region,
                callback = callback,
                args = *arrayOf(appId, roomUuid, count, nextId, if (reverse) 0 else 1)
        )
    }

    fun sendRoomCustomMessage(appId: String, roomUuid: String,
                              message: String, region: String, callback: RequestCallback<Any>) {
        AgoraRequestClient.send(
                request = Request.SendRoomCustomMessage,
                region = region,
                callback = callback,
                args = *arrayOf(appId, roomUuid, EduRoomMsgReq(message))
        )
    }

    fun sendPeerMessage(appId: String, roomUuid: String, toUserId: String,
                        message: String, region: String, callback: RequestCallback<Any>) {
        AgoraRequestClient.send(
                request = Request.SendPeerMessage,
                region = region,
                callback = callback,
                args = *arrayOf(appId, roomUuid, toUserId,
                        EduUserChatMsgReq(message, EduChatMessageType.Text.value))
        )
    }

    fun sendPeerCustomMessage(appId: String, roomUuid: String, toUserId: String,
                              message: String, region: String, callback: RequestCallback<Any>) {
        AgoraRequestClient.send(
                request = Request.SendPeerCustomMessage,
                region = region,
                callback = callback,
                args = *arrayOf(appId, roomUuid, toUserId, EduUserMsgReq(message))
        )
    }

    fun sendConversationMessage(appId: String, roomUuid: String, userId: String,
                                message: String, region: String, callback: RequestCallback<Any>) {
        AgoraRequestClient.send(
                request = Request.SendConversationMessage,
                region = region,
                callback = callback,
                args = *arrayOf(appId, roomUuid, userId,
                        EduRoomChatMsgReq(message, EduChatMessageType.Text.value))
        )
    }

    fun retrieveConversationMessages(appId: String, roomUuid: String, userId: String,
                                     nextId: String?, reverse: Boolean, region: String,
                                     callback: RequestCallback<Any>) {
        AgoraRequestClient.send(
                request = Request.GetConversationMessage,
                region = region,
                callback = callback,
                args = *arrayOf(appId, roomUuid, userId, nextId, if (reverse) 0 else 1)
        )
    }
}