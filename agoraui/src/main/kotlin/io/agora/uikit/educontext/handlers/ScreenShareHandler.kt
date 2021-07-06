package io.agora.uikit.educontext.handlers

import io.agora.educontext.EduContextScreenShareState
import io.agora.educontext.eventHandler.IScreenShareHandler

open class ScreenShareHandler : IScreenShareHandler {
    override fun onScreenShareStateUpdated(state: EduContextScreenShareState, streamUuid: String) {

    }

    override fun onSelectScreenShare(select: Boolean) {
    }

    override fun onScreenShareTip(tips: String) {

    }
}