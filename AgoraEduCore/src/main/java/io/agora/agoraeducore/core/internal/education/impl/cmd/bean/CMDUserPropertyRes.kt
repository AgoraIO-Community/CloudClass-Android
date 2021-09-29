package io.agora.agoraeducore.core.internal.education.impl.cmd.bean

import io.agora.agoraeducore.core.internal.framework.EduBaseUserInfo

class CMDUserPropertyRes(
        val fromUser: EduBaseUserInfo,
        action: Int,
        changeProperties: MutableMap<String, Any>,
        cause: MutableMap<String, Any>?,
        operator: OperatorUserInfo?
) : CMDRoomPropertyRes(action, changeProperties, cause, operator) {
}