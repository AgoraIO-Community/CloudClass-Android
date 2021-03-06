package io.agora.uikit.educontext.handlers

import android.content.Context
import android.view.View
import io.agora.educontext.EduContextUserDetailInfo
import io.agora.educontext.EduContextUserInfo
import io.agora.educontext.eventHandler.IUserHandler

open class UserHandler : IUserHandler {
    override fun onUserListUpdated(list: MutableList<EduContextUserDetailInfo>) {
    }

    override fun onCoHostListUpdated(list: MutableList<EduContextUserDetailInfo>) {
    }

    override fun onUserReward(userInfo: EduContextUserInfo) {
    }

    override fun onKickOut() {
    }

    override fun onVolumeUpdated(volume: Int, streamUuid: String) {
    }

    override fun onUserTip(tip: String) {
    }

    override fun onRoster(context: Context, anchor: View, type: Int?) {
    }

    override fun onFlexUserPropsChanged(changedProperties: MutableMap<String, Any>, properties: MutableMap<String, Any>, cause: MutableMap<String, Any>?, fromUser: EduContextUserDetailInfo, operator: EduContextUserInfo?) {
    }
}