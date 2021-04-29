package io.agora.uikit.impl.container

import android.content.res.Resources
import android.graphics.Rect
import android.view.ViewGroup
import io.agora.educontext.EduContextPool
import io.agora.educontext.EduContextUserDetailInfo
import io.agora.educontext.EduContextVideoMode
import io.agora.uikit.R
import io.agora.uikit.educontext.handlers.UserHandler
import io.agora.uikit.impl.chat.AgoraUIChatWindow
import io.agora.uikit.impl.chat.OnChatWindowAnimateListener
import io.agora.uikit.impl.handsup.AgoraUIHandsUp
import io.agora.uikit.impl.room.AgoraUIRoomStatus
import io.agora.uikit.impl.screenshare.AgoraUIScreenShare
import io.agora.uikit.impl.tool.AgoraUIToolBarBuilder
import io.agora.uikit.impl.tool.AgoraUIToolType
import io.agora.uikit.impl.users.*
import io.agora.uikit.impl.video.AgoraUIVideoGroup
import io.agora.uikit.impl.whiteboard.AgoraUIWhiteBoardBuilder
import io.agora.uikit.impl.whiteboard.paging.AgoraUIPagingControlBuilder

class AgoraUILargeClassContainer(eduContext: EduContextPool?) : AbsUIContainer(eduContext) {
    private val tag = "AgoraUILargeClassContainer"

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

    private val chatRect = Rect()
    private val chatFullScreenRect = Rect()
    private val chatFullScreenHideRect = Rect()
    private val whiteboardDefaultRect = Rect()
    private val whiteboardFullScreenRect = Rect()
    private val whiteboardNoStudentVideoRect = Rect()
    private val handsUpRect = Rect()
    private val handsUpFullScreenRect = Rect()
    private var handsUpAnimateRect = Rect()

    private var toolbarTopNoStudent = 0
    private var toolbarTopHasStudent = 0
    private var toolbarHeightNoStudent = 0
    private var toolbarHeightHasStudent = 0

    private var isFullScreen = false

    private val largeContainerUserHandler = object : UserHandler() {
        override fun onCoHostListUpdated(list: MutableList<EduContextUserDetailInfo>) {
            super.onCoHostListUpdated(list)
            studentVideoGroup?.updateCoHostList(list)
            val hasCoHost = list.size > 0
            studentVideoGroup?.show(hasCoHost)
            if (isFullScreen) {
                return
            }
            if (hasCoHost) {
                whiteboardWindow?.setRect(whiteboardDefaultRect)
                screenShareWindow?.setRect(whiteboardDefaultRect)
                toolbar?.setVerticalPosition(toolbarTopHasStudent, toolbarHeightHasStudent)
            } else {
                whiteboardWindow?.setRect(whiteboardNoStudentVideoRect)
                screenShareWindow?.setRect(whiteboardNoStudentVideoRect)
                toolbar?.setVerticalPosition(toolbarTopNoStudent, toolbarHeightNoStudent)
            }
        }

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

        roomStatus = AgoraUIRoomStatus(layout, getEduContext(), width, statusBarHeight, left, top)
        roomStatus!!.setContainer(this)

        calculateVideoSize()
        val teacherVideoW = AgoraUIConfig.SmallClass.teacherVideoWidth
        val teacherVideoH = AgoraUIConfig.SmallClass.teacherVideoHeight
        val teacherVideoTop = statusBarHeight + margin
        val teacherVideoLeft = width - teacherVideoW
        videoGroupWindow = AgoraUIVideoGroup(layout.context, getEduContext(), layout,
                teacherVideoLeft, teacherVideoTop, teacherVideoW,
                teacherVideoH, 0, EduContextVideoMode.Single)
        videoGroupWindow!!.setContainer(this)

        val studentVideoTop = statusBarHeight + margin
        val studentVideoLeft = border
        val studentVideoWidth = teacherVideoLeft - margin - border
        val studentVideoHeight = if (AgoraUIConfig.isLargeScreen)
            AgoraUIConfig.SmallClass.studentVideoHeightLargeScreen
        else AgoraUIConfig.SmallClass.studentVideoHeightSmallScreen

        studentVideoGroup = AgoraUserListVideoLayout(layout.context, getEduContext(), layout,
                studentVideoWidth, studentVideoHeight, studentVideoLeft, studentVideoTop, 0f)
        studentVideoGroup!!.setContainer(this)
        studentVideoGroup!!.show(false)

        val whiteboardW = width - teacherVideoW - margin - border
        val whiteboardH = height - statusBarHeight - margin - border

        // Rect when student video list is shown
        whiteboardDefaultRect.set(border, studentVideoTop + studentVideoHeight + margin,
                whiteboardW, height - border)
        whiteboardNoStudentVideoRect.set(border, statusBarHeight + margin, whiteboardW, height - border)
        whiteboardFullScreenRect.set(border, statusBarHeight + margin, width - border, height - border)
        whiteboardWindow = AgoraUIWhiteBoardBuilder(layout.context, getEduContext(), layout)
                .width(whiteboardW)
                .height(whiteboardH)
                .top(statusBarHeight + margin.toFloat())
                .shadowWidth(0f).build()
        whiteboardWindow!!.setContainer(this)

        screenShareWindow = AgoraUIScreenShare(layout.context, getEduContext(), layout,
                whiteboardW, whiteboardH, border, statusBarHeight + margin, 0f)
        screenShareWindow!!.setContainer(this)

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

        toolbarTopNoStudent = whiteboardNoStudentVideoRect.top + componentMargin
        toolbarTopHasStudent = whiteboardDefaultRect.top + componentMargin
        toolbarHeightNoStudent = pagingControlTop - componentMargin - toolbarTopNoStudent
        toolbarHeightHasStudent = pagingControlTop - componentMargin - toolbarTopHasStudent
        toolbar = AgoraUIToolBarBuilder(layout.context, getEduContext(), layout)
                .foldTop(toolbarTopNoStudent)
                .unfoldTop(toolbarTopNoStudent)
                .unfoldLeft(border + componentMargin)
                .unfoldHeight(toolbarHeightNoStudent)
                .shadowWidth(shadow)
                .build()
        toolbar!!.setToolbarType(AgoraUIToolType.All)
        toolbar!!.setContainer(this)
        toolbar?.setVerticalPosition(toolbarTopNoStudent, toolbarHeightNoStudent)

        val chatLeft = width - teacherVideoW - border
        val chatTop = teacherVideoTop + teacherVideoH + margin
        val chatHeight = height - chatTop - border
        chatRect.set(chatLeft, chatTop, chatLeft + teacherVideoW, chatTop + chatHeight)
        chatWindow = AgoraUIChatWindow(layout, getEduContext(), teacherVideoW, chatHeight, chatLeft, chatTop, shadow)
        chatWindow?.let {
            it.setContainer(this)
            it.setClosable(false)
            it.showShadow(false)
        }

        // chat window is larger when whiteboard is full screen
        val chatFullScreenLeft = width - teacherVideoW - margin
        val chatFullScreenRight = width - componentMargin
        val chatFullScreenBottom = height - margin
        val chatFullScreenTop: Int = if (AgoraUIConfig.isLargeScreen) {
            chatFullScreenBottom - (height * AgoraUIConfig.chatHeightLargeScreenRatio).toInt()
        } else {
            statusBarHeight + margin + componentMargin
        }

        chatFullScreenRect.set(chatFullScreenLeft, chatFullScreenTop,
                chatFullScreenRight, chatFullScreenBottom)
        val chatFullScreenHideTop = chatFullScreenBottom - chatWindow?.hideIconSize!!
        val chatFullScreenHideLeft = chatFullScreenRight - chatWindow?.hideIconSize!!
        chatFullScreenHideRect.set(chatFullScreenHideLeft, chatFullScreenHideTop, chatFullScreenRight, chatFullScreenBottom)

        chatWindow!!.setAnimateListener(object : OnChatWindowAnimateListener {
            private var lastLeft = 0

            override fun onChatWindowAnimate(enlarge: Boolean, fraction: Float, left: Int,
                                             top: Int, width: Int, height: Int) {
                if (fraction.compareTo(0) == 0) lastLeft = left

                val chatWindowWidth = chatFullScreenRight - chatFullScreenLeft
                val diff = left - lastLeft
                lastLeft = left

                if (chatWindowWidth - left <= chatWindow!!.hideIconSize) {
                    if (!enlarge) {
                        val rect = Rect(chatFullScreenHideRect.left - (handsUpRect.right - handsUpRect.left) - margin,
                                handsUpRect.top, chatFullScreenHideRect.left - margin,
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

        val handsUpWidth = layout.context.resources.getDimensionPixelSize(R.dimen.agora_hands_up_view_w)
        val handsUpHeight = layout.context.resources.getDimensionPixelSize(R.dimen.agora_hands_up_view_h)
        val handsUpTop = height - margin - handsUpHeight
        val handsUpLeft = whiteboardDefaultRect.right - margin - handsUpWidth
        handsUpRect.set(handsUpLeft, handsUpTop, handsUpLeft + handsUpWidth, handsUpTop + handsUpHeight)
        handsUpWindow = AgoraUIHandsUp(layout.context, getEduContext(), layout, handsUpLeft, handsUpTop, handsUpWidth, handsUpHeight)
        handsUpWindow!!.setContainer(this)
        handsUpFullScreenRect.set(chatFullScreenHideLeft - margin - handsUpWidth, handsUpTop,
                chatFullScreenHideLeft - margin, handsUpTop + handsUpHeight)

        roster = AgoraUIRoster(getEduContext())
        roster!!.setContainer(this)
        getEduContext()?.userContext()?.addHandler(largeContainerUserHandler)
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
        val teacherVideoW = AgoraUIConfig.SmallClass.teacherVideoWidth
        val teacherVideoH = AgoraUIConfig.SmallClass.teacherVideoHeight
        val teacherVideoTop = statusBarHeight + margin
        val teacherVideoLeft = width - teacherVideoW
        videoGroupWindow?.let {
            rect = Rect(teacherVideoLeft, teacherVideoTop, teacherVideoW + teacherVideoLeft,
                    teacherVideoH + teacherVideoTop)
            it.setRect(rect)
        }

        val studentVideoTop = statusBarHeight + margin
        val studentVideoLeft = border
        val studentVideoWidth = teacherVideoLeft - margin - border
        val studentVideoHeight = if (AgoraUIConfig.isLargeScreen)
            AgoraUIConfig.SmallClass.studentVideoHeightLargeScreen
        else AgoraUIConfig.SmallClass.studentVideoHeightSmallScreen
        studentVideoGroup?.let {
            rect = Rect(studentVideoLeft, studentVideoTop, studentVideoWidth + studentVideoLeft,
                    studentVideoHeight + studentVideoTop)
            it.setRect(rect)
        }

        val whiteboardW = width - teacherVideoW - margin - border
        val whiteboardH = height - statusBarHeight - margin - border
        whiteboardDefaultRect.set(border, studentVideoTop + studentVideoHeight + margin,
                whiteboardW, height - border)
        whiteboardNoStudentVideoRect.set(border, statusBarHeight + margin, whiteboardW,
                height - border)
        whiteboardFullScreenRect.set(border, statusBarHeight + margin, width - border,
                height - border)
        val whiteboardTop = if (studentVideoGroup?.isShown() == true) whiteboardDefaultRect.top else
            statusBarHeight + margin
        whiteboardWindow?.let {
            rect = Rect(0, whiteboardTop, whiteboardW, whiteboardH + whiteboardTop)
            it.setRect(rect)
        }

        screenShareWindow?.let {
            rect = Rect(border, statusBarHeight + margin, whiteboardW + border,
                    whiteboardH + statusBarHeight + margin)
            it.setRect(rect)
        }

        val pagingControlHeight = layout.context.resources.getDimensionPixelSize(R.dimen.agora_paging_control_height)
        val pagingControlLeft = componentMargin
        val pagingControlTop = height - pagingControlHeight - border - componentMargin
        pageControlWindow?.let {
            rect = Rect(pagingControlLeft, pagingControlTop, 0, pagingControlHeight + pagingControlTop)
            it.setRect(rect)
        }

        toolbarTopNoStudent = whiteboardNoStudentVideoRect.top + componentMargin
        toolbarTopHasStudent = whiteboardDefaultRect.top + componentMargin
        toolbarHeightNoStudent = pagingControlTop - componentMargin - toolbarTopNoStudent
        toolbarHeightHasStudent = pagingControlTop - componentMargin - toolbarTopHasStudent
        toolbar?.let {
            val top = if (studentVideoGroup?.isShown() == true) toolbarTopHasStudent else toolbarTopNoStudent
            val height = if (studentVideoGroup?.isShown() == true) toolbarHeightHasStudent else toolbarHeightNoStudent
            it.setVerticalPosition(top, height)
        }

        val chatLeft = width - teacherVideoW - border
        val chatTop = teacherVideoTop + teacherVideoH + margin
        val chatHeight = height - chatTop - border
        chatRect.set(chatLeft, chatTop, chatLeft + teacherVideoW, chatTop + chatHeight)

        // chat window is larger when whiteboard is full screen
        val chatFullScreenLeft = width - teacherVideoW - margin
        val chatFullScreenRight = width - componentMargin
        val chatFullScreenBottom = height - margin
        val chatFullScreenTop: Int = if (AgoraUIConfig.isLargeScreen) {
            chatFullScreenBottom - (height * AgoraUIConfig.chatHeightLargeScreenRatio).toInt()
        } else {
            statusBarHeight + margin + componentMargin
        }

        chatFullScreenRect.set(chatFullScreenLeft, chatFullScreenTop,
                chatFullScreenRight, chatFullScreenBottom)
        val chatFullScreenHideTop = chatFullScreenBottom - chatWindow?.hideIconSize!!
        val chatFullScreenHideLeft = chatFullScreenRight - chatWindow?.hideIconSize!!
        chatFullScreenHideRect.set(chatFullScreenHideLeft, chatFullScreenHideTop, chatFullScreenRight, chatFullScreenBottom)

        val handsUpWidth = layout.context.resources.getDimensionPixelSize(R.dimen.agora_hands_up_view_w)
        val handsUpHeight = layout.context.resources.getDimensionPixelSize(R.dimen.agora_hands_up_view_h)
        val handsUpTop = height - margin - handsUpHeight
        val handsUpLeft = whiteboardDefaultRect.right - margin - handsUpWidth
        handsUpRect.set(handsUpLeft, handsUpTop, handsUpLeft + handsUpWidth, handsUpTop + handsUpHeight)
        //
        if (chatWindow?.isShowing() == true) {
            handsUpFullScreenRect.set(chatFullScreenLeft - margin - handsUpWidth, handsUpTop,
                    chatFullScreenLeft - margin, handsUpTop + handsUpHeight)
        } else if (chatWindow?.isShowing() != true) {
            handsUpFullScreenRect.set(chatFullScreenHideLeft - margin - handsUpWidth, handsUpTop,
                    chatFullScreenHideLeft - margin, handsUpTop + handsUpHeight)
        }
        handsUpWindow?.let {
            rect = if (isFullScreen) handsUpFullScreenRect else handsUpRect
            it.setRect(rect)
        }

        if (isFullScreen) {
            whiteboardWindow?.setRect(whiteboardFullScreenRect)
            screenShareWindow?.setRect(whiteboardFullScreenRect)
            chatWindow?.let {
//                it.setFullscreenRect(true, chatFullScreenHideRect)
                it.setFullDisplayRect(chatFullScreenRect)
                it.show(it.isShowing())
                it.setRect(if (it.isShowing()) chatFullScreenRect else chatFullScreenHideRect)
                it.setClosable(true)
                it.showShadow(true)
            }
            handsUpWindow?.setRect(handsUpFullScreenRect)
            handsUpAnimateRect = Rect(handsUpFullScreenRect)
            toolbar?.setVerticalPosition(toolbarTopNoStudent, toolbarHeightNoStudent)
        } else {
            if (studentVideoGroup!!.isShown()) {
                whiteboardWindow?.setRect(whiteboardDefaultRect)
                screenShareWindow?.setRect(whiteboardDefaultRect)
                toolbar?.setVerticalPosition(toolbarTopHasStudent, toolbarHeightHasStudent)
            } else {
                whiteboardWindow?.setRect(whiteboardNoStudentVideoRect)
                screenShareWindow?.setRect(whiteboardNoStudentVideoRect)
                toolbar?.setVerticalPosition(toolbarTopNoStudent, toolbarHeightNoStudent)
            }
            chatWindow?.let {
                it.setFullscreenRect(false, chatRect)
                it.setFullDisplayRect(chatRect)
                it.setClosable(false)
                it.showShadow(false)
                it.show(true)
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
        AgoraUIConfig.SmallClass.teacherVideoWidth =
                minOf((AgoraUIConfig.videoWidthMaxRatio * width).toInt(), AgoraUIConfig.SmallClass.teacherVideoWidth)
        AgoraUIConfig.SmallClass.teacherVideoHeight = (AgoraUIConfig.SmallClass.teacherVideoWidth * AgoraUIConfig.videoRatio).toInt()
        if (AgoraUIConfig.isLargeScreen) {
            AgoraUIConfig.SmallClass.studentVideoHeightLargeScreen = AgoraUIConfig.SmallClass.teacherVideoHeight
        } else {
            AgoraUIConfig.SmallClass.studentVideoHeightSmallScreen =
                    minOf(AgoraUIConfig.SmallClass.teacherVideoHeight,
                            (height * AgoraUIConfig.SmallClass.studentVideoHeightRationSmallScreen).toInt())
        }
    }

    override fun willLaunchExtApp(appIdentifier: String): Int {
        return 0
    }

    override fun setFullScreen(fullScreen: Boolean) {
        isFullScreen = fullScreen
        if (fullScreen) {
            whiteboardWindow?.setRect(whiteboardFullScreenRect)
            screenShareWindow?.setRect(whiteboardFullScreenRect)
            chatWindow?.setFullscreenRect(fullScreen, chatFullScreenHideRect)
            chatWindow?.setFullDisplayRect(chatFullScreenRect)
            chatWindow?.setClosable(true)
            chatWindow?.showShadow(true)
            val rect = Rect(chatFullScreenHideRect.left - (handsUpRect.right - handsUpRect.left) - margin,
                    handsUpRect.top, chatFullScreenHideRect.left - margin,
                    handsUpRect.bottom)
            handsUpWindow?.setRect(rect)
            handsUpAnimateRect = rect
            toolbar?.setVerticalPosition(toolbarTopNoStudent, toolbarHeightNoStudent)
        } else {
            if (studentVideoGroup!!.isShown()) {
                whiteboardWindow?.setRect(whiteboardDefaultRect)
                screenShareWindow?.setRect(whiteboardDefaultRect)
                toolbar?.setVerticalPosition(toolbarTopHasStudent, toolbarHeightHasStudent)
            } else {
                whiteboardWindow?.setRect(whiteboardNoStudentVideoRect)
                screenShareWindow?.setRect(whiteboardNoStudentVideoRect)
                toolbar?.setVerticalPosition(toolbarTopNoStudent, toolbarHeightNoStudent)
            }

            chatWindow?.setFullscreenRect(fullScreen, chatRect)
            chatWindow?.setFullDisplayRect(chatRect)
            chatWindow?.setClosable(false)
            chatWindow?.showShadow(false)
            chatWindow?.show(true)
            handsUpWindow?.setRect(handsUpRect)
        }
    }
}