package io.agora.online.options

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.animation.doOnEnd
import io.agora.online.component.common.AbsAgoraEduComponent
import io.agora.online.component.common.IAgoraUIProvider
import io.agora.online.helper.FcrHandsUpManager
import io.agora.agoraeducore.core.context.FcrCustomMessage
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.education.impl.network.HttpBaseRes
import io.agora.agoraeducore.core.internal.education.impl.network.HttpCallback
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.online.R
import io.agora.online.databinding.FcrOnlineEduHandsupBinding
import java.util.concurrent.atomic.AtomicBoolean


/**
 * author : wf
 * date : 2022/2/8 11:30 上午
 * description :
 */
class AgoraEduHandsUpComponent : AbsAgoraEduComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private var binding: FcrOnlineEduHandsupBinding =
        FcrOnlineEduHandsupBinding.inflate(LayoutInflater.from(context), this, true)

    var setOnHandsUpListener: ((Boolean) -> Unit)? = null
    var isHandsUp: Boolean = false

    // 错峰，减轻服务器压力
    var heartbeatTime = ((2..4).random()) * 1000L
    var isAnimRunning = AtomicBoolean(false)
    var TAG = "AgoraEduHandsUpComponent"

    val handlerX: Handler by lazy {
        Handler(Looper.getMainLooper())
    }

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)

        eduContext?.roomContext()?.addHandler(roomHandler)

        updateHandsUp(isHandsUp)

        binding.root.setOnClickListener {
            if (isAnimRunning.get()) {
                return@setOnClickListener
            }
            isHandsUp = !isHandsUp
            setOnHandsUpListener?.invoke(isHandsUp)
            requestHandsUp(isHandsUp)
            updateHandsUp(isHandsUp)
        }
    }

    fun cancelHandsUp() {
        handlerX.removeCallbacksAndMessages(null)
        isHandsUp = false
        updateHandsUp(false)
    }

    val roomHandler = object : RoomHandler() {
        override fun onReceiveCustomChannelMessage(customMessage: FcrCustomMessage) {
            val cmdValue = customMessage.payload.data[FcrHandsUpManager.CMD]

            if (cmdValue == FcrHandsUpManager.HANDSUP_CMD) {
                val handsUp = customMessage.payload.data[FcrHandsUpManager.DATA] as? Map<String, Any>
                handsUp?.let {
                    val userUuid = handsUp.get("userUuid") as? String
                    val state = ("" + handsUp.get("state")).toDouble().toInt()
                    userUuid?.let {
                        if (state == 0) { // 0: 取消举手
                            if (userUuid == FcrHandsUpManager.userUuid) {
                                // 会多条消息
                                if (customMessage.fromUser.userUuid != FcrHandsUpManager.userUuid) {
                                    ToastManager.showShort(context, R.string.fcr_room_tips_lower_hand)
                                }

                                handlerX.removeCallbacksAndMessages(null)
                                LogX.e(TAG, "取消举手 removeCallbacksAndMessages")
                                setOnHandsUpListener?.invoke(false)
                            }
                        }
                    }
                }
            }else if (cmdValue == FcrHandsUpManager.HANDSUP_CMD_ALL) {
                val handsUpAll = customMessage.payload.data[FcrHandsUpManager.DATA] as? Map<String, Any>
                handsUpAll?.let {
                    val operation = ("" + handsUpAll.get("operation")).toDouble().toInt()
                    if (operation == 0) { // 取消全体举手
                        ToastManager.showShort(context, R.string.fcr_room_tips_lower_all_hand)
                        requestHandsUp(false)
                        setOnHandsUpListener?.invoke(false)
                        LogX.e(TAG, "取消全体举手 removeCallbacksAndMessages")
                    }
                }
            }
        }
    }

    fun updateHandsUp(isHandsUp: Boolean) {
        if (isHandsUp) {
            binding.optionItemHandsup.setImageResource(R.drawable.agora_handsup_select)
        } else {
            binding.optionItemHandsup.setImageResource(R.drawable.agora_handsup_down_img2)
        }
    }

    fun requestHandsUp(isHandsUp: Boolean) {
        LogX.e(TAG, "requestHandsUp isHandsUp=$isHandsUp")
        handlerX.removeCallbacksAndMessages(null)
        eduContext?.roomContext()?.sendCustomChannelMessage(getHandsUpData(isHandsUp), false,
            object : HttpCallback<HttpBaseRes<Any>>() {
                override fun onSuccess(result: HttpBaseRes<Any>?) {
                    updateHandsUp(isHandsUp)
                }

                override fun onComplete() {
                    if (isHandsUp) {
                        handlerX.removeCallbacksAndMessages(null)
                        handlerX.postDelayed({
                            requestHandsUp(true)
                        }, heartbeatTime)
                    }
                }
            })
    }

    fun getHandsUpData(isHandsUp: Boolean): Map<String, Any> {
        val data = mutableMapOf<String, Any>()
        data.put("userUuid", eduContext?.userContext()?.getLocalUserInfo()?.userUuid ?: "")
        // 0: 取消举手，1：举手
        if (isHandsUp) {
            data.put("state", 1)
        } else {
            data.put("state", 0)
        }

        val map = mutableMapOf<String, Any>()
        map.put("cmd", "handsUp")
        map.put("data", data)

        return map
    }

    fun expandAnim(view: View, isExpand: Boolean, targetWidth: Int, duration: Int) {
        LogX.e("expandAnim >>>>>>> isHandsUp=$isHandsUp isAnimRunning=$isAnimRunning")

        if (isAnimRunning.get()) {
            return
        }

        if (isExpand) {
            FcrHandsUpManager.add(eduContext?.userContext()?.getLocalUserInfo()?.userUuid)
        } else {
            FcrHandsUpManager.remove(eduContext?.userContext()?.getLocalUserInfo()?.userUuid)
        }

        updateHandsUp(isExpand)

        isAnimRunning.set(true)
        isHandsUp = isExpand
        view.visibility = View.VISIBLE
        var start = 0
        var end = targetWidth

        if (!isExpand) {
            start = targetWidth
            end = 0
        }
        val valueAnimator: ValueAnimator = ValueAnimator.ofInt(start, end)
        valueAnimator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
            override fun onAnimationUpdate(animation: ValueAnimator) {
                val value = animation.getAnimatedValue() as Int
                val layoutParams: ViewGroup.LayoutParams = view.getLayoutParams()
                layoutParams.width = value
                view.layoutParams = layoutParams
            }
        })
        valueAnimator.doOnEnd {
            if (!isExpand) {
                val params = view.layoutParams
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT
                view.layoutParams = params
                view.visibility = View.GONE
                view.clearAnimation()
            }
            isAnimRunning.set(false)
            LogX.e("expandAnim end  isAnimRunning=${isAnimRunning.get()}")
        }
        valueAnimator.addListener(object :AnimatorListener{
            override fun onAnimationStart(p0: Animator) {
            }

            override fun onAnimationEnd(p0: Animator) {
                end()
            }

            override fun onAnimationCancel(p0: Animator) {
                end()
            }

            override fun onAnimationRepeat(p0: Animator) {
            }

            fun end(){
                if (!isExpand) {
                    val params = view.layoutParams
                    params.width = ViewGroup.LayoutParams.WRAP_CONTENT
                    view.layoutParams = params
                    view.visibility = View.GONE
                    view.clearAnimation()
                }
                isAnimRunning.set(false)
                LogX.e("expandAnim end  isAnimRunning=${isAnimRunning.get()}")
            }
        })

        val objectAnimator: ObjectAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(valueAnimator, objectAnimator)
        animatorSet.duration = duration.toLong()
        animatorSet.start()
    }

    override fun release() {
        cancelHandsUp()
        eduContext?.roomContext()?.removeHandler(roomHandler)
    }
}






















