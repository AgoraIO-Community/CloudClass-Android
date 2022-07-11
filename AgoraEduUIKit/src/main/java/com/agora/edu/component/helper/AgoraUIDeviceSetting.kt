package com.agora.edu.component.helper

/**
 * Used to record UI-layer controlled device states,
 * like camera facing.
 */
object AgoraUIDeviceSetting {
    private var isCameraFront: Boolean = true

    @Synchronized
    fun isFrontCamera(): Boolean {
        return isCameraFront
    }

    @Synchronized
    fun setFrontCamera(isFront: Boolean) {
        isCameraFront = isFront
    }
}