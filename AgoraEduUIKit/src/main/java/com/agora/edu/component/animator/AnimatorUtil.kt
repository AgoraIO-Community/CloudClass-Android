package com.agora.edu.component.animator

import android.animation.*
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * author : cjw
 * date : 2022/4/5
 * description : 动画工具类
 * animator util
 */
class AnimatorUtil {
    private val tag = "AnimatorUtil"

    /**
     * 位移缩放动画，并在动画运行过程中，通过回调把当前估值回调出去
     */
    fun translateScale2(
        view: View, startX: Float, endX: Float?, startY: Float, endY: Float?, originalWidth: Float,
        destWidth: Float?, originalHeight: Float, destHeight: Float?, listener: FCRAnimatorListener? = null
    ) {
        val valueAnimator: ValueAnimator
        val start: Float
        val end: Float
        if (endX != null && startX != endX) {
            start = startX
            end = endX
        } else if (endY != null && startY != endY) {
            start = startY
            end = endY
        } else if (destWidth != null && originalWidth != destWidth) {
            start = originalWidth
            end = destWidth
        } else if (destHeight != null && originalHeight != destHeight) {
            start = originalHeight
            end = destHeight
        } else {
            listener?.onAnimationEnd(null)
            return
        }
        valueAnimator = ValueAnimator.ofFloat(start, end)
        valueAnimator.duration = 250 // 0.25s是苹果动画标准时长
        valueAnimator.interpolator = LinearInterpolator()
        valueAnimator.addUpdateListener { animation ->
            animation?.let {
                // 回调当前值
                listener?.onAnimationUpdate(it.animatedFraction)
            }
        }
        valueAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?, isReverse: Boolean) {
                super.onAnimationStart(animation, isReverse)
                listener?.onAnimationStart(animation, isReverse)
            }

            override fun onAnimationStart(animation: Animator?) {
                listener?.onAnimationStart(animation)
            }

            override fun onAnimationEnd(animation: Animator?, isReverse: Boolean) {
                super.onAnimationEnd(animation, isReverse)
                listener?.onAnimationEnd(animation, isReverse)
                view.clearAnimation()
            }

            override fun onAnimationEnd(animation: Animator?) {
                listener?.onAnimationEnd(animation)
                view.clearAnimation()
            }

            override fun onAnimationCancel(animation: Animator?) {
                listener?.onAnimationCancel(animation)
                view.clearAnimation()
            }

            override fun onAnimationRepeat(animation: Animator?) {
                listener?.onAnimationRepeat(animation)
            }
        })
        valueAnimator.start()
    }
}