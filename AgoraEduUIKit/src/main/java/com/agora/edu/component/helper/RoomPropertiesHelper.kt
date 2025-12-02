package com.agora.edu.component.helper

import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.internal.edu.classroom.bean.PropertyData
import io.agora.agoraeducore.core.internal.log.LogX

/**
 * author : felix
 * date : 2023/1/3
 * description :
 */
object RoomPropertiesHelper {

    fun getExpandedScopeProps(eduCore: AgoraEduCore?): Map<*, *>? {
        return eduCore?.room()?.roomProperties?.get(PropertyData.EXPANDEDSCOPE) as? Map<*, *>
    }

    fun getFlexProps(eduCore: AgoraEduCore?): Map<*, *>? {
        return eduCore?.room()?.roomProperties?.get(PropertyData.FLEX) as? Map<*, *>
    }

    /**
     * 是否开启水印
     */
    fun isOpenWatermark(eduCore: AgoraEduCore?): Boolean {
        return getFlexProps(eduCore)?.get(PropertyData.WATERMARK) == true
    }

    /**
     * 是否打开讲台区域
     */
    fun isOpenStage(eduCore: AgoraEduCore?): Boolean {
        if (getArea(eduCore) != null) {
            //LogX.i("FcrLocalWindowComponent", "是否打开讲台区域 = ${isOpenStageV2(eduCore)}")

            return isOpenStageV2(eduCore)
        }
        return isOpenStageV1(eduCore)
    }

    fun isOpenStageV2(eduCore: AgoraEduCore?): Boolean {
        val area = getArea(eduCore)
        //return area == null || area == 0 || area == 0.0
        return isContain(area, PropertyData.AREA_VIDEO_STAGE)
    }

    fun isOpenStageV1(eduCore: AgoraEduCore?): Boolean {
        val stage = eduCore?.eduContextPool()?.roomContext()?.getRoomProperties()?.get(PropertyData.STAGE)
        return stage == null || stage == 1 || stage == 1.0
    }


    /**
     * 是否开启拓展屏幕
     */
    fun isOpenExternalScreen(eduCore: AgoraEduCore?): Boolean {
        val area = getArea(eduCore)
        //LogX.i("FcrLocalWindowComponent", "是否开启拓展屏幕 = ${isContain(area, PropertyData.AREA_VIDEO_GALLERY)}")
        //return area == 2 || area == 2.0
        return isContain(area, PropertyData.AREA_VIDEO_GALLERY)
    }

    fun getArea(eduCore: AgoraEduCore?): Int? {
        var area = getFlexProps(eduCore)?.get(PropertyData.AREA)
        area = if (area is Double) {
            area.toInt()
        } else {
            area as? Int
        }
        return area
    }

    /**
     * 获取拓展屏里面的人
     */
    fun getVideoGalleryList(eduCore: AgoraEduCore?): List<String>? {
        var expandList: List<String>? = null
        val state = getExpandedScopeProps(eduCore)?.get(PropertyData.STATE)
        if (state == 1 || state == 1.0) {
            // 拓展屏里面的人
            expandList = getExpandedScopeProps(eduCore)?.get(PropertyData.USERUUIDS) as? List<String>
        }
        return expandList
    }

    fun isContain(v1: Int?, v2: Int): Boolean {
        if (v1 != null && ((v1 and v2) == v2)) {
            return true
        }
        return false
    }
}