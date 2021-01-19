package io.agora.education.api.user.listener

import io.agora.education.api.stream.data.EduStreamEvent
import io.agora.education.api.user.data.EduUserEvent
import io.agora.education.api.user.data.EduUserLeftType
import io.agora.education.api.user.data.EduUserStateChangeType

interface EduUserEventListener {

    fun onLocalUserUpdated(userEvent: EduUserEvent, type: EduUserStateChangeType)

    fun onLocalStreamAdded(streamEvent: EduStreamEvent)

    fun onLocalStreamUpdated(streamEvent: EduStreamEvent)

    fun onLocalStreamRemoved(streamEvent: EduStreamEvent)

    fun onLocalUserLeft(userEvent: EduUserEvent, leftType: EduUserLeftType)
}