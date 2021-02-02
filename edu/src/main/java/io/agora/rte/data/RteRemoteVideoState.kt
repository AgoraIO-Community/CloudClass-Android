package io.agora.rte.data

import io.agora.rtc.Constants

enum class RteRemoteVideoState(val value: Int) {
    REMOTE_VIDEO_STATE_STOPPED(0),
    REMOTE_VIDEO_STATE_STARTING(1),
    REMOTE_VIDEO_STATE_DECODING(2),
    REMOTE_VIDEO_STATE_FROZEN(3),
    REMOTE_VIDEO_STATE_FAILED(3);

    companion object {
        fun convert(value: Int): Int {
            return when (value) {
                REMOTE_VIDEO_STATE_STOPPED.value -> {
                    Constants.REMOTE_VIDEO_STATE_STOPPED
                }
                REMOTE_VIDEO_STATE_STARTING.value -> {
                    Constants.REMOTE_VIDEO_STATE_STARTING
                }
                REMOTE_VIDEO_STATE_DECODING.value -> {
                    Constants.REMOTE_VIDEO_STATE_DECODING
                }
                REMOTE_VIDEO_STATE_FROZEN.value -> {
                    Constants.REMOTE_VIDEO_STATE_FROZEN
                }
                REMOTE_VIDEO_STATE_FAILED.value -> {
                    Constants.REMOTE_VIDEO_STATE_FAILED
                }
                else -> {
                    Constants.REMOTE_VIDEO_STATE_STOPPED
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
                REMOTE_VIDEO_STATE_REASON_INTERNAL.value -> {
                    Constants.REMOTE_VIDEO_STATE_REASON_INTERNAL
                }
                REMOTE_VIDEO_STATE_REASON_NETWORK_CONGESTION.value -> {
                    Constants.REMOTE_VIDEO_STATE_REASON_NETWORK_CONGESTION
                }
                REMOTE_VIDEO_STATE_REASON_NETWORK_RECOVERY.value -> {
                    Constants.REMOTE_VIDEO_STATE_REASON_NETWORK_RECOVERY
                }
                REMOTE_VIDEO_STATE_REASON_LOCAL_MUTED.value -> {
                    Constants.REMOTE_VIDEO_STATE_REASON_LOCAL_MUTED
                }
                REMOTE_VIDEO_STATE_REASON_LOCAL_UNMUTED.value -> {
                    Constants.REMOTE_VIDEO_STATE_REASON_LOCAL_UNMUTED
                }
                REMOTE_VIDEO_STATE_REASON_REMOTE_MUTED.value -> {
                    Constants.REMOTE_VIDEO_STATE_REASON_REMOTE_MUTED
                }
                REMOTE_VIDEO_STATE_REASON_REMOTE_UNMUTED.value -> {
                    Constants.REMOTE_VIDEO_STATE_REASON_REMOTE_UNMUTED
                }
                REMOTE_VIDEO_STATE_REASON_REMOTE_OFFLINE.value -> {
                    Constants.REMOTE_VIDEO_STATE_REASON_REMOTE_OFFLINE
                }
                REMOTE_VIDEO_STATE_REASON_AUDIO_FALLBACK.value -> {
                    Constants.REMOTE_VIDEO_STATE_REASON_AUDIO_FALLBACK
                }
                REMOTE_VIDEO_STATE_REASON_AUDIO_FALLBACK_RECOVERY.value -> {
                    Constants.REMOTE_VIDEO_STATE_REASON_AUDIO_FALLBACK_RECOVERY
                }
                else -> {
                    Constants.REMOTE_VIDEO_STATE_REASON_INTERNAL
                }
            }
        }
    }
}