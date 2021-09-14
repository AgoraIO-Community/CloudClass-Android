package io.agora.edu.core.internal.rte

import android.content.Context
import android.util.Log
import io.agora.edu.core.internal.education.api.stream.data.EduLatencyLevel
import io.agora.edu.core.internal.education.impl.Constants.Companion.AgoraLog
import io.agora.edu.core.internal.framework.data.EduCallback
import io.agora.edu.core.internal.framework.data.EduError
import io.agora.edu.core.internal.report.ReportManager
import io.agora.edu.core.internal.rte.RteEngineImpl.getRtmSessionId
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration
import io.agora.edu.core.internal.rte.data.*
import io.agora.edu.core.internal.rte.data.RteError.Companion.rtmError
import io.agora.edu.core.internal.rte.data.RteRegion.rtcRegion
import io.agora.edu.core.internal.rte.data.RteRegion.rtmRegion
import io.agora.edu.core.internal.rte.listener.*
import io.agora.rtc.Constants.*
import io.agora.rtc.RtcEngineConfig
import io.agora.rtc.internal.EncryptionConfig
import io.agora.rtc.models.ClientRoleOptions
import io.agora.rtm.*
import io.agora.rtm.RtmStatusCode.LoginError.LOGIN_ERR_ALREADY_LOGIN
import io.agora.rtm.internal.RtmManager
import java.io.File

object RteEngineImpl : IRteEngine, IRtmServerDelegate {
    private val tag = RteEngineImpl::javaClass.name

    var eventListener: RteEngineEventListener? = null
    var mediaDeviceListener: RteMediaDeviceListener? = null
    var speakerReportListener: RteSpeakerReportListener? = null

    internal lateinit var rtmClient: RtmClient
    internal lateinit var rtcEngine: RtcEngine
    private val channelMap = mutableMapOf<String, IRteChannel>()
    private val rteChannelEventListenerMap = mutableMapOf<String, RteChannelEventListener>()
    private val rteAudioMixingListenerMap = mutableMapOf<String, RteAudioMixingListener>()
    private val channelLatencyMap = mutableMapOf<String, Int>()
    private var rtmLoginSuccess = false

    @Synchronized
    private fun setRtmLoginSuccess(success: Boolean) {
        this.rtmLoginSuccess = success
    }

    @Synchronized
    private fun rtmLoginSuccess(): Boolean {
        return this.rtmLoginSuccess
    }

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
            Log.i(tag, "onAudioRouteChanged->routing->$routing")
            mediaDeviceListener?.onAudioRouteChanged(routing)
        }

        override fun onAudioMixingFinished() {
            super.onAudioMixingFinished()
            rteAudioMixingListenerMap.forEach {
                it.value.onAudioMixingFinished()
            }
        }

        override fun onAudioMixingStateChanged(state: Int, errorCode: Int) {
            super.onAudioMixingStateChanged(state, errorCode)
            rteAudioMixingListenerMap.forEach {
                it.value.onAudioMixingStateChanged(state, errorCode)
            }
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
            val state = RteLocalVideoState.convert(localVideoState)
            val err = RteLocalVideoError.convert(error)
            rteChannelEventListenerMap.forEach {
                it.value.onLocalVideoStateChanged(state, err)
            }
        }

        override fun onLocalAudioStateChanged(localAudioState: Int, error: Int) {
            super.onLocalAudioStateChanged(localAudioState, error)
            val state = RteLocalAudioState.convert(localAudioState)
            val err = RteLocalAudioError.convert(error)
            rteChannelEventListenerMap.forEach {
                it.value.onLocalAudioStateChanged(state, err)
            }
        }
    }

    override fun init(context: Context, appId: String, logFileDir: String, rtcRegion: String?,
                      rtmRegion: String?) {
        Log.i(tag, "init")
        var path = logFileDir.plus(File.separatorChar).plus("agorartm.log")
        val serviceContext = RtmServiceContext()
        serviceContext.areaCode = rtmRegion(rtmRegion)
        val code = RtmClient.setRtmServiceContext(serviceContext)
        rtmClient = RtmClient.createInstance(context, appId, rtmClientListener)
        rtmClient.setLogFile(path)

        path = logFileDir.plus(File.separatorChar).plus("agorasdk.log")

        val config = RtcEngineConfig()
        config.mContext = context
        config.mAppId = appId
        config.mAreaCode = rtcRegion(rtcRegion)
        config.mEventHandler = rtcEngineEventHandler
        rtcEngine = RtcEngine.create(config)
        rtcEngine.setChannelProfile(CHANNEL_PROFILE_LIVE_BROADCASTING)
        rtcEngine.setLogFile(path)
        enableVideo()

        rtcEngine.setParameters("{\"che.video.h264ProfileNegotiated\": 66}")
        rtcEngine.setParameters("{\"che.video.web_h264_interop_enable\": true}")
    }

    override fun setRtcParameters(parameters: String): Int {
        return rtcEngine.setParameters(parameters)
    }

    override fun loginRtm(rtmUid: String, rtmToken: String, callback: RteCallback<Unit>) {
        val reporter = ReportManager.getRteReporter()
        reporter.reportRtmLoginStart()
        rtmClient.login(rtmToken, rtmUid, object : ResultCallback<Void> {
            override fun onSuccess(p0: Void?) {
                setRtmLoginSuccess(true)
                callback.onSuccess(Unit)
                reporter.reportRtmLoginResult("1", null, null)
            }

            override fun onFailure(p0: ErrorInfo?) {
                setRtmLoginSuccess(false)
                if (p0?.errorCode == LOGIN_ERR_ALREADY_LOGIN) {
                    callback.onSuccess(Unit)
                } else {
                    // release,Otherwise, rtmClient.setrtmservicecontext will fail
                    rtmClient.release()
                    callback.onFailure(rtmError(p0 ?: ErrorInfo(-1)))
                    reporter.reportRtmLoginResult("0", p0?.errorCode?.toString(), null)
                }
            }
        })
    }

    override fun logoutRtm() {
        rtmClient.logout(object : ResultCallback<Void> {
            override fun onSuccess(p0: Void?) {
                setRtmLoginSuccess(false)
                Log.i(tag, "rtm logout success")
            }

            override fun onFailure(p0: ErrorInfo?) {
                Log.i(tag, "rmt logout fail:${p0?.errorDescription}")
                if (p0?.errorCode == RtmStatusCode.LeaveChannelError.LEAVE_CHANNEL_ERR_USER_NOT_LOGGED_IN) {
                    setRtmLoginSuccess(false)
                }
            }
        })
    }

    override fun createChannel(channelId: String, eventListener: RteChannelEventListener,
                               mixingListener: RteAudioMixingListener): IRteChannel {
        val rteChannel = RteChannelImpl(channelId, eventListener)
        channelMap[channelId] = rteChannel
        rteChannelEventListenerMap[channelId] = eventListener
        rteAudioMixingListenerMap[channelId] = mixingListener
        return rteChannel
    }

    override fun getRtcCallId(id: String): String {
        return channelMap[id]?.getRtcCallId() ?: ""
    }

    override fun getRtmSessionId(): String {
        return RtmManager.getRtmSessionId(rtmClient)
    }

    override fun setLocalRenderMode(renderMode: Int, mirrorMode: Int): Int {
        return rtcEngine.setLocalRenderMode(renderMode, mirrorMode)
    }

    override fun setRemoteRenderMode(channelId: String, uid: Int, renderMode: Int, mirrorMode: Int): Int {
        if (channelMap.isNotEmpty()) {
            return (channelMap[channelId] as RteChannelImpl).rtcChannel.setRemoteRenderMode(uid,
                    renderMode, mirrorMode)
        }
        return -1
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

    override fun setLatencyLevel(channelId: String, level: Int) {
        channelLatencyMap[channelId] = level
    }

    override fun setClientRole(channelId: String, role: Int): Int {
        if (channelMap.isEmpty()) {
            AgoraLog.e("$tag:channelMap is Empty, failed to set client role->$role")
            return -1
        }
        val options = ClientRoleOptions()
        var code: Int
        if (role == CLIENT_ROLE_AUDIENCE) {
            options.audienceLatencyLevel = channelLatencyMap[channelId]
                    ?: EduLatencyLevel.EduLatencyLevelUltraLow.value
            code = (channelMap[channelId] as RteChannelImpl).rtcChannel.setClientRole(role,
                    options)
        } else {
            code = (channelMap[channelId] as RteChannelImpl).rtcChannel.setClientRole(role)
        }
        if (code == 0) {
            AgoraLog.e("$tag:set client role success to:$role")
        } else {
            AgoraLog.e("$tag:failed to set client role->$role")
        }
        return code
    }

    /** at the moment, only used by preview */
    override fun setClientRole(role: Int): Int {
        val code = rtcEngine.setClientRole(role)
        if (code == 0) {
            Log.e(tag, "rtcEngine set client role success to:$role")
        }
        return code
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

    override fun updateLocalAudioStream(channelId: String, hasAudio: Boolean): Int {
        val a = rtcEngine.enableLocalAudio(hasAudio)
        if (a != ERR_OK) {
            return a
        }

        val c = (channelMap[channelId] as RteChannelImpl).rtcChannel.muteLocalAudioStream(!hasAudio)
        if (c != ERR_OK) {
            return c
        }
        return ERR_OK
    }

    override fun updateLocalVideoStream(channelId: String, hasVideo: Boolean): Int {

        val b = rtcEngine.enableLocalVideo(hasVideo)
        if (b != ERR_OK) {
            return b
        }

        val d = (channelMap[channelId] as RteChannelImpl).rtcChannel.muteLocalVideoStream(!hasVideo)
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

    override fun muteLocalStream(channelId: String, muteAudio: Boolean, muteVideo: Boolean): Int {
        val code0 = (channelMap[channelId] as RteChannelImpl).rtcChannel.muteLocalAudioStream(muteAudio)
        val code1 = (channelMap[channelId] as RteChannelImpl).rtcChannel.muteLocalVideoStream(muteVideo)
        return if (code0 == ERR_OK && code1 == ERR_OK) ERR_OK else -1
    }

    override fun muteLocalAudioStream(channelId: String, muteAudio: Boolean): Int {
        return (channelMap[channelId] as RteChannelImpl).rtcChannel.muteLocalAudioStream(muteAudio)
    }

    override fun muteLocalVideoStream(channelId: String, muteVideo: Boolean): Int {
        return (channelMap[channelId] as RteChannelImpl).rtcChannel.muteLocalVideoStream(muteVideo)
    }

    override fun setLocalRenderMode(mode: Int): Int {
        return rtcEngine.setLocalRenderMode(mode)
    }

    override fun startPreview(): Int {
        return rtcEngine.startPreview()
    }

    override fun stopPreview(): Int {
        return rtcEngine.stopPreview()
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

    override fun disableVideo(): Int {
        return rtcEngine.disableVideo()
    }

    override fun disableAudio(): Int {
        return rtcEngine.disableAudio()
    }

    override fun enableLocalVideo(enabled: Boolean): Int {
        return rtcEngine.enableLocalVideo(enabled)
    }

    override fun enableLocalAudio(enabled: Boolean): Int {
        return rtcEngine.enableLocalAudio(enabled)
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

    override fun setEnableSpeakerphone(enabled: Boolean): Int {
        return rtcEngine.setEnableSpeakerphone(enabled)
    }

    override fun isSpeakerphoneEnabled(): Boolean {
        return rtcEngine.isSpeakerphoneEnabled
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

    override fun setPrivateParam(value: String) {
        rtcEngine.setParameters(value)
    }

    override fun setChannelMode(mode: Int) {
        rtcEngine.setChannelProfile(mode)
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

    override fun enableEncryption(enabled: Boolean, config: EncryptionConfig): Int {
        return rtcEngine.enableEncryption(enabled, config)
    }

    override fun dispose() {
        rtmClient.release()
        RtcEngine.destroy()
        channelMap.clear()
        rteChannelEventListenerMap.clear()
    }

    override fun sendRtmServerRequest(text: String, peerId: String,
                                      callback: EduCallback<Void>?): RtmServerRequestResult {
        if (!rtmLoginSuccess()) {
            return RtmServerRequestResult.RtmNotLogin
        }

        val options = SendMessageOptions()
        options.enableOfflineMessaging = false
        options.enableHistoricalMessaging = false

        rtmClient.sendMessageToPeer(peerId, rtmClient.createMessage(text),
                options, object : ResultCallback<Void> {
            override fun onSuccess(p0: Void?) {
                callback?.onSuccess(p0)
            }

            override fun onFailure(p0: ErrorInfo?) {
                callback?.onFailure(
                        if (p0 != null) EduError(p0.errorCode, p0.errorDescription)
                        else EduError(-1, ""))
            }
        })

        return RtmServerRequestResult.Success
    }

    override fun rtmServerPeerOnlineStatus(serverIdList: List<String>, callback: EduCallback<Map<String, Boolean>>?) {
        if (rtmLoginSuccess()) {
            rtmClient.queryPeersOnlineStatus(serverIdList.toSet(), object : ResultCallback<Map<String, Boolean>> {
                override fun onSuccess(p0: Map<String, Boolean>?) {
                    p0?.let {
                        callback?.onSuccess(it)
                    }
                }

                override fun onFailure(p0: ErrorInfo?) {
                    callback?.onFailure(EduError(p0?.errorCode ?: -1, p0?.errorDescription ?: ""))
                }
            })
        }
    }
}
