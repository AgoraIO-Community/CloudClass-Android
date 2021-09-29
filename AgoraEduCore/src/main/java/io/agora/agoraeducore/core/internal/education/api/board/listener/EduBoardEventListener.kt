package io.agora.agoraeducore.core.internal.education.api.board.listener

import io.agora.agoraeducore.core.internal.framework.EduUserInfo

interface EduBoardEventListener {
    fun onFollowMode(enable: Boolean)

    fun onPermissionGranted(student: EduUserInfo)

    fun onPermissionRevoked(student: EduUserInfo)
}
