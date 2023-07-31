package com.agora.edu.component.common

import android.view.View
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeduuikit.provider.UIDataProvider

/**
 * author : felix
 * date : 2022/1/26
 * description : 提供给UI组件数据
 */
interface IAgoraUIProvider {
    /**
     * RTE 数据
     */
    fun getAgoraEduCore(): AgoraEduCore?

    fun getUIDataProvider(): UIDataProvider?

    //获取大窗component的容器  agoraLargeWindowContainer
    fun getLargeVideoArea(): View
}