package io.agora.agoraeducontext.handlerimpl

import io.agora.agoraeducontext.EduContextUserDetailInfo
import io.agora.agoraeducore.core.context.IVideoHandler

open class VideoHandler : IVideoHandler {
    override fun onUserDetailInfoUpdated(info: EduContextUserDetailInfo) {

    }

    override fun onVolumeUpdated(volume: Int, streamUuid: String) {

    }

    override fun onMessageUpdated(msg: String) {

    }
}