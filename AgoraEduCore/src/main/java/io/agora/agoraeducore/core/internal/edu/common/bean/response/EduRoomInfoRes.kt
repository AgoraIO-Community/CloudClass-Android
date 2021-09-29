package io.agora.agoraeducore.core.internal.edu.common.bean.response

import io.agora.agoraeducore.core.internal.education.api.room.data.EduRoomInfo

internal class EduRoomInfoRes(
        roomUuid: String,
        roomName: String,
        val createTime: Long
): EduRoomInfo(roomUuid, roomName) {
}