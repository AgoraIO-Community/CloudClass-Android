package io.agora.agoraeduuikit.impl.container

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import com.google.gson.Gson
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.education.impl.Constants.Companion.AgoraLog
import io.agora.agoraeducore.core.internal.framework.impl.handler.UserHandler
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.agoraeduuikit.provider.UIDataProvider
import io.agora.agoraeduuikit.provider.UIDataProviderListenerImpl
import io.agora.agoraeduuikit.impl.chat.ChatWidget
import io.agora.agoraeduuikit.impl.loading.AgoraUILoading
import io.agora.agoraeduuikit.impl.room.AgoraUIRoomStatusArt
import io.agora.agoraeducore.core.context.EduContextScreenShareState.Start
import io.agora.agoraeducore.core.context.EduContextScreenShareState.Stop
import io.agora.agoraeducore.core.internal.framework.impl.handler.MonitorHandler
import io.agora.agoraeduuikit.impl.screenshare.AgoraUIScreenShare
import io.agora.agoraeduuikit.impl.users.AgoraUIReward
import io.agora.agoraeduuikit.impl.users.AgoraUserListVideoLayout
import io.agora.agoraeduuikit.impl.video.AgoraUIVideoGroup
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionPacket
import io.agora.agoraeduuikit.interfaces.protocols.IAgoraUIContainer
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionSignal.RTCAudioMixingStateChanged
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionSignal.BoardAudioMixingRequest
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetDefaultId.WhiteBoard
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetMessageObserver
import io.agora.agoraeduuikit.impl.room.AgoraUIRoomStatusOne2One
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardAudioMixingRequestData
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardAudioMixingRequestType

abstract class AbsUIContainer(
        private val eduContext: EduContextPool?,
        protected val config: AgoraContainerConfig) : IAgoraUIContainer {
    private val tag = "AbsUIContainer"

    /**
     * Container needs to receive and display room errors and tips
     */
    private val monitorHandler = object : MonitorHandler() {
        override fun onLocalConnectionUpdated(state: EduContextConnectionState) {
            super.onLocalConnectionUpdated(state)
            AgoraLog.i("$tag->container room connection state changed ${state.name}")
            agoraUILoading?.setVisibility(if (state == EduContextConnectionState.Connected) GONE else VISIBLE)
            if (state == EduContextConnectionState.Connecting) {
                agoraUILoading?.setContent(true)
            } else if (state == EduContextConnectionState.Reconnecting) {
                agoraUILoading?.setContent(false)
            }
        }
    }
    private val containerUserHandler3 = object : UserHandler() {
        override fun onUserRewarded(user: AgoraEduContextUserInfo, rewardCount: Int, operator: AgoraEduContextUserInfo?) {
            super.onUserRewarded(user, rewardCount, operator)
            if (rewardWindow?.isShowing() == false) {
                rewardWindow?.show()
            }
        }
    }
    protected var whiteboardContainer: LinearLayout? = null
    protected var roomStatusOne2One: AgoraUIRoomStatusOne2One? = null
    protected var roomStatus: AgoraUIRoomStatusArt? = null
    protected var roomStatusArt: AgoraUIRoomStatusArt? = null
    protected var teacherVideoWindow: AgoraUIVideoGroup? = null
    protected var screenShareWindow: AgoraUIScreenShare? = null
    protected var studentsVideoWindow: AgoraUserListVideoLayout? = null
    protected var rewardWindow: AgoraUIReward? = null
    protected var agoraUILoading: AgoraUILoading? = null

    // Hyphenate chat im, a separate chat widget, different
    // from agora chat window, with its own identifier.
    // Containers have both agora and hyphenate chat widgets,
    // and they are independent of each other
    protected var chat: ChatWidget? = null
    protected var whiteBoardWidget: AgoraBaseWidget? = null
    private var context: Context? = null
    private var layout: ViewGroup? = null
    private var activity: Activity? = null
    protected var uiDataProvider: UIDataProvider? = null

    private val baseUIDataProviderListener = object : UIDataProviderListenerImpl() {
        override fun onAudioMixingStateChanged(state: Int, errorCode: Int) {
            super.onAudioMixingStateChanged(state, errorCode)
            val pair = Pair(state, errorCode)
            val packet = AgoraBoardInteractionPacket(RTCAudioMixingStateChanged, pair)
            eduContext?.widgetContext()?.sendMessageToWidget(Gson().toJson(packet), WhiteBoard.id)
        }

        override fun onLocalUserKickedOut() {
            super.onLocalUserKickedOut()
            kickOut()
        }

        override fun onScreenShareStart(info: AgoraUIUserDetailInfo) {
            screenShareWindow?.updateScreenShareState(Start, info.streamUuid)
        }

        override fun onScreenShareStop(info: AgoraUIUserDetailInfo) {
            screenShareWindow?.updateScreenShareState(Stop, info.streamUuid)
        }
    }

    private val widgetMessageObserver = object : AgoraWidgetMessageObserver {
        override fun onMessageReceived(msg: String, id: String) {
            val packet = Gson().fromJson(msg, AgoraBoardInteractionPacket::class.java)
            if (id == WhiteBoard.id && packet.signal == BoardAudioMixingRequest && packet.body is AgoraBoardAudioMixingRequestData) {
                val data = packet.body
                when (data.type) {
                    AgoraBoardAudioMixingRequestType.Start -> {
                        eduContext?.mediaContext()?.startAudioMixing(data.filepath, data.loopback,
                                data.replace, data.cycle)
                    }
                    AgoraBoardAudioMixingRequestType.Stop -> {
                        eduContext?.mediaContext()?.stopAudioMixing()
                    }
                    AgoraBoardAudioMixingRequestType.SetPosition -> {
                        eduContext?.mediaContext()?.setAudioMixingPosition(data.position)
                    }
                }
            }
        }
    }

    init {
        AgoraUIDeviceSetting.setFrontCamera(true)
        uiDataProvider = UIDataProvider(eduContext)
        eduContext?.monitorContext()?.addHandler(monitorHandler)
        eduContext?.userContext()?.addHandler(containerUserHandler3)
        eduContext?.widgetContext()?.addWidgetMessageObserver(widgetMessageObserver, WhiteBoard.id)
    }

    protected fun getEduContext(): EduContextPool? {
        return eduContext
    }

    override fun showLeave() {
        roomStatus?.showLeaveDialog()
        roomStatusArt?.showLeaveDialog()
        roomStatusOne2One?.showLeaveDialog()
    }

    override fun kickOut() {
        roomStatus?.kickOut()
        roomStatusArt?.kickOut()
    }

    override fun showError(error: EduContextError) {
        context?.let {
            AgoraUIToast.error(context = it, text = error.msg)
        }
    }

    override fun showTips(msg: String) {
        context?.let {
            AgoraUIToast.info(context = it, text = msg)
        }
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

    override fun init(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int) {//layout: contentLayout
        AgoraUIConfig.isGARegion = config.isGARegion
        val screenLayout = layout.context.resources.configuration.screenLayout
        AgoraUIConfig.isLargeScreen =
                screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >=
                        Configuration.SCREENLAYOUT_SIZE_LARGE
        rewardWindow = AgoraUIReward(layout.context, layout, 0, 0, width, height)
        context = layout.context
        this.layout = layout
        // register baseUIDataProviderListener for kickout
        uiDataProvider?.addListener(baseUIDataProviderListener)
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

    protected abstract fun calculateComponentSize()

    protected fun getContext(): Context? {
        return layout()?.context
    }

    override fun layout(): ViewGroup? {
        return this.layout
    }

    override fun release() {
        activity = null
    }
}

enum class AgoraContainerType {
    OneToOne, SmallClass, SmallClassArt, LargeClass, Debug
}

data class AgoraContainerConfig(
        val isGARegion: Boolean = false
)

object AgoraUIConfig {
    const val carouselMaxItem = 7
    const val videoWidthMaxRatio = 0.25487f
    const val videoRatio1 = 9 / 16f
    const val clickInterval = 500L
    var isLargeScreen: Boolean = false
    const val videoPlaceHolderImgSizePercent = 0.6f
    const val videoOptionIconSizePercent = 0.14f
    const val videoOptionIconSizeMax = 54
    const val videoOptionIconSizeMaxWithLargeScreen = 36
    const val audioVolumeIconWidthRatio = 0.72727273f
    const val audioVolumeIconAspect = 0.1875f
    const val chatHeightLargeScreenRatio = 0.7f
    var isGARegion = false
    var keepVideoListItemRatio = false

    const val baseUIHeightSmallScreen = 375f
    const val baseUIHeightLargeScreen = 574f

    object OneToOneClass {
        var teacherVideoWidth = 600
    }

    object SmallClass {
        var videoListVideoWidth = 600
        var videoListVideoHeight = 336
    }

    object LargeClass {
        const val coHostMaxItem = 4

        // whiteBoard and teacherVideo
        const val componentRatio = 9f / 16f
        const val statusBarPercent = 0.026f
        const val teacherVideoWidthMaxRatio = 0.29985f

        var studentVideoWidth = 600
        var studentVideoHeight = 336
        var teacherVideoWidth = 600
        var teacherVideoHeight = 336
    }
}

/**
 * Used to record UI-layer controlled device states,
 * like camera facing.
 */
object AgoraUIDeviceSetting {
    private var isCameraFront: Boolean = true

    @Synchronized
    fun isFrontCamera(): Boolean {
        return isCameraFront
    }

    @Synchronized
    fun setFrontCamera(isFront: Boolean) {
        isCameraFront = isFront
    }
}