package io.agora.agoraeducore.core.internal.rte

import android.content.Context
import androidx.annotation.NonNull
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration
import io.agora.agoraeducore.core.internal.rte.data.RteAudioReverbPreset
import io.agora.agoraeducore.core.internal.rte.data.RteAudioVoiceChanger
import io.agora.agoraeducore.core.internal.rte.listener.RteAudioMixingListener
import io.agora.agoraeducore.core.internal.rte.listener.RteChannelEventListener
import io.agora.agoraeducore.core.internal.rte.listener.RteStatisticsReportListener
import io.agora.rtc.internal.EncryptionConfig

interface IRteEngine {
    fun init(context: Context, appId: String, logFileDir: String, rtcRegion: String?,
             rtmRegion: String?)

    fun setRtcParameters(parameters: String): Int

    fun loginRtm(rtmUid: String, rtmToken: String, @NonNull callback: RteCallback<Unit>)

    fun logoutRtm()

    /**作用于rteChannel*/
    fun createChannel(channelId: String, eventListener: RteChannelEventListener,
                      mixingListener: RteAudioMixingListener): IRteChannel

    fun getRtcCallId(id: String): String

    fun getRtmSessionId(): String

    fun setLocalRenderMode(renderMode: Int, mirrorMode: Int): Int

    fun setRemoteRenderMode(channelId: String, uid: Int, renderMode: Int, mirrorMode: Int): Int

    /**作用于全局*/
    fun enableLocalMedia(audio: Boolean, video: Boolean): Int

    /**作用于rtcChannel*/
    fun setLatencyLevel(channelId: String, level: Int)

    /**作用于rtcChannel*/
    fun setClientRole(channelId: String, role: Int): Int

    fun setClientRole(role: Int): Int

    /**作用于rtcChannel*/
    fun publish(channelId: String): Int

    /**作用于rtcChannel*/
    fun unpublish(channelId: String): Int

    fun updateLocalAudioStream(channelId: String, hasAudio: Boolean): Int

    fun updateLocalVideoStream(channelId: String, hasVideo: Boolean): Int

    /**作用于rtcChannel*/
    fun muteRemoteStream(channelId: String, uid: Int, muteAudio: Boolean, muteVideo: Boolean): Int

    /**作用于全局*/
    fun muteLocalStream(channelId: String, muteAudio: Boolean, muteVideo: Boolean): Int

    fun muteLocalAudioStream(channelId: String, muteAudio: Boolean): Int

    fun muteLocalVideoStream(channelId: String, muteVideo: Boolean): Int

    fun setLocalRenderMode(mode: Int): Int

    fun startPreview(): Int

    fun stopPreview(): Int

    /**作用于全局*/
    fun setVideoEncoderConfiguration(config: VideoEncoderConfiguration): Int

    /**作用于全局*/
    fun enableVideo(): Int
    fun enableAudio(): Int
    fun disableVideo(): Int
    fun disableAudio(): Int

    fun enableLocalVideo(enabled: Boolean): Int
    fun enableLocalAudio(enabled: Boolean): Int

    /**作用于全局*/
    fun switchCamera(): Int

    /**作用于全局*/
    fun setupLocalVideo(local: VideoCanvas): Int
    fun setupRemoteVideo(local: VideoCanvas): Int

    fun setEnableSpeakerphone(enabled: Boolean): Int

    fun isSpeakerphoneEnabled(): Boolean

    /*AudioMixing*/
    fun startAudioMixing(filePath: String, loopback: Boolean, replace: Boolean, cycle: Int): Int

    fun setAudioMixingPosition(pos: Int): Int

    fun pauseAudioMixing(): Int

    fun resumeAudioMixing(): Int

    fun stopAudioMixing(): Int

    fun getAudioMixingDuration(): Int

    fun getAudioMixingCurrentPosition(): Int

    /*AudioEffect*/
    fun setLocalVoiceChanger(voiceManager: RteAudioVoiceChanger): Int

    fun setLocalVoiceReverbPreset(preset: RteAudioReverbPreset): Int

    /*MediaDevice*/
    fun enableInEarMonitoring(enabled: Boolean): Int

    fun enableAudioVolumeIndication(interval: Int, smooth: Int, report_vad: Boolean)

    fun setStatisticsReportListener(channelId: String, listener: RteStatisticsReportListener): Int

    fun setPrivateParam(value: String)

    fun setChannelMode(mode: Int)

    fun getError(code: Int): String

    fun OK(): Int

    fun version(): String

    fun enableEncryption(enabled: Boolean, config: EncryptionConfig): Int

    fun dispose()
}
