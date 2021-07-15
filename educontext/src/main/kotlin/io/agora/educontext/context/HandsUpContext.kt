package io.agora.educontext.context

import io.agora.educontext.AbsHandlerPool
import io.agora.educontext.EduContextCallback
import io.agora.educontext.EduContextHandsUpState
import io.agora.educontext.eventHandler.IHandsUpHandler

abstract class HandsUpContext : AbsHandlerPool<IHandsUpHandler>() {
    abstract fun performHandsUp(state: EduContextHandsUpState, callback: EduContextCallback<Boolean>? = null)
}