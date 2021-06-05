package io.agora.uikit.educontext.handlers

import io.agora.educontext.AgoraScreenShareState
import io.agora.educontext.eventHandler.IScreenShareHandler

open class ScreenShareHandler : IScreenShareHandler {
    override fun onScreenShareStateUpdated(state: AgoraScreenShareState, streamUuid: String) {

    }

    override fun onSelectScreenShare(select: Boolean) {
    }

    override fun onScreenShareTip(tips: String) {

    }
}