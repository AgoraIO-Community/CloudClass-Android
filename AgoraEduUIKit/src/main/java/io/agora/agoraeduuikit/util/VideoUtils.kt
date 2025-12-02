package io.agora.agoraeduuikit.util

import io.agora.agoraeducore.core.context.AgoraEduContextLocalStreamConfig
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType

class VideoUtils {
    private val defaultVideoWidth = 320
    private val defaultVideoHeight = 240
    private val defaultFrameRate = 15
    private val defaultBitrate = 200
    private val defaultMirrorMode = false

    private val videoEditWidth = 1920
    private val videoEditHeight = 1080
    private val videoEditFrameRate = 10

    private val hdVideoWidth = 640
    private val hdVideoHeight = 480
    private val hdFrameRate = 15
    private val hdBitrate = 1000

    private val smallVideoWidth = 160
    private val smallVideoHeight = 120
    private val smallFrameRate = 15
    private val smallVideoBitrate = 200

    fun getDefaultVideoEncoderConfigs(): AgoraEduContextLocalStreamConfig {
        return AgoraEduContextLocalStreamConfig(
            defaultVideoWidth,
            defaultVideoHeight,
            defaultFrameRate,
            defaultBitrate,
            defaultMirrorMode)
    }

    fun getVideoEditEncoderConfigs(): AgoraEduContextLocalStreamConfig {
        return AgoraEduContextLocalStreamConfig(
            videoEditWidth,
            videoEditHeight,
            videoEditFrameRate,
            defaultBitrate,
            defaultMirrorMode)
    }

    fun getHDEncoderConfigs(): AgoraEduContextLocalStreamConfig {
        return AgoraEduContextLocalStreamConfig(
            hdVideoWidth,
            hdVideoHeight,
            hdFrameRate,
            hdBitrate,
            defaultMirrorMode)
    }

    fun getSmallVideoEncoderConfigs(): AgoraEduContextLocalStreamConfig {
        return AgoraEduContextLocalStreamConfig(
            smallVideoWidth,
            smallVideoHeight,
            smallFrameRate,
            smallVideoBitrate,
            defaultMirrorMode)
    }

    fun getDefaultVideoEncoderConfigs(roomType: Int): AgoraEduContextLocalStreamConfig {
        return when (roomType) {
            RoomType.SMALL_CLASS.value -> getSmallVideoEncoderConfigs()
            else -> getDefaultVideoEncoderConfigs()
        }
    }
}