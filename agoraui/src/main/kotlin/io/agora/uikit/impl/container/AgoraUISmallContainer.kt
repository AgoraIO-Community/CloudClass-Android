package io.agora.uikit.impl.container

import android.content.res.Resources
import android.graphics.Rect
import android.view.ViewGroup
import io.agora.educontext.EduContextPool
import io.agora.educontext.EduContextUserDetailInfo
import io.agora.uicomponent.UiWidgetManager
import io.agora.uikit.R
import io.agora.uikit.educontext.handlers.UserHandler
import io.agora.uikit.educontext.handlers.VideoHandler
import io.agora.uikit.impl.AgoraUIVideoList
import io.agora.uikit.impl.chat.AgoraUIChatWindow
import io.agora.uikit.impl.chat.EaseChatWidget
import io.agora.uikit.impl.chat.OnChatWindowAnimateListener
import io.agora.uikit.impl.chat.OnEaseChatWidgetAnimateListener
import io.agora.uikit.impl.handsup.AgoraUIHandsUp
import io.agora.uikit.impl.loading.AgoraUILoading
import io.agora.uikit.impl.room.AgoraUIRoomStatus
import io.agora.uikit.impl.screenshare.AgoraUIFullScreenBtn
import io.agora.uikit.impl.screenshare.AgoraUIScreenShare
import io.agora.uikit.impl.tool.AgoraUIToolBarBuilder
import io.agora.uikit.impl.tool.AgoraUIToolType
import io.agora.uikit.impl.users.*
import io.agora.uikit.impl.whiteboard.AgoraUIWhiteBoardBuilder
import io.agora.uikit.impl.whiteboard.paging.AgoraUIPagingControlBuilder

class AgoraUISmallClassContainer(
        eduContext: EduContextPool?,
        configs: AgoraContainerConfig) : AbsUIContainer(eduContext, configs) {
    private val tag = "AgoraUISmallClassContainer"

    private var statusBarHeight = 0

    // margin for tool and page control
    private var componentMargin = 0

    private var margin = 0
    private var shadow = 0
    private var border = 0

    private var width = 0
    private var height = 0
    private var top = 0
    private var left = 0

    private var videoListWindow: AgoraUIVideoList? = null

    private val chatRect = Rect()
    private val chatFullScreenRect = Rect()
    private val chatFullScreenHideRect = Rect()
    private val whiteboardDefaultRect = Rect()
    private val whiteboardFullScreenRect = Rect()
    private val whiteboardNoVideoRect = Rect()
    private val handsUpRect = Rect()
    private val handsUpFullScreenRect = Rect()
    private var handsUpAnimateRect = Rect()

    private var toolbarTopNoVideo = 0
    private var toolbarTopHasVideo = 0
    private var toolbarHeightNoVideo = 0
    private var toolbarHeightHasVideo = 0

    private var isFullScreen = false

    private val widgetManager = UiWidgetManager()

    private var teacherDetailInfo: EduContextUserDetailInfo? = null
    private var coHostList: MutableList<EduContextUserDetailInfo> = mutableListOf()
    private val smallContainerTeacherVideoHandler = object : VideoHandler() {
        override fun onUserDetailInfoUpdated(info: EduContextUserDetailInfo) {
            super.onUserDetailInfoUpdated(info)
            teacherDetailInfo = info
            notifyVideos()
        }
    }
    private val smallContainerUserHandler = object : UserHandler() {
        override fun onCoHostListUpdated(list: MutableList<EduContextUserDetailInfo>) {
            super.onCoHostListUpdated(list)
            coHostList = list
            notifyVideos()
        }

        override fun onKickOut() {
            super.onKickOut()
            kickOut()
        }
    }

    private fun notifyVideos() {
        val hasTeacher = teacherDetailInfo?.onLine == true
        videoListWindow?.showTeacher(hasTeacher)
        val hasCoHost = hasTeacher || coHostList.size > 0
        videoListWindow?.showStudents(hasCoHost)
        if (isFullScreen) {
            return
        }
        if (hasCoHost) {
            whiteboardWindow?.setRect(whiteboardDefaultRect)
            screenShareWindow?.setRect(whiteboardDefaultRect)
            toolbar?.setVerticalPosition(toolbarTopHasVideo, toolbarHeightHasVideo)
            agoraUILoading?.setRect(whiteboardDefaultRect)
        } else {
            whiteboardWindow?.setRect(whiteboardNoVideoRect)
            screenShareWindow?.setRect(whiteboardNoVideoRect)
            toolbar?.setVerticalPosition(toolbarTopNoVideo, toolbarHeightNoVideo)
            agoraUILoading?.setRect(whiteboardNoVideoRect)
        }
    }

    override fun init(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int) {
        super.init(layout, left, top, width, height)

        this.width = width
        this.height = height
        this.left = left
        this.top = top

        initValues(layout.context.resources)

        roomStatus = AgoraUIRoomStatus(layout, getEduContext(), width, statusBarHeight, left, top)
        roomStatus!!.setContainer(this)

        calculateVideoSize()
        if (getContext() == null) {
            return
        }

        videoListWindow = AgoraUIVideoList(layout.context, getEduContext(), layout, 0,
                statusBarHeight, ViewGroup.LayoutParams.MATCH_PARENT,
                AgoraUIConfig.SmallClass.teacherVideoHeight + margin * 2, margin, border)
        val videosContainerTop = videoListWindow?.getVideosContainerTop() ?: 0
        val videosContainerH = videoListWindow?.getVideosContainerH() ?: 0

        val whiteboardW = width - border * 2
        val whiteboardH = height - statusBarHeight - border * 2
        screenShareWindow = AgoraUIScreenShare(layout.context,
                getEduContext(), layout,
                whiteboardW, whiteboardH, border, statusBarHeight + margin, 0f)
        screenShareWindow!!.setContainer(this)

        // Rect when student video list is shown
        whiteboardDefaultRect.set(border, videosContainerTop + videosContainerH,
                whiteboardW, height - border)
        whiteboardNoVideoRect.set(border, statusBarHeight + border, whiteboardW, height - border)
        whiteboardFullScreenRect.set(border, statusBarHeight + border, whiteboardW, height - border)
        whiteboardWindow = AgoraUIWhiteBoardBuilder(layout.context, getEduContext(), layout)
                .width(whiteboardW)
                .height(whiteboardH)
                .left(border.toFloat())
                .top(statusBarHeight + border.toFloat())
                .shadowWidth(0f).build()
        whiteboardWindow!!.setContainer(this)

        val pagingControlHeight = layout.context.resources.getDimensionPixelSize(R.dimen.agora_paging_control_height)
        val pagingControlLeft = componentMargin
        val pagingControlTop = height - pagingControlHeight - margin - componentMargin
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
        val fullScreenBtnTop = height - fullScreenBtnHeight - margin
        fullScreenBtn = AgoraUIFullScreenBtn(layout.context, getEduContext(), layout,
                fullScreenBtnWidth, fullScreenBtnHeight, fullScreenBtnLeft, fullScreenBtnTop)
        fullScreenBtn!!.setContainer(this)

        toolbarTopNoVideo = whiteboardNoVideoRect.top + componentMargin
        toolbarTopHasVideo = whiteboardDefaultRect.top + componentMargin
        toolbarHeightNoVideo = pagingControlTop - componentMargin - toolbarTopNoVideo
        toolbarHeightHasVideo = pagingControlTop - componentMargin - toolbarTopHasVideo
        toolbar = AgoraUIToolBarBuilder(layout.context, getEduContext(), layout)
                .foldTop(toolbarTopNoVideo)
                .unfoldTop(toolbarTopNoVideo)
                .unfoldLeft(margin + componentMargin)
                .unfoldHeight(toolbarHeightNoVideo)
                .shadowWidth(shadow)
                .build()
        toolbar!!.setToolbarType(AgoraUIToolType.All)
        toolbar!!.setContainer(this)
        toolbar?.setVerticalPosition(toolbarTopNoVideo, toolbarHeightNoVideo)

        val chatWidth = (AgoraUIConfig.SmallClass.chatWidthMaxRatio * width).toInt()
        val chatHeight = height - border - margin - videosContainerH - statusBarHeight
        val chatLeft = width - border - chatWidth
        val chatTop = statusBarHeight + videosContainerH
        chatRect.set(chatLeft, chatTop, chatLeft + chatWidth, chatTop + chatHeight)

        // chat window is larger when whiteboard is full screen
        val chatFullScreenLeft = chatLeft
        val chatFullScreenTop: Int = chatTop
        val chatFullScreenRight = chatLeft + chatWidth
        val chatFullScreenBottom = chatTop + chatHeight
        chatFullScreenRect.set(chatFullScreenLeft, chatFullScreenTop, chatFullScreenRight,
                chatFullScreenBottom)

        // add loading(show/hide follow rtmConnectionState)
        agoraUILoading = AgoraUILoading(layout, whiteboardDefaultRect)

        // ease chat window
        val easeChatW = chatWidth
        val easeChatH = chatHeight
        val easeChatTop = chatTop
        val easeChatLeft = chatLeft
        easeChat = widgetManager.create(UiWidgetManager.DefaultWidgetId.HyphenateChat.name, getEduContext()) as? EaseChatWidget
        easeChat?.init(layout, easeChatW, easeChatH, easeChatTop, easeChatLeft)
        easeChat?.setContainer(this)
        easeChat?.setAnimateListener(object : OnEaseChatWidgetAnimateListener {
            private var lastLeft = 0
            override fun onChatWindowAnimate(enlarge: Boolean, fraction: Float, left: Int, top: Int, width: Int, height: Int) {
                if (fraction.compareTo(0) == 0) lastLeft = left

                val chatWindowWidth = chatFullScreenRight - chatFullScreenLeft
                val diff = left - lastLeft
                lastLeft = left

                if (chatWindowWidth - left <= easeChat!!.hideIconSize) {
                    if (!enlarge) {
                        val rect = Rect(chatFullScreenHideRect.left - (handsUpRect.right - handsUpRect.left) - componentMargin,
                                handsUpRect.top, chatFullScreenHideRect.left - componentMargin,
                                handsUpRect.bottom)
                        handsUpWindow?.setRect(rect)
                        handsUpAnimateRect = rect
                    }
                    return
                }

                handsUpAnimateRect.left += diff
                handsUpAnimateRect.right += diff
                handsUpWindow?.setRect(handsUpAnimateRect)
            }
        })
        easeChat?.show(false)
        // ease chat window
        val chatFullScreenHideTop = chatFullScreenBottom - (easeChat?.hideIconSize ?: 0)
        val chatFullScreenHideLeft = chatFullScreenRight - (easeChat?.hideIconSize ?: 0)
        chatFullScreenHideRect.set(chatFullScreenHideLeft, chatFullScreenHideTop, chatFullScreenRight, chatFullScreenBottom)

        // handsup window
        val handsUpWidth = layout.context.resources.getDimensionPixelSize(R.dimen.agora_hands_up_view_w)
        val handsUpHeight = layout.context.resources.getDimensionPixelSize(R.dimen.agora_hands_up_view_h)
        val handsUpTop = height - margin - border - handsUpHeight
        val handsUpLeft = chatFullScreenHideLeft - componentMargin - handsUpWidth
        handsUpRect.set(handsUpLeft, handsUpTop, handsUpLeft + handsUpWidth, handsUpTop + handsUpHeight)
        handsUpWindow = AgoraUIHandsUp(layout.context, getEduContext(), layout, handsUpLeft, handsUpTop, handsUpWidth, handsUpHeight)
        handsUpWindow!!.setContainer(this)
        handsUpFullScreenRect.set(chatFullScreenHideLeft - componentMargin - handsUpWidth,
                handsUpTop, chatFullScreenHideLeft - componentMargin, handsUpTop + handsUpHeight)
        handsUpAnimateRect = Rect(handsUpRect)
        // handsup window

        // toolbar monitors the rosterDismiss event for restore Status of itemSelected
        AgoraUIRoster.dismissListener = toolbar?.rosterDismissListener
        roster = AgoraUIRoster(getEduContext())
        roster!!.setContainer(this)


        // register videoHandler
        getEduContext()?.videoContext()?.addHandler(smallContainerTeacherVideoHandler)
        // register userHandler
        getEduContext()?.userContext()?.addHandler(smallContainerUserHandler)
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

        val whiteboardW = width - border * 2
        val videosContainerH = videoListWindow?.getVideosContainerH() ?: 0
        val whiteboardH = height - border - videosContainerH - statusBarHeight
        screenShareWindow?.let {
            rect = Rect(border, statusBarHeight + videosContainerH, whiteboardW + border,
                    height - border)
            it.setRect(rect)
        }

        whiteboardDefaultRect.set(border, statusBarHeight + videosContainerH,
                whiteboardW + border, height - border)
        whiteboardNoVideoRect.set(border, statusBarHeight + border, whiteboardW + border,
                height - border)
        whiteboardFullScreenRect.set(border, statusBarHeight + border, whiteboardW + border,
                height - border)
        val whiteboardTop = if (videoListWindow?.studentsIsShown() == true) whiteboardDefaultRect.top else
            statusBarHeight + border
        whiteboardWindow?.let {
            rect = Rect(border, whiteboardTop, whiteboardW + border, whiteboardTop + whiteboardH)
            it.setRect(rect)
        }

        val pagingControlHeight = layout.context.resources.getDimensionPixelSize(R.dimen.agora_paging_control_height)
        val pagingControlLeft = componentMargin
        val pagingControlTop = height - border - componentMargin - pagingControlHeight
        pageControlWindow?.let {
            rect = Rect(pagingControlLeft, pagingControlTop, 0, pagingControlHeight + pagingControlTop)
            it.setRect(rect)
        }

        val fullScreenBtnWidth = layout.context.resources.getDimensionPixelSize(R.dimen.full_screen_btn_size)
        val fullScreenBtnHeight = layout.context.resources.getDimensionPixelSize(R.dimen.full_screen_btn_size)
        val fullScreenBtnLeft = 0
        val fullScreenBtnTop = height - border - fullScreenBtnHeight
        fullScreenBtn?.let {
            rect = Rect(fullScreenBtnLeft, fullScreenBtnTop, fullScreenBtnLeft + fullScreenBtnWidth,
                    fullScreenBtnTop + fullScreenBtnHeight)
            it.setRect(rect)
        }

        toolbarTopNoVideo = whiteboardNoVideoRect.top + componentMargin
        toolbarTopHasVideo = whiteboardDefaultRect.top + componentMargin
        toolbarHeightNoVideo = pagingControlTop - componentMargin - toolbarTopNoVideo
        toolbarHeightHasVideo = pagingControlTop - componentMargin - toolbarTopHasVideo
        toolbar?.let {
            val top = if (videoListWindow?.studentsIsShown() == true) toolbarTopHasVideo else toolbarTopNoVideo
            val height = if (videoListWindow?.studentsIsShown() == true) toolbarHeightHasVideo else toolbarHeightNoVideo
            it.setVerticalPosition(top, height)
        }

        val chatWidth = (AgoraUIConfig.SmallClass.chatWidthMaxRatio * width).toInt()
        val chatHeight = height - border - margin - videosContainerH - statusBarHeight
        val chatLeft = width - chatWidth - border
        val chatTop = statusBarHeight + videosContainerH
        chatRect.set(chatLeft, chatTop, chatLeft + chatWidth, chatTop + chatHeight)

        // chat window is larger when whiteboard is full screen
        val chatFullScreenLeft = chatLeft
        val chatFullScreenTop: Int = chatTop
        val chatFullScreenRight = chatLeft + chatWidth
        val chatFullScreenBottom = chatTop + chatHeight
        chatFullScreenRect.set(chatFullScreenLeft, chatFullScreenTop,
                chatFullScreenRight, chatFullScreenBottom)
        val chatFullScreenHideTop = chatFullScreenBottom - (easeChat?.hideIconSize ?: 0)
        val chatFullScreenHideLeft = chatFullScreenRight - (easeChat?.hideIconSize ?: 0)
        chatFullScreenHideRect.set(chatFullScreenHideLeft, chatFullScreenHideTop, chatFullScreenRight, chatFullScreenBottom)

        val handsUpWidth = layout.context.resources.getDimensionPixelSize(R.dimen.agora_hands_up_view_w)
        val handsUpHeight = layout.context.resources.getDimensionPixelSize(R.dimen.agora_hands_up_view_h)
        val handsUpTop = height - border - margin - handsUpHeight

        if (easeChat?.isShowing() == true) {
            handsUpFullScreenRect.set(
                    chatFullScreenLeft - componentMargin - handsUpWidth,
                    handsUpTop,
                    chatFullScreenLeft - margin,
                    handsUpTop + handsUpHeight)
            handsUpRect.set(
                    chatFullScreenLeft - componentMargin - handsUpWidth,
                    handsUpTop,
                    chatFullScreenLeft - componentMargin,
                    handsUpTop + handsUpHeight)
        } else {
            handsUpFullScreenRect.set(
                    chatFullScreenHideLeft - componentMargin - handsUpWidth,
                    handsUpTop,
                    chatFullScreenHideLeft - margin,
                    handsUpTop + handsUpHeight)
            handsUpRect.set(
                    chatFullScreenHideLeft - componentMargin - handsUpWidth,
                    handsUpTop,
                    chatFullScreenHideLeft - componentMargin,
                    handsUpTop + handsUpHeight)
        }

        handsUpWindow?.let {
            rect = if (isFullScreen) handsUpFullScreenRect else handsUpRect
            it.setRect(rect)
        }

        if (isFullScreen) {
            whiteboardWindow?.setRect(whiteboardFullScreenRect)
            screenShareWindow?.setRect(whiteboardFullScreenRect)
            easeChat?.let {
                it.setFullscreenRect(true, chatFullScreenHideRect)
                it.setFullDisplayRect(chatFullScreenRect)
//                it.show(false)
                it.setRect(if (it.isShowing()) chatFullScreenRect else chatFullScreenHideRect)
//                it.setClosable(true)
//                it.showShadow(false)
            }

            handsUpWindow?.setRect(handsUpFullScreenRect)
            handsUpAnimateRect = Rect(handsUpFullScreenRect)
            toolbar?.setVerticalPosition(toolbarTopNoVideo, toolbarHeightNoVideo)
            agoraUILoading?.setRect(whiteboardFullScreenRect)
        } else {
            if (videoListWindow?.studentsIsShown() == true) {
                whiteboardWindow?.setRect(whiteboardDefaultRect)
                screenShareWindow?.setRect(whiteboardDefaultRect)
                toolbar?.setVerticalPosition(toolbarTopHasVideo, toolbarHeightHasVideo)
                agoraUILoading?.setRect(whiteboardDefaultRect)
            } else {
                whiteboardWindow?.setRect(whiteboardNoVideoRect)
                screenShareWindow?.setRect(whiteboardNoVideoRect)
                toolbar?.setVerticalPosition(toolbarTopNoVideo, toolbarHeightNoVideo)
                agoraUILoading?.setRect(whiteboardNoVideoRect)
            }
            easeChat?.let {
                it.setFullscreenRect(false, chatRect)
                it.setFullDisplayRect(chatRect)
//                it.setClosable(true)
//                it.showShadow(false)
//                it.show(false)
            }
            handsUpWindow?.setRect(handsUpRect)
        }
    }

    private fun initValues(resources: Resources) {
        statusBarHeight = resources.getDimensionPixelSize(R.dimen.agora_status_bar_height)
        componentMargin = resources.getDimensionPixelSize(R.dimen.margin_medium)
        margin = resources.getDimensionPixelSize(R.dimen.margin_smaller)
        shadow = resources.getDimensionPixelSize(R.dimen.shadow_width)
        border = resources.getDimensionPixelSize(R.dimen.stroke_small)
    }

    override fun calculateVideoSize() {
        val videosLayoutMaxW = this.width - margin * (AgoraUIConfig.carouselMaxItem - 1)
        val videoMaxW = videosLayoutMaxW / AgoraUIConfig.carouselMaxItem
        AgoraUIConfig.SmallClass.teacherVideoWidth = videoMaxW
        AgoraUIConfig.SmallClass.teacherVideoHeight = (videoMaxW * AgoraUIConfig.videoRatio1).toInt()
//        if (AgoraUIConfig.isLargeScreen) {
//            AgoraUIConfig.SmallClass.studentVideoHeightLargeScreen = AgoraUIConfig.SmallClass.teacherVideoHeight
//        } else {
//            AgoraUIConfig.SmallClass.studentVideoHeightSmallScreen =
//                    minOf(AgoraUIConfig.SmallClass.teacherVideoHeight,
//                            (height * AgoraUIConfig.SmallClass.studentVideoHeightRationSmallScreen).toInt())
//        }
    }

    override fun release() {
        chatWindow?.release()
        widgetManager.release()
        easeChat?.release()
    }

    override fun willLaunchExtApp(appIdentifier: String): Int {
        return 0
    }

    override fun setFullScreen(fullScreen: Boolean) {
        isFullScreen = fullScreen
        if (fullScreen) {
            whiteboardWindow?.setRect(whiteboardFullScreenRect)
            screenShareWindow?.setRect(whiteboardFullScreenRect)
            easeChat?.let {
                it.setFullscreenRect(fullScreen, chatFullScreenHideRect)
                it.setFullDisplayRect(chatFullScreenRect)
                it.show(false)
//                it.setClosable(true)
//                it.showShadow(false)
            }
            val rect = Rect(chatFullScreenHideRect.left - (handsUpRect.right - handsUpRect.left) - componentMargin,
                    handsUpRect.top, chatFullScreenHideRect.left - componentMargin,
                    handsUpRect.bottom)
            handsUpWindow?.setRect(rect)
            handsUpAnimateRect = rect
            toolbar?.setVerticalPosition(toolbarTopNoVideo, toolbarHeightNoVideo)
            agoraUILoading?.setRect(whiteboardFullScreenRect)
        } else {
            if (videoListWindow?.studentsIsShown() == true) {
                whiteboardWindow?.setRect(whiteboardDefaultRect)
                screenShareWindow?.setRect(whiteboardDefaultRect)
                toolbar?.setVerticalPosition(toolbarTopHasVideo, toolbarHeightHasVideo)
                agoraUILoading?.setRect(whiteboardDefaultRect)
            } else {
                whiteboardWindow?.setRect(whiteboardNoVideoRect)
                screenShareWindow?.setRect(whiteboardNoVideoRect)
                toolbar?.setVerticalPosition(toolbarTopNoVideo, toolbarHeightNoVideo)
                agoraUILoading?.setRect(whiteboardNoVideoRect)
            }

            easeChat?.let {
                it.setFullscreenRect(fullScreen, chatRect)
                it.setFullDisplayRect(chatRect)
//                it.setClosable(true)
//                it.showShadow(false)
                it.show(false)
            }
            handsUpWindow?.setRect(handsUpRect)
        }
    }
}