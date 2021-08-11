package io.agora.education.api.media

import android.view.ViewGroup
import io.agora.education.api.stream.data.EduRenderConfig
import io.agora.education.api.stream.data.EduVideoEncoderConfig

interface EduMediaTrack {
    fun start(): Int
    fun stop(): Int
}

interface EduCameraVideoTrack : EduMediaTrack {
    fun switchCamera(): Int
    fun setView(container: ViewGroup?): Int
    fun setRenderConfig(config: EduRenderConfig): Int
    fun setVideoEncoderConfig(configEdu: EduVideoEncoderConfig): Int
}

interface EduMicrophoneAudioTrack : EduMediaTrack {
}