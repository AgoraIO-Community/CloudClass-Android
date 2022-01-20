package io.agora.agoraeduuikit.interfaces.protocols

import android.graphics.Color
import io.agora.agoraeduuikit.impl.tool.AgoraUIApplianceType

data class AgoraUIDrawingConfig(
    var activeAppliance: AgoraUIApplianceType = AgoraUIApplianceType.Clicker,
    var color: Int = Color.WHITE,
    var fontSize: Int = 18,
    var thick: Int = 12) {

    fun set(config: AgoraUIDrawingConfig) {
        this.activeAppliance = config.activeAppliance
        this.color = config.color
        this.fontSize = config.fontSize
        this.thick = config.thick
    }
}