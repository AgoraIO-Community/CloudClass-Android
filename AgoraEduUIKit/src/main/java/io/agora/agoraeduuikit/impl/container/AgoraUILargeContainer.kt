package io.agora.agoraeduuikit.impl.container

import android.content.res.Resources
import android.graphics.Rect
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.RelativeLayout
import io.agora.agoraeduuikit.impl.options.OptionLayoutMode
import io.agora.agoraeduuikit.impl.options.OptionsLayout
import io.agora.agoraeduuikit.impl.options.OptionsLayoutListener
import io.agora.agoraeduuikit.R
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetDefaultId
import io.agora.agoraeduuikit.impl.users.AgoraUserListVideoLayout
import io.agora.agoraeduuikit.impl.loading.AgoraUILoading
import io.agora.agoraeduuikit.impl.room.AgoraUIRoomStatusArt
import io.agora.agoraeduuikit.impl.screenshare.AgoraUIScreenShare
import io.agora.agoraeduuikit.impl.video.AgoraUIVideoGroup
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetDefaultId.Chat
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeduuikit.provider.UIDataProviderListenerImpl
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeduuikit.impl.chat.ChatWidget
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.LargeClass.coHostMaxItem
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.LargeClass.componentRatio
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.LargeClass.statusBarPercent
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.LargeClass.teacherVideoHeight
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.LargeClass.teacherVideoWidth
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.LargeClass.teacherVideoWidthMaxRatio
import io.agora.agoraeduuikit.impl.users.AgoraUIHandsUpToastPopUp

class AgoraUILargeClassContainer(
    eduContext: EduContextPool?,
    configs: AgoraContainerConfig) : AbsUIContainer(eduContext, configs) {
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

    private val uiDataProviderListener = object : UIDataProviderListenerImpl() {
        override fun onCoHostListChanged(userList: List<AgoraUIUserDetailInfo>) {
            super.onCoHostListChanged(userList)
            onCoHostListUpdated(userList)
        }
    }

    private fun onCoHostListUpdated(list: List<AgoraUIUserDetailInfo>) {
        val hasCoHost = list.isNotEmpty()
        studentsVideoWindow?.show(hasCoHost)
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

            this@AgoraUILargeClassContainer.layout()?.let {
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
                        this@AgoraUILargeClassContainer.width = layout.width
                        this@AgoraUILargeClassContainer.height = layout.height
                        this@AgoraUILargeClassContainer.left = left
                        this@AgoraUILargeClassContainer.top = top
                        initLayout(layout,
                            this@AgoraUILargeClassContainer.left,
                            this@AgoraUILargeClassContainer.top,
                            this@AgoraUILargeClassContainer.width,
                            this@AgoraUILargeClassContainer.height)
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
        teacherVideoWindow = AgoraUIVideoGroup(layout.context, getEduContext(), layout,
            teacherVideoLeft, teacherVideoTop, teacherVideoW,
            teacherVideoH, 0, EduContextVideoMode.Single)
        teacherVideoWindow!!.setContainer(this)

        val whiteboardW = width - teacherVideoW - margin
        val whiteboardH = (whiteboardW * componentRatio).toInt()

        // Rect when student video list is shown
        whiteboardRect.set(0, height - whiteboardH, whiteboardW, height)
        whiteboardContainer = LinearLayout(getContext())
        layout.addView(whiteboardContainer)
        val params = whiteboardContainer!!.layoutParams as ViewGroup.MarginLayoutParams
        params.width = whiteboardW
        params.height = whiteboardH
        params.topMargin = whiteboardRect.top
        whiteboardContainer!!.layoutParams = params

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
        studentsVideoWindow = AgoraUserListVideoLayout(layout.context, getEduContext(), studentContainer,
            ViewGroup.LayoutParams.WRAP_CONTENT, studentVideoHeight, 0, 0, 0f, margin)
        studentsVideoWindow!!.setContainer(this)
        studentsVideoWindow!!.show(false)

        val chatTop = teacherVideoTop + teacherVideoH + margin
        val chatRight = teacherVideoLeft + teacherVideoW
        chatRect.set(teacherVideoLeft, chatTop, chatRight, height)

        //register uiDataProviderListener for components
        teacherVideoWindow?.let {
            uiDataProvider?.addListener(it.uiDataProviderListener)
        }
        studentsVideoWindow?.let {
            uiDataProvider?.addListener(it.uiDataProviderListener)
        }

        initOptionLayout(layout)

        // add loading(show/hide follow rtmConnectionState)
        agoraUILoading = AgoraUILoading(layout, whiteboardRect)
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
                // getEduContext()?.userContext()?.kickOutUser(userId, false)
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
}