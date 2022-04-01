package io.agora.agoraeduuikit.util

import io.agora.agoraeducore.core.context.AgoraEduContextLocalStreamConfig
import io.agora.agoraeducore.core.internal.launch.AgoraEduRoomType

class VideoUtils {
    private val defaultVideoWidth = 320
    private val defaultVideoHeight = 240
    private val defaultFrameRate = 15
    private val defaultBitrate = 0
    private val defaultMirrorMode = false

    private val videoEditWidth = 1920
    private val videoEditHeight = 1080
    private val videoEditFrameRate = 10

    private val smallVideoWidth = 160
    private val smallVideoHeight = 120
    private val smallVideoBitrate = 65

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

    fun getSmallVideoEncoderConfigs(): AgoraEduContextLocalStreamConfig {
        return AgoraEduContextLocalStreamConfig(
            smallVideoWidth,
            smallVideoHeight,
            videoEditFrameRate,
            smallVideoBitrate,
            defaultMirrorMode)
    }

    fun getDefaultVideoEncoderConfigs(roomType: Int): AgoraEduContextLocalStreamConfig {
        return when (roomType) {
            AgoraEduRoomType.AgoraEduRoomTypeSmall.value -> getSmallVideoEncoderConfigs()
            else -> getDefaultVideoEncoderConfigs()
        }
    }
}