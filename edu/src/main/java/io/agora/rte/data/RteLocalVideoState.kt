package io.agora.rte.data

import io.agora.rtc.Constants

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