package io.agora.agoraeducontext.handlerimpl

import io.agora.agoraeducontext.EduContextScreenShareState
import io.agora.agoraeducore.core.context.IScreenShareHandler

open class ScreenShareHandler : IScreenShareHandler {
    override fun onScreenShareStateUpdated(state: EduContextScreenShareState, streamUuid: String) {

    }

    override fun onSelectScreenShare(select: Boolean) {
    }

    override fun onScreenShareTip(tips: String) {

    }
}