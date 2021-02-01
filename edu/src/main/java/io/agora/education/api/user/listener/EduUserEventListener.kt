package io.agora.education.api.user.listener

import io.agora.education.api.stream.data.EduStreamEvent
import io.agora.education.api.user.data.EduUserEvent
import io.agora.education.api.user.data.EduUserLeftType
import io.agora.education.api.user.data.EduUserStateChangeType
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcChannel

interface EduUserEventListener {

    fun onRemoteVideoStateChanged(rtcChannel: RtcChannel?, uid: Int, state: Int, reason: Int, elapsed: Int)

    fun onLocalVideoStateChanged(localVideoState: Int, error: Int)

    fun onAudioVolumeIndicationOfLocalSpeaker(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?, totalVolume: Int)

    fun onAudioVolumeIndicationOfRemoteSpeaker(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?, totalVolume: Int)

    fun onLocalUserUpdated(userEvent: EduUserEvent, type: EduUserStateChangeType)

    fun onLocalStreamAdded(streamEvent: EduStreamEvent)

    fun onLocalStreamUpdated(streamEvent: EduStreamEvent)

    fun onLocalStreamRemoved(streamEvent: EduStreamEvent)

    fun onLocalUserLeft(userEvent: EduUserEvent, leftType: EduUserLeftType)
}