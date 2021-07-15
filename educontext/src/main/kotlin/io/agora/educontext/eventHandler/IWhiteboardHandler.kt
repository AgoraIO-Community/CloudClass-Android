package io.agora.educontext.eventHandler

import android.view.ViewGroup
import io.agora.educontext.EduBoardRoomPhase
import io.agora.educontext.WhiteboardDrawingConfig

interface IWhiteboardHandler {

    /**
     * Gets the parent container of the whiteboard
     * */
    fun getBoardContainer(): ViewGroup?

    /**
     * Called when the whiteboard drawing config is to be set,
     * like pencil shapes, color, or font size
     */
    fun onDrawingConfig(config: WhiteboardDrawingConfig)

    /**
     * Called when the change of whiteboard drawing config
     * is enabled or disabled.
     */
    fun onDrawingEnabled(enabled: Boolean)

    /**
     * Set current page number and page count for current ppt
     */
    fun onPageNo(no: Int, count: Int)

    /**
     * Set if the page control and tool bar is allowed
     */
    fun onPagingEnabled(enabled: Boolean)

    /**
     * Set if the zooming of whiteboard is enabled
     */
    fun onZoomEnabled(zoomOutEnabled: Boolean?, zoomInEnabled: Boolean?)

    /**
     * Set if whiteboard is allowed to be made full screen
     */
    fun onFullScreenEnabled(enabled: Boolean)

    /**
     * Called whether the whiteboard is made full screen or not
     */
    fun onFullScreenChanged(isFullScreen: Boolean)

    /**
     * Called when the interaction with whiteboard is enabled
     * or disabled, including paging, zooming and resizing
     * (full screen)
     */
    fun onInteractionEnabled(enabled: Boolean)

    fun onBoardPhaseChanged(phase: EduBoardRoomPhase)

    fun onDownloadProgress(url: String, progress: Float)

    fun onDownloadTimeout(url: String)

    fun onDownloadCompleted(url: String)

    fun onDownloadError(url: String)

    fun onDownloadCanceled(url: String)

    /**
     * Called when the whiteboard authorization is granted or not
     */
    fun onPermissionGranted(granted: Boolean)
}