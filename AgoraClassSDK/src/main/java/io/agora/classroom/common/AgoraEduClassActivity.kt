package io.agora.classroom.common

import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.agora.edu.component.AgoraEduRewardWindow
import com.agora.edu.component.common.IAgoraUIProvider
import com.agora.edu.component.helper.AgoraUIDeviceSetting
import com.agora.edu.component.view.AgoraEduFullLoadingDialog
import com.google.gson.Gson
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.AgoraEduCoreManager
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.group.FCRGroupClassUtils
import io.agora.agoraeducore.core.group.FCRGroupHandler
import io.agora.agoraeducore.core.group.bean.*
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.education.impl.cmd.manager.FCRGroupCMDManager
import io.agora.agoraeducore.core.internal.framework.data.EduCallback
import io.agora.agoraeducore.core.internal.framework.data.EduError
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.UserHandler
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.state.FCRHandlerManager
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.agoraeduuikit.provider.UIDataProvider
import io.agora.classroom.sdk.AgoraClassroomSDK
import io.agora.classroom.ui.AgoraClassUIController
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 教室基类
 */
abstract class AgoraEduClassActivity : AgoraBaseClassActivity(), IAgoraUIProvider {
    private val TAG = "AgoraEduClassActivity"
    protected var uiController: AgoraClassUIController = AgoraClassUIController()

    /**
     * 页面ID
     */
    protected var uuid: String = UUID.randomUUID().toString()
    private var rewardWindow: AgoraEduRewardWindow? = null
    protected lateinit var fullLoading: AgoraEduFullLoadingDialog
    protected lateinit var context: AgoraBaseClassActivity
    private var isCheckGroup = AtomicInteger(0)
    protected var lockObject = Any()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this
        fullLoading = AgoraEduFullLoadingDialog(context)
        Constants.AgoraLog?.i("Group create fullLoading $this")
    }

    override fun onCreateEduCore(isSuccess: Boolean) {
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
        Constants.AgoraLog?.e("connectionState -> $connectionState")

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

            eduCore()?.eduContextPool()?.roomContext()?.leaveRoom(object : EduContextCallback<Unit> {
                override fun onSuccess(target: Unit?) {
                    val roomUuid = eduCore()?.eduContextPool()?.roomContext()?.getRoomInfo()?.roomUuid
                    FCRHandlerManager.roomHandlerMap.forEach {
                        if (roomUuid == it.key) {
                            it.value.onRoomStateUpdated(AgoraEduContextUserLeaveReason.KICKOUT)
                        }
                    }
                    finish()
                }

                override fun onFailure(error: EduContextError?) {
                }
            })
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
                eduCore()?.eduContextPool()?.mediaContext()?.apply {
                    openSystemDevice(
                        if (AgoraUIDeviceSetting.isFrontCamera()) {
                            AgoraEduContextSystemDevice.CameraFront
                        } else {
                            AgoraEduContextSystemDevice.CameraBack
                        }
                    )
                    openSystemDevice(AgoraEduContextSystemDevice.Microphone)
                }
            } else {
                eduCore()?.eduContextPool()?.mediaContext()?.apply {
                    closeSystemDevice(AgoraEduContextSystemDevice.CameraFront)
                    closeSystemDevice(AgoraEduContextSystemDevice.CameraBack)
                    closeSystemDevice(AgoraEduContextSystemDevice.Microphone)
                }
            }
        }
    }

    override fun onBackPressed() {
        //super.onBackPressed()
        classManager?.showLeaveRoom()
    }

    protected fun join() {
        Constants.AgoraLog?.e("joinRoom start")
        eduCore()?.eduContextPool()?.roomContext()?.joinRoom(object : EduContextCallback<Unit> {
            override fun onSuccess(target: Unit?) {
                Constants.AgoraLog?.e("joinRoom success")
                eduCore()?.setOnlyJoinRtcChannel(false)
                isCheckGroup.incrementAndGet()
                checkGroup()
                dismissLoading()
            }

            override fun onFailure(error: EduContextError?) {
                dismissLoading()
                Constants.AgoraLog?.e("joinRoom error=${error?.msg} || ${error?.code}")
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        rewardWindow = null
        fullLoading.dismiss()
        uiHandler.removeCallbacksAndMessages(null)
        Constants.AgoraLog?.i("Group dismissFullDialog $this")
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
            Constants.AgoraLog?.d("$TAG->classroom ${roomInfo.roomUuid} joined success")
            isCheckGroup.incrementAndGet()
            checkGroup()
        }

        override fun onJoinRoomFailure(roomInfo: EduContextRoomInfo, error: EduContextError) {
            super.onJoinRoomFailure(roomInfo, error)
            AgoraUIToast.error(applicationContext, text = error.msg)
            Constants.AgoraLog?.e("$TAG->classroom ${roomInfo.roomUuid} joined fail:${Gson().toJson(error)}")

        }

        override fun onClassStateUpdated(state: AgoraEduContextClassState) {
            super.onClassStateUpdated(state)
            Constants.AgoraLog?.d("$TAG->class state updated: ${state.name}")
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
                    rewardWindow = AgoraEduRewardWindow(context)
                }
                if (rewardWindow?.isShowing == false) {
                    rewardWindow?.show()
                }
            }
        }
    }

    fun leaveRtcChannel() {
        // 房间的RTC
        val roomUuid = eduCore()?.eduContextPool()?.roomContext()?.getRoomInfo()?.roomUuid
        roomUuid?.let {
            eduCore()?.leaveRtcChannel(roomUuid)
        }
    }

    var groupInvitedUuid: String? = null

    /**
     * 正在加入房间
     */
    var isJoining = AtomicBoolean(false)

    val groupHandler = object : FCRGroupHandler() {
        override fun onUserListInvitedToSubRoom(all: MutableList<FCRGroupInfo>, current: FCRGroupInfo?) {
            super.onUserListInvitedToSubRoom(all, current)
            current?.apply {
                //  收到邀请
                Constants.AgoraLog?.i("Group 1、收到邀请分组")

                // 这里有个问题，加入到房间，会触发 onAllGroupUpdated
                classManager?.showGroupInvited(fullLoading, isJoining, current) { groupUuid ->
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
                }
            }
        }

        override fun onGroupInfoUpdated(groupInfo: FCREduContextGroupInfo) {
            super.onGroupInfoUpdated(groupInfo)
            Constants.AgoraLog?.i("Group 是否开启分组：${groupInfo.state}")

            if (!groupInfo.state && getRoomType() == RoomType.GROUPING_CLASS) {  // 关闭分组，直接返回大房间
                ContextCompat.getMainExecutor(context).execute {
                    AgoraUIToast.info(applicationContext, text = resources.getString(R.string.fcr_group_close_group))
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
                        Constants.AgoraLog?.i("Group 删除分组，返回到大房间")
                        fullLoading.setContent(getString(R.string.fcr_group_back_main_room))
                        launchMainRoom()
                    }
                }
            }
        }

        override fun onAllGroupUpdated(groupInfo: FCRAllGroupsInfo) {
            super.onAllGroupUpdated(groupInfo)
            Constants.AgoraLog?.e("Group 分组数据：$groupInfo")

            synchronized(lockObject) {
                val userUuid = eduCore()?.eduContextPool()?.userContext()?.getLocalUserInfo()?.userUuid
                val roomUuid = eduCore()?.eduContextPool()?.roomContext()?.getRoomInfo()?.roomUuid
                val info = groupInfo.details?.get(roomUuid)
                groupInfo.details?.forEach { group ->
                    group.value?.users?.forEach { user ->
                        if (userUuid == user.userUuid) { // 移动分组
                            val groupInfo = FCREduContextGroupInfo()
                            groupInfo.groupName = group.value.groupName
                            groupInfo.groupUuid = group.value.groupUuid

                            ContextCompat.getMainExecutor(context).execute {
                                if (isJoining.get()) {
                                    Constants.AgoraLog?.i("Group 正在进入教室，不操作")
                                    return@execute
                                }

                                if (roomUuid != null && roomUuid == groupInfo.groupUuid) {
                                    Constants.AgoraLog?.i("Group 已经在这个组里面")
                                    return@execute
                                }

                                // 这个分组是通过邀请进入到
                                if (groupInvitedUuid == groupInfo.groupUuid) {
                                    Constants.AgoraLog?.e("Group 这个分组是通过邀请进入到，不需要再加入了")
                                    groupInvitedUuid = null
                                    return@execute
                                }

                                if (getRoomType() == RoomType.GROUPING_CLASS) {
                                    // 切换到新的分组
                                    Constants.AgoraLog?.i("Group 当前分组：$roomUuid , 切换到新的分组：${groupInfo.groupUuid}")

                                    AgoraUIToast.info(
                                        applicationContext,
                                        text = String.format(
                                            resources.getString(R.string.fcr_group_invitation),
                                            groupInfo.groupName
                                        )
                                    )

                                    isJoining.set(true)
                                    showFullDialog()
                                    classManager?.launchSubRoom(groupInfo, true) {
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
                                    }
                                } else {
                                    isJoining.set(true)
                                    Constants.AgoraLog?.i("Group 当前是大房间，直接进入分组")
                                    // 直接进入小房间
                                    showFullDialog()
                                    classManager?.launchSubRoom(groupInfo, false) {
                                        leaveRtcChannel()
                                        releaseData()
                                        finish()
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
                        AgoraEduCoreManager.getEduCore(launch.roomUuid)?.reset()
                        //eduCore()?.releaseData()
                        releaseRTC()
                        releaseData()
                        finish()
                    }
                }
            }

            override fun onFailure(error: EduError) {
                Constants.AgoraLog?.e("$TAG-> leave group failed:$error")
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
                Constants.AgoraLog?.i("Group 当前分组：$roomUuid , 切换到新的分组：${toSubRoomUuid}")

                AgoraUIToast.info(
                    applicationContext,
                    text = String.format(
                        resources.getString(R.string.fcr_group_invitation),
                        groupInfo.groupName
                    )
                )

                isJoining.set(true)
                showFullDialog()
                classManager?.launchSubRoom(groupInfo, true) {
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
                }
            } else {
                isJoining.set(true)
                Constants.AgoraLog?.i("Group 当前是大房间，直接进入分组")
                // 直接进入小房间
                showFullDialog()
                classManager?.launchSubRoom(groupInfo, false) {
                    leaveRtcChannel()
                    releaseData()
                    finish()
                    isJoining.set(false)
                }
            }
        }
    }

    open fun leaveSubRoom() {

    }

    fun showFullDialog() {
        Constants.AgoraLog?.i("Group showFullDialog $this")
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
        Constants.AgoraLog?.i("Group dismissFullDialog $this")
        if (Looper.myLooper() == Looper.getMainLooper()) {
            fullLoading.dismiss()
        } else {
            runOnUiThread {
                fullLoading.dismiss()
            }
        }
    }
}
