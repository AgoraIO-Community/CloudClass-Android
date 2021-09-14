package io.agora.edu.core.internal.edu.common.api

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import io.agora.edu.core.internal.edu.common.listener.BoardPreloadListener

interface BoardPreload {

    fun isAvailable(): Boolean

    fun preload(link: String, listener: BoardPreloadListener?)

    fun cancelCurPreloadTask(url: String)

    fun cancelAllPreloadTask()

    fun checkCache(request: WebResourceRequest): WebResourceResponse?
}