package io.agora.uikit.impl.container

import android.content.res.Resources
import android.graphics.Rect
import android.view.ViewGroup
import io.agora.educontext.EduContextPool
import io.agora.educontext.EduContextUserDetailInfo
import io.agora.uicomponent.UiWidgetConfig
import io.agora.uicomponent.UiWidgetManager
import io.agora.uikit.R
import io.agora.uikit.component.dialog.AgoraUIDialogBuilder
import io.agora.uikit.educontext.handlers.UserHandler
import io.agora.uikit.educontext.handlers.VideoHandler
import io.agora.uikit.impl.AgoraUIVideoList
import io.agora.uikit.impl.chat.EaseChatWidget
import io.agora.uikit.impl.chat.EaseChatWidgetPopup
import io.agora.uikit.impl.loading.AgoraUILoading
import io.agora.uikit.impl.options.OptionWindowListener
import io.agora.uikit.impl.options.OptionItem
import io.agora.uikit.impl.options.OptionLayout
import io.agora.uikit.impl.room.AgoraUIRoomStatus
import io.agora.uikit.impl.screenshare.AgoraUIScreenShare
import io.agora.uikit.impl.setting.AgoraUIDeviceSettingPopUp
import io.agora.uikit.impl.tool.AgoraUIToolBarBuilder
import io.agora.uikit.impl.tool.AgoraUIToolType
import io.agora.uikit.impl.users.*
import io.agora.uikit.impl.whiteboard.AgoraUIWhiteBoardBuilder

class AgoraUISmallClassContainer(
        eduContext: EduContextPool?,
        configs: AgoraContainerConfig) : AbsUIContainer(eduContext, configs) {
    private val tag = "AgoraUISmallClassContainer"

    private var statusBarHeight = 0

    private var whiteboardHeight = 0

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
    private var optionLayout: OptionLayout? = null
    private var settingPopup: AgoraUIDeviceSettingPopUp? = null
    private var rosterPopup: AgoraUIRosterPopUp? = null
    private var chatPopup: EaseChatWidgetPopup? = null

    private var optionRight = 0
    private var optionBottom = 0
    private var optionIconSize = 0

    private var optionPopupRight = 0
    private var optionPopupBottom = 0

    private var chatWidth = 0
    private var chatHeight = 0

    private val whiteboardRect = Rect()

    private var toolbarTopNoVideo = 0
    private var toolbarTopHasVideo = 0
    private var toolbarHeightNoVideo = 0
    private var toolbarHeightHasVideo = 0

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

        if (hasCoHost) {
            toolbar?.setVerticalPosition(toolbarTopHasVideo, toolbarHeightHasVideo)
            agoraUILoading?.setRect(whiteboardRect)
        } else {
            toolbar?.setVerticalPosition(toolbarTopNoVideo, toolbarHeightNoVideo)
        }
    }

    override fun init(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int) {
        super.init(layout, left, top, width, height)

        this.width = width
        this.height = height
        this.left = left
        this.top = top

        initValues(layout.context.resources, width, height)

        roomStatus = AgoraUIRoomStatus(layout, getEduContext(), width, statusBarHeight, left, top)
        roomStatus!!.setContainer(this)

        calculateVideoSize()
        if (getContext() == null) {
            return
        }

        val whiteboardW = width
        whiteboardRect.top = height - whiteboardHeight
        whiteboardRect.bottom = height
        whiteboardRect.left = 0
        whiteboardRect.right = whiteboardW
        agoraUILoading = AgoraUILoading(layout, whiteboardRect)

        whiteboardWindow = AgoraUIWhiteBoardBuilder(layout.context, getEduContext(), layout)
                .width(whiteboardW)
                .height(whiteboardHeight)
                .left(whiteboardRect.left.toFloat())
                .top(whiteboardRect.top.toFloat())
                .shadowWidth(0f).build()
        whiteboardWindow!!.setContainer(this)

        videoListWindow = AgoraUIVideoList(layout.context, getEduContext(),
            layout, 0, statusBarHeight, width,
            height - whiteboardRect.height() - statusBarHeight, 0, 0)

        toolbarTopNoVideo = whiteboardRect.top + componentMargin
        toolbarTopHasVideo = whiteboardRect.top + componentMargin
        toolbarHeightNoVideo = height - componentMargin - toolbarTopNoVideo
        toolbarHeightHasVideo = height - componentMargin - toolbarTopHasVideo

        screenShareWindow = AgoraUIScreenShare(layout.context,
            getEduContext(), layout,
            whiteboardRect.width(), whiteboardRect.height(),
            whiteboardRect.left, whiteboardRect.top, 0f)
        screenShareWindow!!.setContainer(this)

        toolbar = AgoraUIToolBarBuilder(layout.context, getEduContext(), layout)
                .foldTop(toolbarTopNoVideo)
                .unfoldTop(toolbarTopNoVideo)
                .unfoldLeft(margin + componentMargin)
                .unfoldHeight(toolbarHeightNoVideo)
                .shadowWidth(shadow)
                .build()
        toolbar!!.setToolbarType(AgoraUIToolType.Refact)
        toolbar!!.setContainer(this)
        toolbar?.setVerticalPosition(toolbarTopNoVideo, toolbarHeightNoVideo)

        UiWidgetManager.registerAndReplace(listOf(
            UiWidgetConfig(UiWidgetManager.DefaultWidgetId.HyphenateChat.name, EaseChatWidgetPopup::class.java)
        ))

        chatPopup = widgetManager.create(UiWidgetManager.DefaultWidgetId.HyphenateChat.name,
            getEduContext()) as? EaseChatWidgetPopup
        chatPopup?.initView(layout, chatWidth, chatHeight, optionPopupRight, optionPopupBottom)
        chatPopup?.setContainer(this)
        chatPopup?.dismissRunnable = Runnable {
            optionLayout?.setActivated(OptionItem.Chat, false)
        }

        rosterPopup = AgoraUIRosterPopUp(layout.context)
        rosterPopup?.let { popup ->
            popup.setEduContext(getEduContext())
            popup.setType(RosterType.SmallClass)
            popup.initView(layout, optionPopupRight, optionPopupBottom)
            popup.closeRunnable = Runnable {
                optionLayout?.setActivated(OptionItem.Roster, false)
            }
        }

        optionLayout = OptionLayout(layout, optionRight, optionBottom,
            object : OptionWindowListener {
                override fun onWindowShow(item: OptionItem) {
                    when (item) {
                        OptionItem.Setting -> {
                            showDeviceSettingPopup()
                        }
                        OptionItem.Roster -> {
                            showRosterPopup()
                        }
                        OptionItem.Chat -> {
                            showChatPopup()
                        }
                        else -> {

                        }
                    }
                }

                override fun onWindowDismiss(item: OptionItem) {
                    when (item) {
                        OptionItem.Setting -> {
                            settingPopup?.dismiss()
                        }
                        OptionItem.Roster -> {
                            rosterPopup?.dismiss()
                        }
                        else -> {
                            chatPopup?.dismiss()
                        }
                    }
                }
            }, getEduContext())

        optionLayout?.setIconSize(optionIconSize)

        getEduContext()?.videoContext()?.addHandler(smallContainerTeacherVideoHandler)
        getEduContext()?.userContext()?.addHandler(smallContainerUserHandler)

        getEduContext()?.roomContext()?.joinClassRoom()
    }

    private fun showDeviceSettingPopup() {
        layout()?.let { layout ->
            layout.context?.let { context ->
                settingPopup = AgoraUIDeviceSettingPopUp(context)
                settingPopup!!.initView(layout,
                    getContext()?.resources?.getDimensionPixelSize(R.dimen.agora_setting_dialog_width) ?: 0,
                    getContext()?.resources?.getDimensionPixelSize(R.dimen.agora_setting_dialog_height) ?: 0,
                    optionPopupRight, optionPopupBottom)
                settingPopup!!.setEduContextPool(getEduContext())
                settingPopup!!.leaveRoomRunnable = Runnable {
                    optionLayout?.setActivated(OptionItem.Setting, false)
                    showLeaveDialog()
                }
                settingPopup!!.show()
            }
        }
    }

    private fun showLeaveDialog() {
        layout()?.let {
            it.post {
                AgoraUIDialogBuilder(it.context)
                    .title(it.context.resources.getString(R.string.agora_dialog_end_class_confirm_title))
                    .message(it.context.resources.getString(R.string.agora_dialog_end_class_confirm_message))
                    .negativeText(it.context.resources.getString(R.string.cancel))
                    .positiveText(it.context.resources.getString(R.string.confirm))
                    .positiveClick { getEduContext()?.roomContext()?.leave() }
                    .build()
                    .show()
            }
        }
    }

    private fun showRosterPopup() {
        rosterPopup?.show()
    }

    private fun showChatPopup() {
        chatPopup?.show()
    }

    override fun resize(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int) {
        super.resize(layout, left, top, width, height)
    }

    private fun initValues(resources: Resources, width: Int, height: Int) {
        // 375 is the base height of container height of phones
        // 574 is the base height of tablets
        statusBarHeight = if (AgoraUIConfig.isLargeScreen) {
            (height * 14 / 375f).toInt()
        } else {
            (height * 20 / 574f).toInt()
        }

        whiteboardHeight = (height * 0.82).toInt()

        optionRight = (height * 4 / 375f).toInt()
        optionBottom = (height * 7 / 375f).toInt()

        optionIconSize = if (AgoraUIConfig.isLargeScreen)
                (height * 50 / 575f).toInt() else (height * 42 / 360f).toInt()

        optionPopupRight = if (AgoraUIConfig.isLargeScreen)
            (height * 60 / 576f).toInt() else (height * 50 / 375f).toInt()

        optionPopupBottom = (height * 10 / 375f).toInt()

        chatWidth = if (AgoraUIConfig.isLargeScreen)
                (height * 340 / 576f).toInt() else (height * 200 / 375f).toInt()

        chatHeight = if (AgoraUIConfig.isLargeScreen)
            (height * 400 / 576f).toInt() else (height * 268 / 375f).toInt()

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
    }

    override fun release() {
        UiWidgetManager.registerAndReplace(listOf(
            UiWidgetConfig(UiWidgetManager.DefaultWidgetId.HyphenateChat.name, EaseChatWidget::class.java)
        ))
        chatWindow?.release()
        widgetManager.release()
        easeChat?.release()
        chatPopup?.release()
    }

    override fun willLaunchExtApp(appIdentifier: String): Int {
        return 0
    }

    override fun setFullScreen(fullScreen: Boolean) {

    }
}