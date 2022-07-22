package com.agora.edu.component

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import com.agora.edu.component.chat.AgoraEduEaseChatWidget
import com.agora.edu.component.common.AbsAgoraEduComponent
import com.agora.edu.component.common.IAgoraUIProvider
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.EduContextRoomInfo
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeduuikit.impl.chat.ChatPopupWidget
import io.agora.agoraeduuikit.impl.chat.ChatPopupWidgetListener

/**
 * 聊天组件包装类
 * chat component wrapper class
 */
class AgoraEduChatComponent : AbsAgoraEduComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    var rootContainer: ViewGroup? = null
    var chatWidget: ChatPopupWidget? = null
    var isOnlyChat = false

    var chatListener: ChatPopupWidgetListener? = null

    private val roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)
            // instantiate chat component,must after join success
            val config = eduContext?.widgetContext()?.getWidgetConfig(AgoraWidgetDefaultId.Chat.id)
            config?.let {
                chatWidget = eduContext?.widgetContext()?.create(config) as? ChatPopupWidget
                chatWidget?.let { popup ->
                    uiHandler.post {
                        rootContainer?.let {
                            (popup as? AgoraEduEaseChatWidget)?.setInputViewParent(it)
                            (popup as? AgoraEduEaseChatWidget)?.isNeedRoomMutedStatus = eduContext?.userContext()?.getLocalUserInfo()?.role != AgoraEduContextUserRole.Teacher
                        }
                        popup.init(this@AgoraEduChatComponent)
                        if (isOnlyChat) {
                            popup.setTabDisplayed(false)
                        }
                        eduContext?.userContext()?.getLocalUserInfo()?.let { localUser ->
                            if (localUser.role == AgoraEduContextUserRole.Observer) {
                                popup.setInputViewDisplayed(false)
                            }
                        }
                    }

                    popup.chatWidgetListener = object : ChatPopupWidgetListener {
                        override fun onShowUnread(show: Boolean) {
                            chatListener?.onShowUnread(show)
                        }
                    }
                }
            }
        }
    }

    fun initView(rootContainer: ViewGroup?, agoraUIProvider: IAgoraUIProvider) {
        this.rootContainer = rootContainer
        initView(agoraUIProvider)
    }

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        eduContext?.roomContext()?.addHandler(roomHandler)
    }

    open fun setTabDisplayed(displayed: Boolean) {
        chatWidget?.setTabDisplayed(displayed)
    }

    open fun setInputViewDisplayed(displayed: Boolean) {
        chatWidget?.setInputViewDisplayed(displayed)
    }

    override fun release() {
        super.release()
        chatWidget?.release()
        eduContext?.roomContext()?.removeHandler(roomHandler)
    }
}