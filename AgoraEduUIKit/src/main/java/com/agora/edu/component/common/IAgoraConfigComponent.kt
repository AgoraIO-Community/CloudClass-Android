package com.agora.edu.component.common


/**
 * author : hefeng
 * date : 2022/7/11
 * description : 组件config
 */
interface IAgoraConfigComponent<T> {
    fun getUIConfig(): T
}