package io.agora.edu.core.internal.education.impl.user.data

import io.agora.edu.core.internal.framework.data.EduStreamEvent
import io.agora.edu.core.internal.framework.EduLocalUserInfo
import io.agora.edu.core.internal.framework.EduUserRole

internal class EduLocalUserInfoImpl(
        userUuid: String,
        userName: String,
        role: EduUserRole,
        isChatAllowed: Boolean,
        userToken: String,
        streams: MutableList<EduStreamEvent>,
        val updateTime: Long?)
    : EduLocalUserInfo(userUuid, userName, role, isChatAllowed, userToken, streams) {
}