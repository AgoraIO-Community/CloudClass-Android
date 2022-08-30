package io.agora.agoraeduuikit.interfaces.protocols

import android.graphics.Color
import io.agora.agoraeduuikit.impl.whiteboard.bean.WhiteboardApplianceType

data class AgoraUIDrawingConfig(
    var activeAppliance: WhiteboardApplianceType = WhiteboardApplianceType.Clicker,
    var color: Int = Color.parseColor("#0073FF"), // 颜色
    var fontSize: Int = 36,  // 文字大小
    var thickSize: Int = 2, // 笔的粗细
) {

    fun set(config: AgoraUIDrawingConfig) {
        this.activeAppliance = config.activeAppliance
        if (color > 0) {
            this.color = config.color
        }
        this.fontSize = config.fontSize
        this.thickSize = config.thickSize
    }
}