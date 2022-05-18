package com.agora.edu.component.whiteboard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import com.agora.edu.component.common.AbsAgoraEduComponent
import com.agora.edu.component.common.IAgoraUIProvider
import com.google.gson.Gson
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.EduContextRoomInfo
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.impl.managers.AgoraWidgetActiveObserver
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.agoraeduuikit.databinding.AgoraEduWhiteboardComponetBinding
import io.agora.agoraeduuikit.impl.whiteboard.AgoraWhiteBoardWidget
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionPacket
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionSignal
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId.AgoraCloudDisk
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardGrantData

/**
 *  白板
 */
class AgoraEduWhiteBoardComponent : AbsAgoraEduComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private var binding: AgoraEduWhiteboardComponetBinding =
        AgoraEduWhiteboardComponetBinding.inflate(LayoutInflater.from(context), this, true)
    private var whiteBoardWidget: AgoraWhiteBoardWidget? = null
    var uuid: String? = null
    var appid: String = ""
    var isNeedShowLoading = true // 是否需要显示加载loading

    private val roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)
            if (eduContext?.widgetContext()?.getWidgetActive(AgoraWidgetDefaultId.WhiteBoard.id) == true) {// 获取白板widget的active状态
                createWhiteWidget()
            }
            // Check if there is a screen stream is sharing
//            uiDataProvider?.notifyScreenShareDisplay()
        }
    }

    private val widgetActiveObserver = object : AgoraWidgetActiveObserver {
        override fun onWidgetActive(widgetId: String) {
            //创建白板
            if (widgetId == AgoraWidgetDefaultId.WhiteBoard.id) {
                createWhiteWidget()
            }
        }

        override fun onWidgetInActive(widgetId: String) {
            if (widgetId == AgoraWidgetDefaultId.WhiteBoard.id) {
                uiHandler.post {
                    whiteBoardWidget?.hideWhiteboardTools()//关闭白板时，让白板工具按钮隐藏
                    binding.agoraEduWhiteboardContainer.removeAllViews() //清空白板容器内容
                    whiteBoardWidget?.cloudDiskMsgObserver?.let {
                        eduContext?.widgetContext()?.removeWidgetMessageObserver(it, AgoraCloudDisk.id)
                    }
                    whiteBoardWidget?.release()
                    whiteBoardWidget = null
                }

            }
        }
    }

    fun createWhiteWidget() {
        uiHandler.post {
            val config = eduContext?.widgetContext()?.getWidgetConfig(AgoraWidgetDefaultId.WhiteBoard.id)
            // transfer roomContext to startClass for teacher
            config?.extraInfo = eduContext?.roomContext()
            config?.let {
                whiteBoardWidget = eduContext?.widgetContext()?.create(it) as AgoraWhiteBoardWidget?
            }
            whiteBoardWidget?.let {
                eduContext?.widgetContext()?.addWidgetMessageObserver(it.cloudDiskMsgObserver, AgoraCloudDisk.id)
                it.uuid = uuid
                it.init(binding.agoraEduWhiteboardContainer)
                it.setAppid(appid)
            }

            // 分组小班课，默认有白板权限
            if (eduContext?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Teacher ||
                eduContext?.roomContext()?.getRoomInfo()?.roomType == RoomType.GROUPING_CLASS
            ) {
                writeBroadGrant()
            }
        }
    }

    /**
     * 白板授权
     */
    fun writeBroadGrant() {
        val userUuid = eduContext?.userContext()?.getLocalUserInfo()?.userUuid
        userUuid?.let {
            val data = AgoraBoardGrantData(true, arrayOf(userUuid).toMutableList())
            val packet = AgoraBoardInteractionPacket(AgoraBoardInteractionSignal.BoardGrantDataChanged, data)
            eduContext?.widgetContext()?.sendMessageToWidget(Gson().toJson(packet), AgoraWidgetDefaultId.WhiteBoard.id)
        }
    }

    fun initView(uuid: String?, agoraUIProvider: IAgoraUIProvider) {
        this.uuid = uuid
        initView(agoraUIProvider)
        appid = agoraUIProvider.getAgoraEduCore()?.config!!.appId
    }

    override fun release() {
        super.release()
        whiteBoardWidget?.release()
        whiteBoardWidget = null
    }

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
//        binding.agoraEduScreenShare.initView(agoraUIProvider)
        eduContext?.widgetContext()?.addWidgetActiveObserver(widgetActiveObserver, AgoraWidgetDefaultId.LargeWindow.id)
        eduContext?.widgetContext()?.addWidgetActiveObserver(widgetActiveObserver, AgoraWidgetDefaultId.WhiteBoard.id)
        eduContext?.widgetContext()?.addWidgetMessageObserver(whiteBoardObserver, AgoraWidgetDefaultId.WhiteBoard.id)
        eduContext?.roomContext()?.addHandler(roomHandler)
//        this.uiDataProvider?.addListener(object : UIDataProviderListenerImpl() {
//            override fun onScreenShareStart(info: AgoraUIUserDetailInfo) {
//                binding.agoraEduScreenShare.updateScreenShareState(EduContextScreenShareState.Start, info.streamUuid)
//            }
//
//            override fun onScreenShareStop(info: AgoraUIUserDetailInfo) {
//                binding.agoraEduScreenShare.updateScreenShareState(EduContextScreenShareState.Stop, info.streamUuid)
//            }
//        })
    }

    private val whiteBoardObserver = object : AgoraWidgetMessageObserver {
        override fun onMessageReceived(msg: String, id: String) {
            val packet = GsonUtil.gson.fromJson(msg, AgoraBoardInteractionPacket::class.java)
            when (packet.signal) {
                AgoraBoardInteractionSignal.BoardGrantDataChanged -> {
                    eduContext?.userContext()?.getLocalUserInfo()?.let { localUser ->
                        if (localUser.role == AgoraEduContextUserRole.Student) {
                            var granted = false
                            if (packet.body is MutableList<*>) { // 白板开关的格式
                                granted = (packet.body as? ArrayList<String>)?.contains(localUser.userUuid) ?: false
                            } else { // 白板授权的格式
                                val bodyStr = GsonUtil.gson.toJson(packet.body)
                                val agoraBoard = GsonUtil.gson.fromJson(bodyStr, AgoraBoardGrantData::class.java)
                                if (agoraBoard.granted) {
                                    granted = agoraBoard.userUuids.contains(localUser.userUuid) ?: false
                                }
                            }
                            ContextCompat.getMainExecutor(context).execute {
                                whiteBoardWidget?.setWhiteBoardControlView(granted)
                            }
                        }
                    }
                }
            }
        }
    }

    fun setHiddenLoading(){
        whiteBoardWidget?.setHiddenLoading()
    }

    fun setNotShowWhiteLoading() {
        whiteBoardWidget?.isNeedShowLoading = false
        setHiddenLoading()
    }
}