package io.agora.edu.uikit.handlers

import io.agora.edu.core.context.EduContextCameraFacing
import io.agora.edu.core.context.IDeviceHandler

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