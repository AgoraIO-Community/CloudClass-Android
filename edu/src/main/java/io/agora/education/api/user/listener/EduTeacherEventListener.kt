package io.agora.education.api.user.listener

import io.agora.education.api.base.EduError
import io.agora.education.api.room.data.EduRoomState
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.user.data.EduUserInfo

interface EduTeacherEventListener : EduUserEventListener {

    fun onCourseStateUpdated(roomState: EduRoomState, error: EduError)

    fun onAllStudentChaAllowed(enable: Boolean, error: EduError)

    // TODO
    fun onStudentChatAllowed(studentInfo: EduUserInfo, enable: Boolean, error: EduError)

    fun onCreateOrUpdateStudentStreamCompleted(streamInfo: EduStreamInfo, error: EduError)
}