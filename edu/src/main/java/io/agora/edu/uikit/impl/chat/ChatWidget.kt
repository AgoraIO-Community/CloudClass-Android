package io.agora.edu.uikit.impl.chat

import android.graphics.Rect
import io.agora.edu.uikit.impl.AgoraAbsWidget

abstract class ChatWidget : AgoraAbsWidget(){
    var hideIconSize = 0

    var chatWidgetAnimateListener: OnChatWidgetAnimateListener? = null

    abstract fun setFullscreenRect(fullScreen: Boolean, rect: Rect)

    abstract fun setFullDisplayRect(rect: Rect)

    abstract fun show(show: Boolean)

    abstract fun isShowing(): Boolean

    open fun showShadow(show: Boolean) {
    }

    abstract fun setClosable(closable: Boolean)
}

interface OnChatWidgetAnimateListener {
    fun onChatWidgetAnimate(enlarge: Boolean, fraction: Float, left: Int, top: Int, width: Int, height: Int)
}