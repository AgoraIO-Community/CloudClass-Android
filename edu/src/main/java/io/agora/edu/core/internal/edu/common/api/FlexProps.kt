package io.agora.edu.core.internal.edu.common.api

import io.agora.edu.core.internal.server.struct.request.RoomFlexPropsReq
import io.agora.edu.core.internal.server.struct.request.UserFlexPropsReq
import io.agora.edu.core.internal.framework.data.EduCallback

interface FlexProps {
    fun updateFlexRoomProperties(reqFlex: RoomFlexPropsReq, callback: EduCallback<Boolean>)

    fun updateFlexUserProperties(userUuid: String, reqFlex: UserFlexPropsReq, callback: EduCallback<Boolean>)
}