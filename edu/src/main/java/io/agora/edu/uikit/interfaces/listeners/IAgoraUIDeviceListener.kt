package io.agora.edu.uikit.interfaces.listeners

interface IAgoraUIDeviceListener {
    fun onCameraEnabled(enabled: Boolean)

    fun onCameraFacingChanged(front: Boolean)

    fun onMicEnabled(enabled: Boolean)

    fun onSpeakerEnabled(enabled: Boolean)
}