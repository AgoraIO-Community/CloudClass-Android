package io.agora.rte.data

import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler

class RteLocalVideoStats {
    var sentBitrate = 0
    var sentFrameRate = 0
    var encoderOutputFrameRate = 0
    var rendererOutputFrameRate = 0
    var targetBitrate = 0
    var targetFrameRate = 0
    var qualityAdaptIndication = 0
    var encodedBitrate = 0
    var encodedFrameWidth = 0
    var encodedFrameHeight = 0
    var encodedFrameCount = 0
    var codecType = 0
    var txPacketLossRate = 0
    var captureFrameRate = 0
    var videoQualityPoint = 0

    companion object {
        fun convert(value: IRtcEngineEventHandler.LocalVideoStats): RteLocalVideoStats {
            val rteLocalVideoStats = RteLocalVideoStats()
            rteLocalVideoStats.sentBitrate = value.sentBitrate
            rteLocalVideoStats.sentFrameRate = value.sentFrameRate
            rteLocalVideoStats.encoderOutputFrameRate = value.encoderOutputFrameRate
            rteLocalVideoStats.rendererOutputFrameRate = value.rendererOutputFrameRate
            rteLocalVideoStats.targetBitrate = value.targetBitrate
            rteLocalVideoStats.targetFrameRate = value.targetFrameRate
            rteLocalVideoStats.qualityAdaptIndication = value.qualityAdaptIndication
            rteLocalVideoStats.encodedBitrate = value.encodedBitrate
            rteLocalVideoStats.encodedFrameWidth = value.encodedFrameWidth
            rteLocalVideoStats.encodedFrameHeight = value.encodedFrameHeight
            rteLocalVideoStats.encodedFrameCount = value.encodedFrameCount
            rteLocalVideoStats.codecType = value.codecType
            rteLocalVideoStats.txPacketLossRate = value.txPacketLossRate
            rteLocalVideoStats.captureFrameRate = value.captureFrameRate
//            rteLocalVideoStats.videoQualityPoint = value.videoQualityPoint
            return rteLocalVideoStats
        }
    }
}

enum class RteLocalVideoState(val value: Int) {
    LOCAL_VIDEO_STREAM_STATE_STOPPED(0),
    LOCAL_VIDEO_STREAM_STATE_CAPTURING(1),
    LOCAL_VIDEO_STREAM_STATE_ENCODING(2),
    LOCAL_VIDEO_STREAM_STATE_FAILED(3);

    companion object {
        fun convert(value: Int): Int {
            return when (value) {
                LOCAL_VIDEO_STREAM_STATE_STOPPED.value -> {
                    Constants.LOCAL_VIDEO_STREAM_STATE_STOPPED
                }
                LOCAL_VIDEO_STREAM_STATE_CAPTURING.value -> {
                    Constants.LOCAL_VIDEO_STREAM_STATE_CAPTURING
                }
                LOCAL_VIDEO_STREAM_STATE_ENCODING.value -> {
                    Constants.LOCAL_VIDEO_STREAM_STATE_ENCODING
                }
                LOCAL_VIDEO_STREAM_STATE_FAILED.value -> {
                    Constants.LOCAL_VIDEO_STREAM_STATE_FAILED
                }
                else -> {
                    Constants.LOCAL_VIDEO_STREAM_STATE_FAILED
                }
            }
        }
    }
}

enum class RteLocalVideoError(val value: Int) {
    LOCAL_VIDEO_STREAM_ERROR_OK(0),
    LOCAL_VIDEO_STREAM_ERROR_FAILURE(1),
    LOCAL_VIDEO_STREAM_ERROR_DEVICE_NO_PERMISSION(2),
    LOCAL_VIDEO_STREAM_ERROR_DEVICE_BUSY(3),
    LOCAL_VIDEO_STREAM_ERROR_CAPTURE_FAILURE(4),
    LOCAL_VIDEO_STREAM_ERROR_ENCODE_FAILURE(5);

    companion object {
        fun convert(value: Int): Int {
            return when (value) {
                LOCAL_VIDEO_STREAM_ERROR_OK.value -> {
                    Constants.LOCAL_VIDEO_STREAM_ERROR_OK
                }
                LOCAL_VIDEO_STREAM_ERROR_FAILURE.value -> {
                    Constants.LOCAL_VIDEO_STREAM_ERROR_OK
                }
                LOCAL_VIDEO_STREAM_ERROR_DEVICE_NO_PERMISSION.value -> {
                    Constants.LOCAL_VIDEO_STREAM_ERROR_OK
                }
                LOCAL_VIDEO_STREAM_ERROR_DEVICE_BUSY.value -> {
                    Constants.LOCAL_VIDEO_STREAM_ERROR_OK
                }
                LOCAL_VIDEO_STREAM_ERROR_CAPTURE_FAILURE.value -> {
                    Constants.LOCAL_VIDEO_STREAM_ERROR_OK
                }
                LOCAL_VIDEO_STREAM_ERROR_ENCODE_FAILURE.value -> {
                    Constants.LOCAL_VIDEO_STREAM_ERROR_OK
                }
                else -> {
                    Constants.LOCAL_VIDEO_STREAM_ERROR_OK
                }
            }
        }
    }
}