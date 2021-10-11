package io.agora.edu.core.internal.server.requests.http.retrofit.dispatch

import io.agora.edu.core.internal.server.struct.request.RoomPreCheckReq
import io.agora.edu.core.internal.server.struct.request.EduJoinClassroomReq
import io.agora.edu.core.internal.server.struct.request.EduRemoveRoomPropertyReq
import io.agora.edu.core.internal.server.struct.request.EduUpsertRoomPropertyReq
import io.agora.edu.core.internal.server.struct.request.EduRoomMuteStateReq
import io.agora.edu.core.internal.server.requests.Request
import io.agora.edu.core.internal.server.requests.RequestCallback
import io.agora.edu.core.internal.server.requests.RequestConfig
import io.agora.edu.core.internal.server.requests.http.retrofit.services.RoomService

internal class RoomServiceDispatcher(private val roomService: RoomService) : AbsServiceDispatcher() {
    override fun dispatch(config: RequestConfig, callback: RequestCallback<Any>?, vararg args: Any) {
        if (!Request.isValidArguments(config, args)) {
            return
        }

        when (config.request) {
            Request.RoomConfig -> {
                roomService.pullRemoteConfig(args[0] as String).enqueue(ServiceRespCallbackWithDataBody(callback))
            }
            Request.RoomPreCheck -> {
                roomService.preCheckClassroom(args[0] as String, args[1] as String,
                        args[2] as String, args[3] as RoomPreCheckReq).enqueue(ServiceRespCallbackWithDataBody(callback))
            }
            Request.RoomJoin -> {
                roomService.joinRoom(args[0] as String, args[1] as String, args[2] as String,
                        args[3] as EduJoinClassroomReq).enqueue(ServiceRespCallbackWithDataBody(callback))
            }
            Request.RoomSnapshot -> {
                roomService.fetchSnapshot(args[0] as String, args[1] as String, args[2] as String)
                        .enqueue(ServiceRespCallbackWithDataBody(callback))
            }
            Request.RoomSequence -> {
                roomService.fetchLostSequences(args[0] as String, args[1] as String,
                        args[2] as String, args[3] as Int, args[4] as? Int)
                        .enqueue(ServiceRespCallbackWithDataBody(callback))
            }
            Request.RoomSetProperty -> {
                roomService.setRoomProperties(args[0] as String, args[1] as String,
                        args[2] as EduUpsertRoomPropertyReq).enqueue(ServiceRespCallbackWithDataBody(callback))
            }
            Request.RoomRemoveProperty -> {
                roomService.removeRoomProperties(args[0] as String, args[1] as String,
                        args[2] as EduRemoveRoomPropertyReq).enqueue(ServiceRespCallbackWithDataBody(callback))
            }
            Request.RoomSetRoleMuteState -> {
                roomService.updateRoomMuteStateForRole(args[0] as String, args[1] as String,
                        args[2] as EduRoomMuteStateReq).enqueue(ServiceRespCallbackWithDataBody(callback))
            }
            Request.RoomSetClassState  -> {
                roomService.updateClassState(args[0] as String, args[1] as String, args[2] as Int)
                        .enqueue(ServiceRespCallbackWithDataBody(callback))
            } else -> {

            }
        }
    }
}