package io.agora.agoraeducore.core.internal.edu.classroom.widget.whiteboard

interface BoardPreloadEventListener {

    fun onBoardResourceStartDownload(url: String)

    fun onBoardResourceProgress(url: String, progress: Double)

    fun onBoardResourceLoadTimeout(url: String)

    fun onBoardResourceReady(url: String)

    fun onBoardResourceLoadFailed(url: String)
}