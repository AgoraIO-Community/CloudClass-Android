package io.agora.edu.core.internal.rte.listener

interface RteMediaDeviceListener {
    fun onAudioRouteChanged(routing: Int)
}