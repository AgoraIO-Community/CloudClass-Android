package io.agora.edu.uikit.interfaces.listeners

import android.view.ViewGroup

interface IAgoraUIVideoListener {
    fun onUpdateVideo(enable: Boolean)
    fun onUpdateAudio(enable: Boolean)
    fun onRendererContainer(viewGroup: ViewGroup?, streamUuid: String)
}