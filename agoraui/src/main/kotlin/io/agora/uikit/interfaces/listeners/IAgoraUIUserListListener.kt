package io.agora.uikit.interfaces.listeners

import android.view.ViewGroup

interface IAgoraUIUserListListener {
    // student
    fun onMuteVideo(mute:Boolean)
    fun onMuteAudio(mute:Boolean)
    fun onRendererContainer(viewGroup: ViewGroup?, streamUuid:String)
}