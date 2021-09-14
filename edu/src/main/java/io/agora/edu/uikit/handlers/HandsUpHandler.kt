package io.agora.edu.uikit.handlers

import io.agora.edu.core.context.EduContextError
import io.agora.edu.core.context.EduContextHandsUpState
import io.agora.edu.core.context.IHandsUpHandler

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