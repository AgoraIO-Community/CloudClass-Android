package io.agora.uikit.educontext.handlers

import io.agora.educontext.EduContextError
import io.agora.educontext.EduContextHandsUpState
import io.agora.educontext.eventHandler.IHandsUpHandler

open class HandsUpHandler : IHandsUpHandler {
    override fun onHandsUpEnabled(enabled: Boolean) {

    }

    override fun onHandsUpStateUpdated(state: EduContextHandsUpState, coHost: Boolean) {

    }

    override fun onHandsUpStateResultUpdated(error: EduContextError?) {

    }

    override fun onHandsUpTips(tips: String) {

    }
}