package io.agora.edu.core.internal.rte.data

import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler

class RteRemoteVideoStats {
    var uid = 0

    @Deprecated("")
    var delay = 0
    var width = 0
    var height = 0
    var receivedBitrate = 0
    var decoderOutputFrameRate = 0
    var rendererOutputFrameRate = 0
    var packetLossRate = 0
    var rxStreamType = 0
    var totalFrozenTime = 0
    var frozenRate = 0
    var totalActiveTime = 0
    var publishDuration = 0

    companion object {
        fun convert(value: IRtcEngineEventHandler.RemoteVideoStats): RteRemoteVideoStats {
            val rteRemoteVideoStats = RteRemoteVideoStats()
            rteRemoteVideoStats.uid = value.uid
            rteRemoteVideoStats.delay = value.delay
            rteRemoteVideoStats.width = value.width
            rteRemoteVideoStats.height = value.height
            rteRemoteVideoStats.receivedBitrate = value.receivedBitrate
            rteRemoteVideoStats.decoderOutputFrameRate = value.decoderOutputFrameRate
            rteRemoteVideoStats.rendererOutputFrameRate = value.rendererOutputFrameRate
            rteRemoteVideoStats.packetLossRate = value.packetLossRate
            rteRemoteVideoStats.rxStreamType = value.rxStreamType
            rteRemoteVideoStats.totalFrozenTime = value.totalFrozenTime
            rteRemoteVideoStats.frozenRate = value.frozenRate
            rteRemoteVideoStats.totalActiveTime = value.totalActiveTime
            rteRemoteVideoStats.publishDuration = value.publishDuration
            return rteRemoteVideoStats
        }
    }
}

enum class RteRemoteVideoState(val value: Int) {
    REMOTE_VIDEO_STATE_STOPPED(0),
    REMOTE_VIDEO_STATE_STARTING(1),
    REMOTE_VIDEO_STATE_DECODING(2),
    REMOTE_VIDEO_STATE_FROZEN(3),
    REMOTE_VIDEO_STATE_FAILED(4);

    companion object {
        fun convert(value: Int): Int {
            return when (value) {
                Constants.REMOTE_VIDEO_STATE_STOPPED -> {
                    REMOTE_VIDEO_STATE_STOPPED.value
                }
                Constants.REMOTE_VIDEO_STATE_STARTING -> {
                    REMOTE_VIDEO_STATE_STARTING.value
                }
                Constants.REMOTE_VIDEO_STATE_DECODING -> {
                    REMOTE_VIDEO_STATE_DECODING.value
                }
                Constants.REMOTE_VIDEO_STATE_FROZEN -> {
                    REMOTE_VIDEO_STATE_FROZEN.value
                }
                Constants.REMOTE_VIDEO_STATE_FAILED -> {
                    REMOTE_VIDEO_STATE_FAILED.value
                }
                else -> {
                    REMOTE_VIDEO_STATE_STOPPED.value
                }
            }
        }
    }
}

enum class RteRemoteVideoStateChangeReason(val value: Int) {
    REMOTE_VIDEO_STATE_REASON_INTERNAL(0),
    REMOTE_VIDEO_STATE_REASON_NETWORK_CONGESTION(1),
    REMOTE_VIDEO_STATE_REASON_NETWORK_RECOVERY(2),
    REMOTE_VIDEO_STATE_REASON_LOCAL_MUTED(3),
    REMOTE_VIDEO_STATE_REASON_LOCAL_UNMUTED(4),
    REMOTE_VIDEO_STATE_REASON_REMOTE_MUTED(5),
    REMOTE_VIDEO_STATE_REASON_REMOTE_UNMUTED(6),
    REMOTE_VIDEO_STATE_REASON_REMOTE_OFFLINE(7),
    REMOTE_VIDEO_STATE_REASON_AUDIO_FALLBACK(8),
    REMOTE_VIDEO_STATE_REASON_AUDIO_FALLBACK_RECOVERY(9);

    companion object {
        fun convert(value: Int): Int {
            return when (value) {
                Constants.REMOTE_VIDEO_STATE_REASON_INTERNAL -> {
                    REMOTE_VIDEO_STATE_REASON_INTERNAL.value
                }
                Constants.REMOTE_VIDEO_STATE_REASON_NETWORK_CONGESTION -> {
                    REMOTE_VIDEO_STATE_REASON_NETWORK_CONGESTION.value
                }
                Constants.REMOTE_VIDEO_STATE_REASON_NETWORK_RECOVERY -> {
                    REMOTE_VIDEO_STATE_REASON_NETWORK_RECOVERY.value
                }
                Constants.REMOTE_VIDEO_STATE_REASON_LOCAL_MUTED -> {
                    REMOTE_VIDEO_STATE_REASON_LOCAL_MUTED.value
                }
                Constants.REMOTE_VIDEO_STATE_REASON_LOCAL_UNMUTED -> {
                    REMOTE_VIDEO_STATE_REASON_LOCAL_UNMUTED.value
                }
                Constants.REMOTE_VIDEO_STATE_REASON_REMOTE_MUTED -> {
                    REMOTE_VIDEO_STATE_REASON_REMOTE_MUTED.value
                }
                Constants.REMOTE_VIDEO_STATE_REASON_REMOTE_UNMUTED -> {
                    REMOTE_VIDEO_STATE_REASON_REMOTE_UNMUTED.value
                }
                Constants.REMOTE_VIDEO_STATE_REASON_REMOTE_OFFLINE -> {
                    REMOTE_VIDEO_STATE_REASON_REMOTE_OFFLINE.value
                }
                Constants.REMOTE_VIDEO_STATE_REASON_AUDIO_FALLBACK -> {
                    REMOTE_VIDEO_STATE_REASON_AUDIO_FALLBACK.value
                }
                Constants.REMOTE_VIDEO_STATE_REASON_AUDIO_FALLBACK_RECOVERY -> {
                    REMOTE_VIDEO_STATE_REASON_AUDIO_FALLBACK_RECOVERY.value
                }
                else -> {
                    REMOTE_VIDEO_STATE_REASON_INTERNAL.value
                }
            }
        }
    }
}