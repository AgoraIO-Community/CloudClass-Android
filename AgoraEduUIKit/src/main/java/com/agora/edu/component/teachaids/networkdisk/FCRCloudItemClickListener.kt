package com.agora.edu.component.teachaids.networkdisk

import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCourseware

/**
 * author : cjw
 * date : 2022/3/20
 * description : 云盘课件的点击事件
 * FCRCloudItemClickListener
 *
 * event from adapter to fragment
 */
interface FCRCloudItemClickListener {
    fun onClick(courseware: AgoraEduCourseware)
}