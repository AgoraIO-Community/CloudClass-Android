package io.agora.educontext.eventHandler

import io.agora.educontext.EduContextHandsUpState
import io.agora.educontext.EduContextError

interface IHandsUpHandler {
    fun onHandsUpEnabled(enabled: Boolean)

    fun onHandsUpStateUpdated(state: EduContextHandsUpState, coHost: Boolean)

    fun onHandsUpStateResultUpdated(error: EduContextError?)

    fun onHandsUpTips(tips: String)
}