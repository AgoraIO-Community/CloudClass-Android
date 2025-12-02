package io.agora.online.sdk.vocational

import android.animation.Animator
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity.CENTER
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.UiThread
import androidx.core.content.ContextCompat
import io.agora.online.animator.AnimatorUtil
import io.agora.online.animator.FCRAnimatorListener
import io.agora.online.component.common.AbsAgoraEduComponent
import io.agora.online.component.common.IAgoraUIProvider
import com.google.gson.Gson
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.context.AgoraEduContextVideoSourceType.Camera
import io.agora.agoraeducore.core.internal.edu.classroom.bean.PropertyData.CMD
import io.agora.agoraeducore.core.internal.education.impl.network.HttpBaseRes
import io.agora.agoraeducore.core.internal.education.impl.network.HttpCallback
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.StreamHandler
import io.agora.agoraeducore.core.internal.framework.impl.managers.AgoraWidgetActiveObserver
import io.agora.agoraeducore.core.internal.framework.impl.managers.FCRWidgetSyncFrameObserver
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.launch.AgoraServiceType
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId.LargeWindow
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetFrame
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.online.component.teachaids.AgoraTeachAidWidgetActiveStateChangeData
import io.agora.online.component.teachaids.AgoraTeachAidWidgetInteractionPacket
import io.agora.online.component.teachaids.AgoraTeachAidWidgetInteractionSignal
import io.agora.online.component.teachaids.bean.FCRLargeVideoWidgetExtra
import io.agora.online.component.teachaids.bean.StaticData
import io.agora.online.component.teachaids.bean.StaticData.extraUserUuidKey
import io.agora.online.component.teachaids.bean.StaticData.platformEnableCMD
import io.agora.online.component.teachaids.presenter.FCRLargeWindowManager
import io.agora.online.component.teachaids.presenter.FCRVideoPresenter
import io.agora.online.databinding.FcrOnlineEduLargeWindowContainerComponentBinding
import io.agora.online.impl.video.AgoraLargeWindowInteractionPacket
import io.agora.online.impl.video.AgoraLargeWindowInteractionSignal
import io.agora.online.impl.video.AgoraUILargeVideoWidget
import io.agora.online.provider.AgoraUIUserDetailInfo
import io.agora.online.util.VideoUtils
import kotlin.math.roundToInt

/**
 * author : wufang
 * date : 2022/3/23
 * description :关闭白板后老师大窗显示和屏幕分享的容器
 */
class FCRVocationalLargeWindowContainerComponent : AbsAgoraEduComponent,
    AgoraUILargeVideoWidget.IAgoraUILargeVideoListener, FCRWidgetSyncFrameObserver {
    private val tag = "Vocational-FCR"

    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(
        context,
        attr,
        defStyleAttr
    )

    private val binding = FcrOnlineEduLargeWindowContainerComponentBinding.inflate(
        LayoutInflater.from(context),
        this, true
    )

    private val videoWidgets = mutableMapOf<String, AgoraBaseWidget>()
    private val dash = "-"

    // 位置和宽高比例信息的默认值
    private val defaultPositionPercent = 0.5F
    private val defaultSizePercent = 0.3F
    private val strStreamWindow: String = "streamWindow"
    private var lastScreenInfo = ""//记录上次屏幕分享流
    private val widgetActiveObserver = object : AgoraWidgetActiveObserver {
        override fun onWidgetActive(widgetId: String) {
            // 从讲台点击和拖拽时触发
            ContextCompat.getMainExecutor(context).execute {
                createWidget(widgetId, true)
            }
        }

        override fun onWidgetInActive(widgetId: String) {
            // 从组件区域拖回去和双击时触发
            ContextCompat.getMainExecutor(context).execute {
                destroyWidget(widgetId)
            }
        }
    }

    private val videoWidgetMsgObserver = object : AgoraWidgetMessageObserver {
        override fun onMessageReceived(msg: String, id: String) {
            val packet = GsonUtil.jsonToObject<AgoraTeachAidWidgetInteractionPacket>(msg)
            packet?.let {
                when (packet.signal) {
                    AgoraTeachAidWidgetInteractionSignal.ActiveState -> {
                        GsonUtil.toJson(packet.body)?.let {
                            val data =
                                GsonUtil.jsonToObject<AgoraTeachAidWidgetActiveStateChangeData>(it)
                            if (data?.active == true) {
                                eduContext?.widgetContext()?.setWidgetActive(
                                    widgetId = id,
                                    roomProperties = data.properties
                                )
                            } else {
                                // del widget
                                eduContext?.widgetContext()
                                    ?.setWidgetInActive(widgetId = id, isRemove = true)
                            }
                        }
                    }
                    else -> {
                    }
                }
            }
        }
    }
    private val roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)
            //检查大窗widget状态，如果进入教室时大窗就是打开的，则创建widget，
            // 该widget可能渲染老师大窗视频流也可能是屏幕共享流
            eduContext?.widgetContext()?.getAllWidgetActive()?.forEach { entry ->
                if (entry.key.startsWith(strStreamWindow)) {
                    if (entry.value) {
                        ContextCompat.getMainExecutor(context).execute {
                            createWidget(entry.key)
                        }
                    }
                }
            }
            val platformEnable =
                (eduContext?.roomContext()?.getRoomProperties()
                    ?.get(StaticData.platformEnableKey) as? Double)?.toInt() ?: 1 == State.YES.value
            updateContentMargin(platformEnable)
        }

        override fun onRoomPropertiesUpdated(
            properties: Map<String, Any>, cause: Map<String, Any>?,
            operator: AgoraEduContextUserInfo?
        ) {
            super.onRoomPropertiesUpdated(properties, cause, operator)
            // 解析并判断讲台是否关闭
            cause?.get(CMD)?.let { cmd ->
                if (cmd == platformEnableCMD) {
                    val platformEnable =
                        (properties[StaticData.platformEnableKey] as Double).toInt() == State.YES.value
                    updateContentMargin(platformEnable)
                }
            }
        }
    }

    // widget's direct container
    private val widgetContainerMap = mutableMapOf<String, ViewGroup>()
    private val animatorUtil = AnimatorUtil()

    // 讲台是否关闭(小班课时，讲台关闭后组件区域整体向上移动limitTop/2的高度)
    var platformEnable = true

    // 视频最终显示区域的限制，取决于讲台高度或宽；小班课时讲台在上方，limitTop有效，
    // 一对一和大班课讲台在右边，limitEnd有效
    var limitTop = 0
    var limitEnd = 0
    var videoPresenter: FCRVideoPresenter? = null
    var serviceType: AgoraServiceType? = null
    var localStreamTypeListener: ILocalStreamTypeListener? = null

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        (binding.agoraEduScreenShare.layoutParams as? MarginLayoutParams)?.let {
            it.topMargin = limitTop
            it.rightMargin = limitEnd
        }
        eduContext?.roomContext()?.addHandler(roomHandler)
        eduContext?.streamContext()?.addHandler(streamHandler)
        binding.agoraEduScreenShare.initView(agoraUIProvider)
        addOrRemoveActiveObserver()//观察Active状态
        eduContext?.widgetContext()
            ?.addWidgetMessageObserver(videoWidgetMsgObserver, LargeWindow.id)
    }

    // 当移除屏幕分享的时候，数据流列表就没有了
    private var streamHandler = object : StreamHandler() {
        override fun onStreamLeft(
            streamInfo: AgoraEduContextStreamInfo,
            operator: AgoraEduContextUserInfo?
        ) {
            super.onStreamLeft(streamInfo, operator)
            if (streamInfo.videoSourceType == AgoraEduContextVideoSourceType.Screen) {
                // 关闭屏幕共享，老师退出和关闭的时候，数据流列表就没有了
                uiHandler.post {
                    binding.agoraEduScreenShare.visibility = GONE
                }
                binding.agoraEduScreenShare.updateScreenShareState(
                    EduContextScreenShareState.Stop,
                    streamInfo
                )
            }
        }

        override fun onStreamUpdated(
            streamInfo: AgoraEduContextStreamInfo,
            operator: AgoraEduContextUserInfo?
        ) {
            super.onStreamUpdated(streamInfo, operator)
            if (serviceType != AgoraServiceType.CDN &&
                serviceType != AgoraServiceType.Fusion
            ) {
                return
            }

            if (operator?.role == AgoraEduContextUserRole.Student &&
                !isLocalStream(streamInfo.streamUuid)
            ) {
                if (CDNUtils.haveCdnStream(streamInfo)) {
                    LogX.d(tag, "onStreamUpdated: cdn stream ${streamInfo.streamUuid}")
                    doRenderCDNStream(streamInfo.streamUuid)
                }
            }
        }
    }

    /**
     * 创建widget(分 初始化时创建课堂内原有的widget 和 进入课堂之后添加的widget两种情况)
     * @param widgetId
     * @param needAnimation 是否需要使用动画移动到目的位置
     * 初始化时不需要动画，只有加入房间后添加的widget才需要从讲台区域移动到目的位置
     */
    private fun createWidget(widgetId: String, needAnimation: Boolean = false) {
        if (videoWidgets.contains(widgetId)) { // widgetId：streamWindow-1869983176
            LogX.w(tag, "'$widgetId' is already created")
            return
        }

        if (widgetId.contains(strStreamWindow)) {
            val str = widgetId.split(dash)

            val widgetConfig = eduContext?.widgetContext()?.getWidgetConfig(str[0])
            widgetConfig?.let { config ->
                config.widgetId = widgetId
                // add syncFrameObserver
                addOrRemoveSyncFrameObserver(widgetId = widgetId)
                // create widget and init
                val tmpWidget = eduContext?.widgetContext()?.create(config)
                if (tmpWidget is AgoraUILargeVideoWidget) {
                    tmpWidget.largeVideoListener = this
                }
                tmpWidget?.let {
                    // register some listener for UI
                    (it as? AgoraUILargeVideoWidget)?.let { widget ->
                        widget.largeVideoListener = this
                        uiDataProvider?.addListener(widget.uiDataProviderListener)
                    }
                    // record widget
                    videoWidgets[widgetId] = it
                    // widget's direct parentView
                    val widgetDirectParent = LinearLayout(context).apply { this.gravity = CENTER }
                    // 垂直层级follow老师端
                    getWidgetExtra(it)?.zIndex?.let { zIndex -> widgetDirectParent.z = zIndex }
                    // 获取动画起始位置
                    val rect = videoPresenter?.getVideoPosition(str[1])
                    if (needAnimation && rect != null) {
                        // widget的起始位置和其实宽高
                        val params = LayoutParams(rect.right - rect.left, rect.bottom - rect.top)
                        params.leftMargin = rect.left
                        params.topMargin = rect.top
                        binding.root.addView(widgetDirectParent, params)
                        // init widget's ui
                        it.init(widgetDirectParent)
                        // 获取widget的比例信息(如果为空，则使用默认值)
                        val frame = eduContext?.widgetContext()?.getWidgetSyncFrame(widgetId)
                            ?: AgoraWidgetFrame(
                                defaultPositionPercent, defaultPositionPercent,
                                defaultSizePercent, defaultSizePercent
                            )
                        // 使用动画移动到目的位置
                        animator1(widgetDirectParent, frame)
                        widgetContainerMap[widgetId] = widgetDirectParent
                    } else {
                        // 不需要动画，直接把widget添加到目的位置
                        // add widgetContainer to binding.root(allWidgetsContainer)
                        val params = layoutWidgetDirectParent(binding.root, widgetId)
                        binding.root.addView(widgetDirectParent, params)
                        // init widget's ui
                        it.init(widgetDirectParent)
                    }
                    widgetContainerMap[widgetId] = widgetDirectParent
                    handleLargeWindowEvent(widgetId, true)
                }
            }
        }
    }

    /**
     * layout widget's direct parentView position
     * @param widgetId
     */
    @UiThread
    private fun layoutWidgetDirectParent(
        allWidgetsContainer: ViewGroup,
        widgetId: String
    ): LayoutParams {
        // 获取初始的比例信息，
        val frame = eduContext?.widgetContext()?.getWidgetSyncFrame(widgetId)
        // 计算widget的宽高，如果size比例信息为空，则使用默认比例
        val allWidgetsContainerWidth = allWidgetsContainer.width - limitEnd
        val width = allWidgetsContainerWidth.toFloat() * (frame?.width ?: defaultSizePercent)
        val allWidgetsContainerHeight = allWidgetsContainer.height - limitTop
        val height = allWidgetsContainerHeight.toFloat() * (frame?.height ?: defaultSizePercent)
        val params = LayoutParams(width.roundToInt(), height.roundToInt())
        // 如果frame.size有效，则按照比例计算，否则直接放在allWidgetsContainer的中央
        params.leftMargin = (frame?.x?.let { it * (allWidgetsContainerWidth - width) }
            ?: defaultPositionPercent * (allWidgetsContainerWidth - width)).roundToInt()
        params.topMargin = (frame?.y?.let { it * (allWidgetsContainerHeight - height) + limitTop }
            ?: defaultPositionPercent * (allWidgetsContainerHeight - height) + limitTop).roundToInt()
        return params
    }

    private fun getWidgetExtra(widget: AgoraBaseWidget): FCRLargeVideoWidgetExtra? {
        return widget.widgetInfo?.roomProperties?.let { GsonUtil.toJson(it) }?.let {
            GsonUtil.jsonToObject<FCRLargeVideoWidgetExtra>(it)
        }
    }

    /**
     * 销毁widget
     * 销毁时如果讲台处于打开状态且此用户没有下台，需要从组件区域的起始位置动画移动到讲台区域的目的位置
     */
    private fun destroyWidget(widgetId: String) {
        val destroyWidgetRunnable = {
            handleLargeWindowEvent(widgetId, false)
            // remove from map
            val widget = videoWidgets.remove(widgetId)
            // remove observer
            eduContext?.widgetContext()?.removeWidgetSyncFrameObserver(
                this@FCRVocationalLargeWindowContainerComponent, widgetId
            )
            widget?.let { it1 ->
                it1.container?.let { group ->
                    runOnUIThread { binding.root.removeView(group) }
                    widgetContainerMap.remove(widgetId)
                }
                // 避免多次渲染
                if (it1 is AgoraUILargeVideoWidget) {
                    it1.largeVideoListener = null
                }
                it1.release()
            }
        }
        val streamUuid = widgetId.split(dash)[1]
        eduContext?.streamContext()?.getAllStreamList()?.find { it.streamUuid == streamUuid }?.let {
            if (it.videoSourceType == Camera) {
                widgetContainerMap[widgetId]?.let { directParent ->
                    val animationEndListener = object : FCRAnimatorListener() {
                        override fun onAnimationEnd(animation: Animator, isReverse: Boolean) {
                            super.onAnimationEnd(animation, isReverse)
                            // 动画执行结束，销毁组件区域的视频大窗widget并在讲台区域恢复渲染
                            destroyWidgetRunnable.invoke()
                        }
                    }
                    // 获取视频在讲台区域的位置
                    var rect = videoPresenter?.getVideoPosition(streamUuid)
                    if (rect != null) {
                        // 在讲台区域话还存在则动画恢复
                        animator2(directParent, rect, animationEndListener)
                    } else {
                        // 在讲台区域不存在，则直接恢复
                        (directParent.layoutParams as? LayoutParams)?.let { params ->
                            // 缩成一个点，然后消失
                            rect = Rect(
                                params.leftMargin,
                                params.topMargin,
                                params.leftMargin,
                                params.topMargin
                            )
                            animator2(directParent, rect!!, animationEndListener)
                        }
                    }
                }
            } else if (it.videoSourceType == AgoraEduContextVideoSourceType.Screen) {
                // 是屏幕分享流，直接特殊处理
                destroyWidgetRunnable.invoke()
            }
            return
        }
        // 本地找不到对应的流信息，说明已经下台，直接销毁widget即可
        destroyWidgetRunnable.invoke()
    }

    override fun release() {
        super.release()
        videoWidgets.forEach {
            // remove UIDataProviderListener
            it.value.release()
            // remove syncFrame observer
            addOrRemoveSyncFrameObserver(add = false, it.key)
        }
        videoWidgets.clear()
        widgetContainerMap.clear()
        addOrRemoveActiveObserver(add = false)
        eduContext?.roomContext()?.removeHandler(roomHandler)
        eduContext?.streamContext()?.removeHandler(streamHandler)
        eduContext?.widgetContext()
            ?.removeWidgetMessageObserver(videoWidgetMsgObserver, LargeWindow.id)
    }

    private fun addOrRemoveActiveObserver(add: Boolean = true) {
        if (add) {
            eduContext?.widgetContext()
                ?.addWidgetActiveObserver(widgetActiveObserver, LargeWindow.id)
        } else {
            eduContext?.widgetContext()
                ?.removeWidgetActiveObserver(widgetActiveObserver, LargeWindow.id)
        }
    }

    private fun addOrRemoveSyncFrameObserver(add: Boolean = true, widgetId: String) {
        if (add) {
            eduContext?.widgetContext()?.addWidgetSyncFrameObserver(this, widgetId)
        } else {
            eduContext?.widgetContext()?.removeWidgetSyncFrameObserver(this, widgetId)
        }
    }

    private fun isLocalStream(streamUuid: String): Boolean {
        eduContext?.userContext()?.getLocalUserInfo()?.userUuid?.let { userId ->
            eduContext?.streamContext()?.getStreamList(userId)?.forEach { streamInfo ->
                if (streamInfo.streamUuid == streamUuid) {
                    return true
                }
            }
        }
        return false
    }

    private fun switchLocalStreamType(localIsCoHost: Boolean) {
        val streamUuid = eduCore?.localUser()?.userInfo?.streamUuid
        streamUuid?.let {
            if (localIsCoHost) {
                // 自己上台了，打开自身publish cdn流开关， 渲染自身rtc流，停止其他学生的cdn流，并切换成rtc流
                LogX.d(tag, "local user $streamUuid become cohost and publish cdn stream")
                val encoderConfig = VideoUtils().getDefaultVideoEncoderConfigs()
                val rtmpStreamConfig = FcrRtmpStreamConfig(streamUuid, encoderConfig.width, encoderConfig.height, encoderConfig.bitRate)
                eduContext?.streamContext()?.startPublishStreamToCdn(rtmpStreamConfig, object :
                    HttpCallback<HttpBaseRes<Any>>() {
                    override fun onSuccess(result: HttpBaseRes<Any>?) {
                        LogX.d(tag, "publish cdn stream success ")
                    }
                })
                videoWidgets.forEach {
                    val uuid = it.key.split(dash)[1]
                    // make sure large video view not null
                    val otherViewGroup = (it.value as? AgoraUILargeVideoWidget)?.largeVideoView!!.videoViewParent
                    eduContext?.streamContext()?.getAllStreamList()?.find { st ->
                        st.streamUuid == uuid
                    }?.let { streamInfo ->
                        LogX.d(tag, "student $uuid from cdn to rtc stream")
                        if (!isLocalStream(streamInfo.streamUuid)) {
                            CDNUtils.stopCdnStream(eduCore, streamInfo)
                        }
                        eduContext?.mediaContext()?.startRenderVideo(otherViewGroup, uuid)
                    }
                }
                localStreamTypeListener?.onLocalStreamTypeChange(true)
            } else {
                // 自己下台了，停掉自己publish cdn.停掉所有学生的rtc流（包括自己），并切换其他学生为cdn流
                LogX.d(tag, "local user $streamUuid audience and stop publish cdn stream")
                eduContext?.streamContext()?.stopPublishStreamToCdn(streamUuid, null)
                videoWidgets.forEach {
                    val uuid = it.key.split(dash)[1]
                    LogX.d(tag, "student $uuid from rtc to cdn stream")
                    eduContext?.mediaContext()?.stopRenderVideo(uuid)
                    if (!isLocalStream(uuid)) {
                        doRenderCDNStream(uuid)
                    }
                }
                localStreamTypeListener?.onLocalStreamTypeChange(false)
            }
        }
    }

    private fun doRenderRtcStream(streamUuid: String) {
        eduCore?.eduContextPool()?.streamContext()?.getAllStreamList()?.find {
            it.streamUuid == streamUuid
        }?.let { streamInfo ->
            val key = strStreamWindow.plus("-").plus(streamInfo.streamUuid)
            videoWidgets[key]?.let {
                (it as? AgoraUILargeVideoWidget)?.let { widget ->
                    val viewGroup = widget.largeVideoView?.videoViewParent
                    eduContext?.mediaContext()?.startRenderVideo(viewGroup!!, streamUuid)
                }
            }
        }
    }

    private fun doRenderCDNStream(streamUuid: String) {
        eduCore?.eduContextPool()?.streamContext()?.getAllStreamList()?.find {
            it.streamUuid == streamUuid
        }?.let { streamInfo ->
            val key = strStreamWindow.plus("-").plus(streamInfo.streamUuid)
            videoWidgets[key]?.let {
                (it as? AgoraUILargeVideoWidget)?.let { widget ->
                    val viewGroup = widget.largeVideoView?.videoViewParent
                    if (CDNUtils.haveCdnStream(streamInfo)) {
                        CDNUtils.renderCdnStream(eduCore, streamInfo, viewGroup)
                    }
                }
            }
        }
    }

    /**
     * 恢复小窗渲染
     */
    private fun handleLargeWindowEvent(widgetId: String, active: Boolean) {
        val currentStreamUuid = widgetId.split(dash)[1] //拿到大窗要渲染的streamUuid
        if (active) {
            //为了给小窗item 用户记录相应的大窗开启状态
            eduContext?.roomContext()?.getRoomInfo()?.roomUuid?.let { mRoomUuid ->
                FCRLargeWindowManager.addLargeWindow(mRoomUuid, currentStreamUuid)
                // 切换成大流
                eduContext?.streamContext()?.setRemoteVideoStreamSubscribeLevel(currentStreamUuid, AgoraEduContextVideoSubscribeLevel.HIGH)
            }
        } else {
            //移除该用户大窗打开的状态
            eduContext?.roomContext()?.getRoomInfo()?.roomUuid?.let { mRoomUuid ->
                FCRLargeWindowManager.removeLargeWindow(mRoomUuid, currentStreamUuid)
                // 切换成小流
                eduContext?.streamContext()?.setRemoteVideoStreamSubscribeLevel(currentStreamUuid, AgoraEduContextVideoSubscribeLevel.LOW)
            }
        }
        videoWidgets[widgetId]?.widgetInfo?.let { widgetInfo ->
            widgetInfo.roomProperties?.let { properties ->
                (properties[extraUserUuidKey] as? String)?.let { userId ->
                    // Edu context api does not provide an API to
                    // obtain the info of a certain single user
                    val largeWindowStreamUuid =
                        widgetInfo.widgetId.split(dash)[1] //拿到大窗要渲染的streamUuid
                    eduContext?.userContext()?.getAllUserList()
                        ?.find { eduUserInfo ->//找打userId对应的用户
                            eduUserInfo.userUuid == userId
                        }?.let { userInfo -> //找到properties中对应的用户
                            eduContext?.streamContext()?.getStreamList(userInfo.userUuid)
                                ?.find { eduStreamInfo ->
                                    eduStreamInfo.streamUuid == largeWindowStreamUuid // 现在也可能是屏幕共享流，移除的时候，没有屏幕共享流
                                }?.let { streamInfo ->
                                    //找到对应的用户和用户的流信息
                                    sendToLargeWindow(active, userInfo, streamInfo)
                                }
                        }
                }
            }
        }
    }

    //发送消息给大窗Widget
    private fun sendToLargeWindow(
        active: Boolean,
        userInfo: AgoraEduContextUserInfo,
        streamInfo: AgoraEduContextStreamInfo
    ) {
        //判断流信息为视频流还是屏幕分享流
        if (streamInfo.videoSourceType == AgoraEduContextVideoSourceType.Camera) {
            buildLargeWindowUserInfoData(userInfo, streamInfo)?.let {
                val signal = if (active) {
                    AgoraLargeWindowInteractionSignal.LargeWindowShowed
                } else {
                    AgoraLargeWindowInteractionSignal.LargeWindowClosed
                }
                /*if (active) {//打开大窗 屏幕分享要关闭
                    binding.agoraEduScreenShare.updateScreenShareState(EduContextScreenShareState.Stop, lastScreenInfo)
                }*/
                val packet = AgoraLargeWindowInteractionPacket(signal, it)
                //发送给AgoraUILargeVideoWidget
                eduContext?.widgetContext()?.sendMessageToWidget(
                    Gson().toJson(packet), LargeWindow.id + dash + streamInfo.streamUuid
                )
            }
        } else if (streamInfo.videoSourceType == AgoraEduContextVideoSourceType.Screen) {
            //通知大窗更新屏幕分享component
            buildLargeWindowUserInfoData(userInfo, streamInfo)?.let {
                if (active) {
                    lastScreenInfo = streamInfo.streamUuid//记录本次屏幕分享流
                    uiHandler.post {
                        binding.agoraEduScreenShare.visibility = VISIBLE
                    }
                    binding.agoraEduScreenShare.updateScreenShareState(
                        EduContextScreenShareState.Start,
                        streamInfo
                    )

                } else {
                    uiHandler.post {
                        binding.agoraEduScreenShare.visibility = GONE
                    }
                    binding.agoraEduScreenShare.updateScreenShareState(
                        EduContextScreenShareState.Stop,
                        streamInfo
                    )
                }
            }
        }
    }

    private fun buildLargeWindowUserInfoData(
        userInfo: AgoraEduContextUserInfo,
        streamInfo: AgoraEduContextStreamInfo
    ): AgoraUIUserDetailInfo? {
        val localVideoState: AgoraEduContextDeviceState2?
        val localAudioState: AgoraEduContextDeviceState2?
        if (userInfo.userUuid == eduContext?.userContext()?.getLocalUserInfo()?.userUuid) {
            localVideoState = uiDataProvider?.localVideoState
            localAudioState = uiDataProvider?.localAudioState
        } else {
            localVideoState = null
            localAudioState = null
        }

        return uiDataProvider?.toAgoraUserDetailInfo(
            userInfo,
            true, streamInfo, localAudioState, localVideoState
        )
    }

    override fun onLargeVideoShow(streamUuid: String) {
        //开启大窗后，如果是本地用户的视频流，需要重新设置帧率码率
//        if (isLocalStream(streamUuid)) {
//            val configs = VideoUtils().getHDEncoderConfigs()
//            eduContext?.streamContext()?.setLocalVideoConfig(streamUuid, configs)
//        } else {
//            eduContext?.streamContext()?.setRemoteVideoStreamSubscribeLevel(
//                streamUuid,
//                AgoraEduContextVideoSubscribeLevel.HIGH
//            )
//        }

        if (serviceType == AgoraServiceType.Fusion) {
            if (isLocalStream(streamUuid)) {
                LogX.w(tag, "local user switch rtc")
                switchLocalStreamType(true)
            } else {
                // streamWindow-localstreamuuid"
                val key = strStreamWindow.plus("-").plus(eduCore?.localUser()?.userInfo?.streamUuid)
                if (videoWidgets.contains(key)) {
                    // 当前用户在台上，直接render rtc
                    LogX.d(tag, "$streamUuid add cohost render rtc")
                    doRenderRtcStream(streamUuid)
                } else {
                    // 当前用户在不在台上，直接render cdn流
                    LogX.d(tag, "$streamUuid add cohost render cdn")
                    doRenderCDNStream(streamUuid)
                }
            }
        }
    }

    override fun onLargeVideoDismiss(streamUuid: String) {
        //关闭大窗后，如果是本地用户的视频流，需要重新设置帧率码率
//        if (isLocalStream(streamUuid)) {
//            val configs = VideoUtils().getDefaultVideoEncoderConfigs()
//            eduContext?.streamContext()?.setLocalVideoConfig(streamUuid, configs)
//        } else {
//            eduContext?.streamContext()?.setRemoteVideoStreamSubscribeLevel(
//                streamUuid,
//                AgoraEduContextVideoSubscribeLevel.LOW
//            )
//        }

        if (serviceType == AgoraServiceType.Fusion) {
            if (isLocalStream(streamUuid)) {
                LogX.w(tag, "local user switch cdn")
                switchLocalStreamType(false)
            } else {
                LogX.w(tag, "$streamUuid remove cohost")
                eduCore?.eduContextPool()?.streamContext()?.getAllStreamList()?.find {
                    streamUuid == it.streamUuid
                }.let {
                    CDNUtils.stopCdnStream(eduCore, it)
                }
            }
        }
    }

    // FCRWidgetSyncFrameObserver
    override fun onWidgetSyncFrameUpdated(syncFrame: AgoraWidgetFrame, widgetId: String) {
        Log.e(tag, "onWidgetSyncFrameUpdated:$syncFrame")
        // 根据老师发过来的syncFrame计算widget新的位置和宽高，并使用动画移动至目的地
        // 同时更新其view层级
        widgetContainerMap[widgetId]?.let { directParent ->
            videoWidgets[widgetId]?.let {
                getWidgetExtra(it)?.zIndex?.let { zIndex -> directParent.z = zIndex }
            }
            animator1(directParent, syncFrame)
        }
    }

    /**
     * 根据syncFrame中的比例信息把directParent位移到目的位置，并在此过程中缩放directParent到目的宽高
     * 主要用在视频从讲台移动到组件区域的过程
     */
    //@Synchronized
    private fun animator1(directParent: View, syncFrame: AgoraWidgetFrame) {
        //所有widget的祖父布局
        val grandParent = binding.root
        // widget父布局的layoutParams
        val layoutParams = directParent.layoutParams as? LayoutParams
        layoutParams?.let { params ->
            // 祖父布局中对于widget的有效宽度(可以认为是白板的宽度)
            val allWidgetsContainerWidth = grandParent.width - limitEnd
            // widget宽度
            val width =
                syncFrame.width?.let { allWidgetsContainerWidth * it } ?: params.width.toFloat()
            // 祖父布局中对于widget的有效高度(可以认为是白板的高度)
            val allWidgetsContainerHeight = grandParent.height - limitTop
            // widget高度
            val height =
                syncFrame.height?.let { allWidgetsContainerHeight * it } ?: params.height.toFloat()
            // widget在x轴上可移动的有效范围
            val medWidth = allWidgetsContainerWidth - width
            // widget在y轴上可移动的有效范围
            val medHeight = allWidgetsContainerHeight - height
            // widget的坐标
            val left = syncFrame.x?.let { medWidth * it }
            val top = syncFrame.y?.let { medHeight * it + limitTop }
            LogX.i(
                "$tag->grandParentWidth:${grandParent.width}, grandParentHeight:${grandParent.height}, " +
                    "directParentWidth:${params.width}, directParentHeight:${params.height}, " + "medWidth:$medWidth, " +
                    "medHeight:$medHeight, left:$left, top:$top, width:$width, height:$height"
            )
            // 构建动画参数
            val parameters = arrayOfNulls<Float>(8)
            // widget位移起始点
            parameters[0] = params.leftMargin.toFloat()
            parameters[2] = params.topMargin.toFloat()
            if (syncFrame.positionValid()) {
                // widget位移终点
                parameters[1] = left!!
                parameters[3] = top!!
            }
            // widget缩放起点
            parameters[4] = params.width.toFloat()
            parameters[6] = params.height.toFloat()
            if (syncFrame.sizeValid()) {
                // widget缩放终点
                parameters[5] = width
                parameters[7] = height
            }
            ContextCompat.getMainExecutor(context).execute {
                animatorUtil.translateScale2(directParent,
                    parameters[0]!!,
                    parameters[1],
                    parameters[2]!!,
                    parameters[3],
                    parameters[4]!!,
                    parameters[5],
                    parameters[6]!!,
                    parameters[7],
                    object : FCRAnimatorListener() {
                        override fun onAnimationUpdate(fraction: Float) {
                            super.onAnimationUpdate(fraction)
                            // Log.e(tag, "fraction:$fraction")
                            // 根据fraction计算widget当前的坐标
                            if (syncFrame.positionValid()) {
                                val leftDiff: Float = left!! - parameters[0]!!
                                val leftTmp = leftDiff * fraction
                                val topDiff: Float = top!! - parameters[2]!!
                                val topTmp = topDiff * fraction
                                params.leftMargin = (parameters[0]!! + leftTmp).roundToInt()
                                params.topMargin = (parameters[2]!! + topTmp).roundToInt()
                            }
                            // 根据fraction计算widget当前的宽高
                            if (syncFrame.sizeValid()) {
                                val widthDiff = width - parameters[4]!!
                                val widthTmp = widthDiff * fraction
                                params.width = (parameters[4]!! + widthTmp).roundToInt()
                                val heightDiff = height - parameters[6]!!
                                val heightTmp = heightDiff * fraction
                                params.height = (parameters[6]!! + heightTmp).roundToInt()
                            }
                            directParent.layoutParams = params
                        }

                        override fun onAnimationEnd(animation: Animator, isReverse: Boolean) {
                            super.onAnimationEnd(animation, isReverse)
                            directParent.requestLayout()
                        }
                    })
            }
        }
    }

    /**
     * 根据syncFrame中的比例信息把directParent位移到目的位置，并在此过程中缩放directParent到目的大小
     * 并在动画结束之后回调结束事件
     * 主要用在视频恢复到讲台的过程
     * @param destRect widget的目的位置和宽高
     */
    private fun animator2(
        directParent: View,
        destRect: Rect,
        listener: FCRAnimatorListener? = null
    ) {
        val layoutParams = directParent.layoutParams as? LayoutParams
        layoutParams?.let { params ->
            // 构建动画参数，widget当前的位置和宽高为起始
            val parameters = arrayOfNulls<Float>(8)
            parameters[0] = params.leftMargin.toFloat()
            parameters[2] = params.topMargin.toFloat()
            parameters[1] = destRect.left.toFloat()
            parameters[3] = destRect.top.toFloat()
            parameters[4] = params.width.toFloat()
            parameters[6] = params.height.toFloat()
            parameters[5] = (destRect.right - destRect.left).toFloat()
            parameters[7] = (destRect.bottom - destRect.top).toFloat()
            runOnUIThread {
                animatorUtil.translateScale2(directParent,
                    parameters[0]!!,
                    parameters[1],
                    parameters[2]!!,
                    parameters[3],
                    parameters[4]!!,
                    parameters[5],
                    parameters[6]!!,
                    parameters[7],
                    object : FCRAnimatorListener() {
                        override fun onAnimationEnd(animation: Animator, isReverse: Boolean) {
                            super.onAnimationEnd(animation, isReverse)
                            // 回调动画结束事件
                            listener?.onAnimationEnd(animation, isReverse)
                        }

                        override fun onAnimationUpdate(fraction: Float) {
                            super.onAnimationUpdate(fraction)
                            // 根据fraction计算widget当前的坐标
                            val leftDiff: Float = parameters[1]!! - parameters[0]!!
                            val leftTmp = leftDiff * fraction
                            val topDiff: Float = parameters[3]!! - parameters[2]!!
                            val topTmp = topDiff * fraction
                            params.leftMargin = (parameters[0]!! + leftTmp).roundToInt()
                            params.topMargin = (parameters[2]!! + topTmp).roundToInt()
                            // 根据fraction计算widget当前的宽高
                            val widthDiff = parameters[5]!! - parameters[4]!!
                            val widthTmp = widthDiff * fraction
                            params.width = (parameters[4]!! + widthTmp).roundToInt()
                            val heightDiff = parameters[7]!! - parameters[6]!!
                            val heightTmp = heightDiff * fraction
                            params.height = (parameters[6]!! + heightTmp).roundToInt()
                            directParent.layoutParams = params
                        }
                    })
            }
        }
    }

    /**
     * 当讲台关闭时，布局内容整体向上移动；开启时恢复
     */
    fun updateContentMargin(platformEnable: Boolean) {
        runOnUIThread {
            val margin = (limitTop * 0.5F).roundToInt()
            (binding.root.layoutParams as? MarginLayoutParams)?.let {
                it.topMargin = if (platformEnable) 0 else -margin
                //it.bottomMargin = if (platformEnable) 0 else margin
                binding.root.layoutParams = it
            }
        }
    }

    interface ILocalStreamTypeListener {
        fun onLocalStreamTypeChange(isRtc: Boolean)
    }
}
