package io.agora.classroom.ui.goup

import io.agora.agoraclasssdk.R
import io.agora.agoraeducore.core.context.AgoraEduContextUserInfo
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.EduContextUserLeftReason
import io.agora.agoraeducore.core.group.FCRGroupClassUtils
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.education.impl.network.HttpBaseRes
import io.agora.agoraeducore.core.internal.education.impl.network.HttpCallback
import io.agora.agoraeducore.core.internal.education.impl.room.EduRoomImpl
import io.agora.agoraeducore.core.internal.framework.impl.handler.UserHandler
import io.agora.agoraeducore.core.internal.rte.module.impl.RteChannelMessageListener
import io.agora.agoraeducore.core.internal.util.notNull
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.agoraeducore.core.AgoraEduCoreManager
import io.agora.classroom.ui.AgoraClassSmallActivity
import io.agora.rtm.RtmChannelMember
import io.agora.rtm.RtmMessage

/**
 * author : wufang
 * date : 2022/3/8
 * description : 分组小班
 */
class AgoraClassSmallGroupingActivity : AgoraClassSmallActivity() {
    private val TAG = "AgoraClassGroupingActivity"

    // onCoHostUserListAdded : 上下台的
    // onRemoteUserJoined: 远端用户的离开和加入
    val myUserHandler = object : UserHandler() {
        override fun onRemoteUserJoined(user: AgoraEduContextUserInfo) {
            super.onRemoteUserJoined(user)
            //Constants.AgoraLog?.e("$TAG->onRemoteUserJoined ${user} ")

            // 老师/助教xx进入小组
            if (user.role == AgoraEduContextUserRole.Teacher) {
                AgoraUIToast.info(
                    applicationContext, text = String.format(
                        resources.getString(R.string.fcr_group_enter_group),
                        resources.getString(R.string.fcr_rtm_im_teacher), user.userName
                    )
                )
            } else if (user.role == AgoraEduContextUserRole.Assistant) {
                AgoraUIToast.info(
                    applicationContext, text = String.format(
                        resources.getString(R.string.fcr_group_enter_group),
                        resources.getString(R.string.fcr_rtm_im_role_assistant), user.userName
                    )
                )
            }
        }

        override fun onRemoteUserLeft(
            user: AgoraEduContextUserInfo,
            operator: AgoraEduContextUserInfo?,
            reason: EduContextUserLeftReason
        ) {
            super.onRemoteUserLeft(user, operator, reason)
            //Constants.AgoraLog?.e("$TAG->onRemoteUserLeft ${user} ")

            // 老师/助教xx离开小组
            if (user.role == AgoraEduContextUserRole.Teacher) {
                AgoraUIToast.info(
                    applicationContext, text = String.format(
                        resources.getString(R.string.fcr_group_exit_group),
                        resources.getString(R.string.fcr_rtm_im_teacher), user.userName
                    )
                )
            } else if (user.role == AgoraEduContextUserRole.Assistant) {
                AgoraUIToast.info(
                    applicationContext, text = String.format(
                        resources.getString(R.string.fcr_group_exit_group),
                        resources.getString(R.string.fcr_rtm_im_role_assistant), user.userName
                    )
                )
            }
        }
    }

    override fun onCreateEduCore(isSuccess: Boolean) {
        super.onCreateEduCore(isSuccess)
        getEduContext()?.groupContext()?.groupInfo = FCRGroupClassUtils.groupInfo?.clone()
        getEduContext()?.userContext()?.addHandler(myUserHandler)

        // 监听大房间的RTM消息
//        FCRGroupClassUtils.mainRoomInfo?.apply {
//            RteChannelMessageManager.addChannelMsgListener(roomUuid, rteChannelMessageListener)
//        }
        addMainClassListener()
    }

    fun addMainClassListener() {
        FCRGroupClassUtils.mainLaunchConfig?.apply {
            val eduContextPool = AgoraEduCoreManager.getEduCore(roomUuid)?.eduContextPool()
            eduContextPool?.groupContext()?.addHandler(groupHandler)
        }
    }

    fun removeMainClassListener() {
        FCRGroupClassUtils.mainLaunchConfig?.apply {
            val eduContextPool = AgoraEduCoreManager.getEduCore(roomUuid)?.eduContextPool()
            eduContextPool?.groupContext()?.removeHandler(groupHandler)
        }
    }

    val rteChannelMessageListener = object : RteChannelMessageListener {
        override fun onChannelMsgReceived(channelId: String, message: RtmMessage?, member: RtmChannelMember?) {
            val subEduRoom = eduCore()?.eduRoom as EduRoomImpl
            Constants.AgoraLog?.i("Group 将大房间的RTM分发到分组小班课")
            subEduRoom.dispatchMainGroupMsg(message)
        }
    }

    override fun onBackPressed() {
        //super.onBackPressed()
        classManager?.showLeaveGroupRoom { isExitMainRoom ->
            if (!isExitMainRoom) {
                finish()
            } else {
                leaveSubRoom()
            }
        }
    }

    /**
     * 返回大房间
     */
    override fun leaveSubRoom() {
        showFullDialog()
        // 1、先退出分组
        val userUuid = getEduContext()?.userContext()?.getLocalUserInfo()?.userUuid
        val mainRoomUUid = FCRGroupClassUtils.mainRoomInfo?.roomUuid
        val groupUUid = getEduContext()?.roomContext()?.getRoomInfo()?.roomUuid

        notNull(userUuid, mainRoomUUid, groupUUid) {
            val userList = mutableListOf<String>()
            userList.add(userUuid!!)
            getEduContext()?.groupContext()?.userListRemoveFromSubRoom(userList, Constants.APPID, mainRoomUUid!!, groupUUid!!,
                    object : HttpCallback<HttpBaseRes<String>>() {
                        override fun onSuccess(result: HttpBaseRes<String>?) {
                            Constants.AgoraLog?.i("Group 返回到大房间")
                            // 2、返回到大教室
                            isJoining.set(true)
                            launchMainRoom()
                        }

                        override fun onError(code: Int, message: String?) {
                            super.onError(code, message)
                            dismissFullDialog()
                        }
                    }
                )
        }
    }

    override fun finish() {
        super.finish()
        removeMainClassListener()
        // 为了防止dialog不消失
        uiHandler.postDelayed({
            dismissFullDialog()
        }, 1000)
    }

    override fun onRelease() {
        super.onRelease()
        val roomUuid = eduCore()?.eduContextPool()?.roomContext()?.getRoomInfo()?.roomUuid
        roomUuid?.let { AgoraEduCoreManager.removeEduCore(roomUuid) }
        getEduContext()?.userContext()?.addHandler(myUserHandler)
        //RteChannelMessageManager.removeChannelMsgListener(rteChannelMessageListener)
    }
}