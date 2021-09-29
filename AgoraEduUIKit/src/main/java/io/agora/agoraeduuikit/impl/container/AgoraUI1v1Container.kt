package io.agora.agoraeduuikit.impl.container

import android.content.res.Resources
import android.graphics.Rect
import android.view.ViewGroup
import io.agora.agoraeducontext.EduContextVideoMode
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.impl.chat.AgoraUIChatWidget
import io.agora.agoraeduuikit.impl.chat.ChatWidget
import io.agora.agoraeduuikit.impl.loading.AgoraUILoading
import io.agora.agoraeduuikit.impl.room.AgoraUIRoomStatus
import io.agora.agoraeduuikit.impl.screenshare.AgoraUIFullScreenBtn
import io.agora.agoraeduuikit.impl.screenshare.AgoraUIScreenShare
import io.agora.agoraeduuikit.impl.tool.AgoraUIToolBarBuilder
import io.agora.agoraeduuikit.impl.tool.AgoraUIToolType
import io.agora.agoraeduuikit.impl.video.AgoraUIVideoGroup
import io.agora.agoraeduuikit.impl.whiteboard.AgoraUIWhiteBoardBuilder
import io.agora.agoraeduuikit.impl.whiteboard.paging.AgoraUIPagingControlBuilder

class AgoraUI1v1Container(
        eduContext: io.agora.agoraeducore.core.context.EduContextPool?,
        configs: AgoraContainerConfig) : AbsUIContainer(eduContext, configs) {
    private val tag = "AgoraUI1v1Container"

    private var statusBarHeight = 0
    private var margin = 0
    private var componentMargin = 0
    private var shadow = 0
    private var border = 0

    private var width = 0
    private var height = 0
    private var top = 0
    private var left = 0

    private val chatRect = Rect()
    private val chatFullScreenRect = Rect()
    private val chatFullScreenHideRect = Rect()
    private val whiteboardRect = Rect()
    private val fullScreenRect = Rect()

    private var isFullScreen = false

    private val userHandler = object : io.agora.agoraeducontext.handlerimpl.UserHandler() {
        override fun onKickOut() {
            super.onKickOut()
            kickOut()
        }
    }

    override fun init(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int) {
        super.init(layout, left, top, width, height)

        this.width = width
        this.height = height
        this.left = left
        this.top = top

        initValues(layout.context.resources)
        layout.setBackgroundColor(layout.context.resources.getColor(R.color.theme_gray_lighter))

        roomStatus = AgoraUIRoomStatus(layout, getEduContext(), width, statusBarHeight, left, top)
        roomStatus!!.setContainer(this)

        calculateVideoSize()
        val videoLayoutW = AgoraUIConfig.OneToOneClass.teacherVideoWidth
        val videoLayoutH = height - statusBarHeight - margin - border
        val leftMargin = width - videoLayoutW - border
        val topMargin = statusBarHeight + margin
        teacherVideoWindow = AgoraUIVideoGroup(layout.context, getEduContext(),
                layout, leftMargin, topMargin, videoLayoutW,
                videoLayoutH, margin, EduContextVideoMode.Pair)
        teacherVideoWindow!!.setContainer(this)

        val whiteboardW = width - videoLayoutW - margin - border * 2
        val whiteboardH = height - statusBarHeight - margin - border
        screenShareWindow = AgoraUIScreenShare(layout.context,
                getEduContext(), layout,
                whiteboardW, whiteboardH, border,
                statusBarHeight + margin, 0f)
        screenShareWindow!!.setContainer(this)

        whiteboardRect.set(border, statusBarHeight + margin,
                border + whiteboardW, height - border)
        whiteboardWindow = AgoraUIWhiteBoardBuilder(layout.context, getEduContext(), layout)
                .width(whiteboardW)
                .height(whiteboardH)
                .left(border.toFloat())
                .top(statusBarHeight + margin.toFloat())
                .shadowWidth(0f).build()
        whiteboardWindow!!.setContainer(this)
        fullScreenRect.set(border, statusBarHeight + margin, width - border, height - border)

        val pagingControlHeight = layout.context.resources.getDimensionPixelSize(R.dimen.agora_paging_control_height)
        val pagingControlLeft = componentMargin
        val pagingControlTop = height - pagingControlHeight - border - componentMargin
        pageControlWindow = AgoraUIPagingControlBuilder(layout.context, getEduContext(), layout)
                .height(pagingControlHeight)
                .left(pagingControlLeft.toFloat())
                .top(pagingControlTop.toFloat())
                .shadowWidth(shadow.toFloat())
                .build()
        pageControlWindow!!.setContainer(this)

        val fullScreenBtnWidth = layout.context.resources.getDimensionPixelSize(R.dimen.full_screen_btn_size)
        val fullScreenBtnHeight = layout.context.resources.getDimensionPixelSize(R.dimen.full_screen_btn_size)
        val fullScreenBtnLeft = 0
        val fullScreenBtnTop = height - fullScreenBtnHeight - border
        fullScreenBtn = AgoraUIFullScreenBtn(layout.context, getEduContext(), layout,
                fullScreenBtnWidth, fullScreenBtnHeight, fullScreenBtnLeft, fullScreenBtnTop)
        fullScreenBtn!!.setContainer(this)

        toolbar = AgoraUIToolBarBuilder(layout.context, getEduContext(), layout)
                .foldTop(whiteboardRect.top + componentMargin)
                .unfoldTop(whiteboardRect.top + componentMargin)
                .unfoldLeft(componentMargin)
                .unfoldHeight(pagingControlTop - whiteboardRect.top - componentMargin)
                .shadowWidth(shadow)
                .build()
        toolbar!!.setToolbarType(AgoraUIToolType.Whiteboard)
        toolbar!!.setContainer(this)

        val messageLeft = width - videoLayoutW - videoLayoutW - componentMargin
        val messageTop: Int
        val messageHeight: Int
        // chat window height matches the height of whiteboard with content margins on phones
        // while it is 60% of layout height on tablets
        if (AgoraUIConfig.isLargeScreen) {
            messageHeight = (height * AgoraUIConfig.chatHeightLargeScreenRatio).toInt()
            messageTop = height - componentMargin - messageHeight
        } else {
            messageTop = whiteboardRect.top + componentMargin
            messageHeight = height - componentMargin - messageTop
        }
        chatRect.set(messageLeft, messageTop, messageLeft + videoLayoutW, messageTop + messageHeight)

        val chatFullScreenRight = width - border - componentMargin
        val chatFullScreenBottom = height - componentMargin
        chatFullScreenRect.set(width - videoLayoutW - margin, messageTop, chatFullScreenRight, chatFullScreenBottom)

        // add loading(show/hide follow rtmConnectionState)
        agoraUILoading = AgoraUILoading(layout, whiteboardRect)

        // ease chat window
        val easeChatW = videoLayoutW
        val easeChatH = messageHeight
        val easeChatTop = messageTop
        val easeChatLeft = messageLeft
        chat = widgetManager.create(io.agora.agoraeduwidget.UiWidgetManager.DefaultWidgetId.Chat.name, getEduContext()) as? ChatWidget
        chat?.let {
            if (it is AgoraUIChatWidget) {
                it.setTabConfig(config.chatTabConfigs)
            }
            it.init(layout, easeChatW, easeChatH, easeChatTop, easeChatLeft)
            it.setContainer(this)
            it.show(false)
        }
        val chatFullScreenHideTop = chatFullScreenBottom - (chat?.hideIconSize ?: 0)
        val chatFullScreenHideLeft = chatFullScreenRight - (chat?.hideIconSize ?: 0)
        chatFullScreenHideRect.set(chatFullScreenHideLeft, chatFullScreenHideTop, chatFullScreenRight, chatFullScreenBottom)
        // ease chat window

        getEduContext()?.userContext()?.addHandler(userHandler)
    }

    override fun resize(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int) {
        super.resize(layout, left, top, width, height)
        this.width = width
        this.height = height
        this.left = left
        this.top = top

        var rect: Rect

        roomStatus?.let {
            rect = Rect(left, top, width + left, statusBarHeight + top)
            it.setRect(rect)
        }

        calculateVideoSize()
        val videoLayoutW = AgoraUIConfig.OneToOneClass.teacherVideoWidth
        val videoLayoutH = height - statusBarHeight - margin - border
        val leftMargin = width - videoLayoutW - border
        val topMargin = statusBarHeight + margin
        teacherVideoWindow?.let {
            rect = Rect(leftMargin, topMargin, videoLayoutW + leftMargin, videoLayoutH + topMargin)
            it.setRect(rect)
        }

        val whiteboardW = width - videoLayoutW - margin - border * 2
        val whiteboardH = height - statusBarHeight - margin - border
        screenShareWindow?.let {
            rect = Rect(border, statusBarHeight + margin, whiteboardW + border,
                    whiteboardH + statusBarHeight + margin)
            it.setRect(rect)
        }

        whiteboardRect.set(border, statusBarHeight + margin,
                border + whiteboardW, height - border)
        whiteboardWindow?.let {
            rect = Rect(border, statusBarHeight + margin, whiteboardW, whiteboardH + statusBarHeight + margin)
            it.setRect(rect)
        }
        fullScreenRect.set(border, statusBarHeight + margin, width - border, height - border)

        val pagingControlHeight = layout.context.resources.getDimensionPixelSize(R.dimen.agora_paging_control_height)
        val pagingControlLeft = componentMargin
        val pagingControlTop = height - pagingControlHeight - border - componentMargin
        pageControlWindow?.let {
            rect = Rect(pagingControlLeft, pagingControlTop, 0, pagingControlHeight + pagingControlTop)
            it.setRect(rect)
        }

        val fullScreenBtnWidth = layout.context.resources.getDimensionPixelSize(R.dimen.full_screen_btn_size)
        val fullScreenBtnHeight = layout.context.resources.getDimensionPixelSize(R.dimen.full_screen_btn_size)
        val fullScreenBtnLeft = 0
        val fullScreenBtnTop = height - fullScreenBtnHeight - border
        fullScreenBtn?.let {
            rect = Rect(fullScreenBtnLeft, fullScreenBtnTop, fullScreenBtnLeft + fullScreenBtnWidth,
                    fullScreenBtnTop + fullScreenBtnHeight)
            it.setRect(rect)
        }

//        toolbar?.let {
//            rect = Rect(0, 0, 0, pagingControlTop - whiteboardRect.top - componentMargin)
//            it.setRect(rect)
//        }

        val messageLeft = width - videoLayoutW - videoLayoutW - componentMargin
        val messageTop: Int
        val messageHeight: Int
        // chat window height matches the height of whiteboard with content margins on phones
        // while it is 60% of layout height on tablets
        if (AgoraUIConfig.isLargeScreen) {
            messageHeight = (height * AgoraUIConfig.chatHeightLargeScreenRatio).toInt()
            messageTop = height - componentMargin - messageHeight
        } else {
            messageTop = whiteboardRect.top + componentMargin
            messageHeight = height - componentMargin - messageTop
        }
        chatRect.set(messageLeft, messageTop, messageLeft + videoLayoutW, messageTop + messageHeight)

        val chatFullScreenRight = width - border - componentMargin
        val chatFullScreenBottom = height - componentMargin
        chatFullScreenRect.set(width - videoLayoutW - margin, messageTop, chatFullScreenRight, chatFullScreenBottom)
        val chatFullScreenHideTop = chatFullScreenBottom - (chat?.hideIconSize ?: 0)
        val chatFullScreenHideLeft = chatFullScreenRight - (chat?.hideIconSize ?: 0)
        chatFullScreenHideRect.set(chatFullScreenHideLeft, chatFullScreenHideTop, chatFullScreenRight, chatFullScreenBottom)

        if (isFullScreen) {
            whiteboardWindow?.setRect(fullScreenRect)
            screenShareWindow?.setRect(fullScreenRect)
            chat?.let {
                it.setFullscreenRect(true, chatFullScreenHideRect)
                it.setFullDisplayRect(chatFullScreenRect)
                it.show(it.isShowing())
                it.setRect(if (it.isShowing()) chatFullScreenRect else chatFullScreenHideRect)
            }
            agoraUILoading?.setRect(fullScreenRect)
        } else {
            whiteboardWindow?.setRect(whiteboardRect)
            screenShareWindow?.setRect(whiteboardRect)
            chat?.let {
//                it.setFullscreenRect(false, chatRect)
                it.setFullDisplayRect(chatRect)
                it.show(it.isShowing())
//                it.setRect(chatRect)
            }
            agoraUILoading?.setRect(whiteboardRect)
        }
    }

    private fun initValues(resources: Resources) {
        statusBarHeight = resources.getDimensionPixelSize(R.dimen.agora_status_bar_height)
        margin = resources.getDimensionPixelSize(R.dimen.margin_smaller)
        shadow = resources.getDimensionPixelSize(R.dimen.shadow_width)
        componentMargin = resources.getDimensionPixelSize(R.dimen.margin_medium)
        border = resources.getDimensionPixelSize(R.dimen.stroke_small)
    }

    override fun setFullScreen(fullScreen: Boolean) {
        if (isFullScreen == fullScreen) {
            return
        }

        isFullScreen = fullScreen
        handleFullScreen(fullScreen)
    }

    private fun handleFullScreen(fullScreen: Boolean) {
        if (fullScreen) {
            whiteboardWindow?.setRect(fullScreenRect)
            screenShareWindow?.setRect(fullScreenRect)
            chat?.setFullscreenRect(fullScreen, chatFullScreenHideRect)
            chat?.setFullDisplayRect(chatFullScreenRect)
            chat?.show(false)
            agoraUILoading?.setRect(fullScreenRect)
        } else {
            whiteboardWindow?.setRect(whiteboardRect)
            screenShareWindow?.setRect(whiteboardRect)
            // easeChat?.setFullscreenRect(fullScreen, chatRect)
            chat?.setFullDisplayRect(chatRect)
            chat?.show(false)
            agoraUILoading?.setRect(whiteboardRect)
        }
    }

    override fun calculateVideoSize() {
        AgoraUIConfig.OneToOneClass.teacherVideoWidth =
                minOf((AgoraUIConfig.videoWidthMaxRatio * width).toInt(), AgoraUIConfig.OneToOneClass.teacherVideoWidth)
    }

    override fun willLaunchExtApp(appIdentifier: String): Int {
        return 0
    }

    override fun getWhiteboardContainer(): ViewGroup? {
        return whiteboardWindow?.getWhiteboardContainer()
    }
}