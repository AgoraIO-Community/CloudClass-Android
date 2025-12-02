package com.agora.edu.component.whiteboard.adpater

import io.agora.agoraeduuikit.impl.whiteboard.bean.WhiteboardApplianceType

val UN_SET_IMAGE = -1

/**
 * author : felix
 * date : 2022/2/17
 * description : 通用参数
 */
abstract class AgoraEduWbToolInfo(
    open var iconRes: Int,
    open var iconSelectRes: Int,
    open var iconSize: Int? = null,
    open var itemSize: Int? = null,
) {
    constructor(
        iconRes: Int,
        iconSize: Int? = null,
        itemSize: Int? = null,
    ) : this(iconRes, UN_SET_IMAGE, iconSize, itemSize)
}

/**
 * 白板教具
 */
data class AgoraEduApplianceInfo(
    override var iconRes: Int,
    override var iconSelectRes: Int,
    var activeAppliance: WhiteboardApplianceType
) : AgoraEduWbToolInfo(iconRes) {
    constructor(iconRes: Int, activeAppliance: WhiteboardApplianceType) :
            this(iconRes, UN_SET_IMAGE, activeAppliance)
}

/**
 * 工具
 */
data class AgoraEduToolInfo(
    override var iconRes: Int,
    var activeAppliance: WhiteboardApplianceType
) : AgoraEduWbToolInfo(iconRes)

/**
 * 画笔形状
 */
data class AgoraEduPenShapeInfo(
    override var iconRes: Int,
    var activeAppliance: WhiteboardApplianceType
) : AgoraEduWbToolInfo(iconRes)


/**
 * 画笔粗细
 */
data class AgoraEduThicknessInfo(
    override var iconRes: Int,
    override var iconSize: Int?,
    override var itemSize: Int?,
    var size: Int
) : AgoraEduWbToolInfo(iconRes, iconSize, itemSize)

/**
 * 画笔颜色
 */
data class AgoraEduPenColorInfo(
    override var iconRes: Int,
) : AgoraEduWbToolInfo(iconRes)


/**
 * 文字大小
 */
data class AgoraEduTextSizeInfo(
    override var iconRes: Int,
    override var iconSize: Int?,
    var size: Int
) : AgoraEduWbToolInfo(iconRes, iconSize)