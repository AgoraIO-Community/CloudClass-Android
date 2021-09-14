package io.agora.edu.uikit.handlers

import io.agora.edu.core.context.IRoomHandler

open class RoomHandler : IRoomHandler {
    override fun onClassroomName(name: String) {

    }

    override fun onClassState(state: io.agora.edu.core.context.EduContextClassState) {

    }

    override fun onClassTime(time: String) {

    }

    override fun onNetworkStateChanged(state: io.agora.edu.core.context.EduContextNetworkState) {

    }

    override fun onLogUploaded(logData: String) {

    }

    override fun onConnectionStateChanged(state: io.agora.edu.core.context.EduContextConnectionState) {

    }

    override fun onClassTip(tip: String) {

    }

    override fun onError(error: io.agora.edu.core.context.EduContextError) {

    }

    override fun onClassroomJoinSuccess(roomUuid: String, timestamp: Long) {

    }

    override fun onClassroomJoinFail(roomUuid: String, code: Int?, msg: String?, timestamp: Long) {

    }

    override fun onClassroomLeft(roomUuid: String, timestamp: Long, exit: Boolean) {

    }

    override fun onFlexRoomPropsInitialized(properties: MutableMap<String, Any>) {

    }

    override fun onFlexRoomPropsChanged(changedProperties: MutableMap<String, Any>,
                                        properties: MutableMap<String, Any>,
                                        cause: MutableMap<String, Any>?,
                                        operator: io.agora.edu.core.context.EduContextUserInfo?) {
    }
}