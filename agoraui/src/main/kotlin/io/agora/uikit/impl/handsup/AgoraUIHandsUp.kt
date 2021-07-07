package io.agora.uikit.impl.handsup

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import io.agora.educontext.EduContextError
import io.agora.educontext.EduContextHandsUpState
import io.agora.educontext.EduContextPool
import io.agora.uikit.R
import io.agora.uikit.component.toast.AgoraUIToastManager
import io.agora.uikit.educontext.handlers.HandsUpHandler
import io.agora.uikit.impl.AbsComponent

@SuppressLint("ClickableViewAccessibility")
class AgoraUIHandsUp(
        context: Context,
        private val eduContext: EduContextPool?,
        parent: ViewGroup,
        private var left: Int,
        private var top: Int,
        private var width: Int,
        private var height: Int
) : AbsComponent(), OnTouchListener {
    private val tag = "AgoraUIHandsUp"

    private val layout: View = LayoutInflater.from(context).inflate(R.layout.agora_hands_up_layout, parent, false)
    private val handImg: ImageView = layout.findViewById(R.id.hands_up_img)
    private val countDownText: TextView = layout.findViewById(R.id.count_down_text)
    private val countDownTexts: Array<String> = context.resources.getStringArray(R.array.agora_hands_up_count_down_texts)

    private var curState: EduContextHandsUpState = EduContextHandsUpState.Init
    private var coHost = false

    private var cancelCountDownTimer: CountDownTimer = object : CountDownTimer(3200, 1000) {
        override fun onFinish() {
            countDownText.text = ""
            countDownText.isVisible = false
            handImg.setBackgroundResource(R.drawable.agora_handsup_down_img)
            eduContext?.handsUpContext()?.performHandsUp(EduContextHandsUpState.HandsDown)
        }

        override fun onTick(millisUntilFinished: Long) {
            val index: Int = (3 - (millisUntilFinished / 1000)).toInt() + 1
            countDownText.text = countDownTexts[index]
        }
    }

    private val handsUpHandler = object : HandsUpHandler() {
        override fun onHandsUpEnabled(enabled: Boolean) {
            this@AgoraUIHandsUp.setHandsUpEnable(enabled)
        }

        override fun onHandsUpStateUpdated(state: EduContextHandsUpState, coHost: Boolean) {
            this@AgoraUIHandsUp.updateHandsUpState(state, coHost)
        }

         override fun onHandsUpStateResultUpdated(error: EduContextError?) {
             this@AgoraUIHandsUp.updateHandsUpStateResult(error)
        }
    }

    init {
        parent.addView(layout, width, height)
        val layoutParams = layout.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.leftMargin = left
        layoutParams.topMargin = top
        layout.layoutParams = layoutParams
        handImg.setOnTouchListener(this)

        eduContext?.handsUpContext()?.addHandler(handsUpHandler)
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                if (curState == EduContextHandsUpState.Init || curState == EduContextHandsUpState.HandsDown) {
                    eduContext?.handsUpContext()?.performHandsUp(EduContextHandsUpState.HandsUp)
                }
                cancelCountDownTimer.cancel()
            }
            MotionEvent.ACTION_UP -> {
                handsUpStart()
            }
        }
        return true
    }

    private fun handsUpStart() {
        cancelCountDownTimer.start()
        countDownText.isVisible = true
        countDownText.text = countDownTexts[1]
    }

    fun setHandsUpEnable(enable: Boolean) {
        layout.post {
            // layout.visibility = if (enable) VISIBLE else GONE
            if (enable) {
                layout.visibility = VISIBLE
            } else {
                cancelCountDownTimer.cancel()
                layout.visibility = INVISIBLE
            }
        }
    }

    fun updateHandsUpState(state: EduContextHandsUpState, coHost: Boolean) {
        this.curState = state
        this.coHost = coHost
        layout.post {
            if (coHost) {
                cancelCountDownTimer.cancel()
                countDownText.isVisible = false
                handImg.isEnabled = false
                handImg.setBackgroundResource(R.drawable.agora_handsup_cohost_img)
            } else {
                handImg.isEnabled = true
                if (state == EduContextHandsUpState.HandsDown) {
                    handImg.setBackgroundResource(R.drawable.agora_handsup_down_img)
                }
            }
        }
    }

    fun updateHandsUpStateResult(error: EduContextError?) {
        error?.let {
            AgoraUIToastManager.showShort(it.msg)
        }
    }

    override fun setRect(rect: Rect) {
        layout.post {
            val params = layout.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = rect.top
            params.leftMargin = rect.left
            params.width = rect.right - rect.left
            params.height = rect.bottom - rect.top
            layout.layoutParams = params
            top = rect.top
            left = rect.left
            width = params.width
            height = params.height
        }
    }
}