package io.agora.online.component.teachaids.presenter

import android.graphics.Rect
import io.agora.online.component.AgoraEduVideoComponent
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.EduContextPool

/**
 * author : cjw
 * date : 2022/2/14
 * description : 在大班课中获取streamUuid对应的视频view在讲台区域的坐标
 */
class FCRLargeClassVideoPresenter(
    private val teacherVideo: AgoraEduVideoComponent,
    private val contextPool: EduContextPool
) : FCRVideoPresenter {

    override fun getVideoPosition(streamUuid: String): Rect? {
        return contextPool.streamContext()?.getAllStreamList()?.find { it.streamUuid == streamUuid }?.let {
            if (it.owner.role == AgoraEduContextUserRole.Teacher) {
                teacherVideo.getViewPosition(streamUuid)?.apply {
                    // 老师的视频组件和视频区域组件同级别，所以top强制设置为0
                    this.top = 0
                }
            } else {
                return null
            }
        }
    }
}