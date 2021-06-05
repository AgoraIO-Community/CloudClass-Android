package io.agora.educontext.eventHandler

import io.agora.educontext.EduContextUserDetailInfo
import io.agora.educontext.EduContextUserInfo

interface IVideoHandler {
    fun onUserDetailInfoUpdated(info: EduContextUserDetailInfo)

    fun onVolumeUpdated(volume: Int, streamUuid: String)

    fun onMessageUpdated(msg: String)

//    /** @param  operator this is null when server update props */
//    fun onUserPropertiesChanged(fromUser: EduContextUserDetailInfo, changed: MutableMap<String, String>,
//                                cause: MutableMap<String, String>, operator: EduContextUserInfo?)
}