package io.agora.edu.core.internal.edu.classroom.widget.whiteboard

import android.content.Context
import android.os.Handler
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import io.agora.edu.core.internal.edu.common.impl.BoardPreloadImpl
import io.agora.edu.core.internal.edu.common.listener.BoardPreloadListener

class BoardPreloadManager(
        context: Context,
        private val roomUuid: String
) : BoardPreloadListener {
    private val TAG = "BoardloadManager"

    private var handler: Handler = Handler()

    var listener: BoardPreloadEventListener? = null

    private var curDownloadUrl: String? = null

    private var boardPreload: BoardPreloadImpl = BoardPreloadImpl(context)

    fun preload(url: String) {
        this.curDownloadUrl = url
        boardPreload.preload(curDownloadUrl!!, this)
        handler.postDelayed({
            listener?.onBoardResourceLoadTimeout(curDownloadUrl!!)
        }, 15 * 1000)
    }

    fun checkCache(request: WebResourceRequest): WebResourceResponse? {
        return boardPreload.checkCache(request)
    }

    fun retry() {
        curDownloadUrl?.let {
            boardPreload.preload(curDownloadUrl!!, this)
            handler.postDelayed({
                listener?.onBoardResourceLoadTimeout(it)
            }, 15 * 1000)
        }
    }

    fun cancelPreload() {
        handler.removeCallbacksAndMessages(null)
        curDownloadUrl?.let {
            boardPreload.cancelCurPreloadTask(it)
            curDownloadUrl = null
        }
    }

    override fun onStartDownload(url: String) {
        Log.e(TAG, "onStart")
        listener?.onBoardResourceStartDownload(curDownloadUrl!!)
    }

    override fun onProgress(url: String, progress: Double) {
        Log.e(TAG, "onProgress->$progress")
        listener?.onBoardResourceProgress(curDownloadUrl!!, progress)
    }

    override fun onComplete(url: String) {
        Log.e(TAG, "onComplete")
        handler.removeCallbacksAndMessages(null)
        listener?.onBoardResourceReady(curDownloadUrl!!)
        curDownloadUrl = null
    }

    override fun onFailed(url: String) {
        Log.e(TAG, "onFailed->$url")
        handler.removeCallbacksAndMessages(null)
        curDownloadUrl?.let {
            listener?.onBoardResourceLoadFailed(it)
        }
    }
}