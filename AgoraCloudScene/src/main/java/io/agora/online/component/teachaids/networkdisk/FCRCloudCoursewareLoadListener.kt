package io.agora.online.component.teachaids.networkdisk

import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCourseware

/**
 * author : cjw
 * date : 2022/3/20
 * description : 课件加载监听器
 * FCRCloudCoursewareLoadListener
 *
 * event from resourceFragment to widget
 */
interface FCRCloudCoursewareLoadListener {
    fun onLoad(courseware: AgoraEduCourseware)
}