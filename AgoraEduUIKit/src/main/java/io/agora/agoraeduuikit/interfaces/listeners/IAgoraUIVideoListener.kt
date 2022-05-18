package io.agora.agoraeduuikit.interfaces.listeners

import android.view.ViewGroup

interface IAgoraUIVideoListener {
    fun onUpdateVideo(streamUuid: String, enable: Boolean)
    fun onUpdateAudio(streamUuid: String, enable: Boolean)
    fun onRendererContainer(viewGroup: ViewGroup?, streamUuid: String)
}