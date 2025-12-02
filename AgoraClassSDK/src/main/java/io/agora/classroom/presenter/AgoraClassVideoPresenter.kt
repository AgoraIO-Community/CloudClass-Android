package io.agora.classroom.presenter

import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.agora.edu.component.AgoraEduListVideoComponent
import com.agora.edu.component.AgoraEduVideoComponent
import com.agora.edu.component.common.IAgoraUIProvider
import com.agora.edu.component.helper.AgoraRenderUtils
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.AgoraEduCoreManager
import io.agora.agoraeducore.core.context.AgoraEduContextUserInfo
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.AgoraEduContextVideoSubscribeLevel
import io.agora.agoraeducore.core.context.EduContextRoomInfo
import io.agora.agoraeducore.core.group.FCRGroupClassUtils
import io.agora.agoraeducore.core.group.FCRGroupHandler
import io.agora.agoraeducore.core.group.bean.FCRAllGroupsInfo
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.agoraeduuikit.interfaces.listeners.IAgoraUIVideoListener
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeduuikit.provider.UIDataProviderListenerImpl
import io.agora.classroom.ui.AgoraClassUIController

/**
 * author : felix
 * date : 2022/1/26
 * description : 视频管理类
 */
class AgoraClassVideoPresenter(
    var teacherVideoView: AgoraEduVideoComponent,
    var listVideoView: AgoraEduListVideoComponent? = null
) : IAgoraUIVideoListener {
    private val TAG = "AgoraClassVideoPresenter"

    private var localUserInfo: AgoraEduContextUserInfo? = null
    private var isLocalUserOnStage = false //本地学生是否已上台
    private var isFirstTimeJoinRoom = true //本地学生是否第一次进房间
    private var teacherInfo: AgoraUIUserDetailInfo? = null
    private var studentCoHostList: MutableList<AgoraUIUserDetailInfo> = mutableListOf()
    private var eduCore: AgoraEduCore? = null
    private var roomType: RoomType? = null
    var agoraUIProvider: IAgoraUIProvider? = null
    //教师端视频流加载模式
    var videoSubscribeLevel: AgoraEduContextVideoSubscribeLevel = AgoraEduContextVideoSubscribeLevel.LOW

    fun initView(roomType: RoomType?,videoSubscribeLevel:AgoraEduContextVideoSubscribeLevel?, agoraUIProvider: IAgoraUIProvider, uiController: AgoraClassUIController?) {
        this.roomType = roomType
        this.videoSubscribeLevel = videoSubscribeLevel ?: this.videoSubscribeLevel
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
            LogX.e("老师在分组其他分组")
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
            //本地学生上台状态更新
            if (studentCoHostList.find { it.userUuid == localUserInfo?.userUuid } != null) { //找到了本地用户
                if (!isLocalUserOnStage) {//之前不在台上
                    //toast 本地学生上台
                    isLocalUserOnStage = true
                    if (!isFirstTimeJoinRoom) { //当用户不是进房间时上台的时候，才会显示toast
                        listVideoView?.let {
                            AgoraUIToast.info(it.context, text = String.format(it.context.getString(R.string.fcr_user_local_start_co_hosting)))
                        }
                    }
                    isFirstTimeJoinRoom = false //已经不是第一次进教室了
                }
            } else {//上台列表中没找到本地用户
                if (isLocalUserOnStage) {
                    isLocalUserOnStage = false
                    listVideoView?.let {
                        AgoraUIToast.error(it.context, text = String.format(it.context.getString(R.string.fcr_user_local_stop_co_hosting)))
                    }
                }
            }
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
                    eduCore?.eduContextPool()?.streamContext()?.setRemoteVideoStreamSubscribeLevel(it.streamUuid, this@AgoraClassVideoPresenter.videoSubscribeLevel)
                }
            }
        }

        override fun onVolumeChanged(volume: Int, streamUuid: String) {
            if (streamUuid == teacherInfo?.streamUuid) {
                teacherVideoView.updateAudioVolumeIndication(volume, streamUuid)
            }
        }
    }

    override fun onRendererContainer(viewGroup: ViewGroup?, info: AgoraUIUserDetailInfo) {
        AgoraRenderUtils.renderView(eduCore, viewGroup, info)
    }

    fun release() {
        agoraUIProvider?.getUIDataProvider()?.removeListener(uiDataProviderListener)
    }

}