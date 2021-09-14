package io.agora.edu.core.internal.rte.listener

import io.agora.rtc.IRtcEngineEventHandler

interface RteSpeakerReportListener {
    fun onAudioVolumeIndicationOfLocalSpeaker(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?, totalVolume: Int)

    fun onAudioVolumeIndicationOfRemoteSpeaker(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?, totalVolume: Int)
}