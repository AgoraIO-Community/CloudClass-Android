package io.agora.uikit.interfaces.listeners

import android.view.ViewGroup

interface IAgoraUIVideoGroupListener {
    fun onUpdateVideo(enable: Boolean)
    fun onUpdateAudio(enable: Boolean)
    fun onRendererContainer(viewGroup: ViewGroup?, streamUuid: String)
}