package com.agora.edu.component.options

import com.agora.edu.component.common.IAgoraUIProvider
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.group.FCRGroupClassUtils
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.education.impl.network.HttpBaseRes
import io.agora.agoraeducore.core.internal.education.impl.network.HttpCallback
import io.agora.agoraeducore.core.internal.util.notNull
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.component.dialog.AgoraUIDialogBuilder
import io.agora.agoraeduuikit.databinding.AgoraEduOptionsComponentBinding

/**
 * author : wufang
 * date : 2022/3/8
 * description :分组相关的dialog
 */
class AgoraEduOptionPresenter(var binding: AgoraEduOptionsComponentBinding) {
    var context = binding.root.context

    fun showAskingForHelpDialog(agoraUIProvider: IAgoraUIProvider, listener: (() -> Unit)?) {
        AgoraUIDialogBuilder(context)
            .title(context.getString(R.string.fcr_group_help_title))
            .message(context.getString(R.string.fcr_group_help_content))
            .negativeText(context.getString(R.string.fcr_user_kick_out_cancel))
            .positiveText(context.getString(R.string.fcr_group_invite))
            .positiveClick {
                requestHelp(agoraUIProvider, listener)
            }
            .build()
            .show()
    }

    fun requestHelp(agoraUIProvider: IAgoraUIProvider, listener: (() -> Unit)?) {
        val groupUUid = agoraUIProvider.getAgoraEduCore()?.eduContextPool()?.groupContext()?.groupInfo?.groupUuid
        val mainRoomUUid = FCRGroupClassUtils.mainRoomInfo?.roomUuid

        notNull(groupUUid, mainRoomUUid) {
            Constants.AgoraLog?.i("Group 请求老师帮助：$groupUUid")

            val contextPool = agoraUIProvider.getAgoraEduCore()?.eduContextPool()
            val userList = mutableListOf<String>()

            FCRGroupClassUtils.mainClassRoomInfo.forEach {
                if (it.role == AgoraEduContextUserRole.Teacher || it.role == AgoraEduContextUserRole.Assistant) {
                    userList.add(it.userUuid)
                }
            }

            Constants.AgoraLog?.i("Group 请求老师帮助：${userList.size}")

            if (userList.isNotEmpty()) {
                contextPool?.groupContext()?.inviteUserListToSubRoom(userList,
                    Constants.APPID, mainRoomUUid!!, groupUUid!!, object : HttpCallback<HttpBaseRes<String>>() {
                        override fun onSuccess(result: HttpBaseRes<String>?) {
                            listener?.invoke()
                        }

                        override fun onError(code: Int, message: String?) {
                            super.onError(code, message)
                        }
                    }
                )
            }
        }
    }
}