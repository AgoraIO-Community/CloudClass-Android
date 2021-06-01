package io.agora.educontext.context

import android.view.ViewGroup
import io.agora.educontext.AbsHandlerPool
import io.agora.educontext.EduContextUserInfo
import io.agora.educontext.eventHandler.IUserHandler

abstract class UserContext : AbsHandlerPool<IUserHandler>() {
//    abstract fun startPreview(container: ViewGroup)
//
//    abstract fun stopPreview()
//
//    abstract fun publishLocalStream(hasAudio: Boolean, hasVideo: Boolean)
//
//    abstract fun unPublishLocalStream(hasAudio: Boolean, hasVideo: Boolean)

    abstract fun localUserInfo(): EduContextUserInfo

    abstract fun muteVideo(muted: Boolean)

    abstract fun muteAudio(muted: Boolean)

    abstract fun renderVideo(container: ViewGroup?, streamUuid: String)

//    abstract fun upsertUserProperties(userUuid: String, properties: MutableMap<String, String>,
//                                      cause: MutableMap<String, String>?)

//    abstract fun removeUserProperties(userUuid: String, keys: MutableList<String>,
//                                      cause: MutableMap<String, String>?)
}