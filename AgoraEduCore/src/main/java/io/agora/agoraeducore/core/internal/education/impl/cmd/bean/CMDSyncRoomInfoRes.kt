package io.agora.agoraeducore.core.internal.education.impl.cmd.bean

import io.agora.agoraeducore.core.internal.education.api.room.data.EduRoomInfo
import io.agora.agoraeducore.core.internal.server.struct.response.EduEntryRoomStateRes

class CMDSyncRoomInfoRes(
        val roomInfo: EduRoomInfo,
        val roomState: EduEntryRoomStateRes,
        val roomProperties: Map<String, Any>)