package io.agora.edu.core.internal.education.impl.cmd.bean

import io.agora.edu.core.internal.education.api.room.data.EduRoomInfo
import io.agora.edu.core.internal.server.struct.response.EduEntryRoomStateRes

class CMDSyncRoomInfoRes(
        val roomInfo: EduRoomInfo,
        val roomState: EduEntryRoomStateRes,
        val roomProperties: Map<String, Any>)