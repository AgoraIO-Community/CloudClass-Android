package com.agora.edu.component.helper

import android.util.Log
import android.view.ViewGroup
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.context.AgoraEduContextMediaSourceState
import io.agora.agoraeducore.core.context.AgoraEduContextVideoSubscribeLevel
import io.agora.agoraeducore.core.context.EduContextMirrorMode
import io.agora.agoraeducore.core.context.EduContextRenderConfig
import io.agora.agoraeducore.core.internal.education.impl.Constants.AgoraLog
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo

/**
 * author : hefeng
 * date : 2022/6/21
 * description :
 */
object AgoraRendererUtils {
    fun onRendererContainer(eduCore: AgoraEduCore?, viewGroup: ViewGroup?, info: AgoraUIUserDetailInfo,isLocalStream:Boolean) {
        val streamUuid = info.streamUuid
//        val noneView = viewGroup == null
//        val isLocal = isLocalStream(streamUuid)
        val isLocal = isLocalStream

        Log.e("AgoraRendererUtils", "onRendererContainer>>>>> isLocal= $isLocal viewGroup=$viewGroup streamUuid=$streamUuid")

        if (viewGroup == null) {
            eduCore?.eduContextPool()?.mediaContext()?.stopRenderVideo(streamUuid)
        } else {

            //下面的代码注释打开后，设置里关音频，关视频，再开音频，音频不正常
//            val roomUuid = eduCore?.eduContextPool()?.roomContext()?.getRoomInfo()?.roomUuid ?: ""
//            if (info.audioSourceState == AgoraEduContextMediaSourceState.Open) {
//                eduCore?.eduContextPool()?.mediaContext()?.startPlayAudio(roomUuid, streamUuid)
//            } else {
//                eduCore?.eduContextPool()?.mediaContext()?.stopPlayAudio(roomUuid, streamUuid)
//            }

            if (info.videoSourceState == AgoraEduContextMediaSourceState.Open) {
                eduCore?.eduContextPool()?.mediaContext()?.startRenderVideo(
                    EduContextRenderConfig(mirrorMode = EduContextMirrorMode.DISABLED), viewGroup, streamUuid
                )
                eduCore?.eduContextPool()?.streamContext()
                    ?.setRemoteVideoStreamSubscribeLevel(streamUuid, AgoraEduContextVideoSubscribeLevel.LOW)
            } else {
                eduCore?.eduContextPool()?.mediaContext()?.stopRenderVideo(streamUuid)
            }
        }

//        if (noneView && isLocal) {
//            eduCore?.eduContextPool()?.mediaContext()?.stopRenderVideo(streamUuid)
//        } else if (noneView && !isLocal) {
//            eduCore?.eduContextPool()?.mediaContext()?.stopRenderVideo(streamUuid)
//        } else if (!noneView && isLocal) {
//            eduCore?.eduContextPool()?.mediaContext()?.startRenderVideo(
//                EduContextRenderConfig(mirrorMode = EduContextMirrorMode.DISABLED), viewGroup!!, streamUuid
//            )
//            eduCore?.eduContextPool()?.streamContext()?.setRemoteVideoStreamSubscribeLevel(streamUuid, AgoraEduContextVideoSubscribeLevel.LOW)
//
//        } else if (!noneView && !isLocal) {
//            eduCore?.eduContextPool()?.mediaContext()?.startRenderVideo(
//                EduContextRenderConfig(mirrorMode = EduContextMirrorMode.DISABLED), viewGroup!!, streamUuid
//            )
//            eduCore?.eduContextPool()?.streamContext()?.setRemoteVideoStreamSubscribeLevel(streamUuid, AgoraEduContextVideoSubscribeLevel.LOW)
//        }
    }
}