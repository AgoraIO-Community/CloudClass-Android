package io.agora.agoraeduuikit.interfaces.listeners

import android.view.ViewGroup
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo

interface IAgoraUIVideoListener {
    fun onRendererContainer(viewGroup: ViewGroup?, info: AgoraUIUserDetailInfo)
}