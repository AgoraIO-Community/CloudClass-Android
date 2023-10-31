package io.agora.online.component.common


/**
 * author : felix
 * date : 2022/7/11
 * description : 组件config
 */
interface IAgoraConfigComponent<T> {
    fun getUIConfig(): T
}