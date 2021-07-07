package io.agora.uikit.impl.container

import android.content.res.Resources
import android.graphics.Rect
import android.view.ViewGroup
import io.agora.educontext.EduContextPool
import io.agora.educontext.EduContextVideoMode
import io.agora.uicomponent.UiWidgetManager
import io.agora.uikit.R
import io.agora.uikit.impl.chat.AgoraUIChatWindow
import io.agora.uikit.impl.chat.EaseChatWidget
import io.agora.uikit.impl.loading.AgoraUILoading
import io.agora.uikit.impl.room.AgoraUIRoomStatus
import io.agora.uikit.impl.screenshare.AgoraUIFullScreenBtn
import io.agora.uikit.impl.screenshare.AgoraUIScreenShare
import io.agora.uikit.impl.tool.AgoraUIToolBarBuilder
import io.agora.uikit.impl.tool.AgoraUIToolType
import io.agora.uikit.impl.video.AgoraUIVideoGroup
import io.agora.uikit.impl.whiteboard.AgoraUIWhiteBoardBuilder
import io.agora.uikit.impl.whiteboard.paging.AgoraUIPagingControlBuilder

class AgoraUI1v1Container(
        eduContext: EduContextPool?,
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

    private val widgetManager = UiWidgetManager()

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
        videoGroupWindow = AgoraUIVideoGroup(layout.context, getEduContext(),
                layout, leftMargin, topMargin, videoLayoutW,
                videoLayoutH, margin, EduContextVideoMode.Pair)
        videoGroupWindow!!.setContainer(this)

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

        chatWindow = widgetManager.create(UiWidgetManager.DefaultWidgetId.Chat.name, getEduContext()) as? AgoraUIChatWindow
        chatWindow?.setTabConfig(config.chatTabConfigs)
        chatWindow?.init(layout, videoLayoutW, messageHeight, messageLeft, messageTop)
        chatWindow?.setContainer(this)
        chatWindow?.show(false)

        val chatFullScreenRight = width - border - componentMargin
        val chatFullScreenBottom = height - componentMargin
        chatFullScreenRect.set(width - videoLayoutW - margin, messageTop, chatFullScreenRight, chatFullScreenBottom)
        val chatFullScreenHideTop = chatFullScreenBottom - (chatWindow?.hideIconSize ?: 0)
        val chatFullScreenHideLeft = chatFullScreenRight - (chatWindow?.hideIconSize ?: 0)
        chatFullScreenHideRect.set(chatFullScreenHideLeft, chatFullScreenHideTop, chatFullScreenRight, chatFullScreenBottom)

        // add loading(show/hide follow rtmConnectionState)
        agoraUILoading = AgoraUILoading(layout, whiteboardRect)

        // ease chat window
        val easeChatW = whiteboardW
        val easeChatH = whiteboardH
        val easeChatTop = statusBarHeight + margin
        val easeChatLeft = border
        easeChat = widgetManager.create(UiWidgetManager.DefaultWidgetId.HyphenateChat.name, getEduContext()) as? EaseChatWidget
        easeChat?.init(layout, easeChatW, easeChatH, easeChatTop, easeChatLeft)
        easeChat?.setContainer(this)
        // ease chat window

        // joinRoom
        getEduContext()?.roomContext()?.joinClassRoom()

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
        videoGroupWindow?.let {
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
        val chatFullScreenHideTop = chatFullScreenBottom - (chatWindow?.hideIconSize ?: 0)
        val chatFullScreenHideLeft = chatFullScreenRight - (chatWindow?.hideIconSize ?: 0)
        chatFullScreenHideRect.set(chatFullScreenHideLeft, chatFullScreenHideTop, chatFullScreenRight, chatFullScreenBottom)

        if (isFullScreen) {
            whiteboardWindow?.setRect(fullScreenRect)
            screenShareWindow?.setRect(fullScreenRect)
            chatWindow?.let {
//                it.setFullscreenRect(isFullScreen, if (it.isShowing()) chatFullScreenRect else chatFullScreenHideRect)
                it.setFullDisplayRect(chatFullScreenRect)
                it.show(it.isShowing())
                it.setRect(if (it.isShowing()) chatFullScreenRect else chatFullScreenHideRect)
            }
            agoraUILoading?.setRect(fullScreenRect)
        } else {
            whiteboardWindow?.setRect(whiteboardRect)
            screenShareWindow?.setRect(whiteboardRect)
            chatWindow?.let {
                // it?.setFullscreenRect(fullScreen, chatRect)
                it.setFullDisplayRect(chatRect)
                it.show(it.isShowing())
                it.setRect(chatRect)
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
            chatWindow?.setFullscreenRect(fullScreen, chatFullScreenHideRect)
            chatWindow?.setFullDisplayRect(chatFullScreenRect)
            chatWindow?.show(false)
            agoraUILoading?.setRect(fullScreenRect)
        } else {
            whiteboardWindow?.setRect(whiteboardRect)
            screenShareWindow?.setRect(whiteboardRect)
            // chatWindow?.setFullscreenRect(fullScreen, chatRect)
            chatWindow?.setFullDisplayRect(chatRect)
            chatWindow?.show(false)
            agoraUILoading?.setRect(whiteboardRect)
        }
    }

    override fun calculateVideoSize() {
        AgoraUIConfig.OneToOneClass.teacherVideoWidth =
                minOf((AgoraUIConfig.videoWidthMaxRatio * width).toInt(), AgoraUIConfig.OneToOneClass.teacherVideoWidth)
    }

    override fun release() {
        chatWindow?.release()
        widgetManager.release()
        easeChat?.release()
    }

    override fun willLaunchExtApp(appIdentifier: String): Int {
        return 0
    }
}