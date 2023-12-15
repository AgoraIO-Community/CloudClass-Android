package io.agora.online.component.whiteboard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import io.agora.online.component.common.AbsAgoraEduComponent
import io.agora.online.component.common.IAgoraUIProvider
import io.agora.online.component.whiteboard.data.AgoraEduApplianceData
import com.google.gson.Gson
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.EduContextRoomInfo
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.impl.managers.AgoraWidgetActiveObserver
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.online.impl.whiteboard.AgoraWhiteBoardWidget
import io.agora.online.impl.whiteboard.bean.AgoraBoardInteractionPacket
import io.agora.online.impl.whiteboard.bean.AgoraBoardInteractionSignal
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId.AgoraCloudDisk
import io.agora.online.databinding.FcrOnlineEduWhiteboardComponetBinding
import io.agora.online.impl.whiteboard.bean.AgoraBoardGrantData

/**
 *  白板
 */
class AgoraEduWhiteBoardComponent : AbsAgoraEduComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private var binding: FcrOnlineEduWhiteboardComponetBinding =
        FcrOnlineEduWhiteboardComponetBinding.inflate(LayoutInflater.from(context), this, true)
    private var whiteBoardWidget: AgoraWhiteBoardWidget? = null
    var uuid: String? = null
    var appid: String = ""

    /**
     * 大班课重新进入，需要移除我的白板授权，因为退出，下台了，避免开关白板权限收回
     */
    var isRemoveMyBoardGrants = true

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
                ContextCompat.getMainExecutor(context).execute {
                    whiteBoardWidget?.cloudDiskMsgObserver?.let {
                        eduContext?.widgetContext()?.removeWidgetMessageObserver(it, AgoraCloudDisk.id)
                    }
                    //whiteBoardWidget?.hideWhiteboardTools()//关闭白板时，让白板工具按钮隐藏
                    binding.agoraEduWhiteboardContainer.removeAllViews() //清空白板容器内容
                    whiteBoardWidget?.release()
                    whiteBoardWidget = null
                }
            }
        }
    }

    fun createWhiteWidget() {
        ContextCompat.getMainExecutor(context).execute {
            val config = eduContext?.widgetContext()?.getWidgetConfig(AgoraWidgetDefaultId.WhiteBoard.id)
            config?.let {
                whiteBoardWidget = eduContext?.widgetContext()?.create(it) as? AgoraWhiteBoardWidget
            }
            whiteBoardWidget?.let {
                eduContext?.widgetContext()?.addWidgetMessageObserver(it.cloudDiskMsgObserver, AgoraCloudDisk.id)
                it.uuid = uuid
                it.setAppid(appid)
                it.init(binding.agoraEduWhiteboardContainer)
            }
            //授权用户列表
            //val grantedUsers = whiteBoardWidget?.widgetInfo?.roomProperties?.get("grantedUsers") as? MutableMap<*, *>
            //LogX.e("whiteboard", "grantedUsers=${grantedUsers}")

            // 分组小班课，默认有白板权限
            if (eduContext?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Teacher ||
                eduContext?.roomContext()?.getRoomInfo()?.roomType == RoomType.GROUPING_CLASS
            ) {
                writeBroadGrant(true)
            } else if (isRemoveMyBoardGrants && eduContext?.roomContext()?.getRoomInfo()?.roomType == RoomType.LARGE_CLASS ) {
                // 大班课，重新进入收回权限
                //LogX.e("whiteboard", "大班课，重新进入收回权")
                writeBroadGrant(false)
                val userUuid = eduContext?.userContext()?.getLocalUserInfo()?.userUuid
                AgoraEduApplianceData.removeMyBoardGrant(whiteBoardWidget, userUuid)
            }
            isRemoveMyBoardGrants = false
        }
    }

    /**
     * 白板授权
     */
    fun writeBroadGrant(isGrant: Boolean) {
        val userUuid = eduContext?.userContext()?.getLocalUserInfo()?.userUuid
        userUuid?.let {
            val data = AgoraBoardGrantData(isGrant, arrayOf(userUuid).toMutableList())
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
        eduContext?.widgetContext()?.addWidgetActiveObserver(widgetActiveObserver, AgoraWidgetDefaultId.LargeWindow.id)
        eduContext?.widgetContext()?.addWidgetActiveObserver(widgetActiveObserver, AgoraWidgetDefaultId.WhiteBoard.id)
        eduContext?.widgetContext()?.addWidgetMessageObserver(whiteBoardObserver, AgoraWidgetDefaultId.WhiteBoard.id)
        eduContext?.roomContext()?.addHandler(roomHandler)
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
                            whiteBoardWidget?.setWhiteBoardControlView(granted)
                        }
                    }
                }

                else -> {}
            }
        }
    }

    fun setHiddenLoading(){
        whiteBoardWidget?.setHiddenLoading()
    }

    fun setNotShowWhiteLoading() {
        setHiddenLoading()
    }
}