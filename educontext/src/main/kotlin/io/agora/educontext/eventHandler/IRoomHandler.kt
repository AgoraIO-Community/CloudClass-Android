package io.agora.educontext.eventHandler

import io.agora.educontext.EduContextConnectionState
import io.agora.educontext.EduContextNetworkState
import io.agora.educontext.EduContextError
import io.agora.educontext.EduContextClassState

interface IRoomHandler {
    fun onClassroomName(name: String)

    fun onClassState(state: EduContextClassState)

    fun onClassTime(time: String)

    fun onNetworkStateChanged(state: EduContextNetworkState)

    fun onConnectionStateChanged(state: EduContextConnectionState)

    fun onClassTip(tip: String)

    fun onError(error: EduContextError)
}