package io.agora.agoraeduuikit.impl.whiteboard

import android.content.Context
import android.graphics.Rect
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import io.agora.agoraeducontext.EduBoardRoomPhase
import io.agora.agoraeduuikit.impl.AbsComponent
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.impl.loading.AgoraUILoadingView

class AgoraUIWhiteBoard(
        context: Context,
        private val eduContext: io.agora.agoraeducore.core.context.EduContextPool?,
        parent: ViewGroup,
        width: Int,
        height: Int,
        left: Float,
        top: Float,
        shadowWidth: Float
) : AbsComponent(),
        BoardPreloadProgressListener,
        BoardPreloadFailedListener {
    private val tag = "AgoraUIBoardWindow"

    private val contentView: View = LayoutInflater.from(context).inflate(R.layout.agora_board_layout, parent, false)
    private var rootLayout: RelativeLayout = contentView.findViewById(R.id.root_Layout)
    private var whiteboardContainerOutlineLayout: RelativeLayout = contentView.findViewById(R.id.whiteboard_container_outline)
    private var boardContainer: RelativeLayout = contentView.findViewById(R.id.whiteboard_container)
    private var loadingView: AgoraUILoadingView = contentView.findViewById(R.id.whiteboard_loading_view)
    private var boardPreloadProgressView: AgoraUIBoardPreloadProgressView? = null
    private var boardPreloadFailedView: AgoraUIBoardPreloadFailedView? = null

    private val whiteboardHandler = object : io.agora.agoraeducontext.handlerimpl.WhiteboardHandler() {
        override fun onBoardPhaseChanged(phase: EduBoardRoomPhase) {
            super.onBoardPhaseChanged(phase)
            setLoadingState(phase)
        }

        override fun onDownloadProgress(url: String, progress: Float) {
            super.onDownloadProgress(url, progress)
            this@AgoraUIWhiteBoard.setDownloadProgress(url, progress)
        }

        override fun onDownloadTimeout(url: String) {
            super.onDownloadTimeout(url)
            this@AgoraUIWhiteBoard.setDownloadTimeOut(url)
        }

        override fun onDownloadCompleted(url: String) {
            super.onDownloadCompleted(url)
            this@AgoraUIWhiteBoard.setDownloadComplete(url)
        }

        override fun onDownloadError(url: String) {
            super.onDownloadError(url)
            this@AgoraUIWhiteBoard.downloadError(url)
        }

        override fun onDownloadCanceled(url: String) {
            super.onDownloadCanceled(url)
            this@AgoraUIWhiteBoard.cancelCurDownload()
        }
    }

    // when screenSharing, set whiteBoardWebView transparent.
    private val screenShareHandler = object : io.agora.agoraeducontext.handlerimpl.ScreenShareHandler() {
        override fun onSelectScreenShare(select: Boolean) {
            super.onSelectScreenShare(select)
            whiteboardContainerOutlineLayout.post {
                if (select) {
                    whiteboardContainerOutlineLayout.setBackgroundColor(context.resources
                            .getColor(android.R.color.transparent))
                } else {
                    whiteboardContainerOutlineLayout.background = ContextCompat.getDrawable(context,
                            +R.drawable.agora_class_room_round_rect_stroke_bg)
                }
            }
        }
    }

    init {
        val w = if (width > 0) width else 1
        val h = if (height > 0) height else 1

        parent.addView(contentView, w, h)
        val params = contentView.layoutParams as ViewGroup.MarginLayoutParams
        params.topMargin = top.toInt()
        params.leftMargin = left.toInt()
        contentView.layoutParams = params

        loadingView.z = shadowWidth + 100.0f

        eduContext?.whiteboardContext()?.addHandler(whiteboardHandler)
        eduContext?.screenShareContext()?.addHandler(screenShareHandler)
    }

    fun setLoadingState(phase: EduBoardRoomPhase) {
        loadingView.visibility = if (phase == EduBoardRoomPhase.connected) GONE else VISIBLE
        if (phase == EduBoardRoomPhase.connecting) {
            loadingView.setContent(true)
        } else if (phase == EduBoardRoomPhase.reconnecting) {
            loadingView.setContent(false)
        }
    }

    fun setDownloadProgress(url: String, progress: Float) {
        if (boardPreloadProgressView == null) {
            boardPreloadProgressView = AgoraUIBoardPreloadProgressView.show(rootLayout, url, this)
            boardPreloadProgressView!!.z = loadingView.z + 100.0f
        } else if (boardPreloadProgressView != null && boardPreloadProgressView!!.parent == null) {
            boardPreloadProgressView!!.show(rootLayout, url)
        }

        eduContext?.whiteboardContext()?.setBoardInputEnable(false)
        boardPreloadProgressView!!.updateProgress(progress.toDouble())
    }

    fun setDownloadTimeOut(url: String) {
        boardPreloadProgressView?.timeout()
    }

    fun setDownloadComplete(url: String) {
        boardPreloadProgressView?.dismiss()
    }

    fun downloadError(url: String) {
        boardPreloadProgressView?.dismiss()
        if (boardPreloadFailedView == null) {
            boardPreloadFailedView = AgoraUIBoardPreloadFailedView.show(rootLayout, url, this)
            boardPreloadFailedView!!.z = boardPreloadProgressView!!.z + 100.0f
        } else {
            boardPreloadFailedView?.show(rootLayout, url)
        }

        eduContext?.whiteboardContext()?.setBoardInputEnable(false)
    }

    fun cancelCurDownload() {
        if (boardPreloadProgressView?.isShowing() == true) {
            boardPreloadProgressView?.post {
                boardPreloadProgressView?.dismiss()
            }
        }
        if (boardPreloadFailedView?.isShowing() == true) {
            boardPreloadFailedView?.post {
                boardPreloadFailedView?.dismiss()
            }
        }
    }

    override fun onSkip(url: String?) {
        //whiteBoardListener?.onDownloadSkipped(url)
        eduContext?.whiteboardContext()?.skipDownload(url)
    }

    override fun onProgressViewDismiss(url: String?) {
        eduContext?.whiteboardContext()?.setBoardInputEnable(true)
    }

    override fun onClose(url: String?) {
        eduContext?.whiteboardContext()?.cancelDownload(url)
    }

    override fun onRetry(url: String?) {
        eduContext?.whiteboardContext()?.retryDownload(url)
    }

    override fun onFailedViewDismiss() {
        eduContext?.whiteboardContext()?.setBoardInputEnable(true)
    }

    override fun setRect(rect: Rect) {
        contentView.post {
            val params = contentView.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = rect.top
            params.leftMargin = rect.left
            params.width = rect.right - rect.left
            params.height = rect.bottom - rect.top
            contentView.layoutParams = params
        }
    }

    fun getWhiteboardContainer(): ViewGroup {
        return boardContainer
    }

    fun setVisibility(visibility: Int) {
        contentView.post {
            contentView.visibility = visibility
        }
    }
}