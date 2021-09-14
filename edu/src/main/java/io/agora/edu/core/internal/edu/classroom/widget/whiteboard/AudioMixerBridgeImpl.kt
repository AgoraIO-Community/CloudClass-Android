package io.agora.edu.core.internal.edu.classroom.widget.whiteboard

import com.herewhite.sdk.AudioMixerBridge
import io.agora.rtc.RtcEngine

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