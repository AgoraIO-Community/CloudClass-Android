package com.agora.edu.component.options

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.agora.edu.component.AgoraEduChatComponent
import com.agora.edu.component.AgoraEduSettingComponent
import com.agora.edu.component.common.AbsAgoraEduConfigComponent
import com.agora.edu.component.common.IAgoraUIProvider
import com.agora.edu.component.common.UIUtils
import com.agora.edu.component.whiteboard.data.AgoraEduApplianceData
import com.google.gson.Gson
import com.hyphenate.easeimgroup.modules.utils.TAG
import io.agora.agoraeducore.core.AgoraEduCoreManager
import io.agora.agoraeducore.core.context.AgoraEduContextUserInfo
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.EduContextRoomInfo
import io.agora.agoraeducore.core.context.EduContextUserLeftReason
import io.agora.agoraeducore.core.group.FCRGroupClassUtils
import io.agora.agoraeducore.core.group.FCRGroupHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.UserHandler
import io.agora.agoraeducore.core.internal.framework.impl.managers.AgoraWidgetActiveObserver
import io.agora.agoraeducore.core.internal.framework.impl.managers.AgoraWidgetRoomPropsUpdateReq
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.launch.AgoraEduRoleType
import io.agora.agoraeducore.core.internal.launch.AgoraServiceType
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.agoraeduuikit.config.FcrUIConfig
import io.agora.agoraeduuikit.databinding.AgoraEduOptionsComponentBinding
import io.agora.agoraeduuikit.impl.chat.ChatPopupWidgetListener
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardGrantData
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionPacket
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionSignal
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeduuikit.provider.UIDataProviderListenerImpl

class AgoraEduOptionsComponent : AbsAgoraEduConfigComponent<FcrUIConfig>, IWhiteBoardIconClickListener {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    val tag = "AgoraEduOptionsComponent"
    lateinit var uuid: String
    lateinit var rootContainer: ViewGroup  // 给IM用的,view root
    lateinit var itemContainer: ViewGroup  // 显示侧边栏

    private var binding: AgoraEduOptionsComponentBinding =
        AgoraEduOptionsComponentBinding.inflate(LayoutInflater.from(context), this, true)

    private var agroSettingWidget: AgoraEduSettingComponent? = null
    private var popupViewRoster: AgoraEduRosterComponent? = null
    private var popupViewChat: AgoraEduChatComponent? = null
    private lateinit var optionPresenter: AgoraEduOptionPresenter
    var onExitListener: (() -> Unit)? = null // 退出
    private var isRequestHelp = false // 分组是否请求了帮助
    private var isLocalUserOnStage = false // 本地用户是否在台上 （大班课用）

    fun initView(uuid: String, rootContainer: ViewGroup, itemContainer: ViewGroup, agoraUIProvider: IAgoraUIProvider) {
        this.uuid = uuid
        this.itemContainer = itemContainer
        this.rootContainer = rootContainer
        initView(agoraUIProvider)
    }

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        uiDataProvider?.addListener(uiDataProviderListener)
        optionPresenter = AgoraEduOptionPresenter(binding)

        setAskingIconTeacher()
        initWhiteboardTools()
        initViewItem() // 需要提前init，不然无法监听到加入教室的回调

        binding.optionItemAsking.isActivated = true
        binding.optionItemAsking.setOnClickListener {
            if (binding.optionItemAsking.isActivated) {
                optionPresenter.showAskingForHelpDialog(agoraUIProvider) {
                    isRequestHelp = true
                }
            } else {
                AgoraUIToast.warn(
                    context.applicationContext,
                    text = resources.getString(R.string.fcr_group_teacher_exist_hint)
                )
            }
        }
        binding.optionItemSetting.setOnClickListener {
            //LogX.e(TAG,">>>${eduContext?.userContext()?.getCoHostList()}")
            if (!binding.optionItemSetting.isActivated) {
                showItem(agroSettingWidget)
                setIconActivated(binding.optionItemSetting)
            } else {
                hiddenItem()
                binding.optionItemSetting.isActivated = false
            }
        }
        binding.optionItemRoster.setOnClickListener {
            if (!binding.optionItemRoster.isActivated) {
                popupViewRoster?.isShow = true

                if (eduContext?.roomContext()?.getRoomInfo()?.roomType?.value == RoomType.SMALL_CLASS.value
                    || eduContext?.roomContext()?.getRoomInfo()?.roomType?.value == RoomType.GROUPING_CLASS.value
                ) {
                    showItem(popupViewRoster, R.dimen.agora_userlist_dialog_w, R.dimen.agora_userlist_dialog_h)
                    popupViewRoster?.showUserList()
                } else {
                    popupViewRoster?.updateStuListData()//默认加载第一页数据
                    showItem(
                        popupViewRoster,
                        R.dimen.agora_userlist_dialog_large_w,
                        R.dimen.agora_userlist_dialog_large_h
                    )
                }
                setIconActivated(binding.optionItemRoster)
            } else {
                popupViewRoster?.isShow = false
                hiddenItem()
                binding.optionItemRoster.isActivated = false
            }
        }
        binding.optionItemChat.setOnClickListener {
            if (!binding.optionItemChat.isActivated) {
                showItem(popupViewChat, R.dimen.agora_edu_chat_width, R.dimen.agora_edu_chat_height)
                setIconActivated(binding.optionItemChat)
            } else {
                hiddenItem()
                binding.optionItemChat.isActivated = false
            }
        }

        binding.optionItemOpenBoard.setOnClickListener {
            val info = AgoraWidgetRoomPropsUpdateReq(state = 1)
            eduContext?.widgetContext()?.setWidgetActive(AgoraWidgetDefaultId.WhiteBoard.id, info)
        }

        eduCore?.eduContextPool()?.widgetContext()
            ?.addWidgetMessageObserver(whiteBoardWidgetMsgObserver, AgoraWidgetDefaultId.WhiteBoard.id)
    }

    private var localUserGranted = false //当前本地用户是否授权
    private val whiteBoardWidgetMsgObserver = object : AgoraWidgetMessageObserver {
        override fun onMessageReceived(msg: String, id: String) {
            val packet2 = GsonUtil.gson.fromJson(msg, AgoraBoardInteractionPacket::class.java)
            if (packet2.signal == AgoraBoardInteractionSignal.BoardGrantDataChanged) {
                eduCore?.eduContextPool()?.userContext()?.getLocalUserInfo()?.let { localUser ->
                    if (localUser.role == AgoraEduContextUserRole.Student) {
                        var granted = false
                        if (packet2.body is MutableList<*>) { // 白板开关的格式
                            granted = (packet2.body as? ArrayList<String>)?.contains(localUser.userUuid) ?: false
                        } else { // 白板授权的格式
                            val bodyStr = GsonUtil.gson.toJson(packet2.body)
                            val agoraBoard = GsonUtil.gson.fromJson(bodyStr, AgoraBoardGrantData::class.java)
                            if (agoraBoard.granted) {
                                granted = agoraBoard.userUuids.contains(localUser.userUuid) ?: false
                            }
                        }
                        localUserGranted = granted
                    }
                }
            }
        }
    }

    fun setShareRoomLink(shareUrl: String) {
        agroSettingWidget?.setShareRoom(shareUrl, eduCore?.room()?.getRoomUuid() ?: "")
    }

    private val uiDataProviderListener = object : UIDataProviderListenerImpl() {
        override fun onCoHostListChanged(userList: List<AgoraUIUserDetailInfo>) {
            super.onCoHostListChanged(userList)

            val localUserUuid = eduContext?.userContext()?.getLocalUserInfo()?.userUuid

            if (eduContext?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Student
                && eduContext?.roomContext()?.getRoomInfo()?.roomType == RoomType.LARGE_CLASS
            ) {//大班课,才会进这里的逻辑
                isLocalUserOnStage = eduContext?.userContext()?.getCoHostList()?.find { it.userUuid == localUserUuid } != null
                if (!isLocalUserOnStage) {//本地不在台上了//todo
                    //收回白板权限
                    val data = AgoraBoardGrantData(false, arrayOf(localUserUuid!!).toMutableList())
                    val packet = AgoraBoardInteractionPacket(AgoraBoardInteractionSignal.BoardGrantDataChanged, data)
                    eduContext?.widgetContext()?.sendMessageToWidget(Gson().toJson(packet), AgoraWidgetDefaultId.WhiteBoard.id)
                    setWhiteboardViewTool(false)
                } else {
                    if (userList.find { it.userUuid == localUserUuid }?.whiteBoardGranted == true) {
                        if ((AgoraEduApplianceData.isEnableBoard(eduCore))) {
                            setWhiteboardViewTool(true)
                        }
                    }
                }
            }
        }
    }

    fun setWhiteboardViewTool(isShow: Boolean) {
        LogX.i(TAG, "Whiteboard option isShow = $isShow")
        ContextCompat.getMainExecutor(context).execute {
            binding.optionItemWhiteboardTool.visibility = if (isShow) View.VISIBLE else View.GONE // 隐藏白板按钮
            binding.optionItemWhiteboardTool.resetData()

            if (eduContext?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Teacher) {
                binding.optionItemOpenBoard.visibility = if (!isShow) View.VISIBLE else View.GONE
            }
        }
    }

    fun setAskingIconTeacher() {
        eduContext?.roomContext()?.addHandler(roomHandler)
        eduContext?.userContext()?.addHandler(userHandler)
        eduContext?.groupContext()?.addHandler(groupHandler)

        // 数据从大房间过来的
        FCRGroupClassUtils.mainLaunchConfig?.apply {
            val eduContextPool = AgoraEduCoreManager.getEduCore(roomUuid)?.eduContextPool()
            eduContextPool?.groupContext()?.addHandler(groupHandler)
        }
    }

    val roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)

            eduContext?.userContext()?.getAllUserList()?.forEach {
                // 老师已经在小组
                if (it.role == AgoraEduContextUserRole.Teacher) {
                    binding.optionItemAsking.isActivated = false
                }
            }
        }
    }

    val userHandler = object : UserHandler() {
        override fun onRemoteUserJoined(user: AgoraEduContextUserInfo) {
            super.onRemoteUserJoined(user)
            // 老师进入小组
            if (user.role == AgoraEduContextUserRole.Teacher) {
                binding.optionItemAsking.isActivated = false
                isRequestHelp = false
            }
        }

        override fun onRemoteUserLeft(
            user: AgoraEduContextUserInfo,
            operator: AgoraEduContextUserInfo?,
            reason: EduContextUserLeftReason
        ) {
            super.onRemoteUserLeft(user, operator, reason)
            // 老师离开小组
            if (user.role == AgoraEduContextUserRole.Teacher) {
                binding.optionItemAsking.isActivated = true
            }
        }
    }

    val groupHandler = object : FCRGroupHandler() {
        override fun onTeacherLaterJoin() {
            super.onTeacherLaterJoin()
            if (isRequestHelp) {
                ContextCompat.getMainExecutor(context).execute {
                    AgoraUIToast.info(
                        context.applicationContext,
                        text = resources.getString(R.string.fcr_group_help_teacher_busy_msg)
                    )
                }
                isRequestHelp = false
            }
        }
    }

    fun initViewItem() {
        if (getUIConfig().raiseHand.isVisible && eduCore?.config?.roleType != AgoraEduRoleType.AgoraEduRoleTypeObserver.value) {
            binding.optionItemHandup.initView(agoraUIProvider)
        }

        agroSettingWidget = AgoraEduSettingComponent(context)
        agroSettingWidget?.initView(agoraUIProvider)
        agroSettingWidget?.onExitListener = {
            onExitListener?.invoke()
        }

        if (getUIConfig().roster.isVisible && eduCore?.config?.roleType != AgoraEduRoleType.AgoraEduRoleTypeObserver.value) {
            popupViewRoster = AgoraEduRosterComponent(context)
            popupViewRoster?.initView(agoraUIProvider)
        }
    }

    /**
     * 1、进入的时候，白板是关闭
     * 2、授权
     * 3、白板开关
     */
    fun initWhiteboardTools() {
        binding.optionItemWhiteboardTool.initView(uuid, itemContainer, agoraUIProvider)
        binding.optionItemWhiteboardTool.boardIconClickListener = this
        eduContext?.widgetContext()?.addWidgetMessageObserver(object : AgoraWidgetMessageObserver {
            override fun onMessageReceived(msg: String, id: String) {
                val packet = GsonUtil.gson.fromJson(msg, AgoraBoardInteractionPacket::class.java)
                if (packet.signal == AgoraBoardInteractionSignal.BoardGrantDataChanged) {
                    LogX.i(tag, "BoardGrantDataChanged =" + msg)
                    eduContext?.userContext()?.getLocalUserInfo()?.let { localUser ->
                        if (packet.body is MutableList<*>) { // 白板开关的格式
                            localUserGranted = (packet.body as? ArrayList<String>)?.contains(localUser.userUuid) ?: false
                        } else { // 白板授权的格式
                            val bodyStr = GsonUtil.gson.toJson(packet.body)
                            val agoraBoard = GsonUtil.gson.fromJson(bodyStr, AgoraBoardGrantData::class.java)
                            if (agoraBoard.granted) {
                                localUserGranted = agoraBoard.userUuids.contains(localUser.userUuid) ?: false
                            }
                        }
                        LogX.i(tag, "isOpenBoard=" + AgoraEduApplianceData.isOpenBoardWidget(eduCore)+"granted=" + localUserGranted)
                        if (AgoraEduApplianceData.isOpenBoardWidget(eduCore) && (localUserGranted || localUser.role == AgoraEduContextUserRole.Teacher)) {  // 可以显示白板按钮，如果是老师则默认显示
                            setWhiteboardViewTool(true)
                        } else {
                            setWhiteboardViewTool(false)
                        }
                    }
                }
            }
        }, AgoraWidgetDefaultId.WhiteBoard.id)

        eduContext?.widgetContext()?.addWidgetActiveObserver(object : AgoraWidgetActiveObserver {
            override fun onWidgetActive(widgetId: String) {
                if (widgetId == AgoraWidgetDefaultId.WhiteBoard.id && localUserGranted) {
                    setWhiteboardViewTool(true)
                }
            }

            override fun onWidgetInActive(widgetId: String) {
                if (widgetId == AgoraWidgetDefaultId.WhiteBoard.id) {
                    setWhiteboardViewTool(false)
                }
            }

        }, AgoraWidgetDefaultId.WhiteBoard.id)
    }

    /**
     * 因为动态添加view，可以指定大小
     */
    private fun showItem(item: View?, widthDimenId: Int, heightDimenId: Int) {
        itemContainer.removeAllViews()
        itemContainer.addView(
            item,
            context.resources.getDimensionPixelOffset(widthDimenId),
            context.resources.getDimensionPixelOffset(heightDimenId)
        )
        if (popupViewChat == item) {
            hiddenChatNews()
        }
    }

    fun hiddenChatNews() {
        binding.optionItemChatNews.visibility = View.GONE
    }

    fun setHandsupTimeout(seconds: Int) {
        binding.optionItemHandup.setHandsupTimeout(seconds)
    }

    private fun showItem(item: View?) {
        itemContainer.removeAllViews()
        itemContainer.addView(item)
    }

    private fun hiddenItem() {
        itemContainer.removeAllViews()
    }

    private fun setIconActivated(icon: ImageView) {
        icon.isActivated = true
        binding.optionItemWhiteboardTool.isShowApplianceView = false
        when (icon) {
            binding.optionItemSetting -> {
                binding.optionItemRoster.isActivated = false
                binding.optionItemChat.isActivated = false
            }
            binding.optionItemRoster -> {
                binding.optionItemSetting.isActivated = false
                binding.optionItemChat.isActivated = false
            }
            binding.optionItemChat -> {
                binding.optionItemRoster.isActivated = false
                binding.optionItemSetting.isActivated = false
            }
        }
    }

    fun initChat() {
        if (!getUIConfig().agoraChat.isVisible) {
            return
        }
        //初始化聊天组件
        popupViewChat = AgoraEduChatComponent(context)
        popupViewChat?.initView(rootContainer, agoraUIProvider)
        popupViewChat?.setTabDisplayed(false)
        popupViewChat?.chatListener = object : ChatPopupWidgetListener {
            override fun onShowUnread(show: Boolean) {
                ContextCompat.getMainExecutor(context).execute {
                    if (binding.optionItemSetting.isActivated) {
                        binding.optionItemChatNews.visibility = View.GONE
                    } else if (show) {
                        if (binding.optionItemChat.isActivated) {
                            return@execute
                        }
                        binding.optionItemChatNews.visibility = View.VISIBLE
                    } else {
                        binding.optionItemChatNews.visibility = View.GONE
                    }
                }
            }
        }
    }

    /**
     * 先调用
     * 学生端
     * 大班课：设置+举手+白板工具
     * 小班课：设置+花名册+聊天+举手+白板工具
     * 1v1：没有
     *
     * 老师端
     * 小班课，大班课：设置+toolbar+花名册+聊天+举手+白板工具
     *
     * 观众端
     * 小班课，大班课：设置+聊天
     */
    fun setShowOption(roomType: RoomType, roleType: Int) {
        if (roleType == AgoraEduRoleType.AgoraEduRoleTypeStudent.value) {
            // 学生端
            when (roomType) {
                RoomType.ONE_ON_ONE -> {
                    binding.optionItemSetting.visibility = View.VISIBLE
                    binding.optionItemToolbox.visibility = View.GONE
                    binding.optionItemChat.visibility = View.VISIBLE
                    binding.optionItemRoster.visibility = View.GONE
                    binding.optionItemHandup.visibility = View.GONE
                    binding.optionItemWhiteboardTool.visibility = GONE
                    initChat()
                }
                RoomType.SMALL_CLASS -> {
                    binding.optionItemSetting.visibility = View.VISIBLE
                    binding.optionItemToolbox.visibility = View.GONE
                    binding.optionItemRoster.visibility = View.VISIBLE
                    binding.optionItemHandup.visibility = View.VISIBLE
                    binding.optionItemWhiteboardTool.visibility = GONE
                    // 下面两个要结合使用，否则有可能多个IM冲突
                    binding.optionItemChat.visibility = View.VISIBLE
                    initChat()
                }
                RoomType.LARGE_CLASS -> {
                    binding.optionItemSetting.visibility = View.VISIBLE
                    binding.optionItemToolbox.visibility = View.GONE
                    binding.optionItemChat.visibility = View.GONE
                    binding.optionItemRoster.visibility = View.GONE
                    binding.optionItemHandup.visibility = View.VISIBLE
                    binding.optionItemWhiteboardTool.visibility = GONE
                }
                RoomType.GROUPING_CLASS -> {
                    binding.optionItemAsking.visibility = View.VISIBLE
                    binding.optionItemSetting.visibility = View.VISIBLE
                    binding.optionItemToolbox.visibility = View.GONE
                    binding.optionItemRoster.visibility = View.VISIBLE
                    binding.optionItemHandup.visibility = View.VISIBLE
                    binding.optionItemWhiteboardTool.visibility = GONE
                    // 下面两个要结合使用，否则有可能多个IM冲突
                    binding.optionItemChat.visibility = View.VISIBLE
                    initChat()
                }
            }
        } else if (roleType == AgoraEduRoleType.AgoraEduRoleTypeTeacher.value) {
            // 老师端
            when (roomType) {
                RoomType.ONE_ON_ONE -> {
                    binding.optionItemChat.visibility = View.VISIBLE
                    initChat()
                    binding.optionItemRoster.visibility = View.GONE
                    binding.optionItemHandup.visibility = View.GONE
                }
                RoomType.SMALL_CLASS -> {
                    binding.optionItemRoster.visibility = View.VISIBLE
                    binding.optionItemHandup.visibility = View.VISIBLE
                    // 下面两个要结合使用，否则有可能多个IM冲突
                    binding.optionItemChat.visibility = View.VISIBLE
                    initChat()
                }
                RoomType.LARGE_CLASS -> {
                    binding.optionItemChat.visibility = View.GONE
                    binding.optionItemRoster.visibility = View.VISIBLE
                    binding.optionItemHandup.visibility = View.VISIBLE
                }

                else -> {}
            }

            binding.optionItemToolbox.visibility = View.GONE
            binding.optionItemSetting.visibility = View.VISIBLE
            //setWhiteboardViewTool(AgoraEduApplianceData.isGrantBoard(eduCore))
        } else {
            // 观众端
            when (roomType) {
                RoomType.ONE_ON_ONE -> {
                    binding.optionItemSetting.visibility = View.VISIBLE
                    binding.optionItemToolbox.visibility = View.GONE
                    binding.optionItemChat.visibility = View.VISIBLE
                    initChat()
                    binding.optionItemRoster.visibility = View.GONE
                    binding.optionItemHandup.visibility = View.GONE
                    binding.optionItemWhiteboardTool.visibility = GONE
                }
                RoomType.SMALL_CLASS -> {
                    binding.optionItemSetting.visibility = View.VISIBLE
                    binding.optionItemToolbox.visibility = View.GONE
                    binding.optionItemRoster.visibility = View.GONE
                    binding.optionItemHandup.visibility = View.GONE
                    binding.optionItemWhiteboardTool.visibility = GONE
                    // 下面两个要结合使用，否则有可能多个IM冲突
                    binding.optionItemChat.visibility = View.VISIBLE
                    initChat()
                }

                RoomType.LARGE_CLASS -> {
                    binding.optionItemSetting.visibility = View.VISIBLE
                    binding.optionItemToolbox.visibility = View.GONE
                    binding.optionItemChat.visibility = View.GONE
                    binding.optionItemRoster.visibility = View.GONE
                    binding.optionItemHandup.visibility = View.GONE
                    binding.optionItemWhiteboardTool.visibility = GONE
                }

                else -> {}
            }
        }

        updateUIForConfig(getUIConfig())
    }

    /**
     * Only used for the class type of LargeRoom,
     */
    fun setServiceOption(serviceType: AgoraServiceType, roleType: Int) {
        if (roleType == AgoraEduRoleType.AgoraEduRoleTypeTeacher.value) {
            when (serviceType) {
                AgoraServiceType.MixStreamCDN -> {
                    binding.optionItemSetting.visibility = View.GONE
                    binding.optionItemToolbox.visibility = View.GONE
                    binding.optionItemChat.visibility = View.GONE
                    binding.optionItemRoster.visibility = View.GONE
                    binding.optionItemHandup.visibility = View.GONE
                    binding.optionItemWhiteboardTool.visibility = GONE
                }

                else -> {}
            }
        } else if (roleType == AgoraEduRoleType.AgoraEduRoleTypeStudent.value) {
            when (serviceType) {
                AgoraServiceType.MixStreamCDN -> {
                    binding.optionItemSetting.visibility = View.GONE
                    binding.optionItemToolbox.visibility = View.GONE
                    binding.optionItemChat.visibility = View.GONE
                    binding.optionItemRoster.visibility = View.GONE
                    binding.optionItemHandup.visibility = View.GONE
                    binding.optionItemWhiteboardTool.visibility = GONE
                }

                else -> {}
            }
        }
    }

    override fun release() {
        super.release()
        popupViewChat?.release()
        agroSettingWidget?.release()
        popupViewRoster?.release()
        eduContext?.roomContext()?.removeHandler(roomHandler)
        eduContext?.userContext()?.removeHandler(userHandler)
        eduContext?.groupContext()?.removeHandler(groupHandler)
        ContextCompat.getMainExecutor(context).execute {
            binding.root.removeAllViews()
        }
    }

    override fun onWhiteboardIconClicked() {
        ContextCompat.getMainExecutor(context).execute {
            binding.optionItemSetting.isActivated = false
            binding.optionItemRoster.isActivated = false
            binding.optionItemChat.isActivated = false
        }
    }

    override fun updateUIForConfig(config: FcrUIConfig) {
        UIUtils.setViewVisible(binding.optionItemChat, config.agoraChat.isVisible)
        UIUtils.setViewVisible(binding.optionItemHandup, config.raiseHand.isVisible)
        UIUtils.setViewVisible(binding.optionItemRoster, config.roster.isVisible)
        if (eduContext?.roomContext()?.getRoomInfo()?.roomType == RoomType.LARGE_CLASS) {
            //如果是大班课，默认没有 chat roster按钮
            binding.optionItemChat.visibility = View.GONE
            if (eduContext?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Student) {//
                binding.optionItemRoster.visibility = View.GONE
            }
        }
        if (eduContext?.roomContext()?.getRoomInfo()?.roomType == RoomType.ONE_ON_ONE) {
            binding.optionItemRoster.visibility = View.GONE
            binding.optionItemHandup.visibility = View.GONE
        }

        if (eduCore?.config?.roleType == AgoraEduRoleType.AgoraEduRoleTypeObserver.value) {
            binding.optionItemRoster.visibility = View.GONE
            binding.optionItemHandup.visibility = View.GONE
        }
    }

    override fun getUIConfig(): FcrUIConfig {
        return getTemplateUIConfig()
    }
}





















