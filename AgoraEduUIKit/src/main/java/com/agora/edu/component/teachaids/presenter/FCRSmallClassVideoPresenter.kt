package com.agora.edu.component.teachaids.presenter

import android.graphics.Rect
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.agora.edu.component.AgoraEduListVideoComponent
import com.agora.edu.component.AgoraEduVideoComponent
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.EduContextPool

/**
 * author : cjw
 * date : 2022/2/14
 * description : 在小班课中获取streamUuid对应的视频view在讲台区域的坐标
 */
class FCRSmallClassVideoPresenter(
    private val videoLayout: ViewGroup,
    private val teacherVideo: AgoraEduVideoComponent,
    private val userListVideo: AgoraEduListVideoComponent,
    private val contextPool: EduContextPool
) : FCRVideoPresenter {

    override fun getVideoPosition(streamUuid: String): Rect? {
        // 讲台关闭时，rect为空
        if (videoLayout.visibility != VISIBLE) {
            return null
        }
        return contextPool.streamContext()?.getAllStreamList()?.find { it.streamUuid == streamUuid }?.let {
            if (it.owner.role == AgoraEduContextUserRole.Teacher) {
                teacherVideo.getViewPosition(streamUuid)
            } else {
                userListVideo.getItemViewPosition(streamUuid)
            }
        }
    }
}