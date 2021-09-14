package io.agora.edu.core.internal.server.requests.http

import io.agora.edu.BuildConfig
import io.agora.edu.core.internal.education.impl.Constants
import io.agora.edu.core.internal.server.requests.*
import io.agora.edu.core.internal.server.requests.http.retrofit.RetrofitManager
import okhttp3.logging.HttpLoggingInterceptor

class HttpRequestClient {
    private val tag = "HttpClientImpl"

    private val retrofitManager: RetrofitManager = RetrofitManager(BuildConfig.API_BASE_URL,
            object : HttpLoggingInterceptor.Logger {
                override fun log(message: String) {
                    Constants.AgoraLog.i(message)
                }
            })

    private val headerMap = mutableMapOf<String, String>()

    fun addHeader(key: String, value: String) {
        retrofitManager.addHeader(key, value)
    }

    /**
     * Trigger the server to start a rtm server peer to response to
     * for a specific rtm server request.
     * If Success, it's better to update current available rtm
     * server node list.
     */
    fun triggerCreateRtmServerPeer(callback: RequestCallback<Void>? = null) {

    }

    fun sendRequest(config: RequestConfig, callback: RequestCallback<Any>?, vararg args: Any) {
        retrofitManager.send(config, callback, args)
    }
}