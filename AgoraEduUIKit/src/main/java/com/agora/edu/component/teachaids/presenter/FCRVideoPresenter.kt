package com.agora.edu.component.teachaids.presenter

import android.graphics.Rect

/**
 * author : cjw
 * date : 2022/2/14
 * description : 获取streamUuid对应的视频view在讲台区域的坐标
 */
interface FCRVideoPresenter {
    fun getVideoPosition(streamUuid: String): Rect?
}