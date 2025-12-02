package io.agora.online.interfaces.listeners

import android.view.ViewGroup
import io.agora.online.provider.AgoraUIUserDetailInfo

interface IAgoraUIVideoListener {
    fun onRendererContainer(viewGroup: ViewGroup?, info: AgoraUIUserDetailInfo)
}