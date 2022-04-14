package io.agora.classroom.presenter

import android.view.View
import android.view.ViewGroup
import com.agora.edu.component.AgoraEduListVideoComponent
import com.agora.edu.component.AgoraEduVideoComponent
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
 * author : hefeng
 * date : 2022/1/26
 * description : 视频管理类
 */
class AgoraClassVideoPresenter(
    var teacherVideoView: AgoraEduVideoComponent,
    var listVideoView: AgoraEduListVideoComponent
) : IAgoraUIVideoListener {
    private val TAG = "AgoraClassVideoPresenter"

    private var localUserInfo: AgoraEduContextUserInfo? = null

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
        if (roomType != RoomType.LARGE_CLASS) { // 大班课一直显示
            teacherVideoView.handler.post { teacherVideoView.visibility = if (hasTeacher) View.VISIBLE else View.GONE }
        }

        val hasCoHost = studentCoHostList.size > 0
        listVideoView.handler.post { listVideoView.visibility = if (hasCoHost) View.VISIBLE else View.GONE }
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
            // try find teacher
            teacherInfo = userList.find { it.role == AgoraEduContextUserRole.Teacher }
            // show/hide layout
            notifyVideos()
            // try notify teacher video
            teacherInfo?.let {
                if (!teacherVideoView.largeWindowOpened) {
                    teacherVideoView.upsertUserDetailInfo(it)
                }
            }
        }

        override fun onVolumeChanged(volume: Int, streamUuid: String) {
            if (streamUuid == teacherInfo?.streamUuid) {
                teacherVideoView.updateAudioVolumeIndication(volume, streamUuid)
            }
        }
    }

    override fun onUpdateVideo(streamUuid: String, enable: Boolean) {
        Constants.AgoraLog?.d(TAG, "onAudioUpdated")
    }

    override fun onUpdateAudio(streamUuid: String, enable: Boolean) {
        Constants.AgoraLog?.d(TAG, "onVideoUpdated")
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
        if (localUserInfo != null && teacherInfo != null && streamUuid == teacherInfo?.streamUuid
            && teacherInfo?.userUuid == localUserInfo?.userUuid) {
            return true
        }
        return false;
    }
}