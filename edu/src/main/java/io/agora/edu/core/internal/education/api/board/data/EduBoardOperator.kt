package io.agora.edu.core.internal.education.api.board.data

import io.agora.edu.core.internal.framework.EduUserInfo

data class EduBoardOperator(
        val isPublisher: Boolean,
        val user: EduUserInfo
)
