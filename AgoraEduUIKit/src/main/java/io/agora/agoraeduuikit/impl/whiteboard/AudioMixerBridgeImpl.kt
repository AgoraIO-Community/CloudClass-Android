package io.agora.agoraeduuikit.impl.whiteboard

import com.herewhite.sdk.AudioMixerBridge

class AudioMixerBridgeImpl(val listener: WhiteBoardAudioMixingBridgeListener?) : AudioMixerBridge {
    override fun startAudioMixing(filepath: String, loopback: Boolean, replace: Boolean, cycle: Int) {
        listener?.onStartAudioMixing(filepath, loopback, replace, cycle)
    }

    override fun stopAudioMixing() {
        listener?.onStopAudioMixing()
    }

    override fun setAudioMixingPosition(position: Int) {
        listener?.onSetAudioMixingPosition(position)
    }
}