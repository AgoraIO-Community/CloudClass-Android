package io.agora.uikit.impl.setting

interface IUIDeviceConfig {
    fun setDefault(config: DeviceConfig)

    fun setCameraEnabled(enabled: Boolean)

    fun setCameraFacing(front: Boolean)

    fun setMicEnabled(enabled: Boolean)

    fun setSpeakerEnabled(enabled: Boolean)
}

data class DeviceConfig(
        var cameraEnabled: Boolean = true,
        var cameraFront: Boolean = true,
        var micEnabled: Boolean = true,
        var speakerEnabled: Boolean = true)