package io.agora.edu.uikit.handlers

import io.agora.edu.core.context.EduContextScreenShareState
import io.agora.edu.core.context.IScreenShareHandler

open class ScreenShareHandler : IScreenShareHandler {
    override fun onScreenShareStateUpdated(state: EduContextScreenShareState, streamUuid: String) {

    }

    override fun onSelectScreenShare(select: Boolean) {
    }

    override fun onScreenShareTip(tips: String) {

    }
}