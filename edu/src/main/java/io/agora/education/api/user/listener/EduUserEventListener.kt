package io.agora.education.api.user.listener

import io.agora.education.api.stream.data.EduStreamEvent
import io.agora.education.api.user.data.*
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcChannel
import io.agora.rte.data.RteLocalVideoStats
import io.agora.rte.data.RteRemoteVideoStats

interface EduUserEventListener {

    fun onRemoteVideoStateChanged(rtcChannel: RtcChannel?, uid: Int, state: Int, reason: Int, elapsed: Int)

    fun onRemoteAudioStateChanged(rtcChannel: RtcChannel?, uid: Int, state: Int, reason: Int, elapsed: Int)

    fun onRemoteVideoStats(stats: RteRemoteVideoStats)

    fun onLocalVideoStateChanged(localVideoState: Int, error: Int)

    fun onLocalAudioStateChanged(localAudioState: Int, error: Int)

    fun onLocalVideoStats(stats: RteLocalVideoStats)

    fun onAudioVolumeIndicationOfLocalSpeaker(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?, totalVolume: Int)

    fun onAudioVolumeIndicationOfRemoteSpeaker(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?, totalVolume: Int)

    fun onLocalUserUpdated(userEvent: EduUserEvent, type: EduUserStateChangeType)

    fun onLocalStreamAdded(streamEvent: EduStreamEvent)

    fun onLocalStreamUpdated(streamEvent: EduStreamEvent)

    fun onLocalStreamRemoved(streamEvent: EduStreamEvent)

    fun onLocalUserLeft(userEvent: EduUserEvent, leftType: EduUserLeftType)

    fun onLocalUserPropertiesChanged(userInfo: EduUserInfo, cause: MutableMap<String, Any>?,
                                     operator: EduBaseUserInfo?)
}