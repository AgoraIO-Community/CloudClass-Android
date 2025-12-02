package io.agora.classroom.common

import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.RadioGroup
import androidx.core.content.ContextCompat
import com.agora.edu.component.view.AgoraEduFullLoadingDialog
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.AgoraEduCoreManager
import io.agora.agoraeducore.core.context.AgoraEduContextUserLeaveReason
import io.agora.agoraeducore.core.context.EduContextCallback
import io.agora.agoraeducore.core.context.EduContextError
import io.agora.agoraeducore.core.group.FCRGroupClassUtils
import io.agora.agoraeducore.core.group.bean.FCREduContextGroupInfo
import io.agora.agoraeducore.core.group.bean.FCRGroupInfo
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.education.impl.network.HttpBaseRes
import io.agora.agoraeducore.core.internal.education.impl.network.HttpCallback
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.launch.AgoraEduEvent
import io.agora.agoraeducore.core.internal.launch.AgoraEduLaunchConfig
import io.agora.agoraeducore.core.internal.launch.AgoraEduLaunchConfigClone
import io.agora.agoraeducore.core.internal.launch.AgoraEduRoleType
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.core.internal.state.FCRHandlerManager
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.component.dialog.AgoraUICustomDialogBuilder
import io.agora.agoraeduuikit.component.dialog.AgoraUIDialog
import io.agora.agoraeduuikit.component.dialog.AgoraUIDialogBuilder
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.classroom.sdk.AgoraClassroomSDK
import java.util.concurrent.atomic.AtomicBoolean

/**
 * author : felix
 * date : 2022/4/7
 * description :
 */
class AgoraEduClassManager(var context: AgoraBaseClassActivity, var eduCore: AgoraEduCore?) {
    private var destroyClassDialog: AgoraUIDialog? = null
    private var lockObject = Any()
    private var acceptList = mutableListOf<String>() // 已经同意加入的组
    var inviteGroupDialog: AgoraUIDialog? = null
    var joinGroupDialog: AgoraUIDialog? = null

    fun dismissJoinDialog() {
        inviteGroupDialog?.dismiss()
        joinGroupDialog?.dismiss()
    }

    fun showJoinSubRoomAlert(onClickListener: (() -> Unit)?) {
        ContextCompat.getMainExecutor(context).execute {
            joinGroupDialog = AgoraUIDialogBuilder(context)
                .setCanceledOnTouchOutside(false)
                .title(context.resources.getString(R.string.fcr_group_join))
                .message(context.resources.getString(R.string.fcr_group_join_error))
                .positiveText(context.resources.getString(R.string.fcr_group_button_join))
                .positiveClick {
                    onClickListener?.invoke()
                }
                .build()
            joinGroupDialog?.show()
        }
    }

    /**
     * 弹出接收邀请的Alert
     */
    fun showGroupInvited(
        fullLoading: AgoraEduFullLoadingDialog,
        isJoining: AtomicBoolean,
        info: FCRGroupInfo,
        isRetry: Boolean? = false, // 是否是重试
        onAcceptListener: ((Int, AgoraEduEvent, String?) -> Unit)?
    ) {
        if (isRetry != true) {
            acceptList.clear()
        }
        ContextCompat.getMainExecutor(context).execute {
            if (isRetry == true) {
                info.payload?.let { groupInfo ->
                    accept(fullLoading, isJoining, groupInfo, isRetry, onAcceptListener)
                }
            } else {
                inviteGroupDialog?.dismiss()
                inviteGroupDialog = AgoraUIDialogBuilder(context)
                    .title(context.resources.getString(R.string.fcr_group_join))
                    .message(
                        String.format(
                            context.resources.getString(R.string.fcr_group_invitation),
                            info.payload.groupName ?: ""
                        )
                    )
                    .negativeText(context.resources.getString(R.string.fcr_group_button_later))
                    .positiveText(context.resources.getString(R.string.fcr_group_button_join))
                    .positiveClick {
                        info.payload?.let { groupInfo ->
                            inviteGroupDialog?.dismiss()
                            // 同意进入小组
                            fullLoading.dismiss()

                            fullLoading.setContent(
                                String.format(
                                    context.getString(R.string.fcr_group_joining),
                                    groupInfo.groupName
                                )
                            )

                            accept(fullLoading, isJoining, groupInfo, isRetry, onAcceptListener)
                        }
                    }
                    .build()
                inviteGroupDialog?.show()
            }
        }
    }

    fun accept(
        fullLoading: AgoraEduFullLoadingDialog,
        isJoining: AtomicBoolean,
        groupInfo: FCREduContextGroupInfo,
        isRetry: Boolean? = false, // 是否是重试
        onAcceptListener: ((Int, AgoraEduEvent, String?) -> Unit)?
    ) {
        val roomUUid = eduCore?.eduContextPool()?.roomContext()?.getRoomInfo()?.roomUuid
        if (!TextUtils.isEmpty(roomUUid)) {
            isJoining.set(true)
            if (!fullLoading.isShowing) {
                fullLoading.show()
            }

            if (isRetry == true && acceptList.contains(groupInfo.groupUuid)) {
                LogX.i("Group 2、同意加入到分组")
                launchSubRoom(groupInfo, false, isRetry, onAcceptListener)
            } else {
                eduCore?.eduContextPool()?.groupContext()?.acceptInviteUserToSubRoom(
                    Constants.APPID, roomUUid!!, groupInfo.groupUuid,
                    object : HttpCallback<HttpBaseRes<String>>() {
                        override fun onSuccess(result: HttpBaseRes<String>?) {
                            if (isRetry != true) {
                                acceptList.add(groupInfo.groupUuid)
                            }
                            onAcceptListener?.invoke(200, AgoraEduEvent.AgoraEduEventStartGroup, "")
                            LogX.i("Group 2、同意加入到分组")
                            launchSubRoom(groupInfo, false, isRetry, onAcceptListener)
                        }

                        override fun onError(httpCode: Int, code: Int, message: String?) {
                            super.onError(httpCode, code, message)
                            onAcceptListener?.invoke(httpCode, AgoraEduEvent.AgoraEduEventFailed, message)
                            // fullLoading.dismiss()
                        }

                        override fun onComplete() {
                            super.onComplete()
                            LogX.e("Group onComplete=" + isJoining.get())
                            isJoining.set(false)
                        }
                    })
            }
        }
    }

    fun launchSubRoom(
        groupInfo: FCREduContextGroupInfo,
        isChangeGroup: Boolean, // 是否是换组
        isRetry: Boolean? = false, // 是否是重试
        onAcceptListener: ((Int,AgoraEduEvent, String?) -> Unit)?
    ) {
        synchronized(lockObject) {
            val launchConfig = AgoraClassroomSDK.getCurrentLaunchConfig()

            launchConfig?.let {
                // 避免加入分组失败，没有大房间数据
                if (launchConfig.roomType != RoomType.GROUPING_CLASS.value && !isChangeGroup) {
                    saveMainClassData(groupInfo, launchConfig)
                }

                if (isChangeGroup) { // 切换分组
                    FCREduContextGroupInfo().apply {
                        groupName = groupInfo.groupName
                        groupUuid = groupInfo.groupUuid
                        state = true
                        FCRGroupClassUtils.groupInfo = this
                    }
                }

                val launch = AgoraEduLaunchConfigClone.deepClone(launchConfig)
                launch.roomType = RoomType.GROUPING_CLASS.value
                launch.roleType = AgoraEduRoleType.AgoraEduRoleTypeStudent.value
                launch.roomName = groupInfo.groupName
                launch.roomUuid = groupInfo.groupUuid
                launch.fromPage = 0
                launch.duration = null // 小房间不用传递 duration
                launch.isShowToastError = false

                AgoraClassroomSDK.launch(context, launch) {
                    //fullLoading.dismiss()
                    LogX.i("Group 3、launch isRetry = " + isRetry + "||isChangeGroup=${isChangeGroup}")

                    // 保留大房间数据
                    if (it == AgoraEduEvent.AgoraEduEventReady && !isChangeGroup && launchConfig.roomType
                        != RoomType.GROUPING_CLASS.value // 避免重新加入，数据是分组
                    ) {
                        saveMainClassData(groupInfo, launchConfig)
                    }
                    // 重制摄像头数据
                    if (isChangeGroup) {
                        LogX.e("Media launchSubRoom(move gorup)-> ${launchConfig.roomUuid} reset media")
                        AgoraEduCoreManager.getEduCore(launchConfig.roomUuid)?.reset()
                    } else {
                        FCRGroupClassUtils.mainRoomInfo?.roomUuid?.let {
                            LogX.e("Media launchSubRoom-> ${it} reset media")
                            AgoraEduCoreManager.getEduCore(it)?.reset()
                        }
                    }
                    onAcceptListener?.invoke(200, it, groupInfo.groupUuid)
                }
            }
        }
    }

    fun saveMainClassData(groupInfo: FCREduContextGroupInfo, launchConfig: AgoraEduLaunchConfig) {
        FCREduContextGroupInfo().apply {
            groupName = groupInfo.groupName
            groupUuid = groupInfo.groupUuid
            state = true
            FCRGroupClassUtils.groupInfo = this
        }

        FCRGroupClassUtils.mainRoomInfo = eduCore?.eduContextPool()?.roomContext()?.getRoomInfo()

        FCRGroupClassUtils.mainClassRoomInfo.clear()
        eduCore?.eduContextPool()?.userContext()?.getAllUserList()?.forEach {
            FCRGroupClassUtils.mainClassRoomInfo.add(it)
        }
        FCRGroupClassUtils.mainLaunchConfig = AgoraEduLaunchConfigClone.deepClone(launchConfig)
        LogX.i("Group 4、launch 保存大房间数据：${FCRGroupClassUtils.mainLaunchConfig?.roomUuid}")
    }

    /**
     * 踢出教室
     */
    fun showKickOut() {
        ContextCompat.getMainExecutor(context).execute {
            if (!context.isDestroyPage()) {
                leaveRoom()
                AgoraUIDialogBuilder(context)
                    .title(context.resources.getString(R.string.fcr_user_local_kick_out_notice))
                    .message(context.resources.getString(R.string.fcr_user_local_kick_out))
                    .positiveText(context.resources.getString(R.string.fcr_user_kick_out_submit))
                    .positiveClick {
                        context.finish()
                    }
                    .build()
                    .show()
            }
        }
    }

    /**
     * 退出教室
     */
    fun showLeaveRoom() {
        AgoraUIDialogBuilder(context)
            .title(context.resources.getString(R.string.fcr_room_class_leave_class_title))
            .message(context.resources.getString(R.string.fcr_room_exit_warning))
            .negativeText(context.resources.getString(R.string.fcr_user_kick_out_cancel))
            .positiveText(context.resources.getString(R.string.fcr_user_kick_out_submit))
            .positiveClick {
                // 确保能退出页面
                if (eduCore == null || eduCore?.eduContextPool() == null
                    || eduCore?.eduContextPool()?.roomContext() == null
                ) {
                    context.finish()
                }

                eduCore?.eduContextPool()?.roomContext()?.leaveRoom(object : EduContextCallback<Unit> {
                    override fun onSuccess(target: Unit?) {
                        val roomUuid = eduCore?.eduContextPool()?.roomContext()?.getRoomInfo()?.roomUuid
                        FCRHandlerManager.roomHandlerMap.forEach {
                            if (roomUuid == it.key) {
                                it.value.onRoomStateUpdated(AgoraEduContextUserLeaveReason.NORMAL)
                            }
                        }
                        context.finish()// 须leaveRoom回调成功后销毁页面
                    }

                    override fun onFailure(error: EduContextError?) {
                        error?.let {
                            AgoraUIToast.error(context.applicationContext, text = error.msg)
                        }
                    }
                })
            }
            .build()
            .show()
    }

    /**
     * 离开分组
     */
    fun showLeaveGroupRoom(callback: (Boolean) -> Unit) {
        var isExitMainRoom = true
        context.let {
            val customView = LayoutInflater.from(it).inflate(R.layout.agora_grouping_leave_layout, null)
            val group = customView.findViewById<RadioGroup>(R.id.agora_group_exit)
            group.setOnCheckedChangeListener { group, checkedId ->
                when (checkedId) {
                    R.id.agora_group_exit_big -> {
                        isExitMainRoom = true
                    }

                    R.id.agora_group_exit_page -> {
                        isExitMainRoom = false
                    }
                }
            }
            AgoraUICustomDialogBuilder(it)
                .title(it.resources.getString(R.string.fcr_group_exit_room))
                .negativeText(it.resources.getString(R.string.fcr_user_kick_out_cancel))
                .positiveText(it.resources.getString(R.string.fcr_user_kick_out_submit))
                .positiveClick {
                    callback.invoke(isExitMainRoom)
                    if (isExitMainRoom) {
                        AgoraUIToast.info(context, text = String.format(context.getString(R.string.fcr_group_back_main_room)))
                    }
                }
                .setCustomView(customView, Gravity.CENTER)
                .build()
                .show()
        }
    }


    /**
     * 课程结束
     */
    fun showDestroyRoom() {
        leaveRoom()

        if (destroyClassDialog != null && destroyClassDialog!!.isShowing) {
            return
        }

        ContextCompat.getMainExecutor(context).execute {
            if (!context.isDestroyPage()) {
                destroyClassDialog?.dismiss()
                destroyClassDialog = AgoraUIDialogBuilder(context)
                    .title(context.resources.getString(R.string.fcr_user_dialog_class_destroy_title))
                    .message(context.resources.getString(R.string.fcr_room_class_over))
                    .positiveText(context.resources.getString(R.string.fcr_user_kick_out_submit))
                    .positiveClick {
                        context.finish()
                    }
                    .build()
                destroyClassDialog?.show()
            }
        }
    }

    fun leaveRoom() {
        ContextCompat.getMainExecutor(context).execute {//leaveRoom内部方法执行必须在主线程
            eduCore?.eduContextPool()?.roomContext()?.leaveRoom(object : EduContextCallback<Unit> {
                override fun onSuccess(target: Unit?) {
                    LogX.i("leave room success")
                }

                override fun onFailure(error: EduContextError?) {
                    LogX.e("leave room error:${error?.msg} || code = ${error?.code}")

                    if (!context.isDestroyPage()) {
                        ContextCompat.getMainExecutor(context).execute {
                            error?.let {
                                AgoraUIToast.error(context.applicationContext, text = error.msg)
                            }
                        }
                    }
                }
            })
        }
    }

    fun dismiss() {
        destroyClassDialog?.dismiss()
    }
}