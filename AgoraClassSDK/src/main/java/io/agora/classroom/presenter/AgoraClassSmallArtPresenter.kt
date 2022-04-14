package io.agora.classroom.presenter

import android.view.View
import android.view.ViewGroup
import com.agora.edu.component.AgoraEduListVideoArtComponent
import com.agora.edu.component.AgoraEduListVideoComponent
import com.agora.edu.component.AgoraEduVideoArtComponent
import com.agora.edu.component.common.IAgoraUIProvider
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.context.AgoraEduContextUserInfo
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.EduContextMirrorMode
import io.agora.agoraeducore.core.context.EduContextRenderConfig
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeduuikit.interfaces.listeners.IAgoraUIVideoListener
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeduuikit.provider.UIDataProviderListenerImpl
import io.agora.classroom.ui.AgoraClassUIController

/**
 * author : wufang
 * date : 2022/2/11
 * description : 美术小班课视频管理
 */
class AgoraClassSmallArtPresenter(
    var teacherVideoView: AgoraEduVideoArtComponent,
    var listVideoView: AgoraEduListVideoArtComponent
) : IAgoraUIVideoListener {
    private val tag = "AgoraClassSmallPresenter"


    private var localUserInfo: AgoraEduContextUserInfo? = null
    private var remoteUserDetailInfo: AgoraUIUserDetailInfo? = null
    private var localUserDetailInfo: AgoraUIUserDetailInfo? = null

    private var teacherInfo: AgoraUIUserDetailInfo? = null
    private var studentCoHostList: MutableList<AgoraUIUserDetailInfo> = mutableListOf()
    private var eduCore: AgoraEduCore? = null
    private var roomType: RoomType? = null

    fun initView(roomType: RoomType?, agoraUIProvider: IAgoraUIProvider, uiController: AgoraClassUIController?) {
        this.roomType = roomType
        this.eduCore = agoraUIProvider.getAgoraEduCore()
        this.localUserInfo = eduCore?.eduContextPool()?.userContext()?.getLocalUserInfo()
        // 这个应该放到里面
        uiController?.uiDataProvider?.addListener(uiDataProviderListener)

        teacherVideoView.initView(agoraUIProvider)
        teacherVideoView.videoListener = this
        listVideoView.initView(agoraUIProvider)
    }

    private fun notifyVideos() {
        val hasTeacher = teacherInfo != null
        teacherVideoView.visibility = if (hasTeacher) View.VISIBLE else View.GONE

        val hasCoHost = hasTeacher || studentCoHostList.size > 0
        listVideoView.visibility = if (hasCoHost) View.VISIBLE else View.GONE
    }

    private val uiDataProviderListener = object : UIDataProviderListenerImpl() {
        override fun onCoHostListChanged(userList: List<AgoraUIUserDetailInfo>) {
            super.onCoHostListChanged(userList)
            // only student`s coHost filed can modified
            studentCoHostList = userList.toMutableList()
            notifyVideos()
        }

        override fun onUserListChanged(userList: List<AgoraUIUserDetailInfo>) {
            super.onUserListChanged(userList)
            teacherInfo = userList.find { it.role == AgoraEduContextUserRole.Teacher }
            notifyVideos()

            // 视频逻辑
            val localIsTeacher = localUserInfo?.role == AgoraEduContextUserRole.Teacher
            userList.forEach {
                if ((it.role == AgoraEduContextUserRole.Teacher && localIsTeacher) || it.role == AgoraEduContextUserRole.Student && !localIsTeacher) {
                    // check duplicate data
                    if (localUserDetailInfo == it) {
                        return@forEach
                    }
                    localUserDetailInfo = it
                    //localVideo?.upsertUserDetailInfo(it)
                } else if ((it.role == AgoraEduContextUserRole.Teacher && !localIsTeacher)) {
                    if (remoteUserDetailInfo == it) {
                        return@forEach
                    }
                    remoteUserDetailInfo = it
                    teacherVideoView.upsertUserDetailInfo(it)
                }
            }
            val a = localIsTeacher && userList.find { it.role == AgoraEduContextUserRole.Student } == null
            val b = !localIsTeacher && userList.find { it.role == AgoraEduContextUserRole.Teacher } == null
            if (a || b) {
                remoteUserDetailInfo = null
                teacherVideoView.upsertUserDetailInfo(null)
            }
            //打开老师视频
            if (localIsTeacher || userList.find { it.role == AgoraEduContextUserRole.Teacher } != null) {
                teacherVideoView.upsertUserDetailInfo(userList.find { it.role == AgoraEduContextUserRole.Teacher })
            }
        }

        override fun onVolumeChanged(volume: Int, streamUuid: String) {
            if (streamUuid == remoteUserDetailInfo?.streamUuid) {
                teacherVideoView.updateAudioVolumeIndication(volume, streamUuid)
            }
        }
    }

    override fun onUpdateVideo(streamUuid: String, enable: Boolean) {
        Constants.AgoraLog?.d(tag, "onAudioUpdated")
        //eduContext?.streamContext()?.muteStreams(arrayOf(streamUuid).toMutableList(), Audio)
    }

    override fun onUpdateAudio(streamUuid: String, enable: Boolean) {
        Constants.AgoraLog?.d(tag, "onVideoUpdated")
        //eduContext?.streamContext()?.muteStreams(arrayOf(streamUuid).toMutableList(), Video)
    }

    override fun onRendererContainer(viewGroup: ViewGroup?, streamUuid: String) {
        val noneView = viewGroup == null
        val isLocal = isLocalStream(streamUuid)
        if (noneView && isLocal) {
            eduCore?.eduContextPool()?.mediaContext()?.stopRenderVideo(streamUuid)
        } else if (noneView && !isLocal) {
            eduCore?.eduContextPool()?.mediaContext()?.stopRenderVideo(streamUuid)
        } else if (!noneView && isLocal) {
            eduCore?.eduContextPool()?.mediaContext()?.startRenderVideo(
                EduContextRenderConfig(mirrorMode = EduContextMirrorMode.ENABLED), viewGroup!!, streamUuid
            )
        } else if (!noneView && !isLocal) {
            eduCore?.eduContextPool()?.mediaContext()?.startRenderVideo(
                EduContextRenderConfig(mirrorMode = EduContextMirrorMode.ENABLED), viewGroup!!, streamUuid
            )
        }
    }

    private fun isLocalStream(streamUuid: String): Boolean {
        return when {
            localUserDetailInfo?.streamUuid == streamUuid -> {
                true
            }
            remoteUserDetailInfo?.streamUuid == streamUuid -> {
                false
            }
            else -> {
                false
            }
        }
    }
}