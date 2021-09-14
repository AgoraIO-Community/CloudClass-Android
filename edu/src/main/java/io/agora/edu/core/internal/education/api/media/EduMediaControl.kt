package io.agora.edu.core.internal.education.api.media

interface EduMediaControl {
    fun createCameraVideoTrack(): EduCameraVideoTrack
    fun createMicrophoneTrack(): EduMicrophoneAudioTrack
}