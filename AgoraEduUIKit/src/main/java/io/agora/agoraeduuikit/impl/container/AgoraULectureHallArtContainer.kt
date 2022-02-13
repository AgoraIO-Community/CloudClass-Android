package io.agora.agoraeduuikit.impl.container

import android.content.res.Resources
import android.graphics.Rect
import android.view.*
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.google.gson.Gson
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.impl.managers.AgoraWidgetActiveObserver
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetDefaultId
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetDefaultId.Chat
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.component.dialog.AgoraUICustomDialogBuilder
import io.agora.agoraeduuikit.impl.chat.ChatWidget
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.LargeClass.coHostMaxItem
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.LargeClass.componentRatio
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.LargeClass.statusBarPercent
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.LargeClass.teacherVideoHeight
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.LargeClass.teacherVideoWidth
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.LargeClass.teacherVideoWidthMaxRatio
import io.agora.agoraeduuikit.impl.loading.AgoraUILoading
import io.agora.agoraeduuikit.impl.options.OptionLayoutMode
import io.agora.agoraeduuikit.impl.options.OptionsLayout
import io.agora.agoraeduuikit.impl.options.OptionsLayoutListener
import io.agora.agoraeduuikit.impl.room.AgoraUIRoomStatusArt
import io.agora.agoraeduuikit.impl.screenshare.AgoraUIScreenShare
import io.agora.agoraeduuikit.impl.users.AgoraUIHandsUpToastPopUp
import io.agora.agoraeduuikit.impl.users.AgoraUserListVideoLayoutArt2
import io.agora.agoraeduuikit.impl.video.*
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeduuikit.provider.UIDataProviderListenerImpl
import io.agora.agoraeduuikit.util.VideoUtils

class AgoraUILectureHallArtContainer(
    eduContext: EduContextPool?,
    configs: AgoraContainerConfig) : AbsUIContainer(eduContext, configs), AgoraUILargeVideoWidget.IAgoraUILargeVideoListener {
    private val tag = "AgoraUILargeClassContainer"

    private var statusBarHeight = 0

    private var margin = 0
    private var border = 0

    private var width = 0
    private var height = 0
    private var top = 0
    private var left = 0

    private val chatRect = Rect()
    private val whiteboardRect = Rect()
    private var handsUpPopup: AgoraUIHandsUpToastPopUp? = null
    private var optionLayout: OptionsLayout? = null
    private var optionRight = 0
    private var optionBottom = 0
    private var optionIconSize = 0
    private var optionPopupRight = 0
    private var largeVideoWindowWidget: AgoraBaseWidget? = null
    private var userDetailInfo: AgoraUIUserDetailInfo? = null
    private var lastUserDetailInfo: AgoraUIUserDetailInfo? = null
    private var largeWindowArtLeft: Int = 0 // x坐标
    private var largeWindowArtTop: Int = 0 // y
    private var largeWindowArtHeight: Int = 0
    private var largeWindowArtWidth: Int = 0
    private var videoListWindow: AgoraUserListVideoLayoutArt2? = null
    private var teacherVideoWindowArt: AgoraUIVideoGroupArt? = null

    private val uiDataProviderListener = object : UIDataProviderListenerImpl() {
        override fun onCoHostListChanged(userList: List<AgoraUIUserDetailInfo>) {
            super.onCoHostListChanged(userList)
            onCoHostListUpdated(userList)
        }
    }

    private fun onCoHostListUpdated(list: List<AgoraUIUserDetailInfo>) {
        val hasCoHost = list.isNotEmpty()
//        studentsVideoWindow?.show(hasCoHost)
        videoListWindow?.show(hasCoHost)
    }

    private val roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)
            val config = getEduContext()?.widgetContext()?.getWidgetConfig(AgoraWidgetDefaultId.WhiteBoard.id)
            config?.let {
                whiteBoardWidget = getEduContext()?.widgetContext()?.create(it)
            }

            whiteboardContainer?.let { parent ->
                val w = whiteboardRect.right - whiteboardRect.left
                val h = whiteboardRect.bottom - whiteboardRect.top
                parent.post {
                    whiteBoardWidget?.init(parent, w, h, 0, 0)
                }
            }

            this@AgoraUILectureHallArtContainer.layout()?.let {
                val config1 = getEduContext()?.widgetContext()?.getWidgetConfig(Chat.id)
                config1?.apply {
                    chat = getEduContext()?.widgetContext()?.create(config1) as? ChatWidget
                }
                val w = chatRect.right - chatRect.left
                val h = chatRect.bottom - chatRect.top
                it.post {
                    chat?.init(it, w, h, chatRect.top, chatRect.left)
                    chat?.setClosable(false)
                    chat?.show(true)
                }
            }

            // Check if there is a screen stream is sharing
            uiDataProvider?.notifyScreenShareDisplay()
        }
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

    override fun onRendererContainer(config: EduContextRenderConfig, viewGroup: ViewGroup?, streamUuid: String) {
        val noneView = viewGroup == null
        if (noneView) {
            getEduContext()?.mediaContext()?.stopRenderVideo(streamUuid)
        } else {
            getEduContext()?.mediaContext()?.startRenderVideo(config, viewGroup!!, streamUuid)
        }
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
        // register handler
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
                        this@AgoraUILectureHallArtContainer.width = layout.width
                        this@AgoraUILectureHallArtContainer.height = layout.height
                        this@AgoraUILectureHallArtContainer.left = left
                        this@AgoraUILectureHallArtContainer.top = top
                        initLayout(layout,
                            this@AgoraUILectureHallArtContainer.left,
                            this@AgoraUILectureHallArtContainer.top,
                            this@AgoraUILectureHallArtContainer.width,
                            this@AgoraUILectureHallArtContainer.height)
                    }
                }
            })
    }

    private fun initLayout(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int) {
        initValues(layout.context.resources)

        roomStatus = AgoraUIRoomStatusArt(layout, getEduContext(), width, statusBarHeight, left, top)
        roomStatus!!.setContainer(this)

        calculateComponentSize()
        val teacherVideoW = teacherVideoWidth
        val teacherVideoH = teacherVideoHeight
        val teacherVideoTop = statusBarHeight + margin
        val teacherVideoLeft = width - teacherVideoW
        teacherVideoWindowArt = AgoraUIVideoGroupArt(layout.context, getEduContext(), layout,
            teacherVideoLeft, teacherVideoTop, teacherVideoW,
            teacherVideoH, 0, EduContextVideoMode.Single)
        teacherVideoWindowArt!!.setContainer(this)

        val whiteboardW = width - teacherVideoW - margin
        val whiteboardH = (whiteboardW * componentRatio).toInt()

        // Rect when student video list is shown
        whiteboardRect.set(0, height - whiteboardH, whiteboardW, height)
        whiteboardContainer = LinearLayout(getContext())
        layout.addView(whiteboardContainer)
        largeWindowContainer = LinearLayout(getContext())
        layout.addView(largeWindowContainer)
        val params = whiteboardContainer!!.layoutParams as ViewGroup.MarginLayoutParams
        params.width = whiteboardW
        params.height = whiteboardH
        params.topMargin = whiteboardRect.top
        whiteboardContainer!!.layoutParams = params
        val params2 = largeWindowContainer!!.layoutParams as ViewGroup.MarginLayoutParams
        params2.width = whiteboardW / 2
        params2.height = whiteboardW / 3
        params2.topMargin = whiteboardRect.bottom - whiteboardRect.top / 3
        params2.leftMargin = whiteboardW / 4
        largeWindowContainer!!.layoutParams = params2
        largeWindowArtWidth = whiteboardW / 2
        largeWindowArtHeight = whiteboardW / 3
        largeWindowArtLeft = whiteboardW / 4
        largeWindowArtTop = whiteboardRect.bottom - whiteboardRect.top / 3
        val config = getEduContext()?.widgetContext()?.getWidgetConfig(AgoraWidgetDefaultId.LargeWindow.id)
        config?.let {
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
        screenShareWindow = AgoraUIScreenShare(layout.context, getEduContext(), layout,
            whiteboardW, whiteboardH, 0, height - whiteboardH, 0f)
        screenShareWindow!!.setContainer(this)

        val studentVideoLeft = 0
        val studentVideoTop = statusBarHeight + margin
        val studentVideoWidth = teacherVideoLeft - margin
        val studentVideoHeight = height - whiteboardH - margin - studentVideoTop
        AgoraUIConfig.LargeClass.studentVideoWidth = (studentVideoWidth - (coHostMaxItem - 1) * margin) / coHostMaxItem
        AgoraUIConfig.LargeClass.studentVideoHeight = studentVideoHeight
        val studentParentContainer = LinearLayout(layout.context)
        val studentParentContainerParams = RelativeLayout.LayoutParams(studentVideoWidth, studentVideoHeight)
        studentParentContainerParams.leftMargin = studentVideoLeft
        studentParentContainerParams.topMargin = studentVideoTop
        studentParentContainer.layoutParams = studentParentContainerParams
        studentParentContainer.gravity = Gravity.CENTER
        layout.addView(studentParentContainer)
        val studentContainer = LinearLayout(layout.context)
        val studentContainerW = ViewGroup.LayoutParams.WRAP_CONTENT
        val studentContainerParams = LinearLayout.LayoutParams(studentContainerW, studentVideoHeight)
        studentContainer.layoutParams = studentContainerParams
        studentContainer.gravity = Gravity.CENTER
        studentParentContainer.addView(studentContainer)
//        studentsVideoWindow = AgoraUserListVideoLayout(layout.context, getEduContext(), studentContainer,
//            ViewGroup.LayoutParams.WRAP_CONTENT, studentVideoHeight, 0, 0, 0f, margin)
//        studentsVideoWindow!!.setContainer(this)
//        studentsVideoWindow!!.show(false)
        videoListWindow = AgoraUserListVideoLayoutArt2(layout.context,
            getEduContext(), layout, studentVideoWidth, studentVideoHeight, studentVideoLeft,
            studentVideoTop, 0f, 0)
        videoListWindow!!.setContainer(this)
        videoListWindow!!.show(false)

        val chatTop = teacherVideoTop + teacherVideoH + margin
        val chatRight = teacherVideoLeft + teacherVideoW
        chatRect.set(teacherVideoLeft, chatTop, chatRight, height)

        //register uiDataProviderListener for components
        teacherVideoWindowArt?.let {
            uiDataProvider?.addListener(it.uiDataProviderListener)
        }
        videoListWindow?.let {
            uiDataProvider?.addListener(it.uiDataProviderListener)
        }

        initOptionLayout(layout)

        // add loading(show/hide follow rtmConnectionState)
        agoraUILoading = AgoraUILoading(layout, whiteboardRect)
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

            it.init(getEduContext(), container, role, optionIconSize,
                optionRight + teacherVideoWidth,
                optionBottom, mode, this, handsUpPopup)
        }

        OptionsLayout.listener = object : OptionsLayoutListener {
            override fun onLeave() {
                showLeave()
            }

            override fun onKickout(userId: String, userName: String) {
                showKickDialog(userId)
                // getEduContext()?.userContext()?.kickOutUser(userId, false)
            }
        }

        //双击列表小窗回调，打开大窗
        AgoraUIVideoArt2.userInfoListener = object : IAgoraUserInfoListener2 {
            override fun onUserDoubleClicked(userDetailInfo: AgoraUIUserDetailInfo?) {
                this@AgoraUILectureHallArtContainer.userDetailInfo = userDetailInfo
                if (getEduContext()?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Teacher || getEduContext()?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Student) {//只有老师才能开启大窗
                    getActivity()?.runOnUiThread {
                        //发消息给widget
                        val packet = userDetailInfo?.let { AgoraLargeWindowInteractionPacket(AgoraLargeWindowInteractionSignal.LargeWindowShowed, it) }
                        this@AgoraUILectureHallArtContainer.getEduContext()?.widgetContext()?.sendMessageToWidget(
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
                this@AgoraUILectureHallArtContainer.userDetailInfo = userDetailInfo
                if (getEduContext()?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Teacher || getEduContext()?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Student) {
                    getActivity()?.runOnUiThread {
                        // 视频列表窗视频恢复
                        val packet = userDetailInfo?.let { AgoraLargeWindowInteractionPacket(AgoraLargeWindowInteractionSignal.LargeWindowClosed, it) }
                        this@AgoraUILectureHallArtContainer.getEduContext()?.widgetContext()?.sendMessageToWidget(
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

    private fun initValues(resources: Resources) {
        AgoraUIConfig.keepVideoListItemRatio = false
        val basePhone = 375f
        val baseTablet = 574f

        if (AgoraUIConfig.isLargeScreen) {
            statusBarHeight = (height * 20 / baseTablet).toInt()
            optionRight = (height * 6 / baseTablet).toInt()
            optionBottom = (height * 7 / baseTablet).toInt()
        } else {
            statusBarHeight = (height * 14 / basePhone).toInt()
            optionRight = (height * 6 / basePhone).toInt()
            optionBottom = (height * 7 / basePhone).toInt()
        }

        statusBarHeight = (this.width * statusBarPercent).toInt()
        margin = resources.getDimensionPixelSize(R.dimen.margin_smaller)
        border = resources.getDimensionPixelSize(R.dimen.stroke_small)
        optionIconSize = if (AgoraUIConfig.isLargeScreen)
            (height * 46 / baseTablet).toInt() else (height * 46 / basePhone).toInt()
        optionPopupRight = if (AgoraUIConfig.isLargeScreen)
            (height * 60 / baseTablet).toInt() else (height * 50 / basePhone).toInt()
    }

    override fun calculateComponentSize() {
        teacherVideoWidth = (teacherVideoWidthMaxRatio * width).toInt()
        teacherVideoHeight = (teacherVideoWidth * componentRatio).toInt()
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