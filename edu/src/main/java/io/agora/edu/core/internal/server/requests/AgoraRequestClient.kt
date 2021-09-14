package io.agora.edu.core.internal.server.requests

import io.agora.edu.core.internal.rte.IRtmServerDelegate
import io.agora.edu.core.internal.server.proxy.*
import io.agora.edu.core.internal.server.requests.http.HttpRequestClient
import io.agora.edu.core.internal.server.requests.rtm.RtmRequestClient

object AgoraRequestClient {
    private const val tag = "AgoraRequestClient"

    private const val headerTokenKey = "x-agora-token"
    private const val headerUidKey = "x-agora-uid"

    private const val retryDefault = -1
    private const val retryIndefinite = -2

    private val rtmClient = RtmRequestClient()
    private val httpClient = HttpRequestClient()

    private val proxies: MutableMap<ServiceProxyType, IServiceProxy> = mutableMapOf()

    private var drawbackRequestChannel: Boolean = false

    init {
        initRoomServiceConfigs()
        initMessageServiceConfigs()
        initMediaServiceConfigs()
        initUserServiceConfigs()
        initGeneralServiceConfig()
        initExtensionServiceConfig()

        initRequestProxies()

        rtmClient.setNoAvailableServerPeerHandler {
            httpClient.triggerCreateRtmServerPeer()
        }
    }

    private fun initRequestProxies() {
        proxies[ServiceProxyType.General] = GeneralServiceProxy()
        proxies[ServiceProxyType.Extension] = ExtensionServiceProxy()
        proxies[ServiceProxyType.Room] = RoomServiceProxy()
        proxies[ServiceProxyType.Message] = MessageServiceProxy()
        proxies[ServiceProxyType.Media] = MediaServiceProxy()
        proxies[ServiceProxyType.User] = UserServiceProxy()
    }

    fun getProxy(type: ServiceProxyType) : IServiceProxy? {
        return proxies[type]
    }

    /**
     * Server-side authentication settings as header
     * content both for http requests and rtm requests
     */
    fun setAuthInfo(rtmToken: String, userUuid: String) {
        synchronized(this) {
            httpClient.addHeader(headerTokenKey, rtmToken)
            httpClient.addHeader(headerUidKey, userUuid)
            rtmClient.addHeader(headerTokenKey, rtmToken)
            rtmClient.addHeader(headerUidKey, userUuid)
        }
    }

    /**
     * @param prior if set, the request is sent by desired request channel
     * rather than the default config
     * @param drawback true if the request is sent by a higher priority request
     * channel but fail, it will be sent to a different channel with lower priority,
     * false otherwise. Use global setting if not set (null). Default is false.
     * @param retry retry count on the basis of server client default retry times.
     * Usually it is default, but can be set to -2 if the request should be
     * retried indefinitely if fails. The param is NOT USED at the time.
     */
    fun send(request: Request, prior: RequestChannelPriority = RequestChannelPriority.DEFAULT,
             region: String, drawback: Boolean? = false, retry: Int = retryDefault,
             callback: RequestCallback<Any>?, vararg args: Any?) {
        Request.getRequestConfig(request)?.let { config ->
            val desiredChannel = getSendChannel(config, prior)
            val ifDrawback = drawback ?: drawbackRequestChannel
            sendRequest(config, desiredChannel, region, ifDrawback, callback, args)
        }
    }

    fun sendByRtm(request: Request, region: String,
                  callback: RequestCallback<Any>?, vararg args: Any) {
        send(request = request,
                prior = RequestChannelPriority.RTM,
                region = region,
                callback = callback,
                args = *arrayOf(args))
    }

    fun sendByHttp(request: Request, region: String,
                  callback: RequestCallback<Any>?, vararg args: Any) {
        send(request = request,
                prior = RequestChannelPriority.HTTP,
                region = region,
                callback = callback,
                args = *arrayOf(args))
    }

    /**
     * Get a request channel based on the config and temporary settings
     * @param config predefined request config
     * @param prior temporary request channel priority, usually defined
     * as runtime-specific decision
     */
    internal fun getSendChannel(config: RequestConfig,
                                prior: RequestChannelPriority) : RequestChannelPriority {
        return if (prior == RequestChannelPriority.DEFAULT ||
                prior == config.priority) config.priority
        else prior
    }

    private fun sendRequest(config: RequestConfig, prior: RequestChannelPriority,
                            region: String, drawback: Boolean,
                            callback: RequestCallback<Any>?, vararg args: Any?) {
        if (prior == RequestChannelPriority.RTM) {
            if (rtmClient.rtmChannelAvailable(region) &&
                    rtmClient.hasAvailableServerPeerByRegion(region)) {
                rtmClient.sendRequest(config, region, callback, args)
            } else if (drawback) {
                httpClient.sendRequest(config, callback, args)
            } else {
                callback?.onFailure(RequestError(
                        RequestError.requestServerError,
                        "No available rtm channel server found"))
            }
        } else if (prior == RequestChannelPriority.HTTP) {
            httpClient.sendRequest(config, callback, args)
        }
    }

    /**
     * Check if peer messages come from rtm server peers and can be
     * parsed by rtm request client.
     * @return true if this message comes from rtm server peer, and
     * it should be consumed, thus not being handled by
     * other rtm peer message handlers.
     */
    fun handleRtmRequestResponses(message: String, peerId: String) : Boolean {
        return rtmClient.handleRtmMessage(message, peerId)
    }

    /**
     * Set a rtm client delegate, which is a bridge to obtain
     * agora rtm sdk functions.
     * Note that it must be set before using any rtm requests.
     */
    fun setRtmMessageDelegate(delegate: IRtmServerDelegate) {
        rtmClient.setRtmPeerMessageDelegate(delegate)
    }

    /**
     * Enable or disable rtm request channel.
     * If enabled, the requests that are configured to go rtm
     * channel would first search for online rtm server peers.
     * At meantime, a background thread will be started to
     * periodically check rtm server peer status.
     * If disabled, all requests would be sent as http requests
     * and the background thread is stopped.
     * Rtm server channel is disabled by default
     * @param region starts the rtm server channel for a specific region.
     * Rtm servers are separated by regions
     */
    fun enableRtmRequestChannel(region: String, enabled: Boolean) {
        if (enabled) {
            rtmClient.startWithRegion(region)
        } else {
            rtmClient.stopWithRegion(region)
        }
    }

    /**
     * Set globally as default if a rtm-prior tries http request instead
     * when the rtm request fails. A runtime-specific config replaces
     * this default setting
     */
    fun setDrawbackIfRtmFails(drawback: Boolean) {
        this.drawbackRequestChannel = drawback
    }
}