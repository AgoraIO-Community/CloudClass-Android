package io.agora.educontext.context

import io.agora.educontext.AbsHandlerPool
import io.agora.educontext.EduContextCallback
import io.agora.educontext.EduContextPrivateChatInfo
import io.agora.educontext.EduContextUserInfo
import io.agora.educontext.eventHandler.IPrivateChatHandler

abstract class PrivateChatContext : AbsHandlerPool<IPrivateChatHandler>() {
    abstract fun getLocalUserInfo(): EduContextUserInfo

    abstract fun startPrivateChat(peerId: String, callback: EduContextCallback<EduContextPrivateChatInfo>? = null)

    abstract fun endPrivateChat(callback: EduContextCallback<Boolean>?)
}