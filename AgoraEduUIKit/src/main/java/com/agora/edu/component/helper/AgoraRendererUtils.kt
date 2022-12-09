package com.agora.edu.component.helper

import android.view.ViewGroup
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.context.AgoraEduContextMediaSourceState
import io.agora.agoraeducore.core.context.AgoraEduContextVideoSubscribeLevel
import io.agora.agoraeducore.core.context.EduContextMirrorMode
import io.agora.agoraeducore.core.context.EduContextRenderConfig
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo

/**
 * author : hefeng
 * date : 2022/6/21
 * description :
 */
object AgoraRendererUtils {

    fun onRendererContainer(eduCore: AgoraEduCore?, viewGroup: ViewGroup?, info: AgoraUIUserDetailInfo, isLocalStream: Boolean) {
        val streamUuid = info.streamUuid
        val roomUuid = eduCore?.eduContextPool()?.roomContext()?.getRoomInfo()?.roomUuid ?: ""

        LogX.i("AgoraRendererUtils", "onRendererContainer>>>>> isLocal= $isLocalStream viewGroup=$viewGroup streamUuid=$streamUuid")
        LogX.i("AgoraRendererUtils", "onRendererContainer>>>>> audioSourceState= ${info.audioSourceState},videoSourceState=${info.videoSourceState}")

        if (info.videoSourceState == AgoraEduContextMediaSourceState.Open) {
            if (viewGroup == null) {
                eduCore?.eduContextPool()?.mediaContext()?.stopRenderVideo(streamUuid)
            } else {
                eduCore?.eduContextPool()?.mediaContext()?.startRenderVideo(
                    EduContextRenderConfig(mirrorMode = EduContextMirrorMode.DISABLED), viewGroup, streamUuid)
            }
            eduCore?.eduContextPool()?.streamContext()?.setRemoteVideoStreamSubscribeLevel(streamUuid, AgoraEduContextVideoSubscribeLevel.LOW)
        } else {
            eduCore?.eduContextPool()?.mediaContext()?.stopRenderVideo(streamUuid)
        }

        if (info.audioSourceState == AgoraEduContextMediaSourceState.Open) {
            eduCore?.eduContextPool()?.mediaContext()?.startPlayAudio(roomUuid, streamUuid)
        } else {
            eduCore?.eduContextPool()?.mediaContext()?.stopPlayAudio(roomUuid, streamUuid)
        }
    }
}