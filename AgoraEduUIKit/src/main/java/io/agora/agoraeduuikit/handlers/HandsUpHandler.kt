package io.agora.agoraeduuikit.handlers

import io.agora.agoraeducore.core.context.EduContextError
import io.agora.agoraeducore.core.context.EduContextHandsUpState
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