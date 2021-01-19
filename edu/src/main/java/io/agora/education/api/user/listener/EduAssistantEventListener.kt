package io.agora.education.api.user.listener

import io.agora.education.api.base.EduError
import io.agora.education.api.stream.data.EduStreamInfo

interface EduAssistantEventListener : EduStudentEventListener {

    fun onCreateOrUpdateTeacherStreamCompleted(streamInfo: EduStreamInfo, error: EduError)

    fun onCreateOrUpdateStudentStreamCompleted(streamInfo: EduStreamInfo, error: EduError)
}