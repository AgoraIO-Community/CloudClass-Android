package io.agora.edu.core.internal.server.requests.http.retrofit.dispatch

import io.agora.edu.core.internal.server.requests.RequestCallback
import io.agora.edu.core.internal.server.requests.RequestConfig

interface IServiceDispatcher {
    fun dispatch(config: RequestConfig, callback: RequestCallback<Any>?, vararg args: Any)
}