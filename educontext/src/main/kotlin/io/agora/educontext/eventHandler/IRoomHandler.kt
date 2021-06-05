package io.agora.educontext.eventHandler

import io.agora.educontext.*

interface IRoomHandler {
    fun onClassroomName(name: String)

    fun onClassState(state: EduContextClassState)

    fun onClassTime(time: String)

    fun onNetworkStateChanged(state: EduContextNetworkState)

    fun onLogUploaded(logData: String)

    fun onConnectionStateChanged(state: EduContextConnectionState)

    fun onClassTip(tip: String)

//    /** @param properties all custom props
//     * @param  operator this is null when server update props */
//    fun onRoomPropertiesChanged(properties: MutableMap<String, String>, changed: MutableMap<String, String>,
//                                cause: MutableMap<String, String>?, operator: EduContextUserInfo?)

    fun onError(error: EduContextError)

    fun onClassroomJoined()
}