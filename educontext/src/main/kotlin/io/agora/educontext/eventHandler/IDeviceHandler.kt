package io.agora.educontext.eventHandler

import io.agora.educontext.EduContextCameraFacing

interface IDeviceHandler {
    fun onCameraDeviceEnableChanged(enabled: Boolean)

    fun onCameraFacingChanged(facing: EduContextCameraFacing)

    fun onMicDeviceEnabledChanged(enabled: Boolean)

    fun onSpeakerEnabledChanged(enabled: Boolean)

    fun onDeviceTips(tips: String)
}