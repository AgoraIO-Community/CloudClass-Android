package io.agora.classroom.vocational

import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.agora.edu.component.AgoraEduListVideoComponent
import com.agora.edu.component.AgoraEduVideoComponent
import com.agora.edu.component.common.IAgoraUIProvider
import com.agora.edu.component.helper.AgoraRenderUtils
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.AgoraEduCoreManager
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.group.FCRGroupClassUtils
import io.agora.agoraeducore.core.group.FCRGroupHandler
import io.agora.agoraeducore.core.group.bean.FCRAllGroupsInfo
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.StreamHandler
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.launch.AgoraServiceType
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeduuikit.interfaces.listeners.IAgoraUIVideoListener
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeduuikit.provider.UIDataProviderListenerImpl
import io.agora.classroom.ui.AgoraClassUIController

/**
 * author : felix
 * date : 2022/1/26
 * description : 视频管理类
 */
class AgoraVocationalClassVideoPresenter(
    var teacherVideoView: AgoraEduVideoComponent,
    var listVideoView: AgoraEduListVideoComponent? = null
) : IAgoraUIVideoListener {
    private val tag = "Vocational-Presenter"

    private var localUserInfo: AgoraEduContextUserInfo? = null

    private var teacherInfo: AgoraUIUserDetailInfo? = null
    private var studentCoHostList: MutableList<AgoraUIUserDetailInfo> = mutableListOf()
    private var eduCore: AgoraEduCore? = null
    private var roomType: RoomType? = null
    private var serviceType: AgoraServiceType? = null
    var agoraUIProvider: IAgoraUIProvider? = null
    private var teacherStreamInfo: AgoraEduContextStreamInfo? = null

    fun initView(roomType: RoomType?, serviceType: AgoraServiceType?, agoraUIProvider: IAgoraUIProvider, uiController: AgoraClassUIController?) {
        this.roomType = roomType
        this.eduCore = agoraUIProvider.getAgoraEduCore()
        this.localUserInfo = eduCore?.eduContextPool()?.userContext()?.getLocalUserInfo()
        this.agoraUIProvider = agoraUIProvider
        this.serviceType = serviceType
        // 这个应该放到里面
        uiController?.uiDataProvider?.addListener(uiDataProviderListener)

        teacherVideoView.initView(agoraUIProvider)
        teacherVideoView.videoListener = this
        listVideoView?.initView(agoraUIProvider)
        addGroupListener()
        LogX.d(tag, " ---> class presenter $serviceType")
    }

    private var isTeacherInGroup: Boolean? = null

    /**
     * 用于判断老师是否在分组里面，如果在则不显示老师上台
     */
    private fun addGroupListener() {
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

    private val streamHandler = object : StreamHandler() {
        override fun onStreamUpdated(
            streamInfo: AgoraEduContextStreamInfo,
            operator: AgoraEduContextUserInfo?
        ) {
            super.onStreamUpdated(streamInfo, operator)
            if (operator?.role == AgoraEduContextUserRole.Teacher) {
                if (CDNUtils.haveCdnStream(streamInfo)) {
                    LogX.d(tag, " -> onStreamUpdated: cdn stream ${streamInfo.streamUuid}")
                    if (teacherStreamInfo == null ||
                        teacherStreamInfo?.streamUuid != streamInfo.streamUuid
                    ) {
                        teacherStreamInfo = streamInfo.copy()
                        CDNUtils.renderCdnStream(eduCore, streamInfo, teacherVideoView.videoViewParent)
                    }
                }
            }
        }

        override fun onStreamLeft(
            streamInfo: AgoraEduContextStreamInfo,
            operator: AgoraEduContextUserInfo?
        ) {
            super.onStreamLeft(streamInfo, operator)
            LogX.d(tag, " -> onStreamLeft: stop cdn stream ${streamInfo.streamUuid}")
            if (operator?.role == AgoraEduContextUserRole.Teacher) {
                CDNUtils.stopCdnStream(eduCore, teacherStreamInfo)
                teacherStreamInfo = null
            }
        }
    }

    fun addStreamListener() {
        if (serviceType != AgoraServiceType.CDN &&
            serviceType != AgoraServiceType.Fusion
        ) {
            return
        }

        LogX.w(tag, " --> $serviceType mode")
        var teacher: AgoraEduContextUserInfo? = null
        eduCore?.eduContextPool()?.let { context ->
            context.userContext()?.getAllUserList()?.forEach { user ->
                if (user.role == AgoraEduContextUserRole.Teacher) {
                    teacher = user
                }
            }
        }

        if (teacher != null) {
            eduCore?.eduContextPool()?.streamContext()?.getStreamList(teacher!!.userUuid)?.forEach { streamInfo ->
                if (CDNUtils.haveCdnStream(streamInfo)) {
                    LogX.d(tag, " -> teacher have cdn stream ${streamInfo.streamUuid}")
                    if (teacherStreamInfo == null ||
                        teacherStreamInfo?.streamUuid != streamInfo.streamUuid
                    ) {
                        teacherStreamInfo = streamInfo.copy()
                        CDNUtils.renderCdnStream(
                            eduCore,
                            streamInfo,
                            teacherVideoView.videoViewParent
                        )
                    }
                    return
                } else {
                    LogX.w(tag, " --> teacher in group but have no cdn stream !!!")
                }
            }
        }

        // 老师无在频道内与否，都需要添加handler。因为退出重进这种情况，需要处理
        LogX.d(tag, " --> add teacher stream handler")
        eduCore?.eduContextPool()?.streamContext()?.addHandler(streamHandler)
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

    fun switchTeacherStreamType(isRtc: Boolean) {
        if (serviceType != AgoraServiceType.Fusion) {
            return
        }

        eduCore?.eduContextPool()?.userContext()?.getAllUserList()?.forEach { user ->
            if (user.role == AgoraEduContextUserRole.Teacher) {
                eduCore?.eduContextPool()?.streamContext()?.getStreamList(user.userUuid)?.forEach {
                    if (isRtc) {
                        LogX.d(tag, " teacher ${it.streamUuid} switch from cdn to rtc")
                        CDNUtils.stopCdnStream(eduCore, it)
                        eduCore?.eduContextPool()?.mediaContext()?.startRenderVideo(
                            teacherVideoView.videoViewParent,
                            it.streamUuid
                        )
                        eduCore?.eduContextPool()?.streamContext()
                            ?.setRemoteVideoStreamSubscribeLevel(it.streamUuid, AgoraEduContextVideoSubscribeLevel.LOW)
                    } else {
                        LogX.d(tag, " teacher ${it.streamUuid} switch from rtc to cdn")
                        eduCore?.eduContextPool()?.mediaContext()?.stopRenderVideo(it.streamUuid)
                        CDNUtils.renderCdnStream(eduCore, it, teacherVideoView.videoViewParent)
                    }
                    return
                }
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

    override fun onRendererContainer(viewGroup: ViewGroup?, info: AgoraUIUserDetailInfo) {
        if (serviceType == AgoraServiceType.CDN ||
            serviceType == AgoraServiceType.Fusion
        ) {
            // LogX.d(tag," -> $serviceType mode teacher drop rtc renderer")
            return
        }

        AgoraRenderUtils.renderView(eduCore, viewGroup, info)
    }

    fun release() {
        agoraUIProvider?.getUIDataProvider()?.removeListener(uiDataProviderListener)
        if (serviceType == AgoraServiceType.CDN ||
            serviceType == AgoraServiceType.Fusion
        ) {
            eduCore?.eduContextPool()?.streamContext()?.removeHandler(streamHandler)
        }
    }
}

