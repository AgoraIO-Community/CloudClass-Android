package io.agora.agoraeduuikit.impl.whiteboard

import com.herewhite.sdk.AudioMixerBridge

class AudioMixerBridgeImpl(val listener: WhiteBoardAudioMixingBridgeListener?) : AudioMixerBridge {
    private val dot = "."
    private val mp4Suffix = "mp4"
    private val mp3Suffix = "mp3"

    override fun startAudioMixing(filepath: String, loopback: Boolean, replace: Boolean, cycle: Int) {
        // if filePath endsWith mp4, need replace with mp3, because rtc only accept mp3 with startAudioMixing function.
        var newFilePath = filepath;
        if (filepath.endsWith(dot.plus(mp4Suffix))) {
            newFilePath = filepath.replaceAfterLast(dot, mp3Suffix)
        }
        listener?.onStartAudioMixing(newFilePath, loopback, replace, cycle)
    }

    override fun stopAudioMixing() {
        listener?.onStopAudioMixing()
    }

    override fun setAudioMixingPosition(position: Int) {
        listener?.onSetAudioMixingPosition(position)
    }

    override fun pauseAudioMixing() {
        listener?.pauseAudioMixing()
    }

    override fun resumeAudioMixing() {
        listener?.resumeAudioMixing()
    }
}