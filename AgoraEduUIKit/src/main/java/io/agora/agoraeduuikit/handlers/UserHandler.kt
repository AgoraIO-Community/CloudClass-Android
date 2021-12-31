package io.agora.agoraeduuikit.handlers

import io.agora.agoraeducore.core.context.*

open class UserHandler : IUserHandler {
    override fun onRemoteUserJoined(user: AgoraEduContextUserInfo) {

    }

    override fun onRemoteUserLeft(user: AgoraEduContextUserInfo,
                                  operator: AgoraEduContextUserInfo?,
                                  reason: EduContextUserLeftReason) {

    }

    override fun onUserUpdated(user: AgoraEduContextUserInfo,
                               operator: AgoraEduContextUserInfo?,
                               reason: EduContextUserUpdateReason?) {

    }

    override fun onUserPropertiesUpdated(user: AgoraEduContextUserInfo,
                                         properties: Map<String, Any>,
                                         cause: Map<String, Any>?,
                                         operator: AgoraEduContextUserInfo?) {

    }

    override fun onUserPropertiesDeleted(user: AgoraEduContextUserInfo,
                                         keys: List<String>,
                                         cause: Map<String, Any>?,
                                         operator: AgoraEduContextUserInfo?) {

    }

    override fun onCoHostUserListAdded(userList: List<AgoraEduContextUserInfo>,
                                       operator: AgoraEduContextUserInfo?) {

    }

    override fun onCoHostUserListRemoved(userList: List<AgoraEduContextUserInfo>,
                                         operator: AgoraEduContextUserInfo?) {

    }

    override fun onUserRewarded(user: AgoraEduContextUserInfo,
                                rewardCount: Int,
                                operator: AgoraEduContextUserInfo?) {

    }

    override fun onLocalUserKickedOut() {

    }

    override fun onHandsWaveEnabled(enabled: Boolean) {

    }

    override fun onUserHandsWave(user: AgoraEduContextUserInfo, duration: Int) {

    }

    override fun onUserHandsDown(user: AgoraEduContextUserInfo) {

    }
}