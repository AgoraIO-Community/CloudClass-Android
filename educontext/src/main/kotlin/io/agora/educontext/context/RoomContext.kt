package io.agora.educontext.context

import io.agora.educontext.AbsHandlerPool
import io.agora.educontext.EduContextRoomInfo
import io.agora.educontext.eventHandler.IRoomHandler

abstract class RoomContext : AbsHandlerPool<IRoomHandler>() {
    abstract fun roomInfo(): EduContextRoomInfo

    abstract fun leave()

    abstract fun uploadLog()

    abstract fun updateFlexRoomProps(properties: MutableMap<String, String>, cause: MutableMap<String, String>?)

    abstract fun joinClassRoom()
}