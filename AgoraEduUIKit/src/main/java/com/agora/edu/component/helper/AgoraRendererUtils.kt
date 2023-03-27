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

        //setUnCoHostList(eduCore, info)

        if (info.hasVideo && info.videoSourceState == AgoraEduContextMediaSourceState.Open) {
            if (viewGroup == null) {
                eduCore?.eduContextPool()?.mediaContext()?.stopRenderVideo(streamUuid)
            } else {
                eduCore?.eduContextPool()?.mediaContext()?.startRenderVideo(
                    EduContextRenderConfig(mirrorMode = EduContextMirrorMode.DISABLED), viewGroup, streamUuid)
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

    /**
     * 开启拓展屏，不要订阅台下
     */
    fun setUnCoHostList(eduCore: AgoraEduCore?, info: AgoraUIUserDetailInfo) {
        // VideoGalleryList： 不要订阅   CoHostList：需要订阅
        // LogX.e(TAG, "VideoGalleryList state:"+RoomPropertiesHelper.getExpandedScopeProps(eduCore)?.get(PropertyData.STATE))
        // LogX.e(TAG, "VideoGalleryList:" + RoomPropertiesHelper.getVideoGalleryList(eduCore))
        // LogX.e(TAG, "CoHostList:"+eduCore?.eduContextPool()?.userContext()?.getCoHostList())

        // 开启拓展屏的时候，只订阅CoHostList和老师
        if (RoomPropertiesHelper.isOpenExternalScreen(eduCore)
            && info.role == AgoraEduContextUserRole.Student
            && eduCore?.eduContextPool()?.userContext()?.getLocalUserInfo()?.userUuid != info.userUuid
        ) {
            // 判断当前 userUuid 是否在 CoHostList 里面
            var isExist = false
            run breaking@{
                val list = eduCore?.eduContextPool()?.userContext()?.getCoHostList()
                list?.forEach continuing@{
                    if (it.userUuid == info.userUuid) {
                        isExist = true
                        return@breaking
                    }
                }
            }

            if (!isExist) { // 拓展屏的时候，不在讲台区，不要订阅音视频
                info.hasAudio = false
                info.hasVideo = false
                LogX.e(TAG, "开启拓展屏的时候，只订阅CoHostList和老师，不要订阅音视频 streamUuid=${info.streamUuid} || userUuid=${info.userUuid}")
            }
        }
    }
}