package io.agora.agoraeduuikit.impl.whiteboard

interface WhiteBoardAudioMixingBridgeListener {
    fun onStartAudioMixing(filepath: String, loopback: Boolean, replace: Boolean, cycle: Int)

    fun onStopAudioMixing()

    fun onSetAudioMixingPosition(position: Int)

    fun pauseAudioMixing()

    fun resumeAudioMixing()
}