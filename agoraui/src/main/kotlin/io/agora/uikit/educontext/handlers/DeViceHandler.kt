package io.agora.uikit.educontext.handlers

import io.agora.educontext.EduContextCameraFacing
import io.agora.educontext.eventHandler.IDeviceHandler

open class DeViceHandler : IDeviceHandler {
    override fun onCameraDeviceEnableChanged(enabled: Boolean) {
    }

    override fun onCameraFacingChanged(facing: EduContextCameraFacing) {
    }

    override fun onMicDeviceEnabledChanged(enabled: Boolean) {
    }

    override fun onSpeakerEnabledChanged(enabled: Boolean) {
    }

    override fun onDeviceTips(tips: String) {
    }
}