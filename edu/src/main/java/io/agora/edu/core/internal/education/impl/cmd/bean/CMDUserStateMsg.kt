package io.agora.edu.core.internal.education.impl.cmd.bean

import io.agora.edu.core.internal.education.impl.room.data.response.EduUserRes

class CMDUserStateMsg(
        val userUuid: String,
        val userName: String,
        val role: String,
        val muteChat: Int,
        val updateTime: Long,
        val operator: EduUserRes
) {

}