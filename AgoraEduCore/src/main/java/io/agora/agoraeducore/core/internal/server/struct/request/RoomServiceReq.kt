package io.agora.agoraeducore.core.internal.server.struct.request

import io.agora.agoraeducore.core.internal.launch.StreamState

class RoomPreCheckReq(
        var roomName: String,
        var roomType: Int,
        var role: String,
        var startTime: Long?,
        var duration: Long?,
        var userName: String,
        var stream: StreamState?,
        var userProperties: Map<String, String>?)

/**
 * @param streamUuid default stream id; if not set, use userUuid instead
 * @param publishType if the user wants to publish a stream
 */
class EduJoinClassroomReq(
        val userName: String,
        val role: String,
        val streamUuid: String,
        val publishType: Int)

class EduUpsertRoomPropertyReq(
        val properties: MutableMap<String, Any>,
        val cause: MutableMap<String, String>)

class EduRemoveRoomPropertyReq(
        val properties: MutableList<String>,
        val cause: MutableMap<String, String>)

class EduRoomMuteStateReq(
        private val muteChat: RoleMuteConfig?,
        private val muteVideo: RoleMuteConfig?,
        private val muteAudio: RoleMuteConfig?)

class RoleMuteConfig(
        val host: String?,
        val broadcaster: String?,
        val audience: String?)