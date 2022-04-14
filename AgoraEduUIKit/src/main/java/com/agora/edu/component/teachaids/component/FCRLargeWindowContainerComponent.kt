package com.agora.edu.component.teachaids.component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.agora.edu.component.common.AbsAgoraEduComponent
import com.agora.edu.component.common.IAgoraUIProvider
import com.agora.edu.component.teachaids.*
import io.agora.agoraeducore.core.internal.framework.impl.managers.AgoraWidgetActiveObserver
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId.LargeWindow
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import com.agora.edu.component.teachaids.AgoraTeachAidWidgetInteractionSignal.ActiveState
import com.google.gson.Gson
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.education.impl.Constants.AgoraLog
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeduuikit.databinding.AgoraEduLargeWindowContainerComponentBinding
import io.agora.agoraeduuikit.impl.video.AgoraLargeWindowInteractionPacket
import io.agora.agoraeduuikit.impl.video.AgoraLargeWindowInteractionSignal
import io.agora.agoraeduuikit.impl.video.AgoraUILargeVideoWidget
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeduuikit.util.VideoUtils

/**
 * author : wufang
 * date : 2022/3/23
 * description :
 */
class FCRLargeWindowContainerComponent : AbsAgoraEduComponent, AgoraUILargeVideoWidget.IAgoraUILargeVideoListener {
    private val tag = "AgoraEduLargeWindowContainerComponent"

    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private val binding = AgoraEduLargeWindowContainerComponentBinding.inflate(
        LayoutInflater.from(context),
        this, true
    )

    private var largeVideoWindowWidget: AgoraBaseWidget? = null
    private var largeVideoWindowWidgetScreen: AgoraBaseWidget? = null
    private val teachAidWidgets = mutableMapOf<String, AgoraBaseWidget>()
    private val dash = "-"
    private val strStreamWindow:String = "streamWindow"
    private val widgetActiveObserver = object : AgoraWidgetActiveObserver {
        override fun onWidgetActive(widgetId: String) {
            uiHandler.post {
                createWidget(widgetId)
            }
        }

        override fun onWidgetInActive(widgetId: String) {
            destroyWidget(widgetId)
        }
    }

    private val largeWindowWidgetMsgObserver = object : AgoraWidgetMessageObserver {
        override fun onMessageReceived(msg: String, id: String) {
            val packet = GsonUtil.jsonToObject<AgoraTeachAidWidgetInteractionPacket>(msg)
            packet?.let {
                when (packet.signal) {
                    ActiveState -> {
                        val data = GsonUtil.jsonToObject<AgoraTeachAidWidgetActiveStateChangeData>(packet.body.toString())
                        if (data?.active == true) {
                            eduContext?.widgetContext()?.setWidgetActive(widgetId = id, roomProperties = data.properties)
                        } else {
                            // del widget
                            eduContext?.widgetContext()?.setWidgetInActive(widgetId = id, isRemove = true)
                        }
                    }
                    else -> {
                    }
                }
            }
        }
    }

    // listen joinRoomSuccess event
    private val roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)
            //检查大窗widget状态，如果进入教室时大窗就是打开的，则创建widget，
            // 该widget可能渲染老师大窗视频流也可能是屏幕共享流

            eduContext?.widgetContext()?.getAllWidgetActive()?.forEach {
                if (it.key.startsWith(strStreamWindow)) {
                    if (it.value) {
                        var stream = eduContext?.streamContext()?.getAllStreamList()
                            ?.find { it.owner.role == AgoraEduContextUserRole.Teacher && it.videoSourceType == AgoraEduContextVideoSourceType.Screen }
                        if (stream == null) {
                            stream = eduContext?.streamContext()?.getAllStreamList()
                                ?.find { it.owner.role == AgoraEduContextUserRole.Teacher && it.videoSourceType == AgoraEduContextVideoSourceType.Camera }
                        }
                        createWidget(it.key + dash + stream?.streamUuid)
                    }
                }
            }
        }
    }

    private fun createWidget(widgetId: String) {
        if (teachAidWidgets.contains(widgetId)) {
            AgoraLog?.w("$tag->'$widgetId' is already created")
            return
        }
        if (widgetId.contains(strStreamWindow)) {
            var str = widgetId.split(dash)

            var widgetConfig = eduContext?.widgetContext()?.getWidgetConfig(str[0])
            widgetConfig?.let { config ->
                config.widgetId = widgetId
                //需要判断str[1]是否是屏幕共享流还是大窗视频流
                val streamInfo = eduContext?.streamContext()?.getAllStreamList()?.find { it.streamUuid == str[1] } //找到流uuid对应的流信息
                if (streamInfo?.videoSourceType == AgoraEduContextVideoSourceType.Camera) {
                    //大窗视频流
                    largeVideoWindowWidget = eduContext?.widgetContext()?.create(config)
                    if (largeVideoWindowWidget is AgoraUILargeVideoWidget) {
                        (largeVideoWindowWidget as AgoraUILargeVideoWidget).largeVideoListener = this
                    }
                    largeVideoWindowWidget?.let {
                        // record widget
                        teachAidWidgets[widgetId] = it
                        // create widgetContainer and add to binding.root(allWidgetsContainer)
                        val largeWindowContainer = managerWidgetsContainer(allWidgetsContainer = binding.root)
                        largeWindowContainer?.let { group ->
                            // init widget'ui
                            it.init(group)
                            uiDataProvider?.addListener((largeVideoWindowWidget as AgoraUILargeVideoWidget).uiDataProviderListener)
                        }
                        handleLargeWindowEvent(widgetId, true)
                    }
                } else {
                    //大窗视频流
                    largeVideoWindowWidgetScreen = eduContext?.widgetContext()?.create(config)
                    if (largeVideoWindowWidgetScreen is AgoraUILargeVideoWidget) {
                        (largeVideoWindowWidgetScreen as AgoraUILargeVideoWidget).largeVideoListener = this
                    }
                    largeVideoWindowWidgetScreen?.let {
                        // record widget
                        teachAidWidgets[widgetId] = it
                        // create widgetContainer and add to binding.root(allWidgetsContainer)
                        val largeWindowContainer = managerWidgetsContainer(allWidgetsContainer = binding.root)
                        largeWindowContainer?.let { group ->
                            // init widget'ui
                            it.init(group)
                            uiDataProvider?.addListener((largeVideoWindowWidgetScreen as AgoraUILargeVideoWidget).uiDataProviderListener)
                        }
                        handleLargeWindowEvent(widgetId, true)
                    }
                }


            }
        }
    }

    private fun managerWidgetsContainer(allWidgetsContainer: ViewGroup, willRemovedWidgetContainer: View? = null): ViewGroup? {
        if (willRemovedWidgetContainer == null) {
            val largeWindowContainer = RelativeLayout(context)
            val params = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            )
            params.startToStart = allWidgetsContainer.id
            params.topToTop = allWidgetsContainer.id
            params.endToEnd = allWidgetsContainer.id
            params.bottomToBottom = allWidgetsContainer.id
            allWidgetsContainer.handler.post { allWidgetsContainer.addView(largeWindowContainer, params) }
            return largeWindowContainer
        } else {
            allWidgetsContainer.handler.post {
                allWidgetsContainer.removeView(willRemovedWidgetContainer)
            }
            return null
        }
    }

    private fun destroyWidget(widgetId: String) {
        // remove from map
        val widget = teachAidWidgets.remove(widgetId)
        handleLargeWindowEvent(widgetId, false)
        widget?.let {
            it.container?.let { group ->
                managerWidgetsContainer(
                    allWidgetsContainer = binding.root,
                    willRemovedWidgetContainer = group
                )
            }
            it.release()
        }
    }

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        eduContext?.roomContext()?.addHandler(roomHandler)

        binding.agoraEduScreenShare.initView(agoraUIProvider)

        addAndRemoveActiveObserver()//观察Active状态
        eduContext?.widgetContext()?.addWidgetMessageObserver(largeWindowWidgetMsgObserver, LargeWindow.id)
    }

    override fun release() {
        super.release()
        teachAidWidgets.forEach {
            // remove UIDataProviderListener
            it.value.release()
        }
        teachAidWidgets.clear()
        addAndRemoveActiveObserver(add = false)
    }

    private fun addAndRemoveActiveObserver(add: Boolean = true) {
        if (add) {
            eduContext?.widgetContext()?.addWidgetActiveObserver(widgetActiveObserver, AgoraWidgetDefaultId.LargeWindow.id)
        } else {
            eduContext?.widgetContext()?.removeWidgetActiveObserver(widgetActiveObserver, AgoraWidgetDefaultId.LargeWindow.id)
        }
    }

    private fun isLocalStream(streamUuid: String): Boolean {
        eduContext?.let { context ->
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

    private fun handleLargeWindowEvent(widgetId: String, active: Boolean) {

        var str = widgetId.split(dash)
        val streamInfo = eduContext?.streamContext()?.getAllStreamList()?.find { it.streamUuid == str[1] } //找到流uuid对应的流信息
        if (streamInfo?.videoSourceType == AgoraEduContextVideoSourceType.Camera) {
            largeVideoWindowWidget?.widgetInfo?.let { widgetInfo ->
                widgetInfo.roomProperties?.let { properties ->
                    (properties["userUuid"] as? String)?.let { userId ->
                        // Edu context api does not provide an API to
                        // obtain the info of a certain single user
                        val largeWindowStreamUuid = widgetInfo?.widgetId.split(dash)[1] //拿到大窗要渲染的streamUuid
                        eduContext?.let { context ->
                            context.userContext()?.let { userContext ->
                                userContext.getAllUserList().find { eduUserInfo ->//找打userId对应的用户
                                    eduUserInfo.userUuid == userId
                                }?.let { userInfo -> //找到properties中对应的用户
//                                (properties["streamUuid"] as? String).let {
                                    context.streamContext()?.getStreamList(userInfo.userUuid)?.find { eduStreamInfo ->
                                        eduStreamInfo.streamUuid == largeWindowStreamUuid //现在也可能是屏幕共享流
                                    }?.let { streamInfo ->
                                        //找到对应的用户和用户的流信息
                                        sendToLargeWindow(active, userInfo, streamInfo)
                                    }
//                                }
                                }
                            }
                        }
                    }
                }

            }

        } else {
            largeVideoWindowWidgetScreen?.widgetInfo?.let { widgetInfo ->
                widgetInfo.roomProperties?.let { properties ->
                    (properties["userUuid"] as? String)?.let { userId ->
                        // Edu context api does not provide an API to
                        // obtain the info of a certain single user
                        val largeWindowStreamUuid = widgetInfo?.widgetId.split(dash)[1] //拿到大窗要渲染的streamUuid
                        eduContext?.let { context ->
                            context.userContext()?.let { userContext ->
                                userContext.getAllUserList().find { eduUserInfo ->//找打userId对应的用户
                                    eduUserInfo.userUuid == userId
                                }?.let { userInfo -> //找到properties中对应的用户
//                                (properties["streamUuid"] as? String).let {
                                    context.streamContext()?.getStreamList(userInfo.userUuid)?.find { eduStreamInfo ->
                                        eduStreamInfo.streamUuid == largeWindowStreamUuid //现在也可能是屏幕共享流
                                    }?.let { streamInfo ->
                                        //找到对应的用户和用户的流信息
                                        sendToLargeWindow(active, userInfo, streamInfo)
                                    }
//                                }
                                }
                            }
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

                val packet = AgoraLargeWindowInteractionPacket(signal, it)
                //发送给AgoraUILargeVideoWidget
                eduContext?.widgetContext()?.sendMessageToWidget(
                    Gson().toJson(packet), AgoraWidgetDefaultId.LargeWindow.id + dash + streamInfo.streamUuid
                )
            }
        } else {
            //通知大窗更新屏幕分享component
            buildLargeWindowUserInfoData(userInfo, streamInfo)?.let {
                val signal = if (active) {
                    AgoraLargeWindowInteractionSignal.ScreenShareOpened
                } else {
                    AgoraLargeWindowInteractionSignal.ScreenShareClosed
                }

//                val packet = AgoraLargeWindowInteractionPacket(signal, it)
//                //发送给AgoraUILargeVideoWidget
//                eduContext?.widgetContext()?.sendMessageToWidget(
//                    Gson().toJson(packet), AgoraWidgetDefaultId.LargeWindow.id + mDash + streamInfo.streamUuid
//                )

                if (active) {
                    binding.agoraEduScreenShare.updateScreenShareState(EduContextScreenShareState.Start, streamInfo.streamUuid)

                } else {
                    binding.agoraEduScreenShare.updateScreenShareState(EduContextScreenShareState.Stop, streamInfo.streamUuid)
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
        if (isLocalStream(streamUuid)) {
            val configs = VideoUtils().getVideoEditEncoderConfigs()
            eduContext?.streamContext()?.setLocalVideoConfig(streamUuid, configs)
        }
    }

    override fun onLargeVideoDismiss(streamUuid: String) {
        if (isLocalStream(streamUuid)) {
            val configs = VideoUtils().getSmallVideoEncoderConfigs()
            eduContext?.streamContext()?.setLocalVideoConfig(streamUuid, configs)
        }
    }

    override fun onRendererContainer(config: EduContextRenderConfig, viewGroup: ViewGroup?, streamUuid: String) {
        val noneView = viewGroup == null
        if (noneView) {
            eduContext?.mediaContext()?.stopRenderVideo(streamUuid)
        } else {
            eduContext?.mediaContext()?.startRenderVideo(config, viewGroup!!, streamUuid)
        }
    }
}
