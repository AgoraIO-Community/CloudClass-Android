package io.agora.edu.common.bean.roompre

import io.agora.base.bean.JsonBean

class LocalDeviceState(
        val camera: Int
) : JsonBean() {
    companion object {
        const val DEVICES = "device"
        const val CAMERA = "camera"
        const val UNAVAILABLE = 0
        const val AVAILABLE = 1
    }

    fun isAvailable(): Boolean {
        return camera == AVAILABLE
    }
}