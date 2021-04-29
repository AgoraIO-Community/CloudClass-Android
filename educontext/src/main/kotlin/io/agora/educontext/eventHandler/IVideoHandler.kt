package io.agora.educontext.eventHandler

import io.agora.educontext.EduContextUserDetailInfo

interface IVideoHandler {
    fun onUserDetailInfoUpdated(info: EduContextUserDetailInfo)

    fun onVolumeUpdated(volume: Int, streamUuid: String)

    fun onMessageUpdated(msg: String)
}