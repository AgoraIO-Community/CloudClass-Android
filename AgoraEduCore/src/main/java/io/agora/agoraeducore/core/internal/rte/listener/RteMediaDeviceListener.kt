package io.agora.agoraeducore.core.internal.rte.listener

interface RteMediaDeviceListener {
    fun onAudioRouteChanged(routing: Int)
}