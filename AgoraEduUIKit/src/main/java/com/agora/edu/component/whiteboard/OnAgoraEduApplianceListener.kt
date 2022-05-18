package com.agora.edu.component.whiteboard

import io.agora.agoraeduuikit.impl.whiteboard.bean.WhiteboardApplianceType

/**
 * author : hefeng
 * date : 2022/2/19
 * description :
 */

/**
 * 点击设置教具和工具
 */
interface OnAgoraEduApplianceListener {
    fun onToolsSelected(toolType: WhiteboardApplianceType, iconRes: Int)

    fun onApplianceSelected(type: WhiteboardApplianceType, iconRes: Int)
}

/**
 * 点击设置文本
 */
interface OnAgoraEduTextListener {
    fun onTextSizeSelected(size: Int, iconRes: Int)
    fun onTextColorSelected(color: Int, iconRes: Int)
}

/**
 * 点击设置画笔
 */
interface OnAgoraEduPenListener {
    // 子type
    fun onPenShapeSelected(parentType: WhiteboardApplianceType, childShapeType: WhiteboardApplianceType, iconRes: Int)
    fun onPenThicknessSelected(thick: Int, iconRes: Int)
    fun onPenColorSelected(color: Int, iconRes: Int)
}