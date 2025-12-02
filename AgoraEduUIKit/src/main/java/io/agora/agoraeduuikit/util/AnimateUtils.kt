package io.agora.agoraeduuikit.util

import android.view.View

class PopupAnimationUtil {
    private val duration = 100L
    private val alphaBeforeShow = 0f
    private val alphaAfterShow = 1f
    private val alphaBeforeDismiss = 1f
    private val alphaAfterDismiss = 0f
    private val scaleBeforeShow = 0.8f
    private val scaleAfterShow = 1f
    private val scaleBeforeDismiss = 1f
    private val scaleAfterDismiss = 0.8f

    fun runShowAnimation(view: View, pivotX: Float, pivotY: Float,
                         endListener: Runnable? = null) {
        view.animate().cancel()
        view.alpha = alphaBeforeShow
        view.scaleX = scaleBeforeShow
        view.scaleY = scaleBeforeShow
        view.pivotX = pivotX
        view.pivotY = pivotY

        view.animate()
            .scaleX(scaleAfterShow)
            .scaleY(scaleAfterShow)
            .alpha(alphaAfterShow)
            .withEndAction {
                endListener?.run()
            }
            .duration = duration
    }

    fun runDismissAnimation(view: View, pivotX: Float, pivotY: Float,
                            endListener: Runnable? = null) {
        view.animate().cancel()
        view.alpha = alphaBeforeDismiss
        view.scaleX = scaleBeforeDismiss
        view.scaleY = scaleBeforeDismiss
        view.pivotX = pivotX
        view.pivotY = pivotY

        view.animate()
            .scaleX(scaleAfterDismiss)
            .scaleY(scaleAfterDismiss)
            .alpha(alphaAfterDismiss)
            .withEndAction {
                endListener?.run()
            }
            .duration = duration
    }
}