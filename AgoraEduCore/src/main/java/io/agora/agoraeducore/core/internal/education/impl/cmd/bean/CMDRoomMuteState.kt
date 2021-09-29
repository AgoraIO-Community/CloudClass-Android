package io.agora.agoraeducore.core.internal.education.impl.cmd.bean

import io.agora.agoraeducore.core.internal.education.impl.room.data.response.EduUserRes
import io.agora.agoraeducore.core.internal.server.struct.request.RoleMuteConfig

class CMDRoomMuteState(val muteChat: RoleMuteConfig?, val muteVideo: RoleMuteConfig?,
                       val muteAudio: RoleMuteConfig?, val operator: EduUserRes) {
}