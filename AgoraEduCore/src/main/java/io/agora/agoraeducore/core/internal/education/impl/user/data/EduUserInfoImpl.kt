package io.agora.agoraeducore.core.internal.education.impl.user.data

import io.agora.agoraeducore.core.internal.framework.EduUserInfo
import io.agora.agoraeducore.core.internal.framework.EduUserRole

internal class EduUserInfoImpl(
        userUuid: String,
        userName: String,
        role: EduUserRole,
        isChatAllowed: Boolean,
        val updateTime: Long?)
: EduUserInfo(userUuid, userName, role, isChatAllowed)