package io.agora.agoraeducontext.handlerimpl

import io.agora.agoraeducontext.EduContextError
import io.agora.agoraeducontext.EduContextHandsUpState
import io.agora.agoraeducore.core.context.IHandsUpHandler

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