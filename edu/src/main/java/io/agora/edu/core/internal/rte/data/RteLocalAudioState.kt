package io.agora.edu.core.internal.rte.data

import io.agora.rtc.Constants


enum class RteLocalAudioState(val value: Int) {
    LOCAL_AUDIO_STREAM_STATE_STOPPED(0),
    LOCAL_AUDIO_STREAM_STATE_CAPTURING(1),
    LOCAL_AUDIO_STREAM_STATE_ENCODING(2),
    LOCAL_AUDIO_STREAM_STATE_FAILED(3);

    companion object {
        fun convert(value: Int): Int {
            return when (value) {
                Constants.LOCAL_AUDIO_STREAM_STATE_STOPPED -> {
                    LOCAL_AUDIO_STREAM_STATE_STOPPED.value
                }
                Constants.LOCAL_AUDIO_STREAM_STATE_CAPTURING -> {
                    LOCAL_AUDIO_STREAM_STATE_CAPTURING.value
                }
                Constants.LOCAL_AUDIO_STREAM_STATE_ENCODING -> {
                    LOCAL_AUDIO_STREAM_STATE_ENCODING.value
                }
                Constants.LOCAL_AUDIO_STREAM_STATE_FAILED -> {
                    LOCAL_AUDIO_STREAM_STATE_FAILED.value
                }
                else -> {
                    LOCAL_AUDIO_STREAM_STATE_STOPPED.value
                }
            }
        }
    }
}

enum class RteLocalAudioError(val value: Int) {
    LOCAL_AUDIO_STREAM_ERROR_OK(0),
    LOCAL_AUDIO_STREAM_ERROR_FAILURE(1),
    LOCAL_AUDIO_STREAM_ERROR_DEVICE_NO_PERMISSION(2),
    LOCAL_AUDIO_STREAM_ERROR_DEVICE_BUSY(3),
    LOCAL_AUDIO_STREAM_ERROR_CAPTURE_FAILURE(4),
    LOCAL_AUDIO_STREAM_ERROR_ENCODE_FAILURE(5);

    companion object {
        fun convert(value: Int): Int {
            return when (value) {
                Constants.LOCAL_AUDIO_STREAM_ERROR_OK -> {
                    LOCAL_AUDIO_STREAM_ERROR_OK.value
                }
                Constants.LOCAL_AUDIO_STREAM_ERROR_FAILURE -> {
                    LOCAL_AUDIO_STREAM_ERROR_FAILURE.value
                }
                Constants.LOCAL_AUDIO_STREAM_ERROR_DEVICE_NO_PERMISSION -> {
                    LOCAL_AUDIO_STREAM_ERROR_DEVICE_NO_PERMISSION.value
                }
                Constants.LOCAL_AUDIO_STREAM_ERROR_DEVICE_BUSY -> {
                    LOCAL_AUDIO_STREAM_ERROR_DEVICE_BUSY.value
                }
                Constants.LOCAL_AUDIO_STREAM_ERROR_CAPTURE_FAILURE -> {
                    LOCAL_AUDIO_STREAM_ERROR_CAPTURE_FAILURE.value
                }
                Constants.LOCAL_AUDIO_STREAM_ERROR_ENCODE_FAILURE -> {
                    LOCAL_AUDIO_STREAM_ERROR_ENCODE_FAILURE.value
                }
                else -> {
                    LOCAL_AUDIO_STREAM_ERROR_OK.value
                }
            }
        }
    }
}