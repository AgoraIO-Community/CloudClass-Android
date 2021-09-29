package io.agora.agoraeducore.core.internal.rte

import io.agora.agoraeducore.core.internal.framework.data.EduCallback

/**
 * As a delegate for outside world to send rtm peer messages
 */
interface IRtmServerDelegate {
    /**
     * Send rtm peer message to a remote peer
     * @return send state
     * @see RtmServerRequestResult
     */
    fun sendRtmServerRequest(text: String, peerId: String, callback: EduCallback<Void>?) : RtmServerRequestResult

    fun rtmServerPeerOnlineStatus(serverIdList: List<String>, callback: EduCallback<Map<String, Boolean>>?)
}

enum class RtmServerRequestResult {
    Success, RtmNotLogin
}