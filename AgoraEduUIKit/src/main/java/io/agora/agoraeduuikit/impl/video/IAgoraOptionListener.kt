package io.agora.agoraeduuikit.impl.video

import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo

interface IAgoraOptionListener {
    fun onAudioUpdated(item: AgoraUIUserDetailInfo, enabled: Boolean)
    fun onVideoUpdated(item: AgoraUIUserDetailInfo, enabled: Boolean)
    fun onCohostUpdated(item: AgoraUIUserDetailInfo, isCoHost: Boolean)
    fun onGrantUpdated(item: AgoraUIUserDetailInfo, hasAccess: Boolean)
    fun onRewardUpdated(item: AgoraUIUserDetailInfo, count: Int)

}