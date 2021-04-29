package io.agora.rte

import android.util.Log
import androidx.annotation.NonNull
import io.agora.report.ReportManager
import io.agora.rtc.Constants.ERR_OK
import io.agora.rtc.IRtcChannelEventHandler
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcChannel
import io.agora.rtc.models.ChannelMediaOptions
import io.agora.rte.RteEngineImpl.OK
import io.agora.rte.data.RteError.Companion.rtcError
import io.agora.rte.data.RteError.Companion.rtmError
import io.agora.rte.data.RteRemoteVideoState
import io.agora.rte.data.RteRemoteVideoStateChangeReason
import io.agora.rte.data.RteRemoteVideoStats
import io.agora.rte.listener.RteChannelEventListener
import io.agora.rte.listener.RteStatisticsReportListener
import io.agora.rtm.*
import io.agora.rtm.RtmStatusCode.JoinChannelError.JOIN_CHANNEL_ERR_ALREADY_JOINED
import java.util.*


internal class RteChannelImpl(
        channelId: String,
        private var eventListener: RteChannelEventListener?
) : IRteChannel {

    private val tag = RteChannelImpl::class.java.simpleName
    internal var statisticsReportListener: RteStatisticsReportListener? = null
    private val rtmChannelListener = object : RtmChannelListener {
        override fun onAttributesUpdated(p0: MutableList<RtmChannelAttribute>?) {

        }

        override fun onMessageReceived(p0: RtmMessage?, p1: RtmChannelMember?) {
            Log.e(tag, "Receive channel ${p1?.channelId} message->${p0?.text}")
            eventListener?.onChannelMsgReceived(p0, p1)
        }

        override fun onMemberJoined(p0: RtmChannelMember?) {

        }

        override fun onMemberLeft(p0: RtmChannelMember?) {

        }

        override fun onMemberCountUpdated(p0: Int) {

        }

        override fun onFileMessageReceived(p0: RtmFileMessage?, p1: RtmChannelMember?) {
        }

        override fun onImageMessageReceived(p0: RtmImageMessage?, p1: RtmChannelMember?) {
        }
    }

    private val rtcChannelEventHandler = object : IRtcChannelEventHandler() {
        override fun onChannelError(rtcChannel: RtcChannel?, err: Int) {
            super.onChannelError(rtcChannel, err)
            Log.e(tag, "onChannelError->" + rtcChannel?.channelId() + ",err->" + err)
        }

        override fun onChannelWarning(rtcChannel: RtcChannel?, warn: Int) {
            super.onChannelWarning(rtcChannel, warn)
            Log.e(tag, "onChannelWarning->" + rtcChannel?.channelId() + ",warn->" + warn)
        }

        override fun onNetworkQuality(rtcChannel: RtcChannel?, uid: Int, txQuality: Int, rxQuality: Int) {
            super.onNetworkQuality(rtcChannel, uid, txQuality, rxQuality)
            eventListener?.onNetworkQuality(uid, txQuality, rxQuality)
        }

        override fun onClientRoleChanged(rtcChannel: RtcChannel?, oldRole: Int, newRole: Int) {
            super.onClientRoleChanged(rtcChannel, oldRole, newRole)
            Log.e(tag, rtcChannel?.channelId() + "," + oldRole + "," + newRole)
        }

        override fun onJoinChannelSuccess(rtcChannel: RtcChannel?, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(rtcChannel, uid, elapsed)
            Log.e(tag, String.format("onJoinChannelSuccess channel $rtcChannel uid $uid"))
            ReportManager.getRteReporter().reportRtcJoinResult("1", null, null)
//            timer?.schedule(rteRemoteVideoStatsCallbackTask, interval, interval)
        }

        override fun onUserJoined(rtcChannel: RtcChannel?, uid: Int, elapsed: Int) {
            super.onUserJoined(rtcChannel, uid, elapsed)
            Log.e(tag, "onUserJoined->$uid")
            rteRemoteVideoStatsMap[uid] = RteRemoteVideoStats.convert(IRtcEngineEventHandler.RemoteVideoStats())
        }

        override fun onUserOffline(rtcChannel: RtcChannel?, uid: Int, reason: Int) {
            super.onUserOffline(rtcChannel, uid, reason)
            rteRemoteVideoStatsMap.remove(uid)
        }

        override fun onRemoteVideoStateChanged(rtcChannel: RtcChannel?, uid: Int, state: Int, reason: Int, elapsed: Int) {
            super.onRemoteVideoStateChanged(rtcChannel, uid, state, reason, elapsed)
            rteRemoteVideoStatsMap.forEach {
                if (it.key == uid) {
                    rteRemoteVideoStatsMap[it.key] = RteRemoteVideoStats.convert(IRtcEngineEventHandler.RemoteVideoStats())
                }
            }
            Log.e(tag, "onRemoteVideoStateChanged->$uid, state->$state, reason->$reason")
            val videoState = RteRemoteVideoState.convert(state)
            val changeReason = RteRemoteVideoStateChangeReason.convert(reason)
            eventListener?.onRemoteVideoStateChanged(rtcChannel, uid, videoState, changeReason, elapsed)
        }

        override fun onRemoteVideoStats(rtcChannel: RtcChannel?, stats: IRtcEngineEventHandler.RemoteVideoStats?) {
            super.onRemoteVideoStats(rtcChannel, stats)
            stats?.let {
                rteRemoteVideoStatsMap.forEach {
                    if (it.key == stats.uid) {
                        rteRemoteVideoStatsMap[it.key] = RteRemoteVideoStats.convert(stats)
                    }
                }
            }
        }

        override fun onRtcStats(rtcChannel: RtcChannel?, stats: IRtcEngineEventHandler.RtcStats?) {
            super.onRtcStats(rtcChannel, stats)
            statisticsReportListener?.onRtcStats(rtcChannel, stats)
        }

        override fun onVideoSizeChanged(rtcChannel: RtcChannel?, uid: Int, width: Int, height: Int, rotation: Int) {
            super.onVideoSizeChanged(rtcChannel, uid, width, height, rotation)
            statisticsReportListener?.onVideoSizeChanged(rtcChannel, uid, width, height, rotation)
        }
    }

    private val rtmChannel = RteEngineImpl.rtmClient.createChannel(channelId, rtmChannelListener)
    val rtcChannel: RtcChannel = RteEngineImpl.rtcEngine.createRtcChannel(channelId)
    private val rteRemoteVideoStatsMap = mutableMapOf<Int, RteRemoteVideoStats>()
    private val interval: Long = 2 * 1000
    private var rteRemoteVideoStatsCallbackTask: TimerTask? = object : TimerTask() {
        override fun run() {
            rteRemoteVideoStatsMap.forEach {
                eventListener?.onRemoteVideoStats(it.value)
            }
        }
    }
    private var timer: Timer? = Timer()

    init {
        rtcChannel.setRtcChannelEventHandler(rtcChannelEventHandler)
    }

    override fun join(rtcOptionalInfo: String, rtcToken: String, rtcUid: Long, mediaOptions: ChannelMediaOptions,
                      @NonNull callback: RteCallback<Void>) {
        ReportManager.getRteReporter().reportRtcJoinStart()
        val rtcCode = rtcChannel.joinChannel(rtcToken, rtcOptionalInfo, rtcUid.toInt(), mediaOptions)
        joinRtmChannel(rtcCode, callback)
    }

    private fun joinRtmChannel(rtcCode: Int, @NonNull callback: RteCallback<Void>) {
        val reporter = ReportManager.getRteReporter()
        reporter.reportRtmJoinStart()
        rtmChannel.join(object : ResultCallback<Void> {
            override fun onSuccess(p0: Void?) {
                if (rtcCode == ERR_OK) {
                    callback.onSuccess(p0)
                    reporter.reportRtmJoinResult("1", null, null)
                } else {
                    callback.onFailure(rtcError(rtcCode))
                    reporter.reportRtmJoinResult("0", rtcCode.toString(), null)
                }
            }

            override fun onFailure(p0: ErrorInfo?) {
                if (p0?.errorCode == JOIN_CHANNEL_ERR_ALREADY_JOINED) {
                    Log.i(tag, "rtm already logged in")
                    callback.onSuccess(null)
                    reporter.reportRtmJoinResult("1", null, null)
                } else {
                    callback.onFailure(rtmError(p0 ?: ErrorInfo(-1)))
                    reporter.reportRtmJoinResult("0", p0?.errorCode.toString(), null)
                }
            }
        })
    }

    override fun leave(callback: RteCallback<Unit>) {
        val rtcCode = rtcChannel.leaveChannel()
        Log.e(tag, if (rtcCode == OK()) "leave rtc channel success" else "leave rtc channel fail" +
                "->code:$rtcCode")
        rtmChannel.leave(object : ResultCallback<Void> {
            override fun onSuccess(p0: Void?) {
                Log.e(tag, "leave rtm channel success")
                if (rtcCode == OK()) {
                    callback.onSuccess(Unit)
                } else {
                    callback.onFailure(rtcError(rtcCode))
                }
            }

            override fun onFailure(p0: ErrorInfo?) {
                Log.e(tag, "leave rtm channel fail: ${p0?.errorDescription}")
                callback.onFailure(rtmError(p0 ?: ErrorInfo(-1)))
            }
        })
        eventListener = null
        rteRemoteVideoStatsCallbackTask?.cancel()
        rteRemoteVideoStatsCallbackTask = null
        timer?.purge()
        timer?.cancel()
        timer = null
    }

    override fun release() {
        rtmChannel.release()
        rtcChannel.destroy()
    }
}
