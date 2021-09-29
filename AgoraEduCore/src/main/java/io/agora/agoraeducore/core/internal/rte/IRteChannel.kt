package io.agora.agoraeducore.core.internal.rte

import androidx.annotation.NonNull
import io.agora.rtc.internal.EncryptionConfig
import io.agora.rtc.models.ChannelMediaOptions

interface IRteChannel {

    fun join(rtcOptionalInfo: String, rtcToken: String, rtcUid: Long, mediaOptions: ChannelMediaOptions, encryptionConfig: EncryptionConfig,
             @NonNull callback: RteCallback<Void>)

    fun leave(callback: RteCallback<Unit>)

    fun release()

    fun getRtcCallId() : String

}
