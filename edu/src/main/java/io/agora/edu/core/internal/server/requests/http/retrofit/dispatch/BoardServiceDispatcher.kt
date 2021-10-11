package io.agora.edu.core.internal.server.requests.http.retrofit.dispatch

import io.agora.edu.core.internal.server.requests.RequestCallback
import io.agora.edu.core.internal.server.requests.RequestConfig
import io.agora.edu.core.internal.server.requests.http.retrofit.services.deprecated.BoardService

internal class BoardServiceDispatcher(private val boardService: BoardService) : AbsServiceDispatcher() {
    override fun dispatch(config: RequestConfig, callback: RequestCallback<Any>?, vararg args: Any) {

    }
}