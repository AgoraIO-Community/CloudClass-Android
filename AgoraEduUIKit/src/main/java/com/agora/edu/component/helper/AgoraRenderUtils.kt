package com.agora.edu.component.helper

import android.view.ViewGroup
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo

/**
 * author : felix
 * date : 2022/6/21
 * description :
 */
object AgoraRenderUtils {
    val TAG = "AgoraRenderUtils"

    fun renderView(eduCore: AgoraEduCore?, videoContainer: ViewGroup?, info: AgoraUIUserDetailInfo) {
        renderView(EduContextRenderConfig(), eduCore, videoContainer, info)
    }

    fun renderView(
        renderConfig: EduContextRenderConfig,
        eduCore: AgoraEduCore?,
        videoContainer: ViewGroup?,
        info: AgoraUIUserDetailInfo
    ) {

        val streamUuid = info.streamUuid
        val roomUuid = eduCore?.eduContextPool()?.roomContext()?.getRoomInfo()?.roomUuid ?: ""

        LogX.i(TAG,
            "renderView v2>>>>> streamUuid=$streamUuid," +
                    " hasAudio= ${info.hasAudio},hasVideo=${info.hasVideo}," +
                    " audioSourceState= ${info.audioSourceState},videoSourceState=${info.videoSourceState}"
        )

        // 渲染和订阅视频
        if (info.hasVideo && info.videoSourceState == AgoraEduContextMediaSourceState.Open) {
            if (videoContainer == null) {
                eduCore?.eduContextPool()?.mediaContext()?.stopRenderVideo(streamUuid)
            } else {
                eduCore?.eduContextPool()?.mediaContext()?.startRenderVideo(renderConfig, videoContainer, streamUuid)
            }
        } else {
            eduCore?.eduContextPool()?.mediaContext()?.stopRenderVideo(streamUuid)
        }

        // 订阅声音
        if (info.hasAudio && info.audioSourceState == AgoraEduContextMediaSourceState.Open) {
            eduCore?.eduContextPool()?.mediaContext()?.startPlayAudio(roomUuid, streamUuid)
        } else {
            eduCore?.eduContextPool()?.mediaContext()?.stopPlayAudio(roomUuid, streamUuid)
        }
    }
}