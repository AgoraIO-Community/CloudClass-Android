package io.agora.education.api.media

interface EduMediaControl {
    fun createCameraVideoTrack(): EduCameraVideoTrack
    fun createMicrophoneTrack(): EduMicrophoneAudioTrack
}