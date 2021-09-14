package io.agora.edu.uikit.impl.handsup

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import io.agora.edu.R
import io.agora.edu.uikit.component.toast.AgoraUIToastManager
import io.agora.edu.uikit.handlers.HandsUpHandler
import io.agora.edu.uikit.impl.AbsComponent

@SuppressLint("ClickableViewAccessibility")
class AgoraUIHandsUp(
        context: Context,
        private val eduContext: io.agora.edu.core.context.EduContextPool?,
        parent: ViewGroup,
        private var left: Int,
        private var top: Int,
        private var width: Int,
        private var height: Int
) : AbsComponent(), OnTouchListener {
    private val tag = "AgoraUIHandsUp"

    private val layout: View = LayoutInflater.from(context).inflate(R.layout.agora_hands_up_layout, parent, false)
    private val countDownLayout: RelativeLayout = layout.findViewById(R.id.count_down_layout)
    private val countDownText: AppCompatTextView = layout.findViewById(R.id.count_down_text)
    private val handImg: AppCompatImageView = layout.findViewById(R.id.hands_up_img)
    private val countDownTexts: Array<String> = context.resources.getStringArray(R.array.agora_hands_up_count_down_texts)
    private val handsUpImgs: Array<Int> = arrayOf(
            R.drawable.agora_handsup_up_img,
            R.drawable.agora_handsup_down_img,
            R.drawable.agora_handsup_cohost_img)

    private var curState: io.agora.edu.core.context.EduContextHandsUpState = io.agora.edu.core.context.EduContextHandsUpState.Init
    private var coHost = false

    private var handsUpCountDownTimer: CountDownTimer = object : CountDownTimer(3200, 1000) {
        override fun onFinish() {
            countDownLayout.visibility = INVISIBLE
            countDownText.text = countDownTexts[0]
            eduContext?.handsUpContext()?.performHandsUp(io.agora.edu.core.context.EduContextHandsUpState.HandsUp)
        }

        override fun onTick(millisUntilFinished: Long) {
            val index: Int = (3 - (millisUntilFinished / 1000)).toInt() + 1
            countDownText.text = countDownTexts[index]
        }
    }

    private var cancelCountDownTimer: CountDownTimer = object : CountDownTimer(3200, 1000) {
        override fun onFinish() {
            countDownLayout.visibility = INVISIBLE
            countDownText.text = countDownTexts[0]
            eduContext?.handsUpContext()?.performHandsUp(io.agora.edu.core.context.EduContextHandsUpState.HandsDown)
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

        override fun onHandsUpStateUpdated(state: io.agora.edu.core.context.EduContextHandsUpState, coHost: Boolean) {
            this@AgoraUIHandsUp.updateHandsUpState(state, coHost)
        }

         override fun onHandsUpStateResultUpdated(error: io.agora.edu.core.context.EduContextError?) {
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
                countDownLayout.visibility = VISIBLE
                if (curState == io.agora.edu.core.context.EduContextHandsUpState.Init || curState == io.agora.edu.core.context.EduContextHandsUpState.HandsDown) {
                    handsUpCountDownTimer.start()
                } else if(curState == io.agora.edu.core.context.EduContextHandsUpState.HandsUp) {
                    cancelCountDownTimer.start()
                }
            }
            MotionEvent.ACTION_UP -> {
                countDownLayout.visibility = INVISIBLE
                handsUpCountDownTimer.cancel()
                cancelCountDownTimer.cancel()
            }
        }
        return true
    }

    fun setHandsUpEnable(enable: Boolean) {
        layout.post {
            if (enable) {
                layout.visibility = VISIBLE
            } else {
                handsUpCountDownTimer.cancel()
                countDownLayout.visibility = INVISIBLE
                cancelCountDownTimer.cancel()
                layout.visibility = INVISIBLE
            }
        }
    }

    fun updateHandsUpState(state: io.agora.edu.core.context.EduContextHandsUpState, coHost: Boolean) {
        this.curState = state
        this.coHost = coHost
        layout.post {
            if (coHost) {
                handsUpCountDownTimer.cancel()
                countDownLayout.visibility = INVISIBLE
                handImg.setOnTouchListener(null)
                handImg.setImageResource(handsUpImgs[2])
            } else {
                handImg.setOnTouchListener(this)
                if (state == io.agora.edu.core.context.EduContextHandsUpState.HandsUp) {
                    handImg.setImageResource(handsUpImgs[0])
                } else {
                    handImg.setImageResource(handsUpImgs[1])
                }
            }
        }
    }

    fun updateHandsUpStateResult(error: io.agora.edu.core.context.EduContextError?) {
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