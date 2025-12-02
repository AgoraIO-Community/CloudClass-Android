package io.agora.agoraeduuikit.provider


interface UIDataProviderListener {
    /**
     * Used when only consider the change of co hosts
     */
    fun onCoHostListChanged(userList: List<AgoraUIUserDetailInfo>)

    /**
     * Used when consider both the co host and other
     * audience
     */
    fun onUserListChanged(userList: List<AgoraUIUserDetailInfo>)

    fun onVolumeChanged(volume: Int, streamUuid: String)

    fun onAudioMixingStateChanged(state: Int, errorCode: Int)

    fun onLocalUserKickedOut()

    fun onScreenShareStart(info: AgoraUIUserDetailInfo)

    fun onScreenShareStop(info: AgoraUIUserDetailInfo)

    fun onHandsWaveEnable(enable: Boolean)

    fun onUserHandsWave(userUuid: String, duration: Int, payload: Map<String, Any>?)

    fun onUserHandsDown(userUuid: String, payload: Map<String, Any>?)
}