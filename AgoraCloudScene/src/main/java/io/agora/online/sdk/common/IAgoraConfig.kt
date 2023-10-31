package io.agora.online.sdk.common

import io.agora.online.config.FcrUIConfig


/**
 * author : felix
 * date : 2022/7/11
 * description : 组件config
 */
interface IAgoraConfig {
    fun getUIConfig(): FcrUIConfig
}