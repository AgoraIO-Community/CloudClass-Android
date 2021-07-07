package io.agora.uikit.interfaces.protocols

import android.graphics.Color
import android.view.ViewGroup
import io.agora.uikit.impl.tool.AgoraUIApplianceType

data class AgoraUIDrawingConfig(
        var activeAppliance: AgoraUIApplianceType = AgoraUIApplianceType.Select,
        var color: Int = Color.WHITE,
        var fontSize: Int = 22,
        var thick: Int = 0) {

    fun set(config: AgoraUIDrawingConfig) {
        this.activeAppliance = config.activeAppliance
        this.color = config.color
        this.fontSize = config.fontSize
        this.thick = config.thick
    }
}