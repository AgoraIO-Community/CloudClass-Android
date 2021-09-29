package io.agora.agoraeducore.core.internal.server.requests.rtm

import io.agora.agoraeducore.core.internal.framework.data.EduCallback
import io.agora.agoraeducore.core.internal.framework.data.EduError
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.rte.IRtmServerDelegate
import io.agora.agoraeducore.core.internal.server.requests.*
import io.agora.agoraeducore.core.internal.server.responses.rtm.ServerRtmResp
import java.util.*

class RtmRequestClient {
    companion object {
        const val tag = "RtmRequestClient"
        const val serverMax = 64
        const val maxTryCount = 3
    }

    // Prefix word to identify all rtm server peers logged in under
    // the same agora application id.
    private val serverPrefix = ""

    private val rtmServerPeerRegions = mutableMapOf<String, ServerPeerListLooper>()

    /**
     * Cache message handlers for every rtm region server.
     * Each handler can recognize and process valid message
     * text.
     */
    private val rtmServerMessageHandlers = mutableMapOf<String, RtmResponseHandler>()

    private val requestCache = RtmRequestCache()

    private var rtmServerDelegate: IRtmServerDelegate? = null

    private var noAvailableServerPeerHandler: (() -> Unit)? = null

    private val headerMap = mutableMapOf<String, String>()

    fun addHeader(key: String, value: String) {
        headerMap[key] = value
    }

    /**
     * Start to work with a specific region.
     * This will cause a worker thread to periodically check rtm
     * server status, and thus rtm request channel is accessible.
     */
    fun startWithRegion(region: String) {
        Constants.AgoraLog.d("$tag-> start with region $region")
        val looper = findRtmServerByRegion(region)
        rtmServerMessageHandlers[looper.region] = RtmResponseHandler(looper)

        // Start a timer task to periodically refresh available server peers.
        // Every looper needs a callback here because server update calls
        // agora rtm server and its delegate is set inside here.
        looper.startBackgroundServerCheck(region) {
            refreshServerStatus(looper)
        }

        // Ask to refresh server list right now
        refreshServerStatus(looper)
    }

    private fun refreshServerStatus(looper: ServerPeerListLooper) {
        Constants.AgoraLog.d("$tag-> background refresh server status")
        rtmServerDelegate?.rtmServerPeerOnlineStatus(looper.getRtmPeerIdList(),
                object : EduCallback<Map<String, Boolean>> {
                    override fun onSuccess(res: Map<String, Boolean>?) {
                        res?.let { status ->
                            synchronized(looper) {
                                Constants.AgoraLog.d("$tag-> refresh server status, result size ${status.size}")
                                looper.updateServerPeerOnlineStatus(status,
                                        noAvailableServerPeerHandler)
                            }
                        }
                    }

                    override fun onFailure(error: EduError) {

                    }
                })
    }

    fun stopWithRegion(region: String) {
        Constants.AgoraLog.d("$tag-> stop with region $region")
        rtmServerPeerRegions[region]?.stopServerCheck()
        rtmServerPeerRegions.remove(region)
    }

    @Synchronized
    private fun findRtmServerByRegion(region: String) : ServerPeerListLooper {
        if (rtmServerPeerRegions[region] == null) {
            rtmServerPeerRegions[region] = ServerPeerListLooper(
                    serverPrefix, region, serverMax, rtmServerDelegate)
        }
        return rtmServerPeerRegions[region]!!
    }

    @Synchronized
    private fun findNextOnlineServerPeerByRegion(region: String) : ServerPeer? {
        // Search pre-defined servers to find the first online server peer
        return findRtmServerByRegion(region).findNextOnlineServerPeer()
    }

    fun hasAvailableServerPeerByRegion(region: String) : Boolean {
        return findNextOnlineServerPeerByRegion(region) != null
    }

    @Synchronized
    fun rtmChannelAvailable(region: String) : Boolean {
        return rtmServerPeerRegions[region]?.isRunning() ?: false
    }

    @Synchronized
    fun setRtmPeerMessageDelegate(delegate: IRtmServerDelegate) {
        rtmServerDelegate = delegate
    }

    fun setNoAvailableServerPeerHandler(handler: (() -> Unit)) {
        noAvailableServerPeerHandler = handler
    }

    @Synchronized
    fun sendRequest(config: RequestConfig, region: String,
                    callback: RequestCallback<Any>? = null, vararg args: Any) {
        try {
            val param = RequestBuilder.buildParamWithArgs(config, region, headerMap, callback, args)
            val traceId = requestCache.pushToCache(config, param)
            sendRequest(traceId, config, param, callback)
        } catch (e: IllegalRtmRequestArgumentException) {
            callback?.onFailure(RequestError(RequestError.requestIllegalArgument, e.message ?: ""))
            e.printStackTrace()
        }
    }

    private fun sendRequest(traceId: String, config: RequestConfig,
                            params: RequestParam, callback: RequestCallback<Any>? = null) {
        rtmServerPeerRegions[params.region]?.setRtmRequest(traceId,
                config, params, object : RequestCallback<Any> {
                    override fun onSuccess(t: Any?) {

                    }

                    override fun onMayRetry(t: Any?) {
                        synchronized(this@RtmRequestClient) {
                            requestCache.peekItem(traceId)?.let { item ->
                                item.tried++
                                if (item.tried >= maxTryCount) {
                                    callback?.onFailure(RequestError.MaxTryCount)
                                    requestCache.remove(traceId)
                                } else {
                                    sendRequest(traceId, item.config, item.params, callback)
                                }
                            }
                        }
                    }

                    override fun onFailure(error: RequestError) {
                        callback?.onFailure(error)
                    }
                })
    }

    /**
     *
     */
    fun handleRtmMessage(message: String, peerId: String) : Boolean {
        synchronized(this@RtmRequestClient) {
            var consumed = false
            rtmServerMessageHandlers.forEach { entry ->
                if (entry.value.isValidServerPeer(peerId)) {
                    consumed = true
                    entry.value.parseValidResponseStruct(message)?.let { resp ->
                        entry.value.handleResponse(resp, peerId)
                    }
                }
            }

            return consumed
        }
    }

    inner class RtmRequestCache {
        private val requestCachePool = mutableMapOf<String, RtmRequestTask>()

        @Synchronized
        fun pushToCache(config: RequestConfig, params: RequestParam) : String {
            val item = RtmRequestTask(config, params, TraceIdGenerator.nextId())
            requestCachePool[item.traceId] = item
            return item.traceId
        }

        @Synchronized
        fun remove(traceId: String) : RtmRequestTask? {
            return requestCachePool.remove(traceId)
        }

        @Synchronized
        fun peekItem(traceId: String) : RtmRequestTask? {
            return requestCachePool[traceId]
        }
    }

    inner class RtmRequestTask(
            val config: RequestConfig,
            val params: RequestParam,
            val traceId: String) {
        var tried: Int = 0
    }

    internal object TraceIdGenerator {
        private var currentId: Long = 0

        fun nextId() : String {
            if (currentId != Long.MAX_VALUE) {
                currentId++
            } else {
                currentId = 0L
            }

            return currentId.toString()
        }
    }

    internal inner class RtmResponseHandler(private val peerList: ServerPeerListLooper) {
        fun isValidServerPeer(name: String) : Boolean {
            return peerList.containsServer(name)
        }

        fun parseValidResponseStruct(text: String) : ServerRtmResp? {
            return ServerRtmResp.parse(text)
        }

        /**
         * Process incoming messages without remote peer and text format check
         * @param text message string
         * @param peerId where the message comes from
         * @return True if the server peer should process and
         * consume this message and it will not be dispatched
         * to other handlers. False otherwise, if the handler
         * does not recognize this peer or this message is
         * not in the desired json structures
         */
        fun handleResponse(resp: ServerRtmResp, peerId: String) : Boolean {
            synchronized(this@RtmRequestClient) {
                val id = resp.traceId
                val task = requestCache.remove(id)
                return if (task != null) {
                    task.params.callback?.onSuccess(resp)
                    true
                } else {
                    Constants.AgoraLog.e("$tag-> response ignored, cannot find corresponding" +
                            " request task in local cache. It may be removed for some reason")
                    false
                }
            }
        }
    }
}

