package io.agora.educontext.context

import android.view.ViewGroup
import io.agora.educontext.AbsHandlerPool
import io.agora.educontext.eventHandler.IUserHandler

abstract class UserContext : AbsHandlerPool<IUserHandler>() {
    abstract fun muteVideo(muted: Boolean)

    abstract fun muteAudio(muted: Boolean)

    abstract fun renderVideo(container: ViewGroup?, streamUuid: String)

    abstract fun updateFlexUserProps(userUuid: String, properties: MutableMap<String, String>,
                                     cause: MutableMap<String, String>?)
}