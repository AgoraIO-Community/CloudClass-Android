package io.agora.agoraeducore.core.internal.server.proxy

import io.agora.agoraeducore.core.internal.server.struct.request.EduJoinClassroomReq
import io.agora.agoraeducore.core.internal.server.struct.request.EduRoomMuteStateReq
import io.agora.agoraeducore.core.internal.server.requests.AgoraRequestClient
import io.agora.agoraeducore.core.internal.server.requests.Request
import io.agora.agoraeducore.core.internal.server.requests.RequestCallback

/**
 * Service proxy is the direct interface between the app level users and the aPaaS services
 */
class RoomServiceProxy : IServiceProxy {
    fun preCheckRoom(appId: String, roomId: String, userId: String,
                     region: String, callback: RequestCallback<Any>) {
        AgoraRequestClient.send(
                request = Request.RoomPreCheck,
                region = region,
                callback = callback,
                args = *arrayOf(appId, roomId, userId)
        )
    }

    fun remoteRoomConfig(appId: String, region: String, callback: RequestCallback<Any>) {
        AgoraRequestClient.send(
                request = Request.RoomConfig,
                region = region,
                callback = callback,
                args = *arrayOf(appId))
    }

    fun joinRoom(appId: String, roomId: String, userId: String,
                 userName: String, role: String, streamId: String,
                 publishType: Int, region: String, callback: RequestCallback<Any>) {
        AgoraRequestClient.send(
                request = Request.RoomJoin,
                region = region,
                callback = callback,
                args = *arrayOf(appId, roomId, userId,
                        EduJoinClassroomReq(userName, role, streamId, publishType))
        )
    }

    fun snapshot(appId: String, token: String, roomId: String,
                 region: String, callback: RequestCallback<Any>) {
        AgoraRequestClient.send(
                request = Request.RoomSnapshot,
                region = region,
                callback = callback,
                args = *arrayOf(appId, token, roomId))
    }

    fun sequence(appId: String, token: String, roomId: String, nextId: Int,
                 count: Int, region: String, callback: RequestCallback<Any>) {
        AgoraRequestClient.send(
                request = Request.RoomSequence,
                region = region,
                callback = callback,
                args = *arrayOf(appId, token, roomId, nextId, count))
    }

    fun setRoomProperties(appId: String, roomId: String,
                          region: String, properties: MutableMap<String, Any>,
                          cause: MutableMap<String, String>, callback: RequestCallback<Any>) {
        AgoraRequestClient.send(
                request = Request.RoomSetProperty,
                region = region,
                callback = callback,
                args = *arrayOf(appId, roomId, properties, cause))
    }

    fun removeRoomProperties(appId: String, roomId: String, region: String,
                             properties: MutableList<String>, cause: MutableMap<String, String>,
                             callback: RequestCallback<Any>) {
        AgoraRequestClient.send(
                request = Request.RoomRemoveProperty,
                region = region,
                callback = callback,
                args = *arrayOf(appId, roomId, properties, cause))
    }

    fun updateMuteState(appId: String, roomId: String, region: String,
                        state: EduRoomMuteStateReq, callback: RequestCallback<Any>) {
        AgoraRequestClient.send(
                request = Request.RoomSetRoleMuteState,
                region = region,
                callback = callback,
                args = *arrayOf(appId, roomId, state)
        )
    }

    fun setRoomState(appId: String, roomId: String, region: String,
                     state: Int, callback: RequestCallback<Any>) {
        AgoraRequestClient.send(
                request = Request.RoomSetClassState,
                region = region,
                callback = callback,
                args = *arrayOf(appId, roomId, state))
    }
}