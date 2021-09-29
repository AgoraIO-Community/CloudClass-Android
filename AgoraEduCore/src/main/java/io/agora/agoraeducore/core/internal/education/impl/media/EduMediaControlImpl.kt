package io.agora.agoraeducore.core.internal.education.impl.media

import io.agora.agoraeducore.core.internal.education.api.media.EduCameraVideoTrack
import io.agora.agoraeducore.core.internal.education.api.media.EduMediaControl
import io.agora.agoraeducore.core.internal.education.api.media.EduMicrophoneAudioTrack

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