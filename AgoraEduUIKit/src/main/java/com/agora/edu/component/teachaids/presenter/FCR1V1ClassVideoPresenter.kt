package com.agora.edu.component.teachaids.presenter

import android.graphics.Rect
import com.agora.edu.component.tab.AgoraEduTabGroupComponent

/**
 * author : cjw
 * date : 2022/2/14
 * description : 在1V1中获取streamUuid对应的视频view在讲台区域的坐标
 */
class FCR1V1ClassVideoPresenter(
    private val tabGroupComponent: AgoraEduTabGroupComponent
) : FCRVideoPresenter {

    /**
     * videoGroup被tabGroupComponent包裹，所以rect需要考虑加上tabGroupComponent的数据
     */
    override fun getVideoPosition(streamUuid: String): Rect? {
        return tabGroupComponent.getVideoGroup().getVideoPosition(streamUuid)?.apply {
            this.left += tabGroupComponent.left
            this.top += tabGroupComponent.top
            this.right += tabGroupComponent.left
            this.bottom += tabGroupComponent.top
        }
    }
}