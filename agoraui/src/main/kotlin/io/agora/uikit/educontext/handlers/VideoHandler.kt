package io.agora.uikit.educontext.handlers

import io.agora.educontext.EduContextUserDetailInfo
import io.agora.educontext.eventHandler.IVideoHandler

open class VideoHandler : IVideoHandler {
    override fun onUserDetailInfoUpdated(info: EduContextUserDetailInfo) {

    }

    override fun onVolumeUpdated(volume: Int, streamUuid: String) {

    }

    override fun onMessageUpdated(msg: String) {

    }
}