package io.agora.online.impl.video

import io.agora.online.provider.AgoraUIUserDetailInfo

interface IAgoraOptionListener {
    fun onAudioUpdated(item: AgoraUIUserDetailInfo, enabled: Boolean)
    fun onVideoUpdated(item: AgoraUIUserDetailInfo, enabled: Boolean)
    fun onCohostUpdated(item: AgoraUIUserDetailInfo, isCoHost: Boolean)
    fun onPrivateChatUpdated(item: AgoraUIUserDetailInfo)
    fun onGrantUpdated(item: AgoraUIUserDetailInfo, hasAccess: Boolean)
    fun onRewardUpdated(item: AgoraUIUserDetailInfo, count: Int)

}