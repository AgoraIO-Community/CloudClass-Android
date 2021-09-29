package io.agora.agoraeducore.core.internal.edu.classroom.widget.whiteboard

interface WhiteBoardAudioMixingBridgeListener {
    fun onStartAudioMixing(filepath: String, loopback: Boolean, replace: Boolean, cycle: Int)

    fun onStopAudioMixing()

    fun onSetAudioMixingPosition(position: Int)
}