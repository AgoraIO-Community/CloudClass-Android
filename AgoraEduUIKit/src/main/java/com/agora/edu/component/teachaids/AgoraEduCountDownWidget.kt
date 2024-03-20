package com.agora.edu.component.teachaids

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.agora.edu.component.teachaids.AgoraTeachAidWidgetInteractionSignal.ActiveState
import com.agora.edu.component.teachaids.CountdownLaunchStatus.Init
import com.agora.edu.component.teachaids.CountdownLaunchStatus.Started
import com.agora.edu.component.teachaids.CountdownStatics.DEFAULT_DURATION
import com.agora.edu.component.teachaids.CountdownStatics.MAX_DURATION
import com.agora.edu.component.teachaids.CountdownStatics.PROPERTIES_KEY_DURATION
import com.agora.edu.component.teachaids.CountdownStatics.PROPERTIES_KEY_PAUSE_TIME
import com.agora.edu.component.teachaids.CountdownStatics.PROPERTIES_KEY_START_TIME
import com.agora.edu.component.teachaids.CountdownStatics.PROPERTIES_KEY_STATE
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.internal.framework.data.EduBaseUserInfo
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.core.internal.util.TimeUtil
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetUserInfo
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.databinding.AgoraCountdownWidgetContentBinding
import io.agora.agoraeduuikit.databinding.FcrCountdownClockDigitBinding
import io.agora.agoraeduuikit.databinding.FcrViewSimpleClockBinding
import java.util.concurrent.TimeUnit

/**
 * author : cjw
 * date : 2022/2/14
 * description :
 */
class AgoraTeachAidCountDownWidget : AgoraTeachAidMovableWidget() {
    override val TAG = "AgoraCountDownWidget"

    private var countDownContent: AgoraCountDownWidgetContent? = null

    override fun init(container: ViewGroup) {
        super.init(container)
        container.post {
            widgetInfo?.localUserInfo?.let {
                countDownContent = AgoraCountDownWidgetContent(container, it)
            }
        }
    }

    fun getWidgetMsgObserver(): AgoraWidgetMessageObserver? {
        return null
    }

    override fun onWidgetRoomPropertiesUpdated(
        properties: MutableMap<String, Any>, cause: MutableMap<String, Any>?,
        keys: MutableList<String>, operator: EduBaseUserInfo?
    ) {
        super.onWidgetRoomPropertiesUpdated(properties, cause, keys, operator)
        countDownContent?.parseProperties(properties)
    }

    override fun onWidgetRoomPropertiesDeleted(
        properties: MutableMap<String, Any>, cause: MutableMap<String, Any>?,
        keys: MutableList<String>
    ) {
        super.onWidgetRoomPropertiesDeleted(properties, cause, keys)
        countDownContent?.parseProperties(properties)
    }

    override fun release() {
        countDownContent?.dispose()
        super.release()
    }

    private inner class AgoraCountDownWidgetContent(val container: ViewGroup, val localUserInfo: AgoraWidgetUserInfo) {
        private val tag = "AgoraCountDownWidget"

        // widget's ui has be added to container in here，not repeat add
        private val binding = AgoraCountdownWidgetContentBinding.inflate(
            LayoutInflater.from(container.context),
            container, true
        )
        private var mCountdownStarted = false
        private var mCountdownInitialState = false

        init {
            widgetInfo?.roomProperties?.let {
                parseProperties(it)
            }
            readyUI()
        }

        fun dispose() {
            binding.countdownClock.release()
            ContextCompat.getMainExecutor(container.context).execute {
                container.removeView(binding.root)
            }
        }

        @Synchronized
        fun parseProperties(properties: Map<String, Any>) {
            // Countdown state means whether the count down is started, stopped or paused
            val state = NumberParser.parseStringIntOrZero(properties[PROPERTIES_KEY_STATE])
            val started = state == Started.value
            val initialState = state == Init.value
            val startTimeMillis = NumberParser.parseStringLongOrZero(properties[PROPERTIES_KEY_START_TIME])
            // What does pauseTime do? There are no other platforms, but this logic is not understood,
            // so it will not be deleted for the time being.
            val pauseTime = NumberParser.parseStringLongOrZero(properties[PROPERTIES_KEY_PAUSE_TIME])
            val duration = NumberParser.parseStringIntOrZero(properties[PROPERTIES_KEY_DURATION])
            LogX.i(
                tag, "Countdown properties updated, started:" + started
                    + ", initialState:" + initialState + ", start time:" + startTimeMillis + ", duration:" + duration
            )
            if (!initialState) {
                if (started) {
                    val currentTime = TimeUtil.currentTimeMillis()
                    val leftMillis = (startTimeMillis + duration * 1000L - currentTime)
                    val leftSeconds = leftMillis / 1000L + 1
                    val remainder = leftMillis % 1000L
                    if (leftSeconds > 0) {
                        startCountDownInSeconds(leftSeconds, remainder)
                    }
                }
            } else if (!mCountdownInitialState) {
                if (mCountdownStarted) {
                    // countDown state become initial from running.
                    LogX.i(tag, "Countdown is reset, stop local countdown ticking and set initial state")
                    resetCountdownTicking()
                } else {
                    LogX.i(tag, "Countdown has been reset, resetCountdownTicking")
                    resetCountdownTicking()
                }
            }
            mCountdownStarted = started
            mCountdownInitialState = initialState
        }

        private fun startCountDownInSeconds(seconds: Long, remainder: Long) {
            LogX.i(tag, "startCountDown $seconds sec")
            var left = seconds
            if (left < 0) {
                left = 0
            }
//            else if (left >= MAX_DURATION) {
//                left = (MAX_DURATION - 1).toLong()
//            }
            val finalLeft = left
            if (binding.countdownClock.isAttachedToWindow) {
                binding.countdownClock.setCountdownListener(object : CountDownClock.CountdownCallBack {
                    override fun countdownAboutToFinish() {
                        LogX.i(tag, "Countdown is about to finish")
                        binding.countdownClock.setDigitTextColor(Color.RED)
                    }

                    override fun countdownFinished() {
                        LogX.i(tag, "Countdown finished")
                    }
                })
                binding.countdownClock.postDelayed({
                    binding.countdownClock.startCountDown(finalLeft * 1000)
                }, remainder)
            } else {
                LogX.i(tag, "[0]view has not attached to window, UI operations fail")
            }
        }

        private fun setSpecificCountdownTime(milliSeconds: Long) {
            if (binding.countdownClock.isAttachedToWindow) {
                binding.countdownClock.post { binding.countdownClock.setCountDownTime(milliSeconds) }
            } else {
                LogX.i(tag, "[1]view has not attached to window, UI operations fail")
            }
        }

        private fun resetCountdownTicking() {
            if (binding.countdownClock.isAttachedToWindow) {
                binding.countdownClock.post { binding.countdownClock.setInitState() }
            } else {
                LogX.i(tag, "[2]view has not attached to window, UI operations fail")
            }
        }

        private fun readyUI() {
            val isTeacher = localUserInfo.userRole == AgoraEduContextUserRole.Teacher.value
            if (!isTeacher) {
                binding.durationLayout.visibility = GONE
                binding.actionBtn.visibility = GONE
                binding.closeImg.visibility = GONE
                return
            }
            binding.durationLayout.visibility = VISIBLE
            binding.durationEditText.setText(DEFAULT_DURATION.toString())
            binding.durationEditText.isEnabled = true
            binding.durationEditText.isFocusable = true
            binding.durationEditText.isFocusableInTouchMode = true
            binding.actionBtn.visibility = VISIBLE
            binding.closeImg.setOnClickListener { switchSelf() }
            binding.durationEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    if (!TextUtils.isEmpty(s.toString())) {
                        val duration = s.toString().toInt()
                        binding.durationEditText.setTextColor(
                            container.resources.getColor(
                                if (duration > MAX_DURATION) R.color.fcr_red else R.color.fcr_black
                            )
                        )
                        binding.actionBtn.isEnabled = duration <= MAX_DURATION
                    }
                }
            })
            binding.actionBtn.setOnClickListener {
//                if (actionIsRestart) {
//                    restartCountdownByTeacher()
//                } else {
                startCountdownByTeacher()
//                }
            }
            if (mCountdownStarted) {
                binding.durationLayout.visibility = GONE
                binding.actionBtn.visibility = GONE
            }
        }

        /**
         * sendMsg to AgoraEduTeachAidContainerComponent.countDownWidgetMsgObserver to close mySelf
         * @param close whether unInstall widget's ui
         * @param extraProperties when {close} is false, you want to active this widget for remoter, {extraProperties} is
         * initialize properties.
         */
        private fun switchSelf(close: Boolean = true, extraProperties: Map<String, Any>? = null) {
            val packet = AgoraTeachAidWidgetInteractionPacket(
                ActiveState,
                AgoraTeachAidWidgetActiveStateChangeData(!close, extraProperties)
            )
            val json = GsonUtil.toJson(packet)
            json?.let { sendMessage(it) }
//            // when unInstall this widget, also should clear custom data(del all properties under extra)
//            if (close) {
//                val propertyKeys = mutableListOf<String>()
//                widgetInfo?.roomProperties?.map { it.key }?.toCollection(propertyKeys)
//                deleteRoomProperties()
//            }
        }

        private fun startCountdownByTeacher() {
            val duration: Editable? = binding.durationEditText.text
            if (duration?.isNotEmpty() == true) {
//                binding.actionBtn.text = container.resources.getString(R.string.restart)
//                actionIsRestart = true
                binding.actionBtn.visibility = GONE
                binding.durationLayout.visibility = GONE
                val extraProperties = CountdownStatus(
                    Started.value.toString(),
                    TimeUtil.currentTimeMillis().toString(), null, duration.toString()
                ).convert()
                // active widget and upsert properties(sync for remoter)
                switchSelf(close = false, extraProperties = extraProperties)
            } else {
                LogX.e(tag, "duration is empty or null, please check.")
            }
        }

//        private fun restartCountdownByTeacher() {
//            binding.actionBtn.text = container.resources.getString(R.string.start)
//            actionIsRestart = false
//            binding.durationLayout.visibility = VISIBLE
//            binding.durationEditText.setText(DEFAULT_DURATION.toString())
//            val properties = CountdownStatus(Paused.value.toString(), null, null, null).convert()
//            updateRoomProperties(properties = properties, cause = mutableMapOf())
//        }
    }
}

class AlignedTextView : androidx.appcompat.widget.AppCompatTextView {
    private var alignment = ProperTextAlignment.TOP
    private val textRect = Rect()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AlignedTextView, defStyleAttr, 0)
            val alignment = typedArray.getInt(R.styleable.AlignedTextView_alignment, 0)
            if (alignment != 0) {
                setAlignment(alignment)
            } else {
                Log.e("AlignedTextView", "You did not set an alignment for an AlignedTextView. Default is top alignment.")
            }

            invalidate()
            typedArray.recycle()
        }
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.let {
            canvas.getClipBounds(textRect)
            val cHeight = textRect.height()
            paint.getTextBounds(this.text.toString(), 0, this.text.length, textRect)
            val bottom = textRect.bottom
            textRect.offset(-textRect.left, -textRect.top)
            paint.textAlign = Paint.Align.CENTER
            var drawY = 0f
            if (alignment == ProperTextAlignment.TOP) {
                drawY = (textRect.bottom - bottom).toFloat() - ((textRect.bottom - textRect.top) / 2)
            } else if (alignment == ProperTextAlignment.BOTTOM) {
                drawY = top + cHeight.toFloat() + ((textRect.bottom - textRect.top) / 2)
            }
            val drawX = (canvas.width / 2).toFloat()
            paint.color = this.currentTextColor
            canvas.drawText(this.text.toString(), drawX, drawY, paint)
        }
    }

    private fun setAlignment(alignment: Int) {
        if (alignment == 1) {
            this.alignment = ProperTextAlignment.TOP
        } else if (alignment == 2) {
            this.alignment = ProperTextAlignment.BOTTOM
        }
    }

    private enum class ProperTextAlignment {
        TOP,
        BOTTOM
    }
}

class CountDownClock : LinearLayout {
    private var countDownTimer: CountDownTimer? = null
    private var countdownListener: CountdownCallBack? = null
    private var countdownTickInterval = 1000

    private var almostFinishedCallbackTimeInSeconds: Int = 5

    private var resetSymbol: String = "0"

    private val binding = FcrViewSimpleClockBinding.inflate(
        LayoutInflater.from(context),
        this, true
    )

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        //View.inflate(context, R.layout.fcr_view_simple_clock, this)

        attrs?.let {
            val typedArray = context?.obtainStyledAttributes(attrs, R.styleable.CountDownClock, defStyleAttr, 0)

            val resetSymbol = typedArray?.getString(R.styleable.CountDownClock_resetSymbol)
            if (resetSymbol != null) {
                setResetSymbol(resetSymbol)
            }

            val digitTopDrawable = typedArray?.getDrawable(R.styleable.CountDownClock_digitTopDrawable)
            setDigitTopDrawable(digitTopDrawable)
            val digitBottomDrawable = typedArray?.getDrawable(R.styleable.CountDownClock_digitBottomDrawable)
            setDigitBottomDrawable(digitBottomDrawable)
            val digitDividerColor = typedArray?.getColor(R.styleable.CountDownClock_digitDividerColor, 0)
            setDigitDividerColor(digitDividerColor ?: 0)
            val digitSplitterColor = typedArray?.getColor(R.styleable.CountDownClock_digitSplitterColor, 0)
            setDigitSplitterColor(digitSplitterColor ?: 0)

            val digitTextColor = typedArray?.getColor(R.styleable.CountDownClock_digitTextColor, 0)

            setDigitTextColor(digitTextColor ?: 0)

            val digitTextSize = typedArray?.getDimension(R.styleable.CountDownClock_digitTextSize, 0f)
            setDigitTextSize(digitTextSize ?: 0f)
            setSplitterDigitTextSize((digitTextSize?.div(2)) ?: 0f)

            val digitPadding = typedArray?.getDimension(R.styleable.CountDownClock_digitPadding, 0f)
            setDigitPadding(digitPadding?.toInt() ?: 0)

            val splitterPadding = typedArray?.getDimension(R.styleable.CountDownClock_splitterPadding, 0f)
            setSplitterPadding(splitterPadding?.toInt() ?: 0)

            val halfDigitHeight = typedArray?.getDimensionPixelSize(R.styleable.CountDownClock_halfDigitHeight, 0)
            val digitWidth = typedArray?.getDimensionPixelSize(R.styleable.CountDownClock_digitWidth, 0)
            setHalfDigitHeightAndDigitWidth(halfDigitHeight ?: 0, digitWidth ?: 0)

            val animationDuration = typedArray?.getInt(R.styleable.CountDownClock_animationDuration, 0)
            setAnimationDuration(animationDuration ?: 600)

            val almostFinishedCallbackTimeInSeconds = typedArray?.getInt(R.styleable.CountDownClock_almostFinishedCallbackTimeInSeconds, 5)
            setAlmostFinishedCallbackTimeInSeconds(almostFinishedCallbackTimeInSeconds ?: 5)

            val countdownTickInterval = typedArray?.getInt(R.styleable.CountDownClock_countdownTickInterval, 1000)
            this.countdownTickInterval = countdownTickInterval ?: 1000

            invalidate()
            typedArray?.recycle()
        }
    }

    fun release() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    /**
     * prevent memory leak
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        release()
    }

    /**
     * set CountDownClock is initialize state.
     */
    fun setInitState() {
        countDownTimer?.cancel()
        setDigitTextColor(context.resources.getColor(R.color.fcr_text_level2_color))
        binding.apply{
            firstDigitMinute.setNewText(resetSymbol)
            secondDigitMinute.setNewText(resetSymbol)
            firstDigitSecond.setNewText(resetSymbol)
            secondDigitSecond.setNewText(resetSymbol)
        }
    }

    fun startCountDown(timeToNextEvent: Long) {
        countDownTimer?.cancel()
        setDigitTextColor(context.resources.getColor(R.color.fcr_text_level2_color))
        var hasCalledAlmostFinished = false

        countDownTimer = object : CountDownTimer(timeToNextEvent, countdownTickInterval.toLong()) {
            override fun onTick(millisUntilFinished: Long) {
                if (millisUntilFinished / 1000 <= almostFinishedCallbackTimeInSeconds && !hasCalledAlmostFinished) {
                    hasCalledAlmostFinished = true
                    countdownListener?.countdownAboutToFinish()
                }
                setCountDownTime(millisUntilFinished)
            }

            override fun onFinish() {
                hasCalledAlmostFinished = false
                countdownListener?.countdownFinished()
            }
        }
        countDownTimer?.start()
    }

    fun resetCountdownTimer() {
        countDownTimer?.cancel()
        binding.apply {
            firstDigitMinute.setNewText(resetSymbol)
            secondDigitMinute.setNewText(resetSymbol)
            firstDigitSecond.setNewText(resetSymbol)
            secondDigitSecond.setNewText(resetSymbol)
        }
    }

    /**
     * Pause the countdown timer and keep the
     * time value that is currently displayed
     */
    fun pauseCountdown() {
        countDownTimer?.cancel()
    }

    /**
     * Set the time to a specific value, but not starting or
     * stopping a timer
     */
    fun setCountDownTime(timeToStart: Long) {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeToStart)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeToStart - TimeUnit.MINUTES.toMillis(minutes))
        val minutesString = minutes.toString()
        val secondsString = seconds.toString()

        binding.apply {
            if (minutes >= 0) {
                when (minutesString.length) {
                    1 -> {
                        firstDigitMinute.animateTextChange("0")
                        secondDigitMinute.animateTextChange(minutesString[0].toString())
                    }

                    2 -> {
                        firstDigitMinute.animateTextChange(minutesString[0].toString())
                        secondDigitMinute.animateTextChange(minutesString[1].toString())
                    }

                    else -> {
                        // If the count time starts from longer than 100 minutes,
                        // take it as only 99m 59s maximum
                        firstDigitMinute.animateTextChange("9")
                        secondDigitMinute.animateTextChange("9")
                    }
                }
            }

            if (seconds >= 0) {
                when (secondsString.length) {
                    2 -> {
                        firstDigitSecond.animateTextChange(secondsString[0].toString())
                        secondDigitSecond.animateTextChange(secondsString[1].toString())
                    }

                    1 -> {
                        firstDigitSecond.animateTextChange("0")
                        secondDigitSecond.animateTextChange(secondsString[0].toString())
                    }

                    else -> {
                        // If the seconds calculated is larger than 59s, issues occur
                        // but take it as 59 seconds at most
                        firstDigitSecond.animateTextChange("5")
                        secondDigitSecond.animateTextChange("9")
                    }
                }
            }
        }
    }

    private fun setResetSymbol(resetSymbol: String?) {
        resetSymbol?.let {
            if (it.isNotEmpty()) {
                this.resetSymbol = resetSymbol
            } else {
                this.resetSymbol = ""
            }
        } ?: kotlin.run {
            this.resetSymbol = ""
        }
    }

    private fun setDigitTopDrawable(digitTopDrawable: Drawable?) {
        if (digitTopDrawable != null) {
            binding.apply {
                firstDigitMinute.frontUpper.background = digitTopDrawable
                firstDigitMinute.backUpper.background = digitTopDrawable
                secondDigitMinute.frontUpper.background = digitTopDrawable
                secondDigitMinute.backUpper.background = digitTopDrawable
                firstDigitSecond.frontUpper.background = digitTopDrawable
                firstDigitSecond.backUpper.background = digitTopDrawable
                secondDigitSecond.frontUpper.background = digitTopDrawable
                secondDigitSecond.backUpper.background = digitTopDrawable
            }
        } else {
            setTransparentBackgroundColor()
        }
    }

    private fun setDigitBottomDrawable(digitBottomDrawable: Drawable?) {
        if (digitBottomDrawable != null) {
            binding.apply {
                firstDigitMinute.frontLower.background = digitBottomDrawable
                firstDigitMinute.backLower.background = digitBottomDrawable
                secondDigitMinute.frontLower.background = digitBottomDrawable
                secondDigitMinute.backLower.background = digitBottomDrawable
                firstDigitSecond.frontLower.background = digitBottomDrawable
                firstDigitSecond.backLower.background = digitBottomDrawable
                secondDigitSecond.frontLower.background = digitBottomDrawable
                secondDigitSecond.backLower.background = digitBottomDrawable
            }
        } else {
            setTransparentBackgroundColor()
        }
    }

    private fun setDigitDividerColor(digitDividerColor: Int) {
        var dividerColor = digitDividerColor
        if (dividerColor == 0) {
            dividerColor = ContextCompat.getColor(context, R.color.fcr_transparent)
        }
        binding.apply {
            firstDigitMinute.digitDivider.setBackgroundColor(dividerColor)
            secondDigitMinute.digitDivider.setBackgroundColor(dividerColor)
            firstDigitSecond.digitDivider.setBackgroundColor(dividerColor)
            secondDigitSecond.digitDivider.setBackgroundColor(dividerColor)
        }
    }

    private fun setDigitSplitterColor(digitsSplitterColor: Int) {
        binding.apply {
            if (digitsSplitterColor != 0) {
                digitsSplitter.setTextColor(digitsSplitterColor)
            } else {
                digitsSplitter.setTextColor(ContextCompat.getColor(context, R.color.fcr_transparent))
            }
        }
    }

    private fun setSplitterDigitTextSize(digitsTextSize: Float) {
        binding.digitsSplitter.setTextSize(TypedValue.COMPLEX_UNIT_PX, digitsTextSize)
    }

    private fun setDigitPadding(digitPadding: Int) {
        binding.apply {
            firstDigitMinute.setPadding(digitPadding, digitPadding, digitPadding, digitPadding)
            secondDigitMinute.setPadding(digitPadding, digitPadding, 0, digitPadding)
            firstDigitSecond.setPadding(0, digitPadding, digitPadding, digitPadding)
            secondDigitSecond.setPadding(digitPadding, digitPadding, digitPadding, digitPadding)
        }
    }

    private fun setSplitterPadding(splitterPadding: Int) {
        binding.digitsSplitter.setPadding(splitterPadding, 0, splitterPadding, 0)
    }

    fun setDigitTextColor(digitsTextColor: Int) {
        var textColor = digitsTextColor
        if (textColor == 0) {
            textColor = ContextCompat.getColor(context, R.color.fcr_transparent)
        }
        binding.apply {
            firstDigitMinute.frontUpperText.setTextColor(textColor)
            firstDigitMinute.backUpperText.setTextColor(textColor)
            secondDigitMinute.frontUpperText.setTextColor(textColor)
            secondDigitMinute.backUpperText.setTextColor(textColor)
            firstDigitSecond.frontUpperText.setTextColor(textColor)
            firstDigitSecond.backUpperText.setTextColor(textColor)
            secondDigitSecond.frontUpperText.setTextColor(textColor)
            secondDigitSecond.backUpperText.setTextColor(textColor)

            firstDigitMinute.frontLowerText.setTextColor(textColor)
            firstDigitMinute.backLowerText.setTextColor(textColor)
            secondDigitMinute.frontLowerText.setTextColor(textColor)
            secondDigitMinute.backLowerText.setTextColor(textColor)
            firstDigitSecond.frontLowerText.setTextColor(textColor)
            firstDigitSecond.backLowerText.setTextColor(textColor)
            secondDigitSecond.frontLowerText.setTextColor(textColor)
            secondDigitSecond.backLowerText.setTextColor(textColor)
        }
    }

    private fun setDigitTextSize(digitsTextSize: Float) {
        binding.apply {
            firstDigitMinute.frontUpperText.setTextSize(TypedValue.COMPLEX_UNIT_PX, digitsTextSize)
            firstDigitMinute.backUpperText.setTextSize(TypedValue.COMPLEX_UNIT_PX, digitsTextSize)
            secondDigitMinute.frontUpperText.setTextSize(TypedValue.COMPLEX_UNIT_PX, digitsTextSize)
            secondDigitMinute.backUpperText.setTextSize(TypedValue.COMPLEX_UNIT_PX, digitsTextSize)
            firstDigitSecond.frontUpperText.setTextSize(TypedValue.COMPLEX_UNIT_PX, digitsTextSize)
            firstDigitSecond.backUpperText.setTextSize(TypedValue.COMPLEX_UNIT_PX, digitsTextSize)
            secondDigitSecond.frontUpperText.setTextSize(TypedValue.COMPLEX_UNIT_PX, digitsTextSize)
            secondDigitSecond.backUpperText.setTextSize(TypedValue.COMPLEX_UNIT_PX, digitsTextSize)
            firstDigitMinute.frontLowerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, digitsTextSize)
            firstDigitMinute.backLowerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, digitsTextSize)
            secondDigitMinute.frontLowerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, digitsTextSize)
            secondDigitMinute.backLowerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, digitsTextSize)
            firstDigitSecond.frontLowerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, digitsTextSize)
            firstDigitSecond.backLowerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, digitsTextSize)
            secondDigitSecond.frontLowerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, digitsTextSize)
            secondDigitSecond.backLowerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, digitsTextSize)
        }
    }

    private fun setHalfDigitHeightAndDigitWidth(halfDigitHeight: Int, digitWidth: Int) {
        binding.apply {
            setHeightAndWidthToView(firstDigitMinute.frontUpper, halfDigitHeight, digitWidth)
            setHeightAndWidthToView(firstDigitMinute.backUpper, halfDigitHeight, digitWidth)
            setHeightAndWidthToView(secondDigitMinute.frontUpper, halfDigitHeight, digitWidth)
            setHeightAndWidthToView(secondDigitMinute.backUpper, halfDigitHeight, digitWidth)

            setHeightAndWidthToView(firstDigitSecond.frontUpper, halfDigitHeight, digitWidth)
            setHeightAndWidthToView(firstDigitSecond.backUpper, halfDigitHeight, digitWidth)
            setHeightAndWidthToView(secondDigitSecond.frontUpper, halfDigitHeight, digitWidth)
            setHeightAndWidthToView(secondDigitSecond.backUpper, halfDigitHeight, digitWidth)

            // Lower
            setHeightAndWidthToView(firstDigitMinute.frontLower, halfDigitHeight, digitWidth)
            setHeightAndWidthToView(firstDigitMinute.backLower, halfDigitHeight, digitWidth)
            setHeightAndWidthToView(secondDigitMinute.frontLower, halfDigitHeight, digitWidth)
            setHeightAndWidthToView(secondDigitMinute.backLower, halfDigitHeight, digitWidth)

            setHeightAndWidthToView(firstDigitSecond.frontLower, halfDigitHeight, digitWidth)
            setHeightAndWidthToView(firstDigitSecond.backLower, halfDigitHeight, digitWidth)
            setHeightAndWidthToView(secondDigitSecond.frontLower, halfDigitHeight, digitWidth)
            setHeightAndWidthToView(secondDigitSecond.backLower, halfDigitHeight, digitWidth)

            // Dividers
            firstDigitMinute.digitDivider.layoutParams.width = digitWidth
            secondDigitMinute.digitDivider.layoutParams.width = digitWidth
            firstDigitSecond.digitDivider.layoutParams.width = digitWidth
            secondDigitSecond.digitDivider.layoutParams.width = digitWidth
        }
    }

    private fun setHeightAndWidthToView(view: View, halfDigitHeight: Int, digitWidth: Int) {
        val firstDigitMinuteFrontUpperLayoutParams = view.layoutParams
        firstDigitMinuteFrontUpperLayoutParams.height = halfDigitHeight
        firstDigitMinuteFrontUpperLayoutParams.width = digitWidth
        binding.firstDigitMinute.frontUpper.layoutParams = firstDigitMinuteFrontUpperLayoutParams
    }

    private fun setAnimationDuration(animationDuration: Int) {
        binding.apply {
            firstDigitMinute.setAnimationDuration(animationDuration.toLong())
            secondDigitMinute.setAnimationDuration(animationDuration.toLong())
            firstDigitSecond.setAnimationDuration(animationDuration.toLong())
            secondDigitSecond.setAnimationDuration(animationDuration.toLong())
        }
    }

    private fun setAlmostFinishedCallbackTimeInSeconds(almostFinishedCallbackTimeInSeconds: Int) {
        this.almostFinishedCallbackTimeInSeconds = almostFinishedCallbackTimeInSeconds
    }

    private fun setTransparentBackgroundColor() {
        binding.apply {
            val transparent = ContextCompat.getColor(context, R.color.fcr_transparent)
            firstDigitMinute.frontLower.setBackgroundColor(transparent)
            firstDigitMinute.backLower.setBackgroundColor(transparent)
            secondDigitMinute.frontLower.setBackgroundColor(transparent)
            secondDigitMinute.backLower.setBackgroundColor(transparent)
            firstDigitSecond.frontLower.setBackgroundColor(transparent)
            firstDigitSecond.backLower.setBackgroundColor(transparent)
            secondDigitSecond.frontLower.setBackgroundColor(transparent)
            secondDigitSecond.backLower.setBackgroundColor(transparent)
        }
    }

    fun setCountdownListener(countdownListener: CountdownCallBack) {
        this.countdownListener = countdownListener
    }

    interface CountdownCallBack {
        fun countdownAboutToFinish()
        fun countdownFinished()
    }
}

class CountDownDigit : FrameLayout {
    private var animationDuration = 100L
    private val binding = FcrCountdownClockDigitBinding.inflate(
        LayoutInflater.from(context),
        this, true
    )

    val frontUpper = binding.frontUpper
    val backUpper = binding.backUpper
    val frontLower = binding.frontLower
    val backLower = binding.backLower
    val digitDivider = binding.digitDivider
    val frontUpperText = binding.frontUpperText
    val backUpperText = binding.backUpperText
    val frontLowerText = binding.frontLowerText
    val backLowerText = binding.backLowerText

    constructor(context: Context?) : super(context!!)
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context!!, attrs, defStyleAttr)

    init {
        //inflate(context, R.layout.fcr_countdown_clock_digit, this)
        binding.frontUpperText.measure(0, 0)
        binding.frontLowerText.measure(0, 0)
        binding.backUpperText.measure(0, 0)
        binding.backLowerText.measure(0, 0)
    }

    fun setNewText(newText: String) {
        binding.frontUpper.clearAnimation()
        binding.frontLower.clearAnimation()

        binding.frontUpperText.text = newText
        binding.frontLowerText.text = newText
        binding.backUpperText.text = newText
        binding.backLowerText.text = newText
    }

    fun animateTextChange(newText: String) {
        if (binding.backUpperText.text == newText) {
            return
        }

        binding.apply{
            frontUpper.clearAnimation()
            frontLower.clearAnimation()

            backUpperText.text = newText
            frontUpper.pivotY = frontUpper.bottom.toFloat()
            frontLower.pivotY = frontUpper.top.toFloat()
            frontUpper.pivotX = (frontUpper.right - ((frontUpper.right - frontUpper.left) / 2)).toFloat()
            frontLower.pivotX = (frontUpper.right - ((frontUpper.right - frontUpper.left) / 2)).toFloat()

            frontUpper.animate()
                .setDuration(getHalfOfAnimationDuration())
                .rotationX(-90f)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction {
                    frontUpperText.text = backUpperText.text
                    frontUpper.rotationX = 0f
                    frontLower.rotationX = 90f
                    frontLowerText.text = backUpperText.text
                    frontLower.animate()
                        .setDuration(getHalfOfAnimationDuration())
                        .rotationX(0f)
                        .setInterpolator(DecelerateInterpolator())
                        .withEndAction {
                            backLowerText.text = frontLowerText.text
                        }.start()
                }.start()
        }
    }

    fun setAnimationDuration(duration: Long) {
        this.animationDuration = duration
    }

    private fun getHalfOfAnimationDuration(): Long {
        return animationDuration / 2
    }
}

internal object NumberParser {
    fun parseStringIntOrZero(obj: Any?): Int {
        var value = 0
        obj?.let {
            try {
                value = obj.toString().toDouble().toInt()
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }
        }
        return value
    }

    fun parseStringLongOrZero(obj: Any?): Long {
        var value: Long = 0
        obj?.let {
            try {
                value = obj.toString().toDouble().toLong()
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }
        }
        return value
    }
}

internal object CountdownStatics {
    const val PROPERTIES_KEY_START_TIME = "startTime"
    const val PROPERTIES_KEY_DURATION = "duration"
    const val PROPERTIES_KEY_PAUSE_TIME = "pauseTime"
    const val PROPERTIES_KEY_STATE = "state"
    const val MAX_DURATION = 3600
    const val DEFAULT_DURATION = 60
}

data class CountdownStatus(
    val state: String,
    val startTime: String?,
    val pauseTime: String?,
    val duration: String?
) {
    fun convert(): MutableMap<String, Any> {
        val json = Gson().toJson(this)
        return Gson().fromJson(json, object : TypeToken<MutableMap<String, Any>>() {}.type)
    }
}

enum class CountdownLaunchStatus(val value: Int) {
    Init(0),
    Started(1);
}


























