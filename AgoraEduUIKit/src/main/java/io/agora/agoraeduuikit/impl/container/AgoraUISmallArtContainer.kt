package io.agora.agoraeduuikit.impl.container

import android.content.res.Resources
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View.*
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import io.agora.agoraeduuikit.handlers.WindowPropertyHandler
import io.agora.agoraeduuikit.impl.options.OptionLayoutMode
import io.agora.agoraeduuikit.impl.options.OptionsLayout
import io.agora.agoraeduuikit.impl.options.OptionsLayoutListener
import io.agora.agoraeduuikit.impl.options.OptionsLayoutWhiteboardItem
import io.agora.agoraeduuikit.R
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole.Teacher
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole.Student
import io.agora.agoraeducore.core.internal.edu.common.bean.handsup.Extra
import io.agora.agoraeducore.core.internal.edu.common.bean.handsup.Position
import io.agora.agoraeducore.core.internal.edu.common.bean.handsup.Size
import io.agora.agoraeducore.core.internal.edu.common.bean.handsup.WindowPropertyBody
import io.agora.agoraeducore.core.internal.framework.impl.handler.UserHandler
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetDefaultId
import io.agora.agoraeduuikit.component.dialog.AgoraUICustomDialogBuilder
import io.agora.agoraeduuikit.impl.video.AgoraUIVideoListArt
import io.agora.agoraeduuikit.impl.loading.AgoraUILoading
import io.agora.agoraeduuikit.impl.room.AgoraUIRoomStatusArt
import io.agora.agoraeduuikit.impl.screenshare.AgoraUIScreenShare
import io.agora.agoraeduuikit.impl.users.AgoraUIHandsUpToastPopUp
import io.agora.agoraeduuikit.impl.video.AgoraUILargeVideoArt
import io.agora.agoraeduuikit.impl.video.AgoraUIVideoArt
import io.agora.agoraeduuikit.impl.video.IAgoraUserInfoListener
import io.agora.agoraeduuikit.impl.whiteboard.AgoraUILargeVideoWindowArt

class AgoraUISmallClassArtContainer(
        eduContext: EduContextPool?,
        configs: AgoraContainerConfig) : AbsUIContainer(eduContext, configs) {
    private val tag = "SmallClassArtContainer"

    private var statusBarHeight = 0

    private var whiteboardHeight = 0

    private var margin = 0
    private var shadow = 0
    private var border = 0

    private var width = 0
    private var height = 0
    private var top = 0
    private var left = 0

    private var videoListWindow: AgoraUIVideoListArt? = null
    private var optionLayout: OptionsLayout? = null
    private var largeVideoWindow: AgoraUILargeVideoWindowArt? = null

    private var userDetailInfo: EduContextUserDetailInfo? = null
    private var lastUserDetailInfo: EduContextUserDetailInfo? = null
    private var optionRight = 0
    private var optionBottom = 0
    private var optionIconSize = 0

    private var optionPopupRight = 0

    private var chatWidth = 0
    private var chatHeight = 0

    private val whiteboardRect = Rect()

    private var largeWindowArtLeft: Float = 0f // x坐标
    private var largeWindowArtTop: Float = 0f // y
    private var largeWindowArtHeight: Float = 0f
    private var largeWindowArtWidth: Float = 0f

    private var largeWindowArtLeftRatio: Float = 0f
    private var largeWindowArtTopRatio: Float = 0f
    private var largeWindowArtHeightRatio: Float = 0f
    private var largeWindowArtWidthRatio: Float = 0f

    private var handsUpPopup: AgoraUIHandsUpToastPopUp? = null

    private var teacherInfo: AgoraEduContextUserInfo? = null
    private var teacherDetailInfo: EduContextUserDetailInfo? = null
    private var coHostList: MutableList<EduContextUserDetailInfo> = mutableListOf()
    var allCoHostList: MutableList<EduContextUserDetailInfo> = mutableListOf() //老师和上台学生集合

    private var whiteboardToolItem: OptionsLayoutWhiteboardItem? = null

    private val smallContainerUserHandler = object : UserHandler() {
        override fun onRemoteUserJoined(user: AgoraEduContextUserInfo) {
            super.onRemoteUserJoined(user)
            if (user.role == Teacher) {
                teacherInfo = user.copy()
                notifyVideos()
            } else if (user.role == Student) {

            }
        }

        override fun onRemoteUserLeft(user: AgoraEduContextUserInfo,
                                      operator: AgoraEduContextUserInfo?,
                                      reason: EduContextUserLeftReason) {
            super.onRemoteUserLeft(user, operator, reason)
            if (user.role == Teacher) {
                teacherInfo = null
                notifyVideos()
            } else if (user.role == Student) {
            }
        }

        override fun onLocalUserKickedOut() {
            super.onLocalUserKickedOut()
            kickOut()
        }
    }

    private fun notifyVideos() {
        val hasTeacher = teacherDetailInfo?.onLine == true
        videoListWindow?.showTeacher(hasTeacher)
        val hasCoHost = hasTeacher || coHostList.size > 0
        videoListWindow?.showStudents(hasCoHost)

        if (hasCoHost) {
            agoraUILoading?.setRect(whiteboardRect)
        } else {

        }
    }

    private val windowPropertyHandler = object : WindowPropertyHandler() {
        override fun onWindowPropertyChanged(windowPropertyBody: WindowPropertyBody?) {
            super.onWindowPropertyChanged(windowPropertyBody)
            //根据比例，转成实际高度
            largeWindowArtLeft = windowPropertyBody?.position?.xaxis!!.toFloat() * width
            largeWindowArtTop = windowPropertyBody.position.yaxis.toFloat() * height
            largeWindowArtHeight = windowPropertyBody.size.height.toFloat() * height//720 pingban
            largeWindowArtWidth = windowPropertyBody.size.width.toFloat() * width // 白板的宽:width // 1280 pingban

            allCoHostList.addAll(coHostList)
            teacherDetailInfo?.let { allCoHostList.add(it) }

            val largeWindowUser = windowPropertyBody.extra.userId
            allCoHostList.forEach {
                if (it.user.userUuid == largeWindowUser) {
                    userDetailInfo = it
                }
            }

            //根据比例设置大窗Rect大小
            val largeWindowRect = Rect()
            largeWindowRect.set(largeWindowArtLeft.toInt(), largeWindowArtTop.toInt(), largeWindowArtLeft.toInt() + largeWindowArtWidth.toInt(), largeWindowArtTop.toInt() + largeWindowArtHeight.toInt())

            getActivity()?.runOnUiThread {
                if (userDetailInfo?.user?.userUuid != lastUserDetailInfo?.user?.userUuid && lastUserDetailInfo != null) { //已经打开了大窗 但是切换用户，恢复原用户小窗状态
                    largeVideoWindow!!.setVisibility(INVISIBLE, lastUserDetailInfo)
                    videoListWindow?.setVisibility(VISIBLE, lastUserDetailInfo)
                }
                videoListWindow?.setVisibility(INVISIBLE, userDetailInfo)// 没用上
                largeVideoWindow!!.setLocation(largeWindowRect)//根据rect设置大窗位置
                largeVideoWindow!!.setVisibility(VISIBLE, userDetailInfo)
                lastUserDetailInfo = userDetailInfo
            }
        }

        override fun onWindowPropertyDeleted() {
            if (Teacher != eduContext?.userContext()?.getLocalUserInfo()?.role) {
                getActivity()?.runOnUiThread {
                    largeVideoWindow!!.setVisibility(INVISIBLE, userDetailInfo)
                    // 视频列表窗视频恢复
                    videoListWindow?.setVisibility(VISIBLE, userDetailInfo)
                }
            }
        }

        override fun onWindowPropertyUpdated(streamUuid: String) {
            allCoHostList.forEach {
                if (it.streamUuid == streamUuid) {
                    userDetailInfo = it// 拿到streamUuid对应的用户
                }
            }

            val largeWindowRect = Rect()
            largeWindowRect.set(largeWindowArtLeft.toInt(), largeWindowArtTop.toInt(), largeWindowArtLeft.toInt() + largeWindowArtWidth.toInt(), largeWindowArtTop.toInt() + largeWindowArtHeight.toInt())

            getActivity()?.runOnUiThread {
                if (userDetailInfo?.user?.userUuid != lastUserDetailInfo?.user?.userUuid && lastUserDetailInfo != null) { //已经打开了大窗 但是切换用户，恢复原用户小窗状态
                    largeVideoWindow!!.setVisibility(INVISIBLE, lastUserDetailInfo)
                    videoListWindow?.setVisibility(VISIBLE, lastUserDetailInfo)
                }
                videoListWindow?.setVisibility(INVISIBLE, userDetailInfo)
                largeVideoWindow!!.setLocation(largeWindowRect)//根据rect设置大窗位置
                largeVideoWindow!!.setVisibility(VISIBLE, userDetailInfo)
                lastUserDetailInfo = userDetailInfo
            }
        }
    }

    override fun init(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int) {//layout: contentLayout
        super.init(layout, left, top, width, height)

        this.width = width
        this.height = height
        this.left = left
        this.top = top

        initValues(layout.context.resources, width, height)
        roomStatusArt = AgoraUIRoomStatusArt(layout, getEduContext(), width, statusBarHeight, left, top)
        roomStatusArt!!.setContainer(this)

        calculateComponentSize()
        if (getContext() == null) {
            return
        }

        val whiteboardW = width
        whiteboardRect.top = height - whiteboardHeight
        whiteboardRect.bottom = height
        whiteboardRect.left = 0
        whiteboardRect.right = whiteboardW
        agoraUILoading = AgoraUILoading(layout, whiteboardRect)

        whiteboardContainer = LinearLayout(getContext())
        layout.addView(whiteboardContainer)
        val params = whiteboardContainer!!.layoutParams as ViewGroup.MarginLayoutParams
        params.width = whiteboardW
        params.height = whiteboardHeight
        params.topMargin = whiteboardRect.top
        whiteboardContainer!!.layoutParams = params
        val widgetConfig = getEduContext()?.widgetContext()?.getWidgetConfig(AgoraWidgetDefaultId.WhiteBoard.id)
        widgetConfig?.let {
            whiteBoardWidget = getEduContext()?.widgetContext()?.create(it)
        }

        largeWindowArtWidth = whiteboardW / 2.toFloat()
        largeWindowArtHeight = whiteboardW / 3.toFloat()
        largeWindowArtLeft = whiteboardW / 4.toFloat()

        largeWindowArtTop = whiteboardHeight / 8.toFloat()
        largeVideoWindow = AgoraUILargeVideoWindowArt(layout.context, getEduContext(), layout,
                largeWindowArtWidth.toInt()/*width*/, largeWindowArtHeight.toInt()/*height*/, largeWindowArtLeft.toFloat()/*left*/, largeWindowArtTop.toFloat()/*top*/, userDetailInfo)
        largeVideoWindow?.setContainer(this)
        largeVideoWindow?.setVisibility(INVISIBLE, userDetailInfo)

        videoListWindow = AgoraUIVideoListArt(layout.context, getEduContext(),
                layout, 0, statusBarHeight, width,
                height - whiteboardRect.height() - statusBarHeight, 3, 0)

        screenShareWindow = AgoraUIScreenShare(layout.context,
                getEduContext(), layout,
                whiteboardRect.width(), whiteboardRect.height(),
                whiteboardRect.left, whiteboardRect.top, 0f)
        screenShareWindow!!.setContainer(this)

        initOptionLayout(layout)

        getEduContext()?.userContext()?.addHandler(smallContainerUserHandler)
        getEduContext()?.windowPropertiesContext()?.addHandler(windowPropertyHandler)
    }

    private fun initOptionLayout(layout: ViewGroup) {
        val role = getEduContext()?.userContext()?.getLocalUserInfo()?.role ?: Student
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
            it.init(getEduContext(), container, role,
                optionIconSize, optionRight, optionBottom,
                OptionLayoutMode.Joint, this, handsUpPopup)
        }

        OptionsLayout.listener = object : OptionsLayoutListener {
            override fun onLeave() {
                showLeave()
            }

            override fun onKickout(userId: String, userName: String) {
                showKickDialog(userId)
            }
        }

        //双击列表小窗回调
        AgoraUIVideoArt.userInfoListener = object : IAgoraUserInfoListener {
            override fun onUserDoubleClicked(userDetailInfo: EduContextUserDetailInfo?) {
                this@AgoraUISmallClassArtContainer.userDetailInfo = userDetailInfo
                if (getEduContext()?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Teacher) {//只有老师才能开启大窗
                    getActivity()?.runOnUiThread {
                        if (userDetailInfo?.user?.userUuid != lastUserDetailInfo?.user?.userUuid && lastUserDetailInfo != null) {//已经打开了大窗 但是切换用户，恢复原用户小窗状态
                            //如果当前视频打开了，就关掉
                            largeVideoWindow!!.setVisibility(INVISIBLE, lastUserDetailInfo)
                            videoListWindow?.setVisibility(VISIBLE, lastUserDetailInfo)
                        }
//                        videoListWindow?.setVisibility(INVISIBLE, userDetailInfo)
                        largeVideoWindow!!.setVisibility(VISIBLE, userDetailInfo)
                        lastUserDetailInfo = userDetailInfo
                    }

                    largeWindowArtLeftRatio = largeWindowArtLeft / width
                    largeWindowArtTopRatio = largeWindowArtTop / height
                    largeWindowArtHeightRatio = largeWindowArtHeight / height
                    largeWindowArtWidthRatio = largeWindowArtWidth / width // 白板的宽:width


                    var windowPropertyBody = WindowPropertyBody(userDetailInfo?.user?.userUuid, Position(largeWindowArtLeftRatio, largeWindowArtTopRatio), Size(largeWindowArtHeightRatio.toDouble(), largeWindowArtWidthRatio.toDouble()), Extra(userDetailInfo?.user?.userUuid, true))
                    getEduContext()?.windowPropertiesContext()?.performWindowProperties(windowPropertyBody)//通知其他端设置属性
                }
            }
        }

        //双击大窗回调
        AgoraUILargeVideoArt.userInfoListener = object : IAgoraUserInfoListener {
            override fun onUserDoubleClicked(userDetailInfo: EduContextUserDetailInfo?) {
                this@AgoraUISmallClassArtContainer.userDetailInfo = userDetailInfo
                if (getEduContext()?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Teacher) {
                    getActivity()?.runOnUiThread {
                        largeVideoWindow!!.setVisibility(INVISIBLE, userDetailInfo)
                        // 视频列表窗视频恢复
                        videoListWindow?.setVisibility(VISIBLE, userDetailInfo)
                    }
                    //发消息通知其他端移除属性
                    var windowPropertyBody = WindowPropertyBody(userDetailInfo?.user?.userUuid, Position(largeWindowArtLeft, largeWindowArtTop), Size(largeWindowArtHeight.toDouble(), largeWindowArtWidth.toDouble()), Extra(userDetailInfo?.user?.userUuid, true))

                    getEduContext()?.windowPropertiesContext()?.deleteWindowProperties(windowPropertyBody)

                }
            }

        }
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

    private fun initValues(resources: Resources, width: Int, height: Int) {
        // 375 is the base height of container height of phones
        // 574 is the base height of tablets
        val basePhone = 375f
        val baseTablet = 574f
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
        shadow = resources.getDimensionPixelSize(R.dimen.shadow_width)
        border = resources.getDimensionPixelSize(R.dimen.stroke_small)
    }

    override fun calculateComponentSize() {
        val videosLayoutMaxW = this.width - margin * (AgoraUIConfig.carouselMaxItem - 1)
        val videoMaxW = videosLayoutMaxW / AgoraUIConfig.carouselMaxItem
        AgoraUIConfig.SmallClass.videoListVideoWidth = videoMaxW
        AgoraUIConfig.SmallClass.videoListVideoHeight = (videoMaxW * AgoraUIConfig.videoRatio1).toInt()
    }

    override fun release() {

    }

    override fun willLaunchExtApp(appIdentifier: String): Int {
        return 0
    }

    override fun setFullScreen(fullScreen: Boolean) {

    }
}