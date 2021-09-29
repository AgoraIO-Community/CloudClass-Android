package io.agora.agoraeducontext.handlerimpl

import android.view.ViewGroup
import io.agora.agoraeducontext.EduBoardRoomPhase
import io.agora.agoraeducore.core.context.IWhiteboardHandler
import io.agora.agoraeducontext.WhiteboardDrawingConfig

open class WhiteboardHandler : IWhiteboardHandler {
    override fun onWhiteboardJoinSuccess(config: WhiteboardDrawingConfig) {

    }

    override fun onWhiteboardJoinFail(msg: String) {

    }

    override fun onWhiteboardLeft(boardId: String, timestamp: Long) {

    }

    override fun getBoardContainer(): ViewGroup? {
        return null
    }

    override fun onDrawingConfig(config: WhiteboardDrawingConfig) {

    }

    override fun onDrawingEnabled(enabled: Boolean) {

    }

    override fun onPageNo(no: Int, count: Int) {

    }

    override fun onPagingEnabled(enabled: Boolean) {

    }

    override fun onZoomEnabled(zoomOutEnabled: Boolean?, zoomInEnabled: Boolean?) {

    }

    override fun onFullScreenEnabled(enabled: Boolean) {

    }

    override fun onFullScreenChanged(isFullScreen: Boolean) {

    }

    override fun onInteractionEnabled(enabled: Boolean) {

    }

    override fun onBoardPhaseChanged(phase: EduBoardRoomPhase) {
    }

    override fun onDownloadProgress(url: String, progress: Float) {

    }

    override fun onDownloadTimeout(url: String) {

    }

    override fun onDownloadCompleted(url: String) {

    }

    override fun onDownloadError(url: String) {

    }

    override fun onDownloadCanceled(url: String) {

    }

    override fun onPermissionGranted(granted: Boolean) {

    }
}