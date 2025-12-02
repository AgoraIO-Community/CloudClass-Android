package io.agora.agoraeduuikit.impl.chat

import android.graphics.Rect
import android.view.ViewGroup
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget

abstract class ChatWidget : AgoraBaseWidget() {
    protected val userIdKey = "userId"
    protected val appNameKey = "appName"
    protected val chatRoomIdKey = "chatRoomId"

    var hideIconSize = 0

    abstract fun setFullscreenRect(fullScreen: Boolean, rect: Rect)

    abstract fun setFullDisplayRect(rect: Rect)

    abstract fun show(show: Boolean)

    abstract fun isShowing(): Boolean

    open fun showShadow(show: Boolean) {

    }

    abstract fun setClosable(closable: Boolean)

    abstract fun setBackground(back: Int)

}

abstract class ChatPopupWidget : ChatWidget() {
    var chatWidgetListener: ChatPopupWidgetListener? = null
    var token: String? = null

    abstract fun setTabDisplayed(displayed: Boolean)
//    abstract fun setRoomType(roomType: Int)
    abstract fun setMuteViewDisplayed(displayed: Boolean)
    abstract fun setChatLayoutBackground(displayed: Int)
    abstract fun setInputViewDisplayed(displayed: Boolean)

    open fun getLayout(): ViewGroup? {
        return null
    }

    open fun setChatMuted(muted: Boolean) {

    }
}

interface ChatPopupWidgetListener {
    fun onShowUnread(show: Boolean)
}