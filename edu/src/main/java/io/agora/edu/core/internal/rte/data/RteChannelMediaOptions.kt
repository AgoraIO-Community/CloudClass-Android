package io.agora.edu.core.internal.rte.data

import io.agora.rtc.models.ChannelMediaOptions

class RteChannelMediaOptions(autoSubscribeAudio: Boolean, autoSubscribeVideo: Boolean) :
        ChannelMediaOptions() {

    init {
        this.autoSubscribeAudio = autoSubscribeAudio
        this.autoSubscribeVideo = autoSubscribeVideo
        // 不自动发流，必须调用publish接口才能发流，和3.4.5之前的版本保持一致
        this.publishLocalAudio = false
        this.publishLocalVideo = false
    }
}