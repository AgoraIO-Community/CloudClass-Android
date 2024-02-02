package io.agora.online.sdk.common

import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import io.agora.online.component.AgoraEduRewardWindow
import io.agora.online.component.common.IAgoraUIProvider
import io.agora.online.helper.AgoraUIDeviceSetting
import io.agora.online.helper.FcrHandsUpManager
import io.agora.online.view.AgoraEduFullLoadingDialog
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.AgoraEduCoreManager
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.context.user.FcrEventBatch
import io.agora.agoraeducore.core.context.user.FcrUserPropertiesEvent
import io.agora.agoraeducore.core.group.FCRGroupClassUtils
import io.agora.agoraeducore.core.group.FCRGroupHandler
import io.agora.agoraeducore.core.group.FcrGroupUserManager
import io.agora.agoraeducore.core.group.bean.AgoraEduContextSubRoomInfo
import io.agora.agoraeducore.core.group.bean.FCRAllGroupsInfo
import io.agora.agoraeducore.core.group.bean.FCREduContextGroupInfo
import io.agora.agoraeducore.core.group.bean.FCRGroupInfo
import io.agora.agoraeducore.core.internal.education.impl.cmd.manager.FCRGroupCMDManager
import io.agora.agoraeducore.core.internal.framework.data.EduCallback
import io.agora.agoraeducore.core.internal.framework.data.EduError
import io.agora.agoraeducore.core.internal.framework.impl.handler.MonitorHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.StreamHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.UserHandler
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.launch.AgoraEduEvent
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.core.internal.state.FCRHandlerManager
import io.agora.online.sdk.ui.AgoraClassUIController
import io.agora.online.R
import io.agora.online.component.toast.AgoraUIToast
import io.agora.online.config.FcrUIConfig
import io.agora.online.config.FcrUIConfigFactory
import io.agora.online.provider.UIDataProvider
import io.agora.online.sdk.AgoraOnlineClassroomSDK
import io.agora.online.sdk.helper.FcrStreamParameters
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 教室基类
 */
abstract class AgoraEduClassActivity : AgoraBaseClassActivity(), IAgoraUIProvider, IAgoraConfig {
    override var TAG = "AgoraEduClassActivity"
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
    var groupInvitedUuid: String? = null

    /**
     * 正在加入房间
     */
    var isJoining = AtomicBoolean(false)

    private val streamHandler = object : StreamHandler() {
        override fun onStreamJoined(streamInfo: AgoraEduContextStreamInfo, operator: AgoraEduContextUserInfo?) {
            super.onStreamJoined(streamInfo, operator)
            if (localStreamInfo == null && streamInfo.owner.userUuid == eduCore()?.eduContextPool()?.userContext()
                    ?.getLocalUserInfo()?.userUuid) {
                localStreamInfo = streamInfo
            }
        }

        override fun onStreamLeft(streamInfo: AgoraEduContextStreamInfo, operator: AgoraEduContextUserInfo?) {
            super.onStreamLeft(streamInfo, operator)
            if (streamInfo.owner.userUuid == eduCore()?.eduContextPool()?.userContext()?.getLocalUserInfo()?.userUuid) {
                eduCore()?.eduContextPool()?.streamContext()?.unPublishLocalStream()
            }
        }

        override fun onStreamUpdated(streamInfo: AgoraEduContextStreamInfo, operator: AgoraEduContextUserInfo?) {
            super.onStreamUpdated(streamInfo, operator)
            updateDevice(streamInfo)
            eduCore()?.eduContextPool()?.userContext()?.getLocalUserInfo()?.let { localUser ->
                if (localUser.role == AgoraEduContextUserRole.Student
                    && localUser.userUuid == streamInfo.owner.userUuid
                    && operator?.role != AgoraEduContextUserRole.Student) {

                    if (localStreamInfo?.videoState?.value != streamInfo.videoState.value) {//如果上一次的视频状态和本次的视频状态不一样
                        when (streamInfo.streamType) {
                            AgoraEduContextMediaStreamType.Video, AgoraEduContextMediaStreamType.Both -> {//更新视频状态的提示
//                                AgoraUIToast.info(
//                                    applicationContext,
//                                    text = String.format(context.getString(R.string.fcr_stream_start_video))
//                                )
                            }
                            AgoraEduContextMediaStreamType.Audio, AgoraEduContextMediaStreamType.None -> {
                                AgoraUIToast.error(
                                    applicationContext,
                                    text = String.format(context.getString(R.string.fcr_switch_tips_banned_video))
                                )
                            }
                        }
                    }

                    if (localStreamInfo?.audioState?.value != streamInfo.audioState.value) {
                        when (streamInfo.streamType) {
                            AgoraEduContextMediaStreamType.Audio, AgoraEduContextMediaStreamType.Both -> {
//                                AgoraUIToast.info(
//                                    applicationContext,
//                                    text = String.format(context.getString(R.string.fcr_stream_start_audio))
//                                )
                            }
                            AgoraEduContextMediaStreamType.Video, AgoraEduContextMediaStreamType.None -> {
                                AgoraUIToast.error(
                                    applicationContext,
                                    text = String.format(context.getString(R.string.fcr_switch_tips_muted))
                                )
                            }
                        }
                    }
                    localStreamInfo = streamInfo//保存本地的流信息
                }
            }
        }
    }

    fun updateDevice(streamInfo: AgoraEduContextStreamInfo) {
        eduCore()?.eduContextPool()?.userContext()?.getLocalUserInfo()?.let { localUser ->
            if (localUser.userUuid == streamInfo.owner.userUuid) {
                if (streamInfo.videoState == AgoraEduMediaState.Off) {
                    val device = if (AgoraUIDeviceSetting.isFrontCamera()) {
                        AgoraEduContextSystemDevice.CameraFront
                    } else {
                        AgoraEduContextSystemDevice.CameraBack
                    }

                    FcrHandsUpManager.getDeviceState(eduCore(), device){
                        if(it){
                            getEduContext()?.mediaContext()?.closeSystemDevice(device)
                        }
                    }
                }

                if (streamInfo.audioState == AgoraEduMediaState.Off) {
                    FcrHandsUpManager.getDeviceState(eduCore(), AgoraEduContextSystemDevice.Microphone){
                        if(it){
                            getEduContext()?.mediaContext()?.closeSystemDevice(AgoraEduContextSystemDevice.Microphone)
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this
        fullLoading = AgoraEduFullLoadingDialog(context)
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
        if (eduCore()?.eduContextPool()?.roomContext()?.getRoomInfo()?.roomType?.value == RoomType.GROUPING_CLASS.value) {
            //LogX.e(TAG, "checkGroup : allGroupInfo -> ${FCRGroupClassUtils.allGroupInfo}")

            if (FCRGroupClassUtils.allGroupInfo == null || FCRGroupClassUtils.allGroupInfo?.details == null) { // 分组关闭
                ContextCompat.getMainExecutor(this).execute {
                    val info = FCREduContextGroupInfo()
                    info.state = false
                    groupHandler.onGroupInfoUpdated(info)
                }
            } else {
                FCRGroupClassUtils.mainLaunchConfig?.apply {
                    val eduContextPool = AgoraEduCoreManager.getEduCore(roomUuid)?.eduContextPool()
                    eduContextPool?.groupContext()?.addHandler(groupHandler)
                    AgoraEduCoreManager.getEduCore(roomUuid)?.room()?.roomProperties?.get("groups")?.let {
                        val groups = it as Map<*, *>
                        FCRGroupCMDManager(eduCore()!!).handleAllInfoGroup(groups)
                    }
                }
            }
        } else {
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
                dismissLoading()
                localStreamInfo = eduCore()?.eduContextPool()?.streamContext()?.getMyStreamInfo()
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
        FcrHandsUpManager.removeHandsUpListener(eduCore())
        LogX.i(TAG, "Group dismissFullDialog $this")
    }

    override fun finish() {
        super.finish()
        removeHandler()
    }
    override fun onRelease() {
        super.onRelease()
        removeHandler()
    }

    fun removeHandler() {
        getUIDataProvider()?.release()
        getEduContext()?.monitorContext()?.removeHandler(monitorHandler)
        getEduContext()?.roomContext()?.removeHandler(myRoomHandler)
        getEduContext()?.userContext()?.removeHandler(userHandler)
        getEduContext()?.groupContext()?.removeHandler(groupHandler)
    }

    val monitorHandler = object : MonitorHandler() {
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
        override fun onRemoteUserLeft(
            user: AgoraEduContextUserInfo,
            operator: AgoraEduContextUserInfo?,
            reason: EduContextUserLeftReason
        ) {
            super.onRemoteUserLeft(user, operator, reason)
            //FcrHandsUpManager.remove(user.userUuid)
        }
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

        override fun onUserRewardedList(
            list: List<FcrUserPropertiesEvent>,
            batch: FcrEventBatch,
            cause: Map<String, Any>,
            operator: AgoraEduContextUserInfo?
        ) {
            super.onUserRewardedList(list, batch, cause, operator)
            ContextCompat.getMainExecutor(context).execute {
                if (rewardWindow == null) {
                    rewardWindow = AgoraEduRewardWindow(context)
                }
                if (rewardWindow?.isShowing == false) {
                    rewardWindow?.setShowTips(getTips(list))
                    rewardWindow?.show()
                }
            }
        }

        fun getTips(list: List<FcrUserPropertiesEvent>): String {
            var size = list.size
            var name = ""
            if (size > 3) {
                size = 3
            }

            for (i in 0 until size) {
                name += list[i]?.user?.userName
                if (i != (size - 1)) {
                    name += "、"
                }
            }

            val tips = if (list.size > 3) String.format(
                context.resources.getString(R.string.fcr_room_tips_reward_congratulation_multiplayer),
                name,
                "" + list.size
            ) else String.format(context.resources.getString(R.string.fcr_room_tips_reward_congratulation_single), name)

            return tips
        }

        // play reward animation
        override fun onUserRewarded(
            user: AgoraEduContextUserInfo,
            rewardCount: Int,
            operator: AgoraEduContextUserInfo?
        ) {
            super.onUserRewarded(user, rewardCount, operator)
            ContextCompat.getMainExecutor(context).execute {
                val tips = String.format(context.resources.getString(R.string.fcr_room_tips_reward_congratulation_single), user.userName)
                if (rewardWindow == null) {
                    rewardWindow = AgoraEduRewardWindow(context)
                }
                if (rewardWindow?.isShowing == false) {
                    rewardWindow?.setShowTips(tips)
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

    fun leaveRtcChannel() { // 房间的RTC
        val roomUuid = eduCore()?.eduContextPool()?.roomContext()?.getRoomInfo()?.roomUuid
        roomUuid?.let {
            eduCore()?.leaveRtcChannel(roomUuid)
            eduCore()?.eduRoom?.leaveRtcChannelForAPI()
        }
    }

    var isJoinMainRoom = AtomicBoolean(false)

    val groupHandler = object : FCRGroupHandler() {
        override fun onUserListInvitedToSubRoom(all: MutableList<FCRGroupInfo>, current: FCRGroupInfo?) {
            super.onUserListInvitedToSubRoom(all, current)
            current?.apply {
                //  收到邀请
                LogX.i(TAG, "Group 1、收到邀请分组")
                cancelHandsUp()
                // 这里有个问题，加入到房间，会触发 onAllGroupUpdated
                accept(current)
            }
        }

        fun accept(current: FCRGroupInfo, isRetry: Boolean? = false) {
            classManager?.showGroupInvited(fullLoading, isJoining, current, isRetry) { code, state, groupUuid ->
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
                    if (code != 404) { // 404: group release
                        classManager?.showJoinSubRoomAlert {
                            accept(current, true)
                        }
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
                if(!groupInfo.state){
                    FcrGroupUserManager.clearGroupList()
                }
                ContextCompat.getMainExecutor(context).execute {
                    if (!isJoinMainRoom.get()) {
                        isJoinMainRoom.set(true)
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
                            LogX.i(TAG, "Group 移动分组")

                            val groupInfo = FCREduContextGroupInfo()
                            groupInfo.groupName = group.value.groupName
                            groupInfo.groupUuid = group.value.groupUuid

                            ContextCompat.getMainExecutor(context).execute {
                                cancelHandsUp()

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
                                    classManager?.launchSubRoom(groupInfo, true) { code, state, groupUuid ->
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
                                    classManager?.launchSubRoom(groupInfo, false) { code, state, groupUuid ->
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
                    AgoraOnlineClassroomSDK.launch(context, launch) {
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
                isJoinMainRoom.set(false)
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
                classManager?.launchSubRoom(groupInfo, true) { code, state, groupUuid ->
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
                classManager?.launchSubRoom(groupInfo, false) { code, state, groupUuid ->
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

    open fun cancelHandsUp(){}

    override fun getUIConfig(): FcrUIConfig {
        eduCore()?.config?.roomType?.let {
            return FcrUIConfigFactory.getConfig(it)
        }
        return FcrUIConfigFactory.getDefUIConfig()
    }
}
