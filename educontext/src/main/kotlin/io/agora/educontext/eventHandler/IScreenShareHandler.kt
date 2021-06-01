package io.agora.educontext.eventHandler

import io.agora.educontext.AgoraScreenShareState

interface IScreenShareHandler {
    // only control the render of screenShare
    fun onScreenShareStateUpdated(state: AgoraScreenShareState, streamUuid: String)

    // only control the display and hide of screenShare
    fun onSelectScreenShare(select: Boolean)

    fun onScreenShareTip(tips: String)
}