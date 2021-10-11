package io.agora.edu.uikit.handlers

import io.agora.edu.core.context.EduContextUserDetailInfo
import io.agora.edu.core.context.IVideoHandler

open class VideoHandler : IVideoHandler {
    override fun onUserDetailInfoUpdated(info: EduContextUserDetailInfo) {

    }

    override fun onVolumeUpdated(volume: Int, streamUuid: String) {

    }

    override fun onMessageUpdated(msg: String) {

    }
}