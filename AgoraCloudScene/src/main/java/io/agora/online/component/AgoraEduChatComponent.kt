package io.agora.online.component

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.agora.online.component.chat.AgoraEduEaseChatWidget
import io.agora.online.component.common.AbsAgoraEduComponent
import io.agora.online.component.common.IAgoraUIProvider
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.EduContextRoomInfo
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.online.impl.chat.ChatPopupWidget
import io.agora.online.impl.chat.ChatPopupWidgetListener

/**
 * 聊天组件包装类
 * chat component wrapper class
 */
class AgoraEduChatComponent : AbsAgoraEduComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private val TAG = "AgoraEduChatComponent"
    var rootContainer: ViewGroup? = null
    var chatWidget: ChatPopupWidget? = null
    var isOnlyChat = false // 只有聊天，没有公告
    var chatListener: ChatPopupWidgetListener? = null

    private val roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)
            // instantiate chat component,must after join success
            val config = eduContext?.widgetContext()?.getWidgetConfig(AgoraWidgetDefaultId.Chat.id)
            config?.let {
                chatWidget = eduContext?.widgetContext()?.create(config) as? ChatPopupWidget
                chatWidget?.let { popup ->
                    ContextCompat.getMainExecutor(context).execute {
                        rootContainer?.let {
                            (popup as? AgoraEduEaseChatWidget)?.setInputViewParent(it)
                            (popup as? AgoraEduEaseChatWidget)?.isNeedRoomMutedStatus =
                                eduContext?.userContext()?.getLocalUserInfo()?.role != AgoraEduContextUserRole.Teacher
                        }
                        popup.token = eduCore?.config?.rtmToken
                        popup.init(this@AgoraEduChatComponent)
//                        popup.setRoomType(roomInfo.roomType.value)
                        if (roomInfo.roomType == RoomType.ONE_ON_ONE) {
                            popup.setMuteViewDisplayed(false)
                            popup.setTabDisplayed(false)
                        }
                        if (roomInfo.roomType == RoomType.ONE_ON_ONE || roomInfo.roomType == RoomType.SMALL_CLASS || roomInfo.roomType == RoomType.GROUPING_CLASS) {
                            popup.setChatLayoutBackground(io.agora.online.R.drawable.fcr_popup_bg)
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
        LogX.d(TAG, "release AgoraEduChatComponent")
        super.release()
        chatWidget?.release()
        eduContext?.roomContext()?.removeHandler(roomHandler)
    }
}