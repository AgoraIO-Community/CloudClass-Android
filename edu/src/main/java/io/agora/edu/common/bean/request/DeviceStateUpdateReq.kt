package io.agora.edu.common.bean.request

open class DeviceStateUpdateReq(
        val camera: Int? = null,
        val facing: Int? = null,
        val mic: Int? = null,
        val speaker: Int? = null) {
}