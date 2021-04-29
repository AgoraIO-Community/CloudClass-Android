package io.agora.educontext.eventHandler

import android.content.Context
import android.view.View
import io.agora.educontext.EduContextUserDetailInfo
import io.agora.educontext.EduContextUserInfo

interface IUserHandler {
    fun onUserListUpdated(list: MutableList<EduContextUserDetailInfo>)

    fun onCoHostListUpdated(list: MutableList<EduContextUserDetailInfo>)

    fun onUserReward(userInfo: EduContextUserInfo)

    fun onKickOut()

    fun onVolumeUpdated(volume: Int, streamUuid: String)

    fun onUserTip(tip: String)

    fun onRoster(context: Context, anchor: View, type: Int?)
}