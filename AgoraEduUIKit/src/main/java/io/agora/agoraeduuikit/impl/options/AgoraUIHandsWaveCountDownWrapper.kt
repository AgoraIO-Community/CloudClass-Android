package io.agora.agoraeduuikit.impl.options

import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import io.agora.agoraeducore.core.context.EduContextPool
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeduuikit.impl.users.AgoraUIHandsUpToastPopUp

class AgoraUIHandsWaveCountDownWrapper(private val anchor: View,
                                       private val parent: ViewGroup,
                                       private val tipRightMargin: Int,
                                       private val eduContext: EduContextPool?,
                                       private val listener: AgoraUIHandsWaveCountDownListener? = null) {

    private val tag = "AgoraUIHandsUpWrapper"

    private val handsWaveTimeout = 3200L
    private val timerTick = 1000L
    private var tipsToastPopUp: AgoraUIHandsUpToastPopUp? = null
    private var tipToastShown = false

    private var isHandsUpButtonHolding = false

    private val scaleFactor = 1.1f

    private val touchDownTimer: HandsWaveTimer =
        HandsWaveTimer(handsWaveTimeout, timerTick,
            object : ((Int) -> Unit) {
                override fun invoke(timeout: Int) {
                    handleTimerStart(timeout)
                }
            },
            object : ((Int, Int) -> Unit) {
                override fun invoke(timeout: Int, remainsInSec: Int) {
                    // Displays the max waiting time when the button is holding
                    handleTimerTick(timeout)
                }
            },
            object : (() -> Unit) {
                override fun invoke() {
                    handleTouchDownTimerStop()
                }
            })

    private val touchCancelTimer =
        HandsWaveTimer(handsWaveTimeout, timerTick,
            object : ((Int) -> Unit) {
                override fun invoke(timeout: Int) {
                    handleTimerStart(timeout)
                }
            },
            object : ((Int, Int) -> Unit) {
                override fun invoke(timeout: Int, remainsInSec: Int) {
                    handleTimerTick(remainsInSec)
                }
            },
            object : (() -> Unit) {
                override fun invoke() {
                    handleTouchCancelTimerStop()
                }
            })

    private fun handleTimerStart(timeout: Int) {
        listener?.onCountDownStart(timeout)
    }

    private fun handleTimerTick(remainSeconds: Int) {
        listener?.onCountDownTick(remainSeconds)
    }

    private fun handleTouchDownTimerStop() {
        synchronized(this) {
            if (isHandsUpButtonHolding) {
                touchDownTimer.startTimer()
            }
        }
    }

    private fun handleTouchCancelTimerStop() {
        synchronized(this) {
            listener?.onCountDownEnd()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private val handsWaveTouchListener = View.OnTouchListener { _, event ->
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isHandsUpButtonHolding) {
                    Constants.AgoraLog.i("$tag->timer has already been " +
                            "holding, ignore this touch event")
                    return@OnTouchListener true
                }

                synchronized(this@AgoraUIHandsWaveCountDownWrapper) {
                    isHandsUpButtonHolding = true
                }
                anchorScale(true)
                showTipToast()
                touchDownTimer.cancel()
                touchCancelTimer.cancel()
                touchDownTimer.startTimer()
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (!isHandsUpButtonHolding) {
                    Constants.AgoraLog.i("$tag->no timer is holding " +
                            "anymore, ignore this touch event")
                    return@OnTouchListener true
                }

                synchronized(this@AgoraUIHandsWaveCountDownWrapper) {
                    isHandsUpButtonHolding = false
                }
                anchorScale(false)
                touchDownTimer.cancel()
                touchCancelTimer.startTimer()
            }
        }
        false
    }

    private fun anchorScale(scaled: Boolean) {
        if (scaled) {
            anchor.scaleX = scaleFactor
            anchor.scaleY = scaleFactor
        } else {
            anchor.scaleX = 1f
            anchor.scaleY = 1f
        }
    }

    @Synchronized
    private fun showTipToast() {
        if (!tipToastShown && !tipToastShown()) {
            tipToastShown = true
            AgoraUIHandsUpToastPopUp(parent.context).let {
                it.setEduContext(eduContext)
                val positions = calculateTipPosition()
                it.initView(parent, positions[1], positions[0])
                val height = (anchor.height * 2f / 3).toInt()
                val width = (height * 120f / 46).toInt()
                it.show(width, height)
            }
        }
    }

    private fun calculateTipPosition(): IntArray {
        val anchorPos = intArrayOf(0, 0)
        anchor.getLocationOnScreen(anchorPos)
        val containerPos = intArrayOf(0, 0)
        parent.getLocationOnScreen(containerPos)
        val top = anchorPos[1] - containerPos[1]
        val right = anchor.width + tipRightMargin
        val result = intArrayOf(0, 0)
        result[0] = top + anchor.height / 2
        result[1] = right
        return result
    }

    private fun tipToastShown(): Boolean {
        return tipsToastPopUp != null && tipsToastPopUp!!.isShowing
    }

    init {
        anchor.setOnTouchListener(handsWaveTouchListener)
    }

    private fun getSeconds(milliSec: Int): Int {
        return (milliSec / 1000f).toInt()
    }

    private inner class HandsWaveTimer(private val timeout: Long, tickInterval: Long,
                                       private val start: ((Int) -> Unit)? = null,
                                       private val tick: ((Int, Int) -> Unit)? = null,
                                       private val end: (() -> Unit)? = null) : CountDownTimer(timeout, tickInterval) {

        private val timeoutInSeconds = getSeconds(timeout.toInt())

        fun startTimer() {
            start?.invoke(timeoutInSeconds)
            start()
        }

        override fun onTick(millisUntilFinished: Long) {
            tick?.invoke(timeoutInSeconds, getSeconds(millisUntilFinished.toInt()))
        }

        override fun onFinish() {
            end?.invoke()
        }
    }
}

interface AgoraUIHandsWaveCountDownListener {
    fun onCountDownStart(timeoutInSeconds: Int)

    fun onCountDownTick(secondsToFinish: Int)

    fun onCountDownEnd()
}