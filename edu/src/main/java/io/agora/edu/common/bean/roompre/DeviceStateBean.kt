package io.agora.edu.common.bean.roompre

import io.agora.base.bean.JsonBean

class DeviceStateBean(
        val camera: Int?,
        val facing: Int?,
        val mic: Int?,
        val speaker: Int?
) : JsonBean() {
    companion object {
        const val DEVICES = "device"
    }
}