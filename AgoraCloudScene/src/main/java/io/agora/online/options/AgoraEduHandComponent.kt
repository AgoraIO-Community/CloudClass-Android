package io.agora.online.options

import android.annotation.SuppressLint
import android.content.Context
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import io.agora.online.component.common.AbsAgoraEduComponent
import io.agora.online.component.common.IAgoraUIProvider
import io.agora.agoraeducore.core.context.AgoraEduContextUserInfo
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.EduContextUserLeftReason
import io.agora.agoraeducore.core.internal.framework.impl.handler.UserHandler
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.online.R
import io.agora.online.databinding.FcrOnlineEduHandComponentBinding
import io.agora.online.provider.AgoraUIUserDetailInfo
import io.agora.online.provider.UIDataProviderListenerImpl
import java.lang.Thread.sleep
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * author : wf
 * date : 2022/2/8 11:30 上午
 * description :
 */
class AgoraEduHandComponent : AbsAgoraEduComponent, AgoraUIHandsWaveCountDownListener {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)


    private var countdownMax = 3
    private var waveHandsTimeoutInSeconds = 3

    private var binding: FcrOnlineEduHandComponentBinding = FcrOnlineEduHandComponentBinding.inflate(LayoutInflater.from(context), this, true)
    private var rlHandsupIcon = binding.rlOptionItemHandsup
    private var handsupIcon = binding.optionItemHandsup
    private val countdownText = binding.tvCountDownText
    private var popupView: AgoraEduHandUpToastPopupComponent? = null
    private var wavingCountText2 = binding.wavingCountText

    private val tag = "AgoraUIHandsUpWrapper"

    private var handsWaveTimeout = 3200L
    private val timerTick = 1000L

    private var isHandsUpButtonHolding = AtomicBoolean(false)

    private val scaleFactor = 1.1f
    private var handsUpListPopUp: AgoraEduHandsUpListPopupComponent? = null
    private var stuList: MutableList<AgoraUIUserDetailInfo> = mutableListOf()//所有学生列表
    private var wavingList: MutableList<HandsUpUser> = mutableListOf()//正在举手的user列表
    private var wavingListOpened = false //举手列表是否打开
    var timecount = 0

    private var mThreadExecutor: ExecutorService? = Executors.newSingleThreadExecutor()

    private val uiDataProviderListener = object : UIDataProviderListenerImpl() {
        override fun onUserListChanged(mUserList: List<AgoraUIUserDetailInfo>) {
            super.onUserListChanged(mUserList)
            stuList = mUserList as MutableList<AgoraUIUserDetailInfo>
        }
    }

    private val userHandler = object : UserHandler() {
        override fun onRemoteUserJoined(user: AgoraEduContextUserInfo) {

        }

        override fun onRemoteUserLeft(
            user: AgoraEduContextUserInfo,
            operator: AgoraEduContextUserInfo?,
            reason: EduContextUserLeftReason
        ) {

        }

        override fun onUserHandsWave(
            userUuid: String,//举手的用户uuid
            duration: Int,
            payload: Map<String, Any>?
        ) {
            super.onUserHandsWave(userUuid, duration, payload)
            if (eduContext?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Teacher) {//老师端 当有人举手时，举手控件闪烁
                var isCoHost = false //当前user的上台状态
                if (eduContext?.userContext()?.getCoHostList()?.find { it.userUuid == userUuid } != null) {
                    isCoHost = true
                }
                var hansUpUser = HandsUpUser(userUuid, payload?.get("userName") as String, isCoHost)
                if (!wavingList.contains(hansUpUser)) {
                    wavingList.add(hansUpUser)
                }
                onUserListUpdated(wavingList)
                //老师端图标闪烁
                wavingBtnUpdated()
            }
        }

        override fun onUserHandsDown(userUuid: String, payload: Map<String, Any>?) {
            super.onUserHandsDown(userUuid, payload)
            if (eduContext?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Teacher) {//老师端 当有人取消举手时，举手控件停止闪烁
                userUuid.let {
                    var tempUser = wavingList.find { userUuid == it.userUuid }
                    if (tempUser != null) {
                        wavingList.remove(tempUser)
                    }
                }
                onUserListUpdated(wavingList)
                if (wavingList.size == 0) {
                    handsUpListPopUp?.agoraHandsupListPopup?.visibility = GONE
                    wavingListOpened = false
                }
                wavingBtnUpdated()
            }
        }
    }

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        eduContext?.userContext()?.addHandler(userHandler)
        agoraUIProvider.getUIDataProvider()?.addListener(uiDataProviderListener)
        if (eduContext?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Teacher) {
            //如果是老师
            handsupIcon.setImageDrawable(resources.getDrawable(R.drawable.agora_handsup_teacher_normal))
            handsUpListPopUp = AgoraEduHandsUpListPopupComponent(context)
            handsUpListPopUp?.initView(agoraUIProvider)
            handsupIcon.setOnClickListener {
                if (!wavingListOpened) {
                    showWavingListPopUp()
                } else {
                    handsUpListPopUp?.dismiss()
                    wavingListOpened = false
                }
            }
        } else {
            //如果是学生
            handsupIcon.setOnTouchListener(handsWaveTouchListener)
        }
    }

    fun onUserListUpdated(list: MutableList<HandsUpUser>) {
        post {
            handsUpListPopUp?.agoraHandsupListPopup?.onUserListUpdated(list)
        }
    }

    fun setHandsupTimeout(seconds: Int) {
        countdownMax = seconds
        waveHandsTimeoutInSeconds = seconds
        handsWaveTimeout = seconds * 1000 + 200L

        touchDownTimer.cancel()
        touchCancelTimer.cancel()

        touchDownTimer = HandsWaveTimer(handsWaveTimeout, timerTick,
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

        touchCancelTimer = HandsWaveTimer(handsWaveTimeout, timerTick,
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
        if (eduContext?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Student) {
            handsupIcon.setOnTouchListener(handsWaveTouchListener)
        }
    }

    private val mHandler = object : Handler(Looper.getMainLooper()) { //处理老师端，控制图标闪烁
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                1 -> {
                    showWavingCountText()//红点显示数量
                    if (timecount == 0) {
                        handsupIcon.setImageDrawable(context.getDrawable(R.drawable.agora_handsup_teacher_normal))
                        handsupIcon.isActivated = false
                    } else {
                        handsupIcon.setImageDrawable(context.getDrawable(R.drawable.agora_handsup_teacher_enable))
                        handsupIcon.isActivated = true

                    }
                }
                2 -> {
                    handsupIcon.setImageDrawable(context.getDrawable(R.drawable.agora_handsup_teacher_normal))
                    handsupIcon.isActivated = false
                    handsUpListPopUp?.dismiss()
                    wavingCountText2.visibility = GONE
                    wavingListOpened = false
                }
            }
        }
    }

    private fun wavingBtnUpdated() {
        // get the users waving
        mThreadExecutor?.execute {
            while (wavingList.size != 0) {
                var msg = Message()
                msg.what = 1
                timecount++
                timecount %= 2
                mHandler.sendMessage(msg)
                try {
                    sleep(500)
                } catch (e: Exception) {
                }
            }
            var msg2 = Message()
            msg2.what = 2
            mHandler.sendMessage(msg2)
        }
    }

    private fun showWavingCountText() {
        wavingCountText2.visibility = VISIBLE
        wavingCountText2.text = wavingList.size.toString()
    }

    private var touchDownTimer: HandsWaveTimer = HandsWaveTimer(handsWaveTimeout, timerTick,
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

    private var touchCancelTimer = HandsWaveTimer(handsWaveTimeout, timerTick,
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

    private fun handleTimerStart(timeout: Int) {//学生开始举手，开始计时
        onCountDownStart(timeout)
    }

    private fun handleTimerTick(remainSeconds: Int) {//学生 举手倒计时3秒
        onCountDownTick(remainSeconds)
    }

    private fun handleTouchDownTimerStop() {
        if (isHandsUpButtonHolding.get()) {
            touchDownTimer.startTimer()
        }
    }

    private fun handleTouchCancelTimerStop() {//学生停止举手后UI更新
        onCountDownEnd()
    }

    @SuppressLint("ClickableViewAccessibility")
    private val handsWaveTouchListener = View.OnTouchListener { _, event ->

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isHandsUpButtonHolding.get()) {
                    LogX.i(
                        "$tag->timer has already been " +
                            "holding, ignore this touch event"
                    )
                    return@OnTouchListener true
                }

                isHandsUpButtonHolding.set(true)
                showHandsUpToastPopUp()
                anchorScale(true)
                touchDownTimer.cancel()
                touchCancelTimer.cancel()
                touchDownTimer.startTimer()
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (!isHandsUpButtonHolding.get()) {
                    LogX.i(
                        "$tag->no timer is holding " +
                            "anymore, ignore this touch event"
                    )
                    return@OnTouchListener true
                }
                isHandsUpButtonHolding.set(false)
                anchorScale(false)
                touchDownTimer.cancel()
                touchCancelTimer.startTimer()
            }
        }
        true
    }

    private fun anchorScale(scaled: Boolean) {
        if (scaled) {
            handsupIcon.scaleX = scaleFactor
            handsupIcon.scaleY = scaleFactor
        } else {
            handsupIcon.scaleX = 1f
            handsupIcon.scaleY = 1f
        }
    }

    private fun showHandsUpToastPopUp() {
        if (popupView == null) {
            popupView = AgoraEduHandUpToastPopupComponent(context)
            popupView?.initView(agoraUIProvider)
            popupView!!.popupContainerView?.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val xOffset: Int = -(popupView!!.popupContainerView?.measuredWidth ?: 0)
            val yOffset: Int = -(popupView!!.popupContainerView?.measuredHeight ?: 0)
            // 右边对齐
//        popupView!!.popupWindow?.showAsDropDown(
//            binding.optionItemHandsup,
//            xOffset,
//            0,
//            Gravity.BOTTOM or Gravity.RIGHT
//        )

            // 左边顶部对齐
            popupView!!.popupWindow?.showAsDropDown(
                binding.optionItemHandsup,
                xOffset,
                -binding.optionItemHandsup.height,
                Gravity.TOP or Gravity.LEFT
            )
        }
    }

    private fun showWavingListPopUp() {
        if (wavingList.size != 0) {
            handsUpListPopUp?.popupContainerView?.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val xOffset: Int = -(handsUpListPopUp?.popupContainerView?.measuredWidth ?: 0)
            val yOffset: Int = -(handsUpListPopUp?.popupContainerView?.measuredHeight ?: 0)
            // 左边顶部对齐
            handsUpListPopUp?.agoraHandsupListPopup?.visibility = VISIBLE
            handsUpListPopUp?.popupContainerView?.visibility = VISIBLE
            handsUpListPopUp?.popupWindow?.showAsDropDown(binding.optionItemHandsup, xOffset, yOffset, Gravity.LEFT)
            wavingListOpened = true
        }
    }

    private fun getSeconds(milliSec: Int): Int {
        return (milliSec / 1000f).toInt()
    }

    private inner class HandsWaveTimer(
        private val timeout: Long, tickInterval: Long,
        private val start: ((Int) -> Unit)? = null,
        private val tick: ((Int, Int) -> Unit)? = null,
        private val end: (() -> Unit)? = null
    ) : CountDownTimer(timeout, tickInterval) {

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

    override fun onCountDownStart(timeoutInSeconds: Int) {
        LogX.d(tag, "onCountDownStart, timeout $timeoutInSeconds seconds")
        post {
            handsupIcon.visibility = GONE
            countdownText.visibility = android.view.View.VISIBLE
            if (timeoutInSeconds in 1..countdownMax) {
                countdownText.text = timeoutInSeconds.toString()
            }
        }

        eduContext?.userContext()?.let { context ->
            val localName = context.getLocalUserInfo().userName
            val payload = mutableMapOf<String, Any>()
            payload["userName"] = localName
            context.handsWave(waveHandsTimeoutInSeconds, payload)
        }
    }

    override fun onCountDownTick(secondsToFinish: Int) {
        LogX.d(tag, "onCountDownTick, $secondsToFinish seconds remaining")
        post {
            if (secondsToFinish in 1..countdownMax) {
                countdownText.text = secondsToFinish.toString()
            }
        }
    }

    override fun onCountDownEnd() {
        LogX.d(tag, "onCountDownEnd")
        post {
            handsupIcon.visibility = VISIBLE
            countdownText.visibility = android.view.View.GONE
        }
    }
}






















