package io.agora.online.component.teachaids.networkdisk.mycloud

import io.agora.agoraeducore.core.internal.edu.common.bean.board.sceneppt.BoardCoursewareRes

/**
 * author : cjw
 * date : 2022/3/18
 * description : 我的云盘接口的数据结构
 * struct of myCloud API
 */
class MyCloudCoursewareRes(
    val total: Int,
    val list: List<BoardCoursewareRes>?,
    val pageSize: Int,
    val pageNo: Int,
    val pages: Int
) {
}