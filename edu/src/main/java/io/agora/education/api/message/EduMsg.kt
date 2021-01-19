package io.agora.education.api.message

import io.agora.education.api.user.data.EduUserRole

open class EduMsg(
        val fromUser: EduFromUserInfo,
        var message: String,
        val timestamp: Long
)

open class EduFromUserInfo(
        var userUuid: String?,
        var userName: String?,
        var role: EduUserRole?
)
