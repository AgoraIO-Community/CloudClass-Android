package io.agora.classroom.common

import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.agora.edu.component.AgoraEduRewardWindow
import com.agora.edu.component.common.IAgoraUIProvider
import com.agora.edu.component.helper.AgoraUIDeviceSetting
import com.agora.edu.component.view.AgoraEduFullLoadingDialog
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.AgoraEduCoreManager
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.group.FCRGroupClassUtils
import io.agora.agoraeducore.core.group.FCRGroupHandler
import io.agora.agoraeducore.core.group.bean.AgoraEduContextSubRoomInfo
import io.agora.agoraeducore.core.group.bean.FCRAllGroupsInfo
import io.agora.agoraeducore.core.group.bean.FCREduContextGroupInfo
import io.agora.agoraeducore.core.group.bean.FCRGroupInfo
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.education.impl.cmd.manager.FCRGroupCMDManager
import io.agora.agoraeducore.core.internal.framework.data.EduCallback
import io.agora.agoraeducore.core.internal.framework.data.EduError
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.StreamHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.UserHandler
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.launch.AgoraEduEvent
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.core.internal.state.FCRHandlerManager
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.agoraeduuikit.config.FcrUIConfig
import io.agora.agoraeduuikit.config.FcrUIConfigFactory
import io.agora.agoraeduuikit.provider.UIDataProvider
import io.agora.classroom.helper.FcrStreamParameters
import io.agora.classroom.sdk.AgoraClassroomSDK
import io.agora.classroom.ui.AgoraClassUIController
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 教室基类
 */
abstract class AgoraEduClassActivity : AgoraBaseClassActivity(), IAgoraUIProvider, IAgoraConfig {
    override var TAG = "AgoraEduClassActivity"
    private var isCheckGroup = AtomicInteger(0)
    private var rewardWindow: AgoraEduRewardWindow? = null

    protected var uiController: AgoraClassUIController = AgoraClassUIController()

    /**
     * 页面ID
     */
    protected var uuid: String = UUID.randomUUID().toString()
    protected lateinit var fullLoading: AgoraEduFullLoadingDialog
    protected lateinit var context: AgoraBaseClassActivity
    protected var lockObject = Any()

    var localStreamInfo: AgoraEduContextStreamInfo? = null//本地的流信息
    var lastLocalHasVideo = true //本地上一次视频状态
    var lastLocalHasAudio = true
    var curLocalHasVideo = true //本地当前视频状态
    var curLocalHasAudio = true
    var groupInvitedUuid: String? = null

    /**
     * 正在加入房间
     */
    var isJoining = AtomicBoolean(false)

    private val streamHandler = object : StreamHandler() {
        override fun onStreamJoined(streamInfo: AgoraEduContextStreamInfo, operator: AgoraEduContextUserInfo?) {
            eduCore()?.eduContextPool()?.userContext()?.getLocalUserInfo()?.let { localUser ->
                if (localUser.userUuid == streamInfo.owner.userUuid) {
                    localStreamInfo = streamInfo
                    openSystemDevices()
                }
            }
        }

        override fun onStreamUpdated(streamInfo: AgoraEduContextStreamInfo, operator: AgoraEduContextUserInfo?) {
            super.onStreamUpdated(streamInfo, operator)
            eduCore()?.eduContextPool()?.userContext()?.getLocalUserInfo()?.let { localUser ->
                if (localUser.userUuid == streamInfo.owner.userUuid && operator?.role != AgoraEduContextUserRole.Student) {
                    //如果是both或者video，那么现在update的video状态是true
                    curLocalHasVideo =
                        streamInfo.streamType == AgoraEduContextMediaStreamType.Video || streamInfo.streamType == AgoraEduContextMediaStreamType.Both
                    curLocalHasAudio =
                        streamInfo.streamType == AgoraEduContextMediaStreamType.Audio || streamInfo.streamType == AgoraEduContextMediaStreamType.Both
                    if (lastLocalHasVideo != curLocalHasVideo) {//如果上一次的视频状态和本次的视频状态不一样
                        when (streamInfo.streamType) {
                            AgoraEduContextMediaStreamType.Video, AgoraEduContextMediaStreamType.Both -> {//更新视频状态的提示
                                AgoraUIToast.info(
                                    applicationContext,
                                    text = String.format(context.getString(R.string.fcr_stream_start_video))
                                )
                                lastLocalHasVideo = true
                            }
                            AgoraEduContextMediaStreamType.Audio, AgoraEduContextMediaStreamType.None -> {
                                AgoraUIToast.error(
                                    applicationContext,
                                    text = String.format(context.getString(R.string.fcr_stream_stop_video))
                                )
                                lastLocalHasVideo = false
                            }
                        }
                    }
                    if (lastLocalHasAudio != curLocalHasAudio) {
                        when (streamInfo.streamType) {
                            AgoraEduContextMediaStreamType.Audio, AgoraEduContextMediaStreamType.Both -> {
                                AgoraUIToast.info(
                                    applicationContext,
                                    text = String.format(context.getString(R.string.fcr_stream_start_audio))
                                )
                                lastLocalHasAudio = true
                            }
                            AgoraEduContextMediaStreamType.Video, AgoraEduContextMediaStreamType.None -> {
                                AgoraUIToast.error(
                                    applicationContext,
                                    text = String.format(context.getString(R.string.fcr_stream_stop_audio))
                                )
                                lastLocalHasAudio = false
                            }
                        }
                    }
                }
            }
            localStreamInfo = streamInfo//保存本地的流信息
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this
        fullLoading = AgoraEduFullLoadingDialog(context)
        LogX.i("Group create fullLoading $this")
    }

    override fun onCreateEduCore(isSuccess: Boolean) {
        super.onCreateEduCore(isSuccess)

        if (isSuccess) {
            uiController.init(eduCore())
            setEduCoreListener()
        }
    }

    /**
     * 检查是否开启分组
     */
    fun checkGroup() {
        if (isCheckGroup.get() == 2) { // join room & edu create success
            eduCore()?.room()?.roomProperties?.get("groups")?.let {
                val groups = it as Map<*, *>
                FCRGroupCMDManager(eduCore()!!).handleAllInfoGroup(groups)
            }
        }
    }

    override fun getAgoraEduCore(): AgoraEduCore? {
        return eduCore()
    }

    override fun getUIDataProvider(): UIDataProvider? {
        return uiController.uiDataProvider
    }

    private fun setEduCoreListener() {
        getEduContext()?.monitorContext()?.addHandler(monitorHandler)
        getEduContext()?.roomContext()?.addHandler(myRoomHandler)
        getEduContext()?.userContext()?.addHandler(userHandler)
        getEduContext()?.groupContext()?.addHandler(groupHandler)
    }

    /**
     * 多设备进教室，退出教室
     */
    fun updateConnectionState(connectionState: EduContextConnectionState) {
        LogX.e(TAG, "connectionState -> $connectionState")

        if (connectionState == EduContextConnectionState.Reconnecting || connectionState == EduContextConnectionState.Connecting) {
            showLoading()  // 重连
        } else {
            dismissLoading()
        }

        if (connectionState == EduContextConnectionState.Aborted) {
            AgoraUIToast.error(
                applicationContext,
                text = resources.getString(R.string.fcr_monitor_login_remote_device)
            )
            val roomUuid = eduCore()?.eduContextPool()?.roomContext()?.getRoomInfo()?.roomUuid
            FCRHandlerManager.roomHandlerMap.forEach {
                if (roomUuid == it.key) {
                    it.value.onRoomStateUpdated(AgoraEduContextUserLeaveReason.KICKOUT)
                }
            }
            ContextCompat.getMainExecutor(context).execute {
                if (!context.isDestroyPage()) {
                    eduCore()?.eduContextPool()?.roomContext()?.leaveRoom(object : EduContextCallback<Unit> {
                        override fun onSuccess(target: Unit?) {
                        }

                        override fun onFailure(error: EduContextError?) {
                            error?.let {
                                AgoraUIToast.error(context.applicationContext, text = error.msg)
                            }
                        }
                    })
                    finish()
                }
            }
        }
    }


    /**
     * Default setting of system devices as below:
     * - system speaker is turned on
     * - system camera is turned on for teachers, and turned off for students
     * - system microphone is turned on for teachers, and turned off for students
     * Note, this method is valid to use only after joining the classroom
     * successfully.
     */
    protected fun initSystemDevices() { // 打开语音，摄像头，麦克风
        eduCore()?.eduContextPool()?.mediaContext()?.openSystemDevice(AgoraEduContextSystemDevice.Speaker)
        eduCore()?.eduContextPool()?.userContext()?.getLocalUserInfo()?.let {
            if (it.role == AgoraEduContextUserRole.Teacher) {
                eduCore()?.eduContextPool()?.mediaContext()?.openSystemDevice(
                    if (AgoraUIDeviceSetting.isFrontCamera()) {
                        AgoraEduContextSystemDevice.CameraFront
                    } else {
                        AgoraEduContextSystemDevice.CameraBack
                    }
                )
                eduCore()?.eduContextPool()?.mediaContext()?.openSystemDevice(AgoraEduContextSystemDevice.Microphone)
            } else {
                eduCore()?.eduContextPool()?.mediaContext()?.closeSystemDevice(AgoraEduContextSystemDevice.CameraFront)
                eduCore()?.eduContextPool()?.mediaContext()?.closeSystemDevice(AgoraEduContextSystemDevice.CameraBack)
                eduCore()?.eduContextPool()?.mediaContext()?.closeSystemDevice(AgoraEduContextSystemDevice.Microphone)
            }
        }
    }

    protected fun openSystemDevices() { // 打开语音，摄像头，麦克风
        eduCore()?.eduContextPool()?.mediaContext()?.openSystemDevice(AgoraEduContextSystemDevice.Speaker)
        eduCore()?.eduContextPool()?.mediaContext()?.openSystemDevice(
            if (AgoraUIDeviceSetting.isFrontCamera()) {
                AgoraEduContextSystemDevice.CameraFront
            } else {
                AgoraEduContextSystemDevice.CameraBack
            }
        )
        eduCore()?.eduContextPool()?.mediaContext()?.openSystemDevice(AgoraEduContextSystemDevice.Microphone)
    }

    override fun onBackPressed() {
        //super.onBackPressed()
        classManager?.showLeaveRoom()
    }

    protected fun join() {
        LogX.e(TAG, "joinRoom start")
        setLowStream()
        eduCore()?.eduContextPool()?.streamContext()?.addHandler(streamHandler)
        eduCore()?.eduContextPool()?.roomContext()?.joinRoom(object : EduContextCallback<Unit> {
            override fun onSuccess(target: Unit?) {
                LogX.e(TAG, "joinRoom success")
                eduCore()?.setOnlyJoinRtcChannel(false)
                isCheckGroup.incrementAndGet()
                checkGroup()
                dismissLoading()
            }

            override fun onFailure(error: EduContextError?) {
                dismissLoading()
                LogX.e(TAG, "joinRoom error=${error?.msg} || ${error?.code}")
            }
        })
    }

    /**
     * 设置小流分辨率
     */
    fun setLowStream() {
        val lowStream = String.format(
            Locale.US,
            "{\"che.video.lowBitRateStreamParameter\": {\"width\":%d,\"height\":%d,\"frameRate\":%d,\"bitRate\":%d}}",
            FcrStreamParameters.LowStream.width,
            FcrStreamParameters.LowStream.height,
            FcrStreamParameters.LowStream.frameRate,
            FcrStreamParameters.LowStream.bitRate
        )
        eduCore()?.eduContextPool()?.streamContext()?.setRtcParameters(lowStream)
        LogX.i(TAG, "joinRoom lowStream = $lowStream")
    }

    override fun onDestroy() {
        super.onDestroy()
        rewardWindow = null
        classManager?.dismiss()
        fullLoading.dismiss()
        eduCore()?.eduContextPool()?.streamContext()?.removeHandler(streamHandler)
        uiHandler.removeCallbacksAndMessages(null)
        LogX.i(TAG, "Group dismissFullDialog $this")
    }

    override fun onRelease() {
        super.onRelease()
        getEduContext()?.monitorContext()?.removeHandler(monitorHandler)
        getEduContext()?.roomContext()?.removeHandler(myRoomHandler)
        getEduContext()?.userContext()?.removeHandler(userHandler)
        getEduContext()?.groupContext()?.removeHandler(groupHandler)
    }

    val monitorHandler = object : IMonitorHandler {
        override fun onLocalNetworkQualityUpdated(quality: EduContextNetworkQuality) {
        }

        override fun onLocalConnectionUpdated(state: EduContextConnectionState) {
            updateConnectionState(state)
        }
    }

    val myRoomHandler = object : RoomHandler() {
        override fun onRoomClosed() {
            super.onRoomClosed()
            classManager?.showDestroyRoom()
        }

        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)
            LogX.d(TAG, "->classroom ${roomInfo.roomUuid} joined success")
            isCheckGroup.incrementAndGet()
            uiController.setGrantedUsers()
            checkGroup()
        }

        override fun onJoinRoomFailure(roomInfo: EduContextRoomInfo, error: EduContextError) {
            super.onJoinRoomFailure(roomInfo, error)
            AgoraUIToast.error(applicationContext, text = error.msg)
            LogX.e(TAG, "->classroom ${roomInfo.roomUuid} joined fail:${error.msg} || ${error.code}")
        }

        override fun onClassStateUpdated(state: AgoraEduContextClassState) {
            super.onClassStateUpdated(state)
            LogX.d(TAG, "->class state updated: ${state.name}")
            val roomUuid = eduCore()?.eduContextPool()?.roomContext()?.getRoomInfo()?.roomUuid
            FCRHandlerManager.roomHandlerMap.forEach {
                if (roomUuid == it.key) {
                    it.value.onClassStateUpdated(state)
                }
            }
        }
    }
    val userHandler = object : UserHandler() {
        override fun onLocalUserKickedOut() {
            super.onLocalUserKickedOut()
            classManager?.showKickOut()
            val roomUuid = eduCore()?.eduContextPool()?.roomContext()?.getRoomInfo()?.roomUuid
            FCRHandlerManager.roomHandlerMap.forEach {
                if (roomUuid == it.key) {
                    it.value.onRoomStateUpdated(AgoraEduContextUserLeaveReason.KICKOUT)
                }
            }
        }

        // play reward animation
        override fun onUserRewarded(
            user: AgoraEduContextUserInfo,
            rewardCount: Int,
            operator: AgoraEduContextUserInfo?
        ) {
            super.onUserRewarded(user, rewardCount, operator)
            ContextCompat.getMainExecutor(context).execute {
                if (rewardWindow == null) {
                    rewardWindow = AgoraEduRewardWindow(context, user.userName)
                }
                if (rewardWindow?.isShowing == false) {
                    rewardWindow?.userName = user.userName
                    rewardWindow?.show()
                }
            }
        }
    }

    /**
     * 进入分组，大房间不要退出channel
     */
    fun stopMainClassVideoAudio() {
        // 取消订阅大房间音视流
        eduCore()?.eduContextPool()?.mediaContext()?.unPublish()

        eduCore()?.eduContextPool()?.roomContext()?.getRoomInfo()?.roomUuid?.let { roomUuid ->
            eduCore()?.eduContextPool()?.streamContext()?.getAllStreamList()?.forEach {
                eduCore()?.eduContextPool()?.mediaContext()?.stopPlayAudio(roomUuid, it.streamUuid)
                eduCore()?.eduContextPool()?.mediaContext()?.stopRenderVideo(it.streamUuid)
            }
        }
    }

    fun leaveRtcChannel() {// 进入分组，大房间不要退出channel
        // 房间的RTC
//        val roomUuid = eduCore()?.eduContextPool()?.roomContext()?.getRoomInfo()?.roomUuid
//        roomUuid?.let {
//            eduCore()?.leaveRtcChannel(roomUuid)
//        }
    }

    val groupHandler = object : FCRGroupHandler() {
        override fun onUserListInvitedToSubRoom(all: MutableList<FCRGroupInfo>, current: FCRGroupInfo?) {
            super.onUserListInvitedToSubRoom(all, current)
            current?.apply {
                //  收到邀请
                LogX.i(TAG, "Group 1、收到邀请分组")

                // 这里有个问题，加入到房间，会触发 onAllGroupUpdated
                accept(current)
            }
        }

        fun accept(current: FCRGroupInfo, isRetry: Boolean? = false) {
            classManager?.showGroupInvited(fullLoading, isJoining, current, isRetry) { state, groupUuid ->
                if (state == AgoraEduEvent.AgoraEduEventStartGroup) {
                    // 跳转分组
                    stopMainClassVideoAudio()
                } else if (state == AgoraEduEvent.AgoraEduEventReady) {
                    groupInvitedUuid = groupUuid
                    leaveRtcChannel()
                    releaseData()
                    finish()
                    // 欢迎加入{xxxx小组名}与大家互动讨论
                    AgoraUIToast.info(
                        applicationContext,
                        text = String.format(
                            context.resources.getString(R.string.fcr_group_enter_welcome),
                            current.payload.groupName
                        )
                    )
                } else {
                    // 加入失败，重新加入
                    isSowGroupDialog = true
                    dismissFullDialog()
                    dismissLoading()
                    setNotShowWhiteLoading()
                    classManager?.showJoinSubRoomAlert {
                        accept(current, true)
                    }
                }
            }
        }

        override fun onGroupInfoUpdated(groupInfo: FCREduContextGroupInfo) {
            super.onGroupInfoUpdated(groupInfo)
            LogX.i(TAG, "Group 是否开启分组：${groupInfo.state}")

            if (!groupInfo.state) {
                classManager?.dismissJoinDialog()
            }

            if (!groupInfo.state && getRoomType() == RoomType.GROUPING_CLASS) {  // 关闭分组，直接返回大房间
                ContextCompat.getMainExecutor(context).execute {
                    AgoraUIToast.info(applicationContext, text = resources.getString(R.string.fcr_group_back_main_room))
                    fullLoading.setContent(getString(R.string.fcr_group_back_main_room))
                    launchMainRoom()
                }
            }
        }

        override fun onSubRoomListRemoved(subRoomList: List<AgoraEduContextSubRoomInfo>) {
            super.onSubRoomListRemoved(subRoomList)
            // 删除分组
            ContextCompat.getMainExecutor(context).execute {
                val roomUuid = eduCore()?.eduContextPool()?.roomContext()?.getRoomInfo()?.roomUuid
                subRoomList.forEach { info ->
                    if (info.subRoomUuid == roomUuid) {
                        LogX.i(TAG, "Group 删除分组，返回到大房间")
                        classManager?.dismissJoinDialog()
                        AgoraUIToast.info(
                            applicationContext,
                            text = resources.getString(R.string.fcr_group_back_main_room)
                        )
                        fullLoading.setContent(getString(R.string.fcr_group_back_main_room))
                        launchMainRoom()
                    }
                }
            }
        }

        override fun onAllGroupUpdated(groupInfo: FCRAllGroupsInfo) {
            super.onAllGroupUpdated(groupInfo)
            LogX.e(TAG, "Group 分组数据：$groupInfo")

            synchronized(lockObject) {
                val userUuid = eduCore()?.eduContextPool()?.userContext()?.getLocalUserInfo()?.userUuid
                val roomUuid = eduCore()?.eduContextPool()?.roomContext()?.getRoomInfo()?.roomUuid
                //val info = groupInfo.details?.get(roomUuid)
                groupInfo.details?.forEach { group ->
                    group.value?.users?.forEach { user ->
                        if (userUuid == user.userUuid) { // 移动分组
                            LogX.i(TAG, "Group 移动分组：${userUuid} || ${user.userUuid}")

                            val groupInfo = FCREduContextGroupInfo()
                            groupInfo.groupName = group.value.groupName
                            groupInfo.groupUuid = group.value.groupUuid

                            ContextCompat.getMainExecutor(context).execute {
                                if (isJoining.get()) {
                                    LogX.i(TAG, "Group 正在进入教室，不操作")
                                    return@execute
                                }

                                if (roomUuid != null && roomUuid == groupInfo.groupUuid) {
                                    LogX.i(TAG, "Group 已经在这个组里面")
                                    return@execute
                                }

                                // 这个分组是通过邀请进入到
                                if (groupInvitedUuid == groupInfo.groupUuid) {
                                    LogX.i(TAG, "Group 这个分组是通过邀请进入到，不需要再加入了")
                                    groupInvitedUuid = null
                                    return@execute
                                }

                                if (getRoomType() == RoomType.GROUPING_CLASS) {
                                    // 切换到新的分组
                                    LogX.i(TAG, "Group 当前分组：$roomUuid , 切换到新的分组：${groupInfo.groupUuid}")

                                    AgoraUIToast.info(
                                        applicationContext,
                                        text = String.format(
                                            resources.getString(R.string.fcr_group_invitation),
                                            groupInfo.groupName
                                        )
                                    )

                                    isJoining.set(true)
                                    showFullDialog()
                                    classManager?.launchSubRoom(groupInfo, true) { state, groupUuid ->
                                        if (state == AgoraEduEvent.AgoraEduEventReady) {
                                            // 关闭当前分组的channel
                                            eduCore()?.eduContextPool()?.roomContext()
                                                ?.leaveRoom(object : EduContextCallback<Unit> {
                                                    override fun onSuccess(target: Unit?) {
                                                        releaseData()
                                                        finish()
                                                        isJoining.set(false)
                                                    }

                                                    override fun onFailure(error: EduContextError?) {
                                                        error?.let {
                                                            AgoraUIToast.error(applicationContext, text = error.msg)
                                                        }
                                                        dismissFullDialog()
                                                        isJoining.set(false)
                                                    }
                                                })
                                        } else {
                                            dismissFullDialog()
                                            isJoining.set(false)
                                        }
                                    }
                                } else {
                                    isJoining.set(true)
                                    LogX.i(TAG, "Group 当前是大房间，直接进入分组")
                                    // 直接进入小房间
                                    showFullDialog()
                                    stopMainClassVideoAudio()
                                    classManager?.launchSubRoom(groupInfo, false) { state, groupUuid ->
                                        if (state == AgoraEduEvent.AgoraEduEventReady) {
                                            leaveRtcChannel()
                                            releaseData()
                                            finish()
                                        } else {
                                            dismissFullDialog()
                                        }
                                        isJoining.set(false)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 返回到大教室
     */
    fun launchMainRoom() {
        eduCore()?.eduRoom?.leave(object : EduCallback<Unit> {
            override fun onSuccess(res: Unit?) {
                // 2、返回到大教室
                FCRGroupClassUtils.mainLaunchConfig?.let { launch ->
                    launch.fromPage = FCRGroupClassUtils.SUBROOM_PAGE
                    AgoraClassroomSDK.launch(context, launch) {
                        dismissFullDialog()

                        eduCore()?.eduContextPool()?.roomContext()?.getRoomInfo()?.roomUuid?.let {
                            LogX.e(TAG, "Media launchMainRoom-> ${it} reset media")
                            AgoraEduCoreManager.getEduCore(it)?.reset()
                        }
                        //eduCore()?.releaseData()
                        releaseRTC()
                        // 先离开大房间的channel
                        eduCore()?.leaveRtcChannel(launch.roomUuid)
                        releaseData()
                        finish()
                    }
                }
            }

            override fun onFailure(error: EduError) {
                LogX.i(TAG, "-> leave group failed:$error")
            }
        })
    }

    fun moveSubRoom(roomUuid: String?, toSubRoomUuid: String) {
        val group = FCRGroupClassUtils.allGroupInfo?.details?.get(toSubRoomUuid)
        group?.let {
            val groupInfo = FCREduContextGroupInfo()
            groupInfo.groupName = it.groupName
            groupInfo.groupUuid = it.groupUuid

            if (getRoomType() == RoomType.GROUPING_CLASS) {
                // 切换到新的分组
                LogX.i(TAG, "Group 当前分组：$roomUuid , 切换到新的分组：${toSubRoomUuid}")

                AgoraUIToast.info(
                    applicationContext,
                    text = String.format(
                        resources.getString(R.string.fcr_group_invitation),
                        groupInfo.groupName
                    )
                )

                isJoining.set(true)
                showFullDialog()
                classManager?.launchSubRoom(groupInfo, true) { state, groupUuid ->
                    if (state == AgoraEduEvent.AgoraEduEventReady) {
                        // 关闭当前分组的channel
                        eduCore()?.eduContextPool()?.roomContext()?.leaveRoom(object : EduContextCallback<Unit> {
                            override fun onSuccess(target: Unit?) {
                                releaseRTC()
                                releaseData()
                                finish()
                                isJoining.set(false)
                            }

                            override fun onFailure(error: EduContextError?) {
                                error?.let {
                                    AgoraUIToast.error(applicationContext, text = error.msg)
                                }
                                isJoining.set(false)
                            }
                        })
                    } else {
                        isJoining.set(false)
                        dismissFullDialog()
                    }
                }
            } else {
                isJoining.set(true)
                LogX.i(TAG, "Group 当前是大房间，直接进入分组")
                // 直接进入小房间
                showFullDialog()
                stopMainClassVideoAudio()
                classManager?.launchSubRoom(groupInfo, false) { state, groupUuid ->
                    if (state == AgoraEduEvent.AgoraEduEventReady) {
                        leaveRtcChannel()
                        releaseData()
                        finish()
                    } else {
                        dismissFullDialog()
                    }
                    isJoining.set(false)
                }
            }
        }
    }

    open fun leaveSubRoom() {

    }

    fun showFullDialog() {
        LogX.i(TAG, "Group showFullDialog $this")
        if (Looper.myLooper() == Looper.getMainLooper()) {
            fullLoading.dismiss()
            fullLoading.show()
        } else {
            runOnUiThread {
                fullLoading.dismiss()
                fullLoading.show()
            }
        }
    }

    fun dismissFullDialog() {
        LogX.i(TAG, "Group dismissFullDialog $this")
        if (Looper.myLooper() == Looper.getMainLooper()) {
            fullLoading.dismiss()
        } else {
            runOnUiThread {
                fullLoading.dismiss()
            }
        }
    }

    override fun getUIConfig(): FcrUIConfig {
        eduCore()?.config?.roomType?.let {
            return FcrUIConfigFactory.getConfig(it)
        }
        return FcrUIConfigFactory.getDefUIConfig()
    }
}
