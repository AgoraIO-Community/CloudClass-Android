package io.agora.edu.uikit.impl.container

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import io.agora.edu.R
import io.agora.edu.core.context.EduContextConnectionState
import io.agora.edu.core.context.EduContextError
import io.agora.edu.core.context.EduContextPool
import io.agora.edu.core.context.EduContextUserInfo
import io.agora.edu.extensions.widgets.UiWidgetManager
import io.agora.edu.uikit.handlers.*
import io.agora.edu.uikit.component.toast.AgoraUIToastManager
import io.agora.edu.uikit.impl.chat.AgoraUIChatWidget
import io.agora.edu.uikit.impl.chat.ChatWidget
import io.agora.edu.uikit.impl.chat.tabs.ChatTabConfig
import io.agora.edu.uikit.impl.handsup.AgoraUIHandsUp
import io.agora.edu.uikit.impl.loading.AgoraUILoading
import io.agora.edu.uikit.impl.room.AgoraUIRoomStatus
import io.agora.edu.uikit.impl.screenshare.AgoraUIFullScreenBtn
import io.agora.edu.uikit.impl.screenshare.AgoraUIScreenShare
import io.agora.edu.uikit.impl.tool.AgoraUIToolBar
import io.agora.edu.uikit.impl.users.AgoraUIReward
import io.agora.edu.uikit.impl.users.AgoraUIRoster
import io.agora.edu.uikit.impl.users.AgoraUserListVideoLayout
import io.agora.edu.uikit.impl.video.AgoraUIVideoGroup
import io.agora.edu.uikit.impl.whiteboard.AgoraUIWhiteBoard
import io.agora.edu.uikit.impl.whiteboard.paging.AgoraUIPagingControl
import io.agora.edu.uikit.interfaces.protocols.IAgoraUIContainer

abstract class AbsUIContainer(
        private val eduContext: EduContextPool?,
        protected val config: AgoraContainerConfig) : IAgoraUIContainer {
    private val tag = "AbsUIContainer"

    /**
     * Container needs to receive and display room errors and tips
     */
    private val containerRoomEventHandler = object : RoomHandler() {
        override fun onConnectionStateChanged(state: EduContextConnectionState) {
            Log.d(tag, "container room connection state changed ${state.name}")
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

        override fun onChatAllowed(allowed: Boolean,
                                   userInfo: EduContextUserInfo,
                                   operator: EduContextUserInfo?,
                                   local: Boolean) {
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
                fullScreenBtn?.setVisibility(VISIBLE)
                pageControlWindow?.setVisibility(GONE)
            } else {
                fullScreenBtn?.setVisibility(GONE)
                pageControlWindow?.setVisibility(VISIBLE)
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
    protected var whiteboardWindow: AgoraUIWhiteBoard? = null
    protected var pageControlWindow: AgoraUIPagingControl? = null
    protected var fullScreenBtn: AgoraUIFullScreenBtn? = null
    protected var teacherVideoWindow: AgoraUIVideoGroup? = null
    protected var screenShareWindow: AgoraUIScreenShare? = null
    protected var handsUpWindow: AgoraUIHandsUp? = null
    protected var studentsVideoWindow: AgoraUserListVideoLayout? = null
    protected var roster: AgoraUIRoster? = null
    protected var rewardWindow: AgoraUIReward? = null
    protected var agoraUILoading: AgoraUILoading? = null

    // Hyphenate chat im, a separate chat widget, different
    // from agora chat window, with its own identifier.
    // Containers have both agora and hyphenate chat widgets,
    // and they are independent of each other
    protected var chat: ChatWidget? = null

    private var context: Context? = null
    private var layout: ViewGroup? = null
    private var activity: Activity? = null

    protected val widgetManager = UiWidgetManager()

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

    override fun setActivity(activity: Activity) {
        this.activity = activity
    }

    override fun getActivity(): Activity? {
        return this.activity
    }

    internal fun hideSoftInput(context: Context, view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    override fun init(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int) {
        AgoraUIConfig.isGARegion = config.isGARegion
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

    override fun release() {
        activity = null
        widgetManager.release()
        chat?.release()
    }
}

enum class AgoraContainerType {
    OneToOne, SmallClass, LargeClass, Debug
}

data class AgoraContainerConfig(
        val isGARegion: Boolean = false,
        val chatTabConfigs: List<ChatTabConfig>
)

object AgoraUIConfig {
    const val carouselMaxItem = 7
    const val videoWidthMaxRatio = 0.312f
    const val videoRatio1 = 9 / 16f
    const val videoRatio2 = 3 / 4f
    const val clickInterval = 500L
    var isLargeScreen: Boolean = false
    const val videoPlaceHolderImgSizePercent = 0.5f
    const val videoOptionIconSizePercent = 0.14f
    const val videoOptionIconSizeMax = 54
    const val videoOptionIconSizeMaxWithLargeScreen = 36
    const val audioVolumeIconWidthRatio = 0.72727273f
    const val audioVolumeIconAspect = 0.1875f
    const val chatHeightLargeScreenRatio = 0.7f
    var isGARegion = false

    object OneToOneClass {
        var teacherVideoWidth = 600
    }

    object SmallClass {
        const val studentVideoHeightRationSmallScreen = 0.24f
        var teacherVideoWidth = 600
        var teacherVideoHeight = 336
        var studentVideoHeightLargeScreen = 336
        var studentVideoHeightSmallScreen = 261
        const val chatWidthMaxRatio = 0.2463f
    }

    object LargeClass {
        const val studentVideoHeightRationSmallScreen = 0.24f
        var teacherVideoWidth = 600
        var teacherVideoHeight = 336
        var studentVideoHeightLargeScreen = 336
        var studentVideoHeightSmallScreen = 261
    }
}