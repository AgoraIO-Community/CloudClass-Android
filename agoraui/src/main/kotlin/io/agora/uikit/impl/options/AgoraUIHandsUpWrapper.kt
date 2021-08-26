package io.agora.uikit.impl.options

import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.util.TypedValue.COMPLEX_UNIT_PX
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import io.agora.educontext.EduContextError
import io.agora.educontext.EduContextHandsUpState
import io.agora.educontext.EduContextPool
import io.agora.uikit.R
import io.agora.uikit.component.toast.AgoraUIToastManager
import io.agora.uikit.educontext.handlers.HandsUpHandler

@SuppressLint("ClickableViewAccessibility")
class AgoraUIHandsUpWrapper(private val parent: ViewGroup,
                            private val eduContext: EduContextPool?,
                            private val anchor: AppCompatImageView,
                            private var width: Int,
                            private var height: Int,
                            private var right: Int,
                            private var bottom: Int) {

    private val tag = "AgoraUIHandsUpWrapper"

    private val timerLimit = 3200L
    private val timerInterval = 1000L

    private val timerView = LayoutInflater.from(anchor.context).inflate(
        R.layout.agora_handsup_countdown_timer_layout, parent, false)

    private val timerText = timerView.findViewById<AppCompatTextView>(R.id.count_down_text)

    private val countDownTexts = parent.context.resources.
        getStringArray(R.array.agora_hands_up_count_down_texts)

    private val handsUpImages = arrayOf(
        R.drawable.agora_handsup_up_img,
        R.drawable.agora_handsup_down_img,
        R.drawable.agora_handsup_cohost_img)

    private var state = EduContextHandsUpState.Init
    private var isCoHost = false

    @SuppressLint("ClickableViewAccessibility")
    private val anchorTouchListener = View.OnTouchListener { _, event ->
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                showTimerText()
                if (state == EduContextHandsUpState.Init ||
                    state == EduContextHandsUpState.HandsDown) {
                    handsUpCountDownTimer.start()
                } else if(state == EduContextHandsUpState.HandsUp) {
                    cancelCountDownTimer.start()
                }
            }
            MotionEvent.ACTION_UP -> {
                hideTimerText()
                handsUpCountDownTimer.cancel()
                cancelCountDownTimer.cancel()
            }
        }
        true
    }

    private var handsUpCountDownTimer: CountDownTimer = object : CountDownTimer(timerLimit, timerInterval) {
        override fun onFinish() {
            hideTimerText()
            timerText.text = countDownTexts[0]
            eduContext?.handsUpContext()?.performHandsUp(EduContextHandsUpState.HandsUp)
        }

        override fun onTick(millisUntilFinished: Long) {
            val index: Int = (3 - (millisUntilFinished / 1000)).toInt() + 1
            timerText.text = countDownTexts[index]
        }
    }

    private var cancelCountDownTimer: CountDownTimer = object : CountDownTimer(timerLimit, timerInterval) {
        override fun onFinish() {
            hideTimerText()
            timerText.text = countDownTexts[0]
            eduContext?.handsUpContext()?.performHandsUp(EduContextHandsUpState.HandsDown)
        }

        override fun onTick(millisUntilFinished: Long) {
            val index: Int = (3 - (millisUntilFinished / 1000)).toInt() + 1
            timerText.text = countDownTexts[index]
        }
    }

    private val handsUpHandler = object : HandsUpHandler() {
        override fun onHandsUpEnabled(enabled: Boolean) {
            this@AgoraUIHandsUpWrapper.setHandsUpEnable(enabled)
        }

        override fun onHandsUpStateUpdated(state: EduContextHandsUpState, coHost: Boolean) {
            this@AgoraUIHandsUpWrapper.updateHandsUpState(state, coHost)
        }

        override fun onHandsUpStateResultUpdated(error: EduContextError?) {
            this@AgoraUIHandsUpWrapper.updateHandsUpStateResult(error)
        }
    }

    init {
        anchor.setOnTouchListener(anchorTouchListener)

        parent.addView(timerView)
        locate()
        hideTimerText()

        eduContext?.handsUpContext()?.addHandler(handsUpHandler)
    }

    private fun locate() {
        val param = timerView.layoutParams as ViewGroup.MarginLayoutParams
        param.width = width
        param.height = height
        param.rightMargin = right
        param.bottomMargin = bottom
        param.leftMargin = parent.width - right - width
        param.topMargin = parent.height - bottom - height
        timerView.layoutParams = param

        timerText.setTextSize(COMPLEX_UNIT_PX, width / 2f)
    }

    private fun showTimerText() {
        timerView.visibility = View.VISIBLE
    }

    private fun hideTimerText() {
        timerView.visibility = View.GONE
    }

    fun setHandsUpEnable(enable: Boolean) {
        anchor.post {
            if (enable) {
                anchor.setImageResource(handsUpImages[0])
            } else {
                handsUpCountDownTimer.cancel()
                cancelCountDownTimer.cancel()
                anchor.setImageResource(handsUpImages[2])
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun updateHandsUpState(state: EduContextHandsUpState, coHost: Boolean) {
        this.state = state
        this.isCoHost = coHost

        anchor.post {
            if (coHost) {
                handsUpCountDownTimer.cancel()
                cancelCountDownTimer.cancel()
                anchor.setOnTouchListener(null)
                anchor.setImageResource(handsUpImages[2])
            } else {
                anchor.setOnTouchListener(anchorTouchListener)
                if (state == EduContextHandsUpState.HandsUp) {
                    anchor.setImageResource(handsUpImages[0])
                } else {
                    anchor.setImageResource(handsUpImages[1])
                }
            }
        }
    }

    fun updateHandsUpStateResult(error: EduContextError?) {
        error?.let {
            AgoraUIToastManager.showShort(it.msg)
        }
    }
}