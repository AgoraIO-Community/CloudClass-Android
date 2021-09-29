package io.agora.agoraeducore.core.internal.server.struct.request

open class DeviceStateUpdateReq(
        val camera: Int? = null,
        val facing: Int? = null,
        val mic: Int? = null,
        val speaker: Int? = null)