package io.agora.educontext.context

import android.view.View
import io.agora.educontext.AbsHandlerPool
import io.agora.educontext.WhiteboardApplianceType
import io.agora.educontext.eventHandler.IWhiteboardHandler

abstract class WhiteboardContext : AbsHandlerPool<IWhiteboardHandler>() {
    // Drawing configs
    abstract fun selectAppliance(type: WhiteboardApplianceType)

    abstract fun selectColor(color: Int)

    abstract fun selectFontSize(size: Int)

    abstract fun selectThickness(thick: Int)

    abstract fun selectRoster(anchor: View)

    // whiteBoard
    abstract fun setBoardInputEnable(enable: Boolean)

    abstract fun skipDownload(url: String?)

    abstract fun cancelDownload(url: String?)

    abstract fun retryDownload(url: String?)

    // page control
    abstract fun setFullScreen(full: Boolean)

    abstract fun setZoomOut()

    abstract fun setZoomIn()

    abstract fun setPrevPage()

    abstract fun setNextPage()
}