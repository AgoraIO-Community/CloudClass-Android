//package io.agora.classroom.helper
//
//import io.agora.agoraeducore.core.group.FCRGroupHandler
//import io.agora.agoraeducore.core.group.bean.FCRGroupInfo
//import io.agora.agoraeducore.core.internal.education.impl.Constants
//import io.agora.agoraeduuikit.R
//import io.agora.agoraeduuikit.component.toast.AgoraUIToast
//import io.agora.classroom.common.AgoraEduClassActivity
//
///**
// * author : hefeng
// * date : 2022/4/18
// * description :
// */
//class FCRMyGroupHandler(var context: AgoraEduClassActivity) : FCRGroupHandler() {
//    override fun onUserListInvitedToSubRoom(all: MutableList<FCRGroupInfo>, current: FCRGroupInfo?) {
//        super.onUserListInvitedToSubRoom(all, current)
//        current?.apply {
//            //  收到邀请
//            Constants.AgoraLog?.i("Group 1、收到邀请分组")
//
//            // 这里有个问题，加入到房间，会触发 onAllGroupUpdated
//            classManager?.showGroupInvited(fullLoading, isJoining, current) { groupUuid ->
//                groupInvitedUuid = groupUuid
//                closeRTC()
//                releaseData()
//                finish()
//                // 欢迎加入{xxxx小组名}与大家互动讨论
//                AgoraUIToast.info(
//                    context,
//                    text = String.format(
//                        context.resources.getString(R.string.fcr_group_enter_welcome),
//                        current.payload.groupName
//                    )
//                )
//            }
//        }
//    }
//}