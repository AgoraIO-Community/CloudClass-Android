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

    fun onFlexRoomPropsInitialized(properties: MutableMap<String, Any>)

    /** @param properties all custom props
     * @param  operator this is null when server update props */
    fun onFlexRoomPropsChanged(changedProperties: MutableMap<String, Any>,
                               properties: MutableMap<String, Any>,
                               cause: MutableMap<String, Any>?, operator: EduContextUserInfo?)

    fun onError(error: EduContextError)
}