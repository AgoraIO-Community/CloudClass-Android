package io.agora.educontext.eventHandler

interface IScreenShareHandler {
    fun onScreenShareStateUpdated(sharing: Boolean, streamUuid: String)

    fun onScreenShareTip(tips: String)
}