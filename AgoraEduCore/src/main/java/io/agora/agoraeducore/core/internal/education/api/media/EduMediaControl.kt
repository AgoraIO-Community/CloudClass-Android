package io.agora.agoraeducore.core.internal.education.api.media

interface EduMediaControl {
    fun createCameraVideoTrack(): EduCameraVideoTrack
    fun createMicrophoneTrack(): EduMicrophoneAudioTrack
}