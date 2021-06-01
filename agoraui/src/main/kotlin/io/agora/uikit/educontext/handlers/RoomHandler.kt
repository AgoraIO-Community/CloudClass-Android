package io.agora.uikit.educontext.handlers

import io.agora.educontext.EduContextClassState
import io.agora.educontext.EduContextConnectionState
import io.agora.educontext.EduContextError
import io.agora.educontext.EduContextNetworkState
import io.agora.educontext.eventHandler.IRoomHandler

open class RoomHandler : IRoomHandler {
    override fun onClassroomName(name: String) {

    }

    override fun onClassState(state: EduContextClassState) {

    }

    override fun onClassTime(time: String) {

    }

    override fun onNetworkStateChanged(state: EduContextNetworkState) {

    }

    override fun onLogUploaded(logData: String) {

    }

    override fun onConnectionStateChanged(state: EduContextConnectionState) {

    }

    override fun onClassTip(tip: String) {

    }

    override fun onError(error: EduContextError) {

    }

    override fun onClassroomJoined() {

    }
}