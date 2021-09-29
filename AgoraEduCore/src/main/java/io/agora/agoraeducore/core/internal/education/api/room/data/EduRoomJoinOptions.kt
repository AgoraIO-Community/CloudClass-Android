package io.agora.agoraeducore.core.internal.education.api.room.data

import io.agora.agoraeducore.core.internal.education.api.stream.data.EduLatencyLevel
import io.agora.agoraeducore.core.internal.education.api.stream.data.EduVideoEncoderConfig
import io.agora.agoraeducore.core.internal.framework.EduUserRole
import io.agora.agoraeducore.core.internal.launch.AgoraEduMediaEncryptionConfigs
import io.agora.agoraeducore.core.internal.rte.data.RteChannelMediaOptions
import io.agora.rtc.internal.EncryptionConfig

data class RoomMediaOptions(
        var autoSubscribe: Boolean = true,
        var autoPublish: Boolean = true,
        val encryptionConfigs: AgoraEduMediaEncryptionConfigs? = null

) {
    /**用户传了primaryStreamId,那么就用他当做streamUuid;如果没传，就是默认值，后端会生成一个streamUuid*/
    var primaryStreamId: Int = DefaultStreamId

    companion object {
        const val DefaultStreamId = 0
    }

    constructor(primaryStreamId: Int) : this() {
        this.primaryStreamId = primaryStreamId
    }

    fun convert(): RteChannelMediaOptions {
        return RteChannelMediaOptions(autoSubscribe, autoSubscribe)
    }

    fun convertedEncryptionMode(mode:Int):EncryptionConfig.EncryptionMode {
        when(mode) {
            EncryptionConfig.EncryptionMode.AES_128_XTS.value -> return EncryptionConfig.EncryptionMode.AES_128_XTS
            EncryptionConfig.EncryptionMode.AES_128_ECB.value -> return EncryptionConfig.EncryptionMode.AES_128_ECB
            EncryptionConfig.EncryptionMode.AES_256_XTS.value -> return EncryptionConfig.EncryptionMode.AES_256_XTS
            EncryptionConfig.EncryptionMode.SM4_128_ECB.value -> return EncryptionConfig.EncryptionMode.SM4_128_ECB
            EncryptionConfig.EncryptionMode.AES_128_GCM.value -> return EncryptionConfig.EncryptionMode.AES_128_GCM
            EncryptionConfig.EncryptionMode.AES_256_GCM.value -> return EncryptionConfig.EncryptionMode.AES_256_GCM
        }
        return EncryptionConfig.EncryptionMode.MODE_END
    }

    fun rteEncryptionConfig(): EncryptionConfig {
        var rteConfig = EncryptionConfig()

        val config = encryptionConfigs
        config?.let {
            rteConfig.encryptionKey = config.encryptionKey
            rteConfig.encryptionMode = convertedEncryptionMode(config.encryptionMode)
        }

        return rteConfig
    }


    fun getPublishType(): AutoPublishItem {
        return if (autoPublish) {
            AutoPublishItem.AutoPublish
        } else {
            AutoPublishItem.NoOperation
        }
    }
}

data class RoomJoinOptions(
        val userUuid: String,
        /**用户可以传空,为空则使用roomImpl中默认的userName*/
        var userName: String?,
        val roleType: EduUserRole,
        val mediaOptions: RoomMediaOptions,
        /*用于RTC-SDK统计各个场景的使用情况*/
        var tag: Int? = null,
        var videoEncoderConfig: EduVideoEncoderConfig? = null,
        val latencyLevel: EduLatencyLevel
) {
    fun closeAutoPublish() {
        mediaOptions.autoPublish = false
    }
}

enum class AutoPublishItem(val value: Int) {
    NoOperation(0),
    AutoPublish(1),
    NoAutoPublish(2)
}
