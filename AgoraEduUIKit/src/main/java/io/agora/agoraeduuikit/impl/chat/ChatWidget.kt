package io.agora.agoraeduuikit.impl.chat

import android.graphics.Rect
import android.view.ViewGroup
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget

abstract class ChatWidget : AgoraBaseWidget() {
    protected val userIdKey = "userId"
    protected val appNameKey = "appName"
    protected val chatRoomIdKey = "chatRoomId"

    var hideIconSize = 0

    var chatWidgetAnimateListener: OnChatWidgetAnimateListener? = null

    abstract fun setFullscreenRect(fullScreen: Boolean, rect: Rect)

    abstract fun setFullDisplayRect(rect: Rect)

    abstract fun show(show: Boolean)

    abstract fun isShowing(): Boolean

    open fun showShadow(show: Boolean) {

    }

    abstract fun setClosable(closable: Boolean)

    abstract fun setRect(rect: Rect)
}

interface OnChatWidgetAnimateListener {
    fun onChatWidgetAnimate(enlarge: Boolean, fraction: Float, left: Int, top: Int, width: Int, height: Int)
}

abstract class ChatPopupWidget : ChatWidget() {
    var chatWidgetListener: ChatPopupWidgetListener? = null

    abstract fun setTabDisplayed(displayed: Boolean)

    open fun getLayout(): ViewGroup? {
        return null
    }

    open fun setChatMuted(muted: Boolean) {

    }
}

interface ChatPopupWidgetListener {
    fun onChatPopupWidgetClosed()
}