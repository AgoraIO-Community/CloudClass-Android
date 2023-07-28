package com.agora.edu.component.teachaids.networkdisk.mycloud

import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCourseware

interface MyCloudItemClickListener {
    fun onSelectClick(courseware: AgoraEduCourseware,position: Int)
}