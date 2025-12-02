package com.agora.edu.component.animator

import android.animation.Animator
import android.animation.ValueAnimator

/**
 * author : cjw
 * date : 2022/4/5
 * description :
 */
abstract class FCRAnimatorListener : Animator.AnimatorListener {
    override fun onAnimationStart(animation: Animator, isReverse: Boolean) {
        super.onAnimationStart(animation, isReverse)
    }

    override fun onAnimationStart(animation: Animator) {

    }

    override fun onAnimationEnd(animation: Animator, isReverse: Boolean) {
        super.onAnimationEnd(animation, isReverse)
    }

    override fun onAnimationEnd(animation: Animator) {
        
    }

    override fun onAnimationCancel(animation: Animator) {
        
    }

    override fun onAnimationRepeat(animation: Animator) {
        
    }

    open fun onAnimationUpdate(fraction: Float) {
    }
}