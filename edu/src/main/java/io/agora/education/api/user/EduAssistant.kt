package io.agora.education.api.user

import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.user.listener.EduAssistantEventListener

interface EduAssistant : EduUser {
    fun setEventListener(eventListener: EduAssistantEventListener?)

    fun createOrUpdateTeacherStream(streamInfo: EduStreamInfo, callback: EduCallback<Unit>)

    fun createOrUpdateStudentStream(streamInfo: EduStreamInfo, callback: EduCallback<Unit>)
}
