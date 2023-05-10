package com.agora.edu.component.helper

import android.view.ViewGroup
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo

/**
 * author : hefeng
 * date : 2022/6/21
 * description :
 */
object AgoraRendererUtils {
    val TAG = "AgoraRendererUtils"

    fun onRendererContainer(eduCore: AgoraEduCore?, viewGroup: ViewGroup?, info: AgoraUIUserDetailInfo, isLocalStream: Boolean) {
        val streamUuid = info.streamUuid
        val roomUuid = eduCore?.eduContextPool()?.roomContext()?.getRoomInfo()?.roomUuid ?: ""

        LogX.i(TAG,
            "onRendererContainer v2>>>>> isLocalStream=$isLocalStream ||streamUuid=$streamUuid," +
                    " hasAudio= ${info.hasAudio},hasVideo=${info.hasVideo}," +
                    " audioSourceState= ${info.audioSourceState},videoSourceState=${info.videoSourceState}"
        )

        if (info.hasVideo && info.videoSourceState == AgoraEduContextMediaSourceState.Open) {
            if (viewGroup == null) {
                eduCore?.eduContextPool()?.mediaContext()?.stopRenderVideo(streamUuid)
            } else {
                eduCore?.eduContextPool()?.mediaContext()?.startRenderVideo(viewGroup, streamUuid)
            }
        } else {
            eduCore?.eduContextPool()?.mediaContext()?.stopRenderVideo(streamUuid)
        }

        if (info.hasAudio && info.audioSourceState == AgoraEduContextMediaSourceState.Open) {
            eduCore?.eduContextPool()?.mediaContext()?.startPlayAudio(roomUuid, streamUuid)
        } else {
            eduCore?.eduContextPool()?.mediaContext()?.stopPlayAudio(roomUuid, streamUuid)
        }
    }
}