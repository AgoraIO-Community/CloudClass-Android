package io.agora.agoraeduuikit.impl.container

import android.content.res.Resources
import android.graphics.Rect
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
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeduuikit.impl.loading.AgoraUILoading
import io.agora.agoraeduuikit.impl.options.*
import io.agora.agoraeduuikit.impl.room.AgoraUIRoomStatusOne2One
import io.agora.agoraeduuikit.impl.screenshare.AgoraUIScreenShare
import io.agora.agoraeduuikit.impl.users.AgoraUIHandsUpToastPopUp
import io.agora.agoraeduuikit.impl.video.AgoraUIVideoGroupWithChat
import io.agora.agoraeduuikit.impl.video.AgoraUIVideoGroupWithChatPad

class AgoraUI1v1Container(
        eduContext: EduContextPool?,
        configs: AgoraContainerConfig) : AbsUIContainer(eduContext, configs) {
    private val tag = "AgoraUI1v1Container"

    private var statusBarHeight = 0
    private var margin = 0
    private var border = 0

    private var width = 0
    private var height = 0
    private var top = 0
    private var left = 0

    private var teacherVideoWidth = 0
    private val whiteboardRect = Rect()
    private var handsUpPopup: AgoraUIHandsUpToastPopUp? = null
    private var optionLayout: OptionsLayout? = null
    private var optionRight = 0
    private var optionBottom = 0
    private var optionIconSize = 0
    private var optionPopupRight = 0
    private var teacherVideoWindowPhone: AgoraUIVideoGroupWithChat? = null
    private var teacherVideoWindowPad: AgoraUIVideoGroupWithChatPad? = null
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
            layout()?.let {
                initOptionLayout(it)
                // add loading(show/hide follow rtmConnectionState)
                agoraUILoading = AgoraUILoading(it, whiteboardRect)
            }
            // Check if there is a screen stream is sharing
            uiDataProvider?.notifyScreenShareDisplay()
        }
    }

    init {
        // register handler
        getEduContext()?.roomContext()?.addHandler(roomHandler)
    }

    override fun init(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int) {
        super.init(layout, left, top, width, height)

        layout.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (layout.width > 0 && layout.height > 0) {
                        layout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        this@AgoraUI1v1Container.width = layout.width
                        this@AgoraUI1v1Container.height = layout.height
                        this@AgoraUI1v1Container.left = left
                        this@AgoraUI1v1Container.top = top
                        initLayout(layout,
                            this@AgoraUI1v1Container.left,
                            this@AgoraUI1v1Container.top,
                            this@AgoraUI1v1Container.width,
                            this@AgoraUI1v1Container.height)
                    }
                }
            })
    }

    private fun initLayout(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int) {
        initValues(layout.context.resources)
        layout.setBackgroundColor(layout.context.resources.getColor(R.color.theme_gray_lighter))
        roomStatusOne2One = AgoraUIRoomStatusOne2One(layout, getEduContext(), width, statusBarHeight, left, top)
        roomStatusOne2One!!.setContainer(this)
        calculateComponentSize()

        teacherVideoWidth = AgoraUIConfig.OneToOneClass.teacherVideoWidth
        val videoLayoutH = height - statusBarHeight - margin - border
        val leftMargin = width - teacherVideoWidth - border
        val topMargin = statusBarHeight + margin
        if (AgoraUIConfig.isLargeScreen) {
            teacherVideoWindowPad = AgoraUIVideoGroupWithChatPad(layout.context, getEduContext(),
                layout, leftMargin, topMargin, teacherVideoWidth,
                videoLayoutH, margin, EduContextVideoMode.Pair, uiDataProvider)
            teacherVideoWindowPad?.setContainer(this)
        } else {
            teacherVideoWindowPhone = AgoraUIVideoGroupWithChat(layout.context, getEduContext(),
                layout, leftMargin, topMargin, teacherVideoWidth,
                videoLayoutH, margin, EduContextVideoMode.Pair, uiDataProvider)
            teacherVideoWindowPhone?.setContainer(this)
        }
        val whiteboardW = width - teacherVideoWidth - margin - border * 2
        val whiteboardH = height - statusBarHeight - margin - border
        whiteboardRect.set(border, height - whiteboardH, whiteboardW, height)
        whiteboardContainer = LinearLayout(getContext())
        layout.addView(whiteboardContainer)
        val params = whiteboardContainer!!.layoutParams as ViewGroup.MarginLayoutParams
        params.width = whiteboardW
        params.height = whiteboardH
        params.topMargin = whiteboardRect.top
        whiteboardContainer!!.layoutParams = params

        screenShareWindow = AgoraUIScreenShare(layout.context,
            getEduContext(), layout,
            whiteboardW, whiteboardH, border,
            statusBarHeight + margin, 0f)
        screenShareWindow!!.setContainer(this)

        // register uiDataListener
        teacherVideoWindow?.let {
            uiDataProvider?.addListener(it.uiDataProviderListener)
        }
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
                optionRight + teacherVideoWidth + margin,
                optionBottom, mode, this, handsUpPopup)
        }

        OptionsLayout.listener = object : OptionsLayoutListener {
            override fun onLeave() {
                showLeave()
            }

            override fun onKickout(userId: String, userName: String) {
                // 1v1 no kickout
            }
        }
    }

    private fun initValues(resources: Resources) {
        val basePhone = 375f
        val baseTablet = 574f

        if (AgoraUIConfig.isLargeScreen) {
            statusBarHeight = (height * 24 / baseTablet).toInt()
            optionRight = (height * 6 / baseTablet).toInt()
            optionBottom = (height * 7 / baseTablet).toInt()
        } else {
            statusBarHeight = (height * 23 / basePhone).toInt()
            optionRight = (height * 6 / basePhone).toInt()
            optionBottom = (height * 7 / basePhone).toInt()
        }

        border = resources.getDimensionPixelSize(R.dimen.stroke_small)
        margin = resources.getDimensionPixelSize(R.dimen.margin_smaller)
        optionIconSize = if (AgoraUIConfig.isLargeScreen)
            (height * 46 / baseTablet).toInt() else (height * 46 / basePhone).toInt()
        optionPopupRight = if (AgoraUIConfig.isLargeScreen)
            (height * 60 / baseTablet).toInt() else (height * 50 / basePhone).toInt()
    }

    override fun setFullScreen(fullScreen: Boolean) {

    }

    override fun calculateComponentSize() {
        AgoraUIConfig.OneToOneClass.teacherVideoWidth =
                minOf((AgoraUIConfig.videoWidthMaxRatio * width).toInt(), AgoraUIConfig.OneToOneClass.teacherVideoWidth)
    }

    override fun willLaunchExtApp(appIdentifier: String): Int {
        return 0
    }
}