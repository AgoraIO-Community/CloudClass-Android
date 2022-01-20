package io.agora.agoraeduuikit.impl.container

import android.content.res.Resources
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.google.gson.Gson
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.UserHandler
import io.agora.agoraeducore.core.internal.framework.impl.managers.AgoraWidgetActiveObserver
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetDefaultId
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.component.dialog.AgoraUICustomDialogBuilder
import io.agora.agoraeduuikit.impl.loading.AgoraUILoading
import io.agora.agoraeduuikit.impl.options.OptionLayoutMode
import io.agora.agoraeduuikit.impl.options.OptionsLayout
import io.agora.agoraeduuikit.impl.options.OptionsLayoutListener
import io.agora.agoraeduuikit.impl.options.OptionsLayoutWhiteboardItem
import io.agora.agoraeduuikit.impl.room.AgoraUIRoomStatusArt
import io.agora.agoraeduuikit.impl.screenshare.AgoraUIScreenShare
import io.agora.agoraeduuikit.impl.users.AgoraUIHandsUpToastPopUp
import io.agora.agoraeduuikit.impl.video.*
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeduuikit.provider.UIDataProviderListenerImpl
import io.agora.agoraeduuikit.util.VideoUtils

class AgoraUISmallClassContainerArt(
    eduContext: EduContextPool?,
    configs: AgoraContainerConfig) : AbsUIContainer(eduContext, configs) {
    private val tag = "AgoraUISmallClassContainer"

    private var statusBarHeight = 0

    private var whiteboardHeight = 0

    private var optionRight = 0
    private var optionBottom = 0
    private var optionIconSize = 0
    private var optionPopupRight = 0

    private var chatWidth = 0
    private var chatHeight = 0

    private val whiteboardRect = Rect()
    private var optionsLayoutContainer: LinearLayout? = null

    private var margin = 0
    private var border = 0

    private var width = 0
    private var height = 0
    private var top = 0
    private var left = 0

    private var videoListWindow: AgoraUIVideoListArt? = null
    private var optionLayout: OptionsLayout? = null
    private var largeVideoWindowWidget: AgoraBaseWidget? = null
    private var userDetailInfo: AgoraUIUserDetailInfo? = null
    private var lastUserDetailInfo: AgoraUIUserDetailInfo? = null
    private var handsUpPopup: AgoraUIHandsUpToastPopUp? = null
    private var whiteboardToolItem: OptionsLayoutWhiteboardItem? = null
    private var largeWindowArtLeft: Int = 0 // x坐标
    private var largeWindowArtTop: Int = 0 // y
    private var largeWindowArtHeight: Int = 0
    private var largeWindowArtWidth: Int = 0
    private var largeWindowArtLeftRatio: Float = 0f
    private var largeWindowArtTopRatio: Float = 0f
    private var largeWindowArtHeightRatio: Float = 0f
    private var largeWindowArtWidthRatio: Float = 0f
    private var teacherInfo: AgoraUIUserDetailInfo? = null
    private var studentCoHostList: MutableList<AgoraUIUserDetailInfo> = mutableListOf()
    private var teacherDetailInfo: AgoraEduContextUserInfo? = null
    private var coHostList: MutableList<AgoraUIUserDetailInfo> = mutableListOf()

    private val uiDataProviderListener = object : UIDataProviderListenerImpl() {
        override fun onCoHostListChanged(userList: List<AgoraUIUserDetailInfo>) {
            super.onCoHostListChanged(userList)
            // only student`s coHost filed can modified
            studentCoHostList = userList.toMutableList()
            notifyVideos()
        }

        override fun onUserListChanged(userList: List<AgoraUIUserDetailInfo>) {
            super.onUserListChanged(userList)
            teacherInfo = userList.find { it.role == AgoraEduContextUserRole.Teacher }
            notifyVideos()
        }
    }

    private fun notifyVideos() {
        val hasTeacher = teacherInfo != null
        videoListWindow?.showTeacher(hasTeacher)
        val hasCoHost = hasTeacher || studentCoHostList.size > 0
        videoListWindow?.showStudents(hasCoHost)

        if (hasCoHost) {
            agoraUILoading?.setRect(whiteboardRect)
        } else {
        }
    }

    private val roomHandler = object : RoomHandler(), AgoraUILargeVideoWidget.IAgoraUILargeVideoListener {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)
            val config = getEduContext()?.widgetContext()?.getWidgetConfig(AgoraWidgetDefaultId.WhiteBoard.id)
            config?.let {
                whiteBoardWidget = getEduContext()?.widgetContext()?.create(it)
            }
            whiteboardContainer?.let {
                whiteBoardWidget?.init(it, whiteboardRect.right - whiteboardRect.left,
                    whiteboardRect.bottom - whiteboardRect.top, 0, 0)
            }
            layout()?.let {
                initOptionLayout(it)
                // add loading(show/hide follow rtmConnectionState)
                agoraUILoading = AgoraUILoading(it, whiteboardRect)
            }
            // Check if there is a screen stream is sharing
            uiDataProvider?.notifyScreenShareDisplay()
            val config2 = getEduContext()?.widgetContext()?.getWidgetConfig(AgoraWidgetDefaultId.LargeWindow.id)
            config2?.let {
                largeVideoWindowWidget = getEduContext()?.widgetContext()?.create(it)
            }
            if (largeVideoWindowWidget is AgoraUILargeVideoWidget) {
                (largeVideoWindowWidget as AgoraUILargeVideoWidget).largeVideoListener = this
            }
            largeWindowContainer?.let {
                largeVideoWindowWidget?.init(it, largeWindowArtWidth, largeWindowArtHeight,
                    0, 0)
                uiDataProvider?.addListener((largeVideoWindowWidget as AgoraUILargeVideoWidget).uiDataProviderListener)
            }
            val isLargeWindowOpened = getEduContext()?.widgetContext()?.getWidgetActive(AgoraWidgetDefaultId.LargeWindow.id)
            isLargeWindowOpened?.let { handleLargeWindowEvent(it) }
        }

        override fun onLargeVideoShow(streamUuid: String) {
            if (isLocalStream(streamUuid)) {
                val configs = VideoUtils().getVideoEditEncoderConfigs()
                getEduContext()?.streamContext()?.setLocalVideoConfig(streamUuid, configs)
            }
        }

        override fun onLargeVideoDismiss(streamUuid: String) {
            if (isLocalStream(streamUuid)) {
                val configs = VideoUtils().getSmallVideoEncoderConfigs()
                getEduContext()?.streamContext()?.setLocalVideoConfig(streamUuid, configs)
            }
        }

        override fun onRendererContainer(config: EduContextRenderConfig, viewGroup: ViewGroup?, streamUuid: String) {
            val noneView = viewGroup == null
            if (noneView) {
                getEduContext()?.mediaContext()?.stopRenderVideo(streamUuid)
            } else {
                getEduContext()?.mediaContext()?.startRenderVideo(config, viewGroup!!, streamUuid)
            }
        }
    }

    private val smallContainerUserHandler = object : UserHandler() {
        override fun onRemoteUserJoined(user: AgoraEduContextUserInfo) {
            super.onRemoteUserJoined(user)
            if (user.role == AgoraEduContextUserRole.Teacher) {
                teacherDetailInfo = user.copy()
                notifyVideos()
            } else if (user.role == AgoraEduContextUserRole.Student) {

            }
        }

        override fun onRemoteUserLeft(user: AgoraEduContextUserInfo,
                                      operator: AgoraEduContextUserInfo?,
                                      reason: EduContextUserLeftReason) {
            super.onRemoteUserLeft(user, operator, reason)
            if (user.role == AgoraEduContextUserRole.Teacher) {
                teacherInfo = null
                notifyVideos()
            } else if (user.role == AgoraEduContextUserRole.Student) {
            }
        }

        override fun onLocalUserKickedOut() {
            super.onLocalUserKickedOut()
            kickOut()
        }
    }

    private fun isLocalStream(streamUuid: String): Boolean {
        getEduContext()?.let { context ->
            val localUserId = context.userContext()?.getLocalUserInfo()?.userUuid
            localUserId?.let { userId ->
                context.streamContext()?.getStreamList(userId)?.forEach { streamInfo ->
                    if (streamInfo.streamUuid == streamUuid) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private val largeWindowActivateObserver = object : AgoraWidgetActiveObserver {
        override fun onWidgetActive(widgetId: String) {
            if (widgetId == AgoraWidgetDefaultId.LargeWindow.id) {
                handleLargeWindowEvent(true)
            }
        }

        override fun onWidgetInActive(widgetId: String) {
            if (widgetId == AgoraWidgetDefaultId.LargeWindow.id) {
                handleLargeWindowEvent(false)
            }
        }
    }

    init {
        // register userHandler
        getEduContext()?.roomContext()?.addHandler(roomHandler)
        uiDataProvider?.addListener(uiDataProviderListener)
    }

    override fun init(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int) {
        super.init(layout, left, top, width, height)

        this.width = width
        this.height = height
        this.left = left
        this.top = top

        layout.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (layout.width > 0 && layout.height > 0) {
                        layout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        this@AgoraUISmallClassContainerArt.width = layout.width
                        this@AgoraUISmallClassContainerArt.height = layout.height
                        this@AgoraUISmallClassContainerArt.left = left
                        this@AgoraUISmallClassContainerArt.top = top
                        initLayout(layout,
                            this@AgoraUISmallClassContainerArt.left,
                            this@AgoraUISmallClassContainerArt.top,
                            this@AgoraUISmallClassContainerArt.width,
                            this@AgoraUISmallClassContainerArt.height)
                    }
                }
            })
    }

    private fun initLayout(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int) {
        initValues(layout.context.resources, width, height)

        roomStatusArt = AgoraUIRoomStatusArt(layout, getEduContext(), width, statusBarHeight, left, top)
        roomStatusArt!!.setContainer(this)

        calculateComponentSize()
        if (getContext() == null) {
            return
        }

        val whiteboardW = width
        whiteboardRect.top = height - whiteboardHeight //白板顶部坐标：屏幕高度- 白板高度
        whiteboardRect.bottom = height
        whiteboardRect.left = 0
        whiteboardRect.right = whiteboardW
        whiteboardContainer = LinearLayout(getContext())
        layout.addView(whiteboardContainer)
        largeWindowContainer = LinearLayout(getContext())
        layout.addView(largeWindowContainer)
        val params = whiteboardContainer!!.layoutParams as ViewGroup.MarginLayoutParams
        params.width = whiteboardW
        params.height = whiteboardHeight
        params.topMargin = whiteboardRect.top
        whiteboardContainer!!.layoutParams = params
        val params2 = largeWindowContainer!!.layoutParams as ViewGroup.MarginLayoutParams
        params2.width = whiteboardW / 2
        params2.height = whiteboardW / 3
        params2.topMargin = whiteboardHeight / 3
        params2.leftMargin = whiteboardW / 4
        largeWindowContainer!!.layoutParams = params2
        largeWindowArtWidth = whiteboardW / 2
        largeWindowArtHeight = whiteboardW / 3
        largeWindowArtLeft = whiteboardW / 4
        largeWindowArtTop = whiteboardHeight / 3
        videoListWindow = AgoraUIVideoListArt(layout.context, getEduContext(),
            layout, 0, statusBarHeight, width,
            height - whiteboardRect.height() - statusBarHeight, 3, 0)

        screenShareWindow = AgoraUIScreenShare(layout.context,
            getEduContext(), layout,
            whiteboardRect.width(), whiteboardRect.height(),
            whiteboardRect.left, whiteboardRect.top, 0f)
        screenShareWindow!!.setContainer(this)

        //register uiDataProviderListener for components
        videoListWindow?.getUiDateProviders()?.forEach {
            uiDataProvider?.addListener(it)
        }

        getEduContext()?.widgetContext()?.addWidgetActiveObserver(
            largeWindowActivateObserver, AgoraWidgetDefaultId.LargeWindow.id)
    }

    private fun initOptionLayout(layout: ViewGroup) {
        val role = getEduContext()?.userContext()?.getLocalUserInfo()?.role
            ?: AgoraEduContextUserRole.Student
        val mode = OptionLayoutMode.Joint
        val container = if (layout is RelativeLayout) {
            layout
        } else {
            val container = RelativeLayout(getContext())
            val params = ViewGroup.MarginLayoutParams(
                ViewGroup.MarginLayoutParams.MATCH_PARENT,
                ViewGroup.MarginLayoutParams.MATCH_PARENT)
            layout.addView(container, params)
            container
        }

        handsUpPopup = AgoraUIHandsUpToastPopUp(layout.context)
        OptionsLayout(getContext()).let {
            optionLayout = it
            it.init(eduContext = getEduContext(),
                parent = container, role = role,
                width = optionIconSize, right = optionRight,
                bottom = optionBottom, mode = mode,
                container = this, handsUpPopup = handsUpPopup)
        }

        OptionsLayout.listener = object : OptionsLayoutListener {
            override fun onLeave() {
                showLeave()
            }

            override fun onKickout(userId: String, userName: String) {
                showKickDialog(userId)
            }
        }

        //双击列表小窗回调，打开大窗
        AgoraUIVideoArt2.userInfoListener = object : IAgoraUserInfoListener2 {
            override fun onUserDoubleClicked(userDetailInfo: AgoraUIUserDetailInfo?) {
                this@AgoraUISmallClassContainerArt.userDetailInfo = userDetailInfo
                if (getEduContext()?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Teacher || getEduContext()?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Student) {//只有老师才能开启大窗
                    getActivity()?.runOnUiThread {
                        //发消息给widget
                        val packet = userDetailInfo?.let { AgoraLargeWindowInteractionPacket(AgoraLargeWindowInteractionSignal.LargeWindowShowed, it) }
                        this@AgoraUISmallClassContainerArt.getEduContext()?.widgetContext()?.sendMessageToWidget(
                            Gson().toJson(packet), AgoraWidgetDefaultId.LargeWindow.id)
                        lastUserDetailInfo = userDetailInfo
                    }
//                    largeWindowArtLeftRatio = (largeWindowArtLeft / width).toFloat()
//                    largeWindowArtTopRatio = (largeWindowArtTop / height).toFloat()
//                    largeWindowArtHeightRatio = (largeWindowArtHeight / height).toFloat()
//                    largeWindowArtWidthRatio = (largeWindowArtWidth / width).toFloat()// 白板的宽:width
//                    var windowPropertyBody = WindowPropertyBody(userDetailInfo?.userUuid, Position(largeWindowArtLeftRatio, largeWindowArtTopRatio), Size(largeWindowArtHeightRatio.toDouble(), largeWindowArtWidthRatio.toDouble()), Extra(userDetailInfo?.userUuid, true))
//                    getEduContext()?.windowPropertiesContext()?.performWindowProperties(windowPropertyBody)//通知其他端设置属性
                }
            }
        }

        //双击大窗回调
        AgoraUILargeVideoArt.userInfoListener = object : IAgoraUserInfoListener2 {
            override fun onUserDoubleClicked(userDetailInfo: AgoraUIUserDetailInfo?) {
                this@AgoraUISmallClassContainerArt.userDetailInfo = userDetailInfo
                if (getEduContext()?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Teacher || getEduContext()?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Student) {
                    getActivity()?.runOnUiThread {
                        // send msg to render video on the video list
                        val packet = userDetailInfo?.let { AgoraLargeWindowInteractionPacket(AgoraLargeWindowInteractionSignal.LargeWindowClosed, it) }
                        this@AgoraUISmallClassContainerArt.getEduContext()?.widgetContext()?.sendMessageToWidget(
                            Gson().toJson(packet), AgoraWidgetDefaultId.LargeWindow.id)
                        lastUserDetailInfo = userDetailInfo
                    }
                    //发消息通知其他端移除属性
//                    var windowPropertyBody = WindowPropertyBody(userDetailInfo?.userUuid, Position(largeWindowArtLeft.toFloat(), largeWindowArtTop.toFloat()), Size(largeWindowArtHeight.toDouble(), largeWindowArtWidth.toDouble()), Extra(userDetailInfo?.userUuid, true))
//                    getEduContext()?.windowPropertiesContext()?.deleteWindowProperties(windowPropertyBody)
                }
            }
        }
    }

    override fun resize(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int) {

    }

    private fun initValues(resources: Resources, width: Int, height: Int) {
        AgoraUIConfig.keepVideoListItemRatio = true
        // 375 is the base height of container height of phones
        // 574 is the base height of tablets
        val basePhone = AgoraUIConfig.baseUIHeightSmallScreen
        val baseTablet = AgoraUIConfig.baseUIHeightLargeScreen
        if (AgoraUIConfig.isLargeScreen) {
            statusBarHeight = (height * 20 / baseTablet).toInt()
            optionRight = (height * 9 / baseTablet).toInt()
            optionBottom = (height * 12 / baseTablet).toInt()
        } else {
            statusBarHeight = (height * 14 / basePhone).toInt()
            optionRight = (height * 6 / basePhone).toInt()
            optionBottom = (height * 7 / basePhone).toInt()
        }

        whiteboardHeight = (height * 0.82).toInt()

        optionIconSize = if (AgoraUIConfig.isLargeScreen)
            (height * 46 / baseTablet).toInt() else (height * 46 / basePhone).toInt()

        optionPopupRight = if (AgoraUIConfig.isLargeScreen)
            (height * 60 / baseTablet).toInt() else (height * 50 / basePhone).toInt()

        chatWidth = if (AgoraUIConfig.isLargeScreen)
            (height * 340 / baseTablet).toInt() else (height * 200 / basePhone).toInt()

        chatHeight = if (AgoraUIConfig.isLargeScreen)
            (height * 400 / baseTablet).toInt() else (height * 268 / basePhone).toInt()

        margin = resources.getDimensionPixelSize(R.dimen.margin_smaller)
        border = resources.getDimensionPixelSize(R.dimen.stroke_small)
    }

    override fun calculateComponentSize() {
        val videosLayoutMaxW = this.width - margin * (AgoraUIConfig.carouselMaxItem - 1)
        val videoMaxW = videosLayoutMaxW / AgoraUIConfig.carouselMaxItem
        AgoraUIConfig.SmallClass.videoListVideoWidth = videoMaxW
        AgoraUIConfig.SmallClass.videoListVideoHeight = (videoMaxW * AgoraUIConfig.videoRatio1).toInt()
    }

    override fun willLaunchExtApp(appIdentifier: String): Int {
        return 0
    }

    override fun setFullScreen(fullScreen: Boolean) {
    }

    private fun showKickDialog(userId: String) {
        layout()?.let {
            it.post {
                val customView = LayoutInflater.from(it.context).inflate(
                    R.layout.agora_kick_dialog_radio_layout, it, false)
                val optionOnce = customView.findViewById<RelativeLayout>(R.id.agora_kick_dialog_once_layout)
                val optionForever = customView.findViewById<RelativeLayout>(R.id.agora_kick_dialog_forever_layout)

                optionOnce.isActivated = true
                optionForever.isActivated = false

                optionOnce.setOnClickListener {
                    optionOnce.isActivated = true
                    optionForever.isActivated = false
                }

                optionForever.setOnClickListener {
                    optionOnce.isActivated = false
                    optionForever.isActivated = true
                }

                AgoraUICustomDialogBuilder(it.context)
                    .title(it.context.resources.getString(R.string.agora_dialog_kick_student_title))
                    .negativeText(it.context.resources.getString(R.string.cancel))
                    .positiveText(it.context.resources.getString(R.string.confirm))
                    .positiveClick {
                        val forever = !optionOnce.isActivated && optionForever.isActivated
                        //getEduContext()?.userContext()?.kickOutUser(userId, forever)
                    }
                    .setCustomView(customView)
                    .build()
                    .show()
            }
        }
    }

    private fun handleLargeWindowEvent(active: Boolean) {
        largeVideoWindowWidget?.widgetInfo?.let { widgetInfo ->
            widgetInfo.roomProperties?.let { properties ->
                (properties["userUuid"] as? String)?.let { userId ->
                    // Edu context api does not provide an API to
                    // obtain the info of a certain single user
                    getEduContext()?.let { context ->
                        context.userContext()?.let { userContext ->
                            userContext.getAllUserList().find { eduUserInfo ->
                                eduUserInfo.userUuid == userId
                            }?.let { userInfo ->
                                (properties["streamUuid"] as? String).let { streamId ->
                                    context.streamContext()?.getStreamList(userInfo.userUuid)?.find { eduStreamInfo ->
                                        eduStreamInfo.streamUuid == streamId
                                    }?.let { streamInfo ->
                                        sendToLargeWindow(active, userInfo, streamInfo)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun sendToLargeWindow(active: Boolean,
                                  userInfo: AgoraEduContextUserInfo,
                                  streamInfo: AgoraEduContextStreamInfo) {
        buildLargeWindowUserInfoData(userInfo, streamInfo)?.let {
            val signal = if (active) {
                AgoraLargeWindowInteractionSignal.LargeWindowShowed
            } else {
                AgoraLargeWindowInteractionSignal.LargeWindowClosed
            }

            val packet = AgoraLargeWindowInteractionPacket(signal, it)
            getEduContext()?.widgetContext()?.sendMessageToWidget(
                Gson().toJson(packet), AgoraWidgetDefaultId.LargeWindow.id)
            lastUserDetailInfo = userDetailInfo
        }
    }

    private fun buildLargeWindowUserInfoData(userInfo: AgoraEduContextUserInfo,
                                             streamInfo: AgoraEduContextStreamInfo): AgoraUIUserDetailInfo? {
        val localVideoState: AgoraEduContextDeviceState2?
        val localAudioState: AgoraEduContextDeviceState2?
        if (userInfo.userUuid == getEduContext()?.userContext()?.getLocalUserInfo()?.userUuid) {
            localVideoState = uiDataProvider?.localVideoState
            localAudioState = uiDataProvider?.localAudioState
        } else {
            localVideoState = null
            localAudioState = null
        }

        return uiDataProvider?.toAgoraUserDetailInfo(userInfo,
            true, streamInfo, localAudioState, localVideoState)
    }
}