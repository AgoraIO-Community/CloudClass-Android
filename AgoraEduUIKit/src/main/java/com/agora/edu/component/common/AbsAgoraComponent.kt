package com.agora.edu.component.common

/**
 * author : felix
 * date : 2022/1/20
 * description : 组件初始化
 */
interface AbsAgoraComponent {
    /**
     * 定义组件，使用到相关数据，必须实现这个方法
     */
    fun initView(agoraUIProvider: IAgoraUIProvider)

    /**
     * 释放资源
     */
    fun release()
}