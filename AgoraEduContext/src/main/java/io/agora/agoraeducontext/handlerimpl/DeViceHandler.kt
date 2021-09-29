package io.agora.agoraeducontext.handlerimpl

import io.agora.agoraeducontext.EduContextCameraFacing
import io.agora.agoraeducore.core.context.IDeviceHandler

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