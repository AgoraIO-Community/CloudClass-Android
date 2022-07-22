package io.agora.classroom.presenter

import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.agora.edu.component.AgoraEduListVideoComponent
import com.agora.edu.component.AgoraEduVideoComponent
import com.agora.edu.component.common.IAgoraUIProvider
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.AgoraEduCoreManager
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.group.FCRGroupClassUtils
import io.agora.agoraeducore.core.group.FCRGroupHandler
import io.agora.agoraeducore.core.group.bean.FCRAllGroupsInfo
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.education.impl.Constants.AgoraLog
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
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
    var listVideoView: AgoraEduListVideoComponent? = null
) : IAgoraUIVideoListener {
    private val TAG = "AgoraClassVideoPresenter"

    private var localUserInfo: AgoraEduContextUserInfo? = null

    private var teacherInfo: AgoraUIUserDetailInfo? = null
    private var studentCoHostList: MutableList<AgoraUIUserDetailInfo> = mutableListOf()
    private var eduCore: AgoraEduCore? = null
    private var roomType: RoomType? = null
    var agoraUIProvider: IAgoraUIProvider? = null

    fun initView(roomType: RoomType?, agoraUIProvider: IAgoraUIProvider, uiController: AgoraClassUIController?) {
        this.roomType = roomType
        this.eduCore = agoraUIProvider.getAgoraEduCore()
        this.localUserInfo = eduCore?.eduContextPool()?.userContext()?.getLocalUserInfo()
        this.agoraUIProvider = agoraUIProvider
        // 这个应该放到里面
        uiController?.uiDataProvider?.addListener(uiDataProviderListener)

        teacherVideoView.initView(agoraUIProvider)
        teacherVideoView.videoListener = this
        listVideoView?.initView(agoraUIProvider)
        addGroupListener()
    }

    var isTeacherInGroup: Boolean? = null

    /**
     * 用于判断老师是否在分组里面，如果在则不显示老师上台
     */
    fun addGroupListener() {
        if (eduCore?.eduContextPool()?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Student) {
            eduCore?.eduContextPool()?.roomContext()?.addHandler(object : RoomHandler() {
                override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
                    super.onJoinRoomSuccess(roomInfo)
                    isTeacherInGroup()
                }
            })

            eduCore?.eduContextPool()?.groupContext()?.addHandler(object : FCRGroupHandler() {
                override fun onAllGroupUpdated(groupInfo: FCRAllGroupsInfo) {
                    super.onAllGroupUpdated(groupInfo)
                    isTeacherInGroup()
                }
            })

            FCRGroupClassUtils.mainLaunchConfig?.apply {
                val eduContextPool = AgoraEduCoreManager.getEduCore(roomUuid)?.eduContextPool()
                eduContextPool?.groupContext()?.addHandler(object : FCRGroupHandler() {
                    override fun onAllGroupUpdated(groupInfo: FCRAllGroupsInfo) {
                        super.onAllGroupUpdated(groupInfo)
                        isTeacherInGroup()
                    }
                })
            }
        }
    }

    fun isTeacherInGroup() {
        eduCore?.eduContextPool()?.userContext()?.getAllUserList()?.forEach { user ->
            if (user.role == AgoraEduContextUserRole.Teacher) {
                var temp: Boolean? = null
                FCRGroupClassUtils.allGroupInfo?.details?.forEach { group ->
                    group.value.users?.forEach { groupUser ->
                        if (groupUser.userUuid == user.userUuid) {
                            // 老师已经在小组
                            temp = true
                        }
                    }
                }
                isTeacherInGroup = temp
                notifyVideos()
            }
        }
    }

    private fun notifyVideos() {
        // 如果老师在分组，大房间不需要显示老师视讯
        if (isTeacherInGroup == true && roomType != RoomType.GROUPING_CLASS) {
            AgoraLog?.e("老师在分组其他分组")
            ContextCompat.getMainExecutor(teacherVideoView.context).execute {
                teacherVideoView.visibility = View.GONE
            }
            return
        }

        val hasTeacher = teacherInfo != null
        if (roomType != RoomType.LARGE_CLASS) { // 大班课一直显示
            ContextCompat.getMainExecutor(teacherVideoView.context)
                .execute { teacherVideoView.visibility = if (hasTeacher) View.VISIBLE else View.GONE }
        }

        val hasCoHost = studentCoHostList.size > 0
        listVideoView?.let {
            ContextCompat.getMainExecutor(teacherVideoView.context)
                .execute { it.visibility = if (hasCoHost) View.VISIBLE else View.GONE }
        }
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
            // 本地是否是老师
            val localIsTeacher = localUserInfo?.role == AgoraEduContextUserRole.Teacher
            val b = !localIsTeacher && userList.find { it.role == AgoraEduContextUserRole.Teacher } == null//本地是不是老师，且老师离线
            if (b) {
                teacherVideoView.upsertUserDetailInfo(null)
            }
            // try notify teacher video
            teacherInfo?.let {
                if (!teacherVideoView.largeWindowOpened) {
                    teacherVideoView.upsertUserDetailInfo(it)
                    eduCore?.eduContextPool()?.streamContext()
                        ?.setRemoteVideoStreamSubscribeLevel(it.streamUuid, AgoraEduContextVideoSubscribeLevel.LOW)
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
                EduContextRenderConfig(mirrorMode = EduContextMirrorMode.DISABLED), viewGroup!!, streamUuid
            )
            eduCore?.eduContextPool()?.streamContext()?.setRemoteVideoStreamSubscribeLevel(streamUuid, AgoraEduContextVideoSubscribeLevel.LOW)

        } else if (!noneView && !isLocal) {
            eduCore?.eduContextPool()?.mediaContext()?.startRenderVideo(
                EduContextRenderConfig(mirrorMode = EduContextMirrorMode.DISABLED), viewGroup!!, streamUuid
            )
            eduCore?.eduContextPool()?.streamContext()?.setRemoteVideoStreamSubscribeLevel(streamUuid, AgoraEduContextVideoSubscribeLevel.LOW)

        }
    }

    private fun isLocalStream(streamUuid: String): Boolean {
        if (localUserInfo != null && teacherInfo != null && streamUuid == teacherInfo?.streamUuid
            && teacherInfo?.userUuid == localUserInfo?.userUuid) {
            return true
        }
        return false
    }

    fun release(){
        agoraUIProvider?.getUIDataProvider()?.removeListener(uiDataProviderListener)
    }

}