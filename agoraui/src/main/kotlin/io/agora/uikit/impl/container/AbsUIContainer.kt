package io.agora.uikit.impl.container

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import io.agora.educontext.*
import io.agora.uikit.R
import io.agora.uikit.component.toast.AgoraUIToastManager
import io.agora.uikit.educontext.handlers.*
import io.agora.uikit.impl.chat.AgoraUIChatWindow
import io.agora.uikit.impl.chat.EaseChatWidget
import io.agora.uikit.impl.chat.tabs.ChatTabConfig
import io.agora.uikit.impl.handsup.AgoraUIHandsUp
import io.agora.uikit.impl.loading.AgoraUILoading
import io.agora.uikit.impl.room.AgoraUIRoomStatus
import io.agora.uikit.impl.screenshare.AgoraUIFullScreenBtn
import io.agora.uikit.impl.screenshare.AgoraUIScreenShare
import io.agora.uikit.impl.tool.AgoraUIToolBar
import io.agora.uikit.impl.users.AgoraUIReward
import io.agora.uikit.impl.users.AgoraUIRoster
import io.agora.uikit.impl.users.AgoraUserListVideoLayout
import io.agora.uikit.impl.video.AgoraUIVideoGroup
import io.agora.uikit.impl.whiteboard.AgoraUIWhiteBoard
import io.agora.uikit.impl.whiteboard.paging.AgoraUIPagingControl
import io.agora.uikit.interfaces.protocols.*

abstract class AbsUIContainer(
        private val eduContext: EduContextPool?,
        protected val config: AgoraContainerConfig) : IAgoraUIContainer {

    private val tag = "AbsUIContainer"

    /**
     * Container needs to receive and display room errors and tips
     */
    private val containerRoomEventHandler = object : RoomHandler() {
        override fun onConnectionStateChanged(state: EduContextConnectionState) {
            super.onConnectionStateChanged(state)
            agoraUILoading?.setVisibility(if (state == EduContextConnectionState.Connected) GONE else VISIBLE)
            if (state == EduContextConnectionState.Connecting) {
                agoraUILoading?.setContent(true)
            } else if (state == EduContextConnectionState.Reconnecting) {
                agoraUILoading?.setContent(false)
            }
        }

        override fun onClassTip(tip: String) {
            AgoraUIToastManager.showShort(tip)
        }

        override fun onError(error: EduContextError) {
            showError(error)
        }
    }

    private val containerDeviceEventHandler = object : DeViceHandler() {
        override fun onDeviceTips(tips: String) {
            super.onDeviceTips(tips)
            AgoraUIToastManager.showShort(tips)
        }
    }

    /**
     * Container needs to receive and display chat tips
     */
    private val containerChatEventHandler = object : ChatHandler() {
        override fun onChatTips(tip: String) {
            AgoraUIToastManager.showShort(tip)
        }

        override fun onChatAllowed(allowed: Boolean, userInfo: EduContextUserInfo,
                                   operator: EduContextUserInfo?, local: Boolean) {
            if (operator == null) {
                return
            }

            layout()?.let {
                val tip: String = if (allowed && local) {
                    val format = it.context.resources.getString(R.string.agora_message_chat_student_allow_local)
                    String.format(format, operator.userName)
                } else if (allowed && !local) {
                    val format = it.context.resources.getString(R.string.agora_message_chat_student_allow_remote)
                    String.format(format, userInfo.userName, operator.userName)
                } else if (!allowed && local) {
                    val format = it.context.resources.getString(R.string.agora_message_chat_student_ban_local)
                    String.format(format, operator.userName)
                } else {
                    val format = it.context.resources.getString(R.string.agora_message_chat_student_ban_remote)
                    String.format(format, userInfo.userName, operator.userName)
                }
                AgoraUIToastManager.showShort(tip)
            }
        }
    }

    /**
     * Container needs to receive permission and full screen events.
     */
    private val containerWhiteboardHandler = object : WhiteboardHandler() {
        override fun getBoardContainer(): ViewGroup? {
            return whiteboardWindow?.getWhiteboardContainer()
        }

        override fun onFullScreenChanged(isFullScreen: Boolean) {
            super.onFullScreenChanged(isFullScreen)
            setFullScreen(isFullScreen)
        }

        override fun onPermissionGranted(granted: Boolean) {
            super.onPermissionGranted(granted)
            AgoraUIToastManager.whiteBoardPermissionTips(granted)
        }
    }

    private val containerScreenShareHandler = object : ScreenShareHandler() {
        override fun onSelectScreenShare(select: Boolean) {
            if (select) {
                fullScreenBtn?.setVisibility(View.VISIBLE)
                pageControlWindow?.setVisibility(View.GONE)
            } else {
                fullScreenBtn?.setVisibility(View.GONE)
                pageControlWindow?.setVisibility(View.VISIBLE)
            }
        }

        override fun onScreenShareTip(tips: String) {
            super.onScreenShareTip(tips)
            AgoraUIToastManager.showShort(tips)
        }
    }

    private val containerHandsUpHandler = object : HandsUpHandler() {
        override fun onHandsUpTips(tips: String) {
            super.onHandsUpTips(tips)
            AgoraUIToastManager.showShort(tips)
        }
    }

    private val containerVideoHandler = object : VideoHandler() {
        override fun onMessageUpdated(msg: String) {

        }
    }

    private val containerUserHandler = object : UserHandler() {
        override fun onUserReward(userInfo: EduContextUserInfo) {
            super.onUserReward(userInfo)
            if (rewardWindow?.isShowing() == false) {
                rewardWindow?.show()
            }
        }
    }

    protected var roomStatus: AgoraUIRoomStatus? = null
    protected var toolbar: AgoraUIToolBar? = null
    protected var chatWindow: AgoraUIChatWindow? = null
    protected var whiteboardWindow: AgoraUIWhiteBoard? = null
    protected var pageControlWindow: AgoraUIPagingControl? = null
    protected var fullScreenBtn: AgoraUIFullScreenBtn? = null
    protected var videoGroupWindow: AgoraUIVideoGroup? = null
    protected var screenShareWindow: AgoraUIScreenShare? = null
    protected var handsUpWindow: AgoraUIHandsUp? = null
    protected var studentVideoGroup: AgoraUserListVideoLayout? = null
    protected var roster: AgoraUIRoster? = null
    protected var rewardWindow: AgoraUIReward? = null
    protected var agoraUILoading: AgoraUILoading? = null

    // Hyphenate chat im, a separate chat widget, different
    // from agora chat window, with its own identifier.
    // Containers have both agora and hyphenate chat widgets,
    // and they are independent of each other
    protected var easeChat: EaseChatWidget? = null

    private var context: Context? = null
    private var layout: ViewGroup? = null

    init {
        eduContext?.roomContext()?.addHandler(containerRoomEventHandler)
        eduContext?.deviceContext()?.addHandler(containerDeviceEventHandler)
        eduContext?.chatContext()?.addHandler(containerChatEventHandler)
        eduContext?.whiteboardContext()?.addHandler(containerWhiteboardHandler)
        eduContext?.screenShareContext()?.addHandler(containerScreenShareHandler)
        eduContext?.handsUpContext()?.addHandler(containerHandsUpHandler)
        eduContext?.videoContext()?.addHandler(containerVideoHandler)
        eduContext?.userContext()?.addHandler(containerUserHandler)
    }

    protected fun getEduContext(): EduContextPool? {
        return eduContext
    }

    override fun showLeave() {
        roomStatus?.showLeaveDialog()
    }

    override fun kickOut() {
        roomStatus?.kickOut()
    }

    override fun showError(error: EduContextError) {
        AgoraUIToastManager.showLong(error.msg)
    }

    override fun showTips(msg: String) {
        AgoraUIToastManager.showLong(msg)
    }

    internal fun hideSoftInput(context: Context, view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    override fun init(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int) {
        val screenLayout = layout.context.resources.configuration.screenLayout
        AgoraUIConfig.isLargeScreen =
                screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >=
                        Configuration.SCREENLAYOUT_SIZE_LARGE
        rewardWindow = AgoraUIReward(layout.context, layout, 0, 0, width, height)
        context = layout.context
        this.layout = layout
    }

    override fun resize(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int) {
        val screenLayout = layout.context.resources.configuration.screenLayout
        AgoraUIConfig.isLargeScreen =
                screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >=
                        Configuration.SCREENLAYOUT_SIZE_LARGE
        rewardWindow?.let {
            val rect = Rect(0, 0, width, height)
            it.setRect(rect)
        }
    }


    protected abstract fun setFullScreen(fullScreen: Boolean)

    protected abstract fun calculateVideoSize()

    protected fun getContext(): Context? {
        return layout()?.context
    }

    override fun layout(): ViewGroup? {
        return this.layout
    }
}

enum class AgoraContainerType {
    OneToOne, SmallClass, LargeClass, Debug
}

data class AgoraContainerConfig(
        val chatTabConfigs: List<ChatTabConfig>
)

object AgoraUIConfig {
    const val videoWidthMaxRatio = 0.312f
    const val videoRatio = 9 / 16f
    const val clickInterval = 500L
    var isLargeScreen: Boolean = false
    const val videoPlaceHolderImgSizePercent = 0.5f
    const val videoOptionIconSizePercent = 0.14f
    const val videoOptionIconSizeMax = 54
    const val videoOptionIconSizeMaxWithLargeScreen = 36
    const val chatHeightLargeScreenRatio = 0.7f

    object SmallClass {
        const val studentVideoHeightRationSmallScreen = 0.24f
        var teacherVideoWidth = 600
        var teacherVideoHeight = 336
        var studentVideoHeightLargeScreen = 336
        var studentVideoHeightSmallScreen = 261
    }

    object OneToOneClass {
        var teacherVideoWidth = 600
    }
}