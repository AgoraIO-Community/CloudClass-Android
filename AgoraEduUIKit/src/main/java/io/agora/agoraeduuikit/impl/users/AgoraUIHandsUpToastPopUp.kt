package io.agora.agoraeduuikit.impl.users

import android.content.Context
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.view.forEach
import io.agora.agoraeduuikit.R
import io.agora.agoraeducore.core.context.EduContextPool
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig

class AgoraUIHandsUpToastPopUp : RelativeLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private val tag = "AgoraUIRosterPopUp"

    private var parent: ViewGroup? = null
    private var contentWidth = 0
    private var contentHeight = 0
    private var marginRight = 0
    private var centerVertical = 0

    var isShowing = false
    private val timerLimit = 3000L
    private val timerInterval = 1000L
    private var eduContext: EduContextPool? = null

    fun setEduContext(eduContextPool: EduContextPool?) {
        this.eduContext = eduContextPool
    }

    fun initView(parent: ViewGroup, right: Int, centerVertical: Int) {
        this.parent = parent
        this.marginRight = right
        this.centerVertical = centerVertical
    }

    private var cancelCountDownTimer: CountDownTimer = object : CountDownTimer(timerLimit, timerInterval) {
        override fun onFinish() {
            dismiss()
        }

        override fun onTick(millisUntilFinished: Long) {

        }
    }

    fun show(width: Int, height: Int) {
        contentWidth = width
        contentHeight = height
        isShowing = true
        this.parent?.let { parent ->
            LayoutInflater.from(parent.context).inflate(R.layout.agora_handup_toast_dialog_layout, this)
            parent.addView(this)
            val param = this.layoutParams as MarginLayoutParams

            param.width = contentWidth
            param.height = contentHeight
            param.rightMargin = marginRight
            param.leftMargin = parent.width - marginRight - contentWidth
            param.topMargin = centerVertical - contentHeight / 2
            this.layoutParams = param
        }
        cancelCountDownTimer.start()
    }

    fun show() {
        if (AgoraUIConfig.isLargeScreen) {
            contentWidth = 120
            contentHeight = 46
        } else {
            contentWidth = 250
            contentHeight = 88
        }

        show(contentWidth, contentHeight)
    }

    fun dismiss() {
        parent?.let { parent ->
            var contains = false
            parent.forEach {
                if (it == this) contains = true
            }
            if (contains) parent.removeView(this)
            this.removeAllViews()
            isShowing = false
        }
    }
}