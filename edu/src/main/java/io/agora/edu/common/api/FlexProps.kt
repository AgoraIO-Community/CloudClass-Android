package io.agora.edu.common.api

import io.agora.edu.common.bean.flexpropes.RoomFlexPropsReq
import io.agora.edu.common.bean.flexpropes.UserFlexPropsReq
import io.agora.education.api.EduCallback

interface FlexProps {
    fun updateFlexRoomProperties(reqFlex: RoomFlexPropsReq, callback: EduCallback<Boolean>)

    fun updateFlexUserProperties(userUuid: String, reqFlex: UserFlexPropsReq, callback: EduCallback<Boolean>)
}