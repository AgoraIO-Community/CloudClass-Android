package io.agora.rte

import android.content.Context
import android.util.Log
import io.agora.report.ReportManager
import io.agora.rtc.Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
import io.agora.rtc.Constants.ERR_OK
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration
import io.agora.rte.data.*
import io.agora.rte.data.RteError.Companion.rtmError
import io.agora.rte.listener.*
import io.agora.rtm.*
import io.agora.rtm.RtmStatusCode.LoginError.LOGIN_ERR_ALREADY_LOGIN
import java.io.File

object RteEngineImpl : IRteEngine {
    private val tag = RteEngineImpl::javaClass.name

    internal lateinit var rtmClient: RtmClient
    internal lateinit var rtcEngine: RtcEngine
    private val channelMap = mutableMapOf<String, IRteChannel>()
    private val rteChannelEventListenerMap = mutableMapOf<String, RteChannelEventListener>()

    var eventListener: RteEngineEventListener? = null
    var mediaDeviceListener: RteMediaDeviceListener? = null
    var audioMixingListener: RteAudioMixingListener? = null
    var speakerReportListener: RteSpeakerReportListener? = null

    var rtmLoginSuccess = false

    private val rtmClientListener = object : RtmClientListener {
        override fun onTokenExpired() {
        }

        override fun onPeersOnlineStatusChanged(p0: MutableMap<String, Int>?) {
        }

        override fun onConnectionStateChanged(p0: Int, p1: Int) {
            eventListener?.onConnectionStateChanged(p0, p1)
        }

        override fun onMessageReceived(p0: RtmMessage?, p1: String?) {
            eventListener?.onPeerMsgReceived(p0, p1)
        }

        override fun onMediaDownloadingProgress(p0: RtmMediaOperationProgress?, p1: Long) {
        }

        override fun onMediaUploadingProgress(p0: RtmMediaOperationProgress?, p1: Long) {
        }

        override fun onImageMessageReceivedFromPeer(p0: RtmImageMessage?, p1: String?) {
        }

        override fun onFileMessageReceivedFromPeer(p0: RtmFileMessage?, p1: String?) {
        }
    }

    private val rtcEngineEventHandler = object : IRtcEngineEventHandler() {

        override fun onError(err: Int) {
            Log.i(tag, String.format("onError code %d message %s", err, RtcEngine.getErrorDescription(err)))
        }

        override fun onWarning(warn: Int) {
            super.onWarning(warn)
            Log.i(tag, String.format("onWarning code %d message %s", warn, RtcEngine.getErrorDescription(warn)));
        }

        override fun onClientRoleChanged(oldRole: Int, newRole: Int) {
            super.onClientRoleChanged(oldRole, newRole)
            Log.i(tag, "onClientRoleChanged, $oldRole, $newRole")
        }

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            Log.i(tag, String.format("onJoinChannelSuccess channel %s uid %d", channel, uid))
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            super.onUserJoined(uid, elapsed)
            Log.i(tag, "onUserJoined->$uid")
        }

        override fun onRemoteVideoStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            super.onRemoteVideoStateChanged(uid, state, reason, elapsed)
            Log.i(tag, "onRemoteVideoStateChanged->$uid, state->$state, reason->$reason")
        }

        override fun onAudioRouteChanged(routing: Int) {
            super.onAudioRouteChanged(routing)
            mediaDeviceListener?.onAudioRouteChanged(routing)
        }

        override fun onAudioMixingFinished() {
            super.onAudioMixingFinished()
            audioMixingListener?.onAudioMixingFinished()
        }

        override fun onAudioMixingStateChanged(state: Int, errorCode: Int) {
            super.onAudioMixingStateChanged(state, errorCode)
            audioMixingListener?.onAudioMixingStateChanged(state, errorCode)
        }

        override fun onAudioVolumeIndication(speakers: Array<out AudioVolumeInfo>?, totalVolume: Int) {
            super.onAudioVolumeIndication(speakers, totalVolume)
            speakers?.let {
                if (speakers.size == 1 && speakers[0].uid == 0) {
                    speakerReportListener?.onAudioVolumeIndicationOfLocalSpeaker(speakers, totalVolume)
                    rteChannelEventListenerMap.forEach {
                        it.value.onAudioVolumeIndicationOfLocalSpeaker(speakers, totalVolume)
                    }
                } else {
                    speakerReportListener?.onAudioVolumeIndicationOfRemoteSpeaker(speakers, totalVolume)
                    rteChannelEventListenerMap.forEach {
                        it.value.onAudioVolumeIndicationOfRemoteSpeaker(speakers, totalVolume)
                    }
                }
            }
        }

        override fun onLocalVideoStats(stats: LocalVideoStats?) {
            super.onLocalVideoStats(stats)
            stats?.let { sta ->
                val rteLocalVideoStats = RteLocalVideoStats.convert(sta)
                rteChannelEventListenerMap.forEach {
                    it.value.onLocalVideoStats(rteLocalVideoStats)
                }
            }
        }

        override fun onLocalVideoStateChanged(localVideoState: Int, error: Int) {
            super.onLocalVideoStateChanged(localVideoState, error)
            rteChannelEventListenerMap.forEach {
                val state = RteLocalVideoState.convert(localVideoState)
                val err = RteLocalVideoError.convert(error)
                it.value.onLocalVideoStateChanged(state, err)
            }
        }


    }

    override fun init(context: Context, appId: String, logFileDir: String) {
        Log.i(tag, "init")
        var path = logFileDir.plus(File.separatorChar).plus("agorartm.log")
        rtmClient = RtmClient.createInstance(context, appId, rtmClientListener)
        rtmClient.setLogFile(path)

        path = logFileDir.plus(File.separatorChar).plus("agorasdk.log")
        rtcEngine = RtcEngine.create(context, appId, rtcEngineEventHandler)
        rtcEngine.setChannelProfile(CHANNEL_PROFILE_LIVE_BROADCASTING)
        rtcEngine.setLogFile(path)

        rtcEngine.setParameters("\"rtc.report_app_scenario\": {\"appScenario\":0, \"serviceType\":0, \"appVersion\":\"1.1.0-offiicial\"}")
        rtcEngine.setParameters("{\"che.video.h264ProfileNegotiated\": 66}")
        rtcEngine.setParameters("{\"che.video.web_h264_interop_enable\": true}")
    }

    override fun loginRtm(rtmUid: String, rtmToken: String, callback: RteCallback<Unit>) {
        val reporter = ReportManager.getRteReporter()
        reporter.reportRtmLoginStart()
        rtmClient.login(rtmToken, rtmUid, object : ResultCallback<Void> {
            override fun onSuccess(p0: Void?) {
                rtmLoginSuccess = true
                callback.onSuccess(Unit)
                reporter.reportRtmLoginResult("1", null, null)
            }

            override fun onFailure(p0: ErrorInfo?) {
                rtmLoginSuccess = false
                if (p0?.errorCode == LOGIN_ERR_ALREADY_LOGIN) {
                    callback.onSuccess(Unit)
                } else {
                    callback.onFailure(rtmError(p0 ?: ErrorInfo(-1)))
                    reporter.reportRtmLoginResult("0", p0?.errorCode?.toString(), null)
                }
            }
        })
    }

    override fun logoutRtm() {
        rtmClient.logout(object : ResultCallback<Void> {
            override fun onSuccess(p0: Void?) {
                rtmLoginSuccess = false
                Log.i(tag, "rtm logout success")
            }

            override fun onFailure(p0: ErrorInfo?) {
                Log.i(tag, "rmt logout fail:${p0?.errorDescription}")
                if (p0?.errorCode == RtmStatusCode.LeaveChannelError.LEAVE_CHANNEL_ERR_USER_NOT_LOGGED_IN) {
                    rtmLoginSuccess = false
                }
            }
        })
    }

    override fun createChannel(channelId: String, eventListener: RteChannelEventListener): IRteChannel {
        val rteChannel = RteChannelImpl(channelId, eventListener)
        channelMap[channelId] = rteChannel
        rteChannelEventListenerMap[channelId] = eventListener
        return rteChannel
    }

    override fun enableLocalMedia(audio: Boolean, video: Boolean): Int {
        val a = rtcEngine.enableLocalAudio(audio)
        if (a != ERR_OK) {
            return a
        }
        val b = rtcEngine.enableLocalVideo(video)
        if (b != ERR_OK) {
            return b
        }
        return ERR_OK
    }

    operator fun get(channelId: String): IRteChannel? {
        return channelMap[channelId]
    }

    override fun setClientRole(channelId: String, role: Int): Int {
        if (channelMap.isNotEmpty()) {
            val code = (channelMap[channelId] as RteChannelImpl).rtcChannel.setClientRole(role)
            if (code == 0) {
                Log.e(tag, "set client role success to:$role")
            }
            return code
        }
        return -1
    }

    override fun publish(channelId: String): Int {
        if (channelMap.isNotEmpty()) {
            return (channelMap[channelId] as RteChannelImpl).rtcChannel.publish()
        }
        return -1
    }

    override fun unpublish(channelId: String): Int {
        if (channelMap.isNotEmpty()) {
            return (channelMap[channelId] as RteChannelImpl).rtcChannel.unpublish()
        }
        return -1
    }

    override fun updateLocalStream(hasAudio: Boolean, hasVideo: Boolean): Int {
        val a = rtcEngine.enableLocalAudio(hasAudio)
        if (a != ERR_OK) {
            return a
        }
        val b = rtcEngine.enableLocalVideo(hasVideo)
        if (b != ERR_OK) {
            return b
        }
        val c = rtcEngine.muteLocalAudioStream(!hasAudio)
        if (c != ERR_OK) {
            return c
        }
        val d = rtcEngine.muteLocalVideoStream(!hasVideo)
        if (d != ERR_OK) {
            return d
        }
        return ERR_OK
    }

    override fun muteRemoteStream(channelId: String, uid: Int, muteAudio: Boolean, muteVideo: Boolean): Int {
        if (channelMap.isNotEmpty()) {
            val channel = (channelMap[channelId] as RteChannelImpl).rtcChannel
            val code0 = channel.muteRemoteAudioStream(uid, muteAudio)
            val code1 = channel.muteRemoteVideoStream(uid, muteVideo)
            return if (code0 == ERR_OK && code1 == ERR_OK) ERR_OK else -1
        }
        return -1
    }

    override fun muteLocalStream(muteAudio: Boolean, muteVideo: Boolean): Int {
        val code0 = rtcEngine.muteLocalAudioStream(muteAudio)
        val code1 = rtcEngine.muteLocalVideoStream(muteVideo)
        return if (code0 == ERR_OK && code1 == ERR_OK) ERR_OK else -1
    }

    override fun setVideoEncoderConfiguration(config: VideoEncoderConfiguration): Int {
        return rtcEngine.setVideoEncoderConfiguration(config)
    }

    override fun enableVideo(): Int {
        return rtcEngine.enableVideo()
    }

    override fun enableAudio(): Int {
        return rtcEngine.enableAudio()
    }

    override fun switchCamera(): Int {
        return rtcEngine.switchCamera()
    }

    override fun setupLocalVideo(local: VideoCanvas): Int {
        return rtcEngine.setupLocalVideo(local)
    }

    override fun setupRemoteVideo(local: VideoCanvas): Int {
        return rtcEngine.setupRemoteVideo(local)
    }

    override fun startAudioMixing(filePath: String, loopback: Boolean, replace: Boolean, cycle: Int): Int {
        return rtcEngine.startAudioMixing(filePath, loopback, replace, cycle)
    }

    override fun setAudioMixingPosition(pos: Int): Int {
        return rtcEngine.setAudioMixingPosition(pos)
    }

    override fun pauseAudioMixing(): Int {
        return rtcEngine.pauseAudioMixing()
    }

    override fun resumeAudioMixing(): Int {
        return rtcEngine.resumeAudioMixing()
    }

    override fun stopAudioMixing(): Int {
        return rtcEngine.stopAudioMixing()
    }

    override fun getAudioMixingDuration(): Int {
        return rtcEngine.audioMixingDuration
    }

    override fun getAudioMixingCurrentPosition(): Int {
        return rtcEngine.audioMixingCurrentPosition
    }

    override fun setLocalVoiceChanger(voiceManager: RteAudioVoiceChanger): Int {
        return rtcEngine.setLocalVoiceChanger(voiceManager.value)
    }

    override fun setLocalVoiceReverbPreset(preset: RteAudioReverbPreset): Int {
        return rtcEngine.setLocalVoiceReverbPreset(preset.value)
    }

    override fun enableInEarMonitoring(enabled: Boolean): Int {
        return rtcEngine.enableInEarMonitoring(enabled)
    }

    override fun enableAudioVolumeIndication(interval: Int, smooth: Int, report_vad: Boolean) {
        rtcEngine.enableAudioVolumeIndication(interval, smooth, report_vad)
    }

    override fun setStatisticsReportListener(channelId: String, listener: RteStatisticsReportListener): Int {
        if (channelMap.isNotEmpty()) {
            val channel = channelMap[channelId] as RteChannelImpl
            channel.statisticsReportListener = listener
            return 0
        }
        return -1
    }

    override fun getError(code: Int): String {
        return RtcEngine.getErrorDescription(code)
    }

    override fun OK(): Int {
        return ERR_OK
    }

    override fun version(): String {
        return RtcEngine.getSdkVersion()
    }

    override fun dispose() {
        rtmClient.release()
        RtcEngine.destroy()
        channelMap.clear()
        rteChannelEventListenerMap.clear()
    }
}
