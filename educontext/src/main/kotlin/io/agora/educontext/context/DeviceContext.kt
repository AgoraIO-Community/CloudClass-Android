package io.agora.educontext.context

import io.agora.educontext.AbsHandlerPool
import io.agora.educontext.EduContextDeviceConfig
import io.agora.educontext.eventHandler.IDeviceHandler
import io.agora.educontext.eventHandler.IRoomHandler

abstract class DeviceContext : AbsHandlerPool<IDeviceHandler>() {
    abstract fun getDeviceConfig(): EduContextDeviceConfig

    abstract fun setCameraDeviceEnable(enable: Boolean)

    abstract fun switchCameraFacing()

    abstract fun setMicDeviceEnable(enable: Boolean)

    abstract fun setSpeakerEnable(enable: Boolean)
}