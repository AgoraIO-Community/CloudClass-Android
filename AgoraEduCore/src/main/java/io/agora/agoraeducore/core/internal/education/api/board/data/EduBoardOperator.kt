package io.agora.agoraeducore.core.internal.education.api.board.data

import io.agora.agoraeducore.core.internal.framework.EduUserInfo

data class EduBoardOperator(
        val isPublisher: Boolean,
        val user: EduUserInfo
)
