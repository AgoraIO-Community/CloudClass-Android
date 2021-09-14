package io.agora.edu.core.internal.server.requests.rtm

import android.util.Log
import io.agora.edu.core.internal.framework.data.EduCallback
import io.agora.edu.core.internal.framework.data.EduError
import io.agora.edu.core.internal.rte.IRtmServerDelegate
import io.agora.edu.core.internal.server.requests.*
import io.agora.rtm.RtmStatusCode
import java.util.*
import kotlin.concurrent.fixedRateTimer

data class ServerPeer(
        val index: Int,
        val peerId: String,
        var isOnline: Boolean
)

internal class ServerPeerListLooper(
        val prefix: String,
        val region: String,
        private val maxLength: Int,
        private val server: IRtmServerDelegate?) {

    private val tag = "RtmServerPeerList"
    private val checkPeriod = 30000L
    private val initialDelay = 30000L

    private val serverIdMap = mutableMapOf<String, ServerPeer>()
    private val serverOnlineMap = mutableMapOf<String, ServerPeer>()
    private val serverOnlineList = mutableListOf<ServerPeer>()
    private val serverIndexMap = mutableMapOf<Int, ServerPeer>()

    private val serverArray = Array(maxLength) { index ->
        val id = "$prefix-$region-$index"
        val peer = ServerPeer(index, id, false)
        serverIdMap[id] = peer
        serverIndexMap[index] = peer
        peer
    }

    private var currentPeerIndex = -1

    private var timer: Timer? = null
    private var serverStatusCheckStarted = false
    private var serverStatusCheckRegion: String? = null
    private var serverStatusCheckRunnable: (() -> Unit)? = null

    /**
     * Check if this peer list contains a server name
     */
    fun containsServer(name: String) : Boolean {
        val splits = name.split("-")
        if (splits.size != 3) return false
        if (splits[0] !== prefix || splits[1] !== region) return false
        val no = splits[2].toIntOrNull()
        if (no == null || no < 0 || no >= maxLength) return false
        return true
    }

    @Synchronized
    fun startBackgroundServerCheck(region: String, runnable: (() -> Unit)) {
        if (!serverStatusCheckStarted) {
            serverStatusCheckRegion = region
            serverStatusCheckRunnable = runnable

            timer?.cancel()
            timer = fixedRateTimer("timer-$region",
                    true, initialDelay, checkPeriod) {
                        serverStatusCheckRunnable?.invoke()
                    }
            serverStatusCheckStarted = true
        }
    }

    @Synchronized
    fun stopServerCheck() {
        if (serverStatusCheckStarted) {
            timer?.cancel()
            serverStatusCheckRegion = null
            serverStatusCheckRunnable = null
            serverStatusCheckStarted = false
            timer = null
        }
    }

    @Synchronized
    fun isRunning() : Boolean {
        return serverStatusCheckStarted
    }

    @Synchronized
    fun findNextOnlineServerPeer() : ServerPeer? {
        if (serverOnlineList.isEmpty()) {
            return null
        }

        val nextId = currentPeerIndex++
        var index = -1

        for (i in serverOnlineList.indices) {
            if (serverOnlineList[i].index >= nextId) {
                currentPeerIndex = serverOnlineList[i].index
                index = i
                break
            }
        }

        if (index == -1) {
            index = 0
        }

        return serverOnlineList[index]
    }

    @Synchronized
    fun updateServerPeerOnlineStatus(status: Map<String, Boolean>, nonUsableCallback: (() -> Unit)? = null) {
        var noAvailable = true
        status.iterator().forEach { entry ->
            if (serverIdMap.containsKey(entry.key)) {
                serverIdMap[entry.key]?.isOnline = entry.value
                if (!entry.value && serverOnlineMap.containsKey(entry.key)) {
                    serverOnlineMap.remove(entry.key)
                    serverOnlineList.remove(serverOnlineMap[entry.key])
                } else if (entry.value && !serverOnlineMap.containsKey(entry.key)) {
                    serverIdMap[entry.key]?.let {
                        serverOnlineMap[entry.key] = it
                        serverOnlineList.add(it)
                        // Sort by server peer index for further online server searching
                        serverOnlineList.sortBy { peer -> peer.index }
                    }
                }
                if (entry.value) noAvailable = false
            } else {
                Log.w(tag, "online server ${entry.key} not exists" +
                        " in current server peer list")
            }
        }

        if (noAvailable) {
            nonUsableCallback?.invoke()
        }
    }

    @Synchronized
    fun getRtmPeerIdList() : List<String> {
        val list = mutableListOf<String>()
        serverArray.forEach {
            list.add(it.peerId)
        }
        return list
    }

    fun setRtmRequest(traceId: String, config: RequestConfig, params: RequestParam,
                      callback: RequestCallback<Any>? = null) {
        val text = Request.toRtmMessageString(traceId, config, params)
        findNextOnlineServerPeer()?.peerId?.let { peerId ->
            server?.sendRtmServerRequest(text, peerId, object : EduCallback<Void> {
                override fun onSuccess(res: Void?) {
                    // Success callback will be determined via
                    // remote server peer message return
                }

                override fun onFailure(error: EduError) {
                    if (error.type == RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_USER_NOT_LOGGED_IN) {
                        callback?.onMayRetry(null)
                    } else {
                        callback?.onFailure(RequestError(error.type, error.msg))
                    }
                }
            })
        }
    }

    @Synchronized
    fun release() {
        serverIdMap.clear()
        serverIndexMap.clear()
    }
}