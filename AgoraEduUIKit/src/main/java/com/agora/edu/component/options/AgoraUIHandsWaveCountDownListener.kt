package com.agora.edu.component.options

/**
 * author : felix
 * date : 2022/2/28
 * description :
 */
interface AgoraUIHandsWaveCountDownListener {
    fun onCountDownStart(timeoutInSeconds: Int)

    fun onCountDownTick(secondsToFinish: Int)

    fun onCountDownEnd()
}