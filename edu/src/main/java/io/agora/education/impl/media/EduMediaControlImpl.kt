package io.agora.education.impl.media

import io.agora.education.api.media.EduCameraVideoTrack
import io.agora.education.api.media.EduMediaControl
import io.agora.education.api.media.EduMicrophoneAudioTrack

internal class EduMediaControlImpl : EduMediaControl {

    private val cameraVideoTrack = EduCameraVideoTrackImpl()
    private val microphoneAudioTrack = EduMicrophoneAudioTrackImpl()

    override fun createCameraVideoTrack(): EduCameraVideoTrack {
        return cameraVideoTrack
    }

    override fun createMicrophoneTrack(): EduMicrophoneAudioTrack {
        return microphoneAudioTrack
    }
}