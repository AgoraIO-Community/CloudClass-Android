package io.agora.educontext.context

import android.view.ViewGroup
import io.agora.educontext.AbsHandlerPool
import io.agora.educontext.AgoraScreenShareState
import io.agora.educontext.eventHandler.IScreenShareHandler

abstract class ScreenShareContext : AbsHandlerPool<IScreenShareHandler>() {
    abstract fun setScreenShareState(state: AgoraScreenShareState)

    abstract fun renderScreenShare(container: ViewGroup?, streamUuid: String)
}