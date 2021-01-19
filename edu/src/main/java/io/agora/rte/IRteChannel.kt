package io.agora.rte

import androidx.annotation.NonNull
import io.agora.rtc.models.ChannelMediaOptions
import io.agora.rtm.ResultCallback

interface IRteChannel {

    fun join(rtcOptionalInfo: String, rtcToken: String, rtcUid: Long, mediaOptions: ChannelMediaOptions,
             @NonNull callback: RteCallback<Void>)

    fun leave(callback: RteCallback<Unit>)

    fun release()
}
