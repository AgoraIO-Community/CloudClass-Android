package io.agora.agoraeducore.core.internal.edu.common.api

import io.agora.agoraeducore.core.internal.server.struct.request.RoomFlexPropsReq
import io.agora.agoraeducore.core.internal.server.struct.request.UserFlexPropsReq
import io.agora.agoraeducore.core.internal.framework.data.EduCallback

interface FlexProps {
    fun updateFlexRoomProperties(reqFlex: RoomFlexPropsReq, callback: EduCallback<Boolean>)

    fun updateFlexUserProperties(userUuid: String, reqFlex: UserFlexPropsReq, callback: EduCallback<Boolean>)
}