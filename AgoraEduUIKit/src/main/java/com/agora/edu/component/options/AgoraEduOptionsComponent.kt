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
import com.agora.edu.component.common.AbsAgoraEduComponent
import com.agora.edu.component.common.IAgoraUIProvider
import io.agora.agoraeducore.core.AgoraEduCoreManager
import io.agora.agoraeducore.core.context.AgoraEduContextUserInfo
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.EduContextRoomInfo
import io.agora.agoraeducore.core.context.EduContextUserLeftReason
import io.agora.agoraeducore.core.group.FCRGroupClassUtils
import io.agora.agoraeducore.core.group.FCRGroupHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.UserHandler
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.launch.AgoraEduRoleType
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.agoraeduuikit.databinding.AgoraEduOptionsComponentBinding
import io.agora.agoraeduuikit.impl.chat.ChatPopupWidgetListener
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardGrantData
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionPacket
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionSignal

class AgoraEduOptionsComponent : AbsAgoraEduComponent {
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

    fun initView(uuid: String, rootContainer: ViewGroup, itemContainer: ViewGroup, agoraUIProvider: IAgoraUIProvider) {
        this.uuid = uuid
        this.itemContainer = itemContainer
        this.rootContainer = rootContainer
        initView(agoraUIProvider)
    }

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        optionPresenter = AgoraEduOptionPresenter(binding)
        setAskingIconTeacher()
        initWhiteboardTools()
        initViewItem() // 需要提前init，不然无法监听到加入教室的回调
        binding.optionItemAsking.isActivated = true
        binding.optionItemAsking.setOnClickListener {
            if (binding.optionItemAsking.isActivated) {
                optionPresenter.showAskingForHelpDialog(agoraUIProvider){
                    isRequestHelp = true
                }
            } else {
                AgoraUIToast.warn(context.applicationContext, text = resources.getString(R.string.fcr_group_teacher_exist_hint))
            }
        }
        binding.optionItemSetting.setOnClickListener {
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
                if (eduContext?.roomContext()?.getRoomInfo()?.roomType?.value == RoomType.SMALL_CLASS.value) {
                    showItem(popupViewRoster, R.dimen.agora_userlist_dialog_w, R.dimen.agora_userlist_dialog_h)
                } else {
                    showItem(popupViewRoster, R.dimen.agora_userlist_dialog_large_w, R.dimen.agora_userlist_dialog_large_h)
                }
                setIconActivated(binding.optionItemRoster)
            } else {
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
        binding.optionItemHandup.initView(agoraUIProvider)

        agroSettingWidget = AgoraEduSettingComponent(context)
        agroSettingWidget?.initView(agoraUIProvider)
        agroSettingWidget?.onExitListener = {
            onExitListener?.invoke()
        }

        popupViewRoster = AgoraEduRosterComponent(context)
        popupViewRoster?.initView(agoraUIProvider)
    }

    fun initWhiteboardTools() {
        binding.optionItemWhiteboardTool.initView(uuid, itemContainer, agoraUIProvider)

        eduContext?.widgetContext()?.addWidgetMessageObserver(object : AgoraWidgetMessageObserver {
            override fun onMessageReceived(msg: String, id: String) {
                val packet = GsonUtil.gson.fromJson(msg, AgoraBoardInteractionPacket::class.java)
                if (packet.signal == AgoraBoardInteractionSignal.BoardGrantDataChanged) {
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
                                if (granted) {  // 可以显示白板按钮
                                    binding.optionItemWhiteboardTool.visibility = View.VISIBLE
                                } else {
                                    binding.optionItemWhiteboardTool.visibility = View.GONE
                                }
                            }
                        }
                    }
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
        hiddenChatNews()
    }

    fun hiddenChatNews() {
        binding.optionItemChatNews.visibility = View.GONE
    }

    private fun showItem(item: View?) {
        itemContainer.removeAllViews()
        itemContainer.addView(item)
        hiddenChatNews()
    }

    private fun hiddenItem() {
        itemContainer.removeAllViews()
    }

    private fun setIconActivated(icon: ImageView) {
        icon.isActivated = true
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
        //初始化聊天组件
        popupViewChat = AgoraEduChatComponent(context)
        popupViewChat?.initView(rootContainer, agoraUIProvider)
        popupViewChat?.setTabDisplayed(false)
        popupViewChat?.chatListener = object : ChatPopupWidgetListener {
            override fun onShowUnread(show: Boolean) {
                when {
                    binding.optionItemSetting.isActivated -> {
                        binding.optionItemChatNews.visibility = View.GONE
                    }
                    show -> {
                        binding.optionItemChatNews.visibility = View.VISIBLE
                    }
                    else -> {
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
                    binding.optionItemChat.visibility = View.GONE
                    binding.optionItemRoster.visibility = View.GONE
                    binding.optionItemHandup.visibility = View.GONE
                    binding.optionItemWhiteboardTool.visibility = GONE
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
                    binding.optionItemRoster.visibility = View.GONE
                    binding.optionItemHandup.visibility = View.VISIBLE
                    binding.optionItemWhiteboardTool.visibility = GONE
                    // 下面两个要结合使用，否则有可能多个IM冲突
                    binding.optionItemChat.visibility = View.VISIBLE
                    initChat()
                }
            }
        } else if (roleType == AgoraEduRoleType.AgoraEduRoleTypeTeacher.value)  {
            // 老师端
            when (roomType) {
                RoomType.ONE_ON_ONE -> {
                    binding.optionItemSetting.visibility = View.VISIBLE
                    binding.optionItemToolbox.visibility = View.GONE
                    binding.optionItemChat.visibility = View.GONE
                    binding.optionItemRoster.visibility = View.GONE
                    binding.optionItemHandup.visibility = View.GONE
                    binding.optionItemWhiteboardTool.visibility = VISIBLE
                }
                RoomType.SMALL_CLASS -> {
                    binding.optionItemSetting.visibility = View.VISIBLE
                    binding.optionItemToolbox.visibility = View.GONE
                    binding.optionItemRoster.visibility = View.VISIBLE
                    binding.optionItemHandup.visibility = View.VISIBLE
                    binding.optionItemWhiteboardTool.visibility = VISIBLE
                    // 下面两个要结合使用，否则有可能多个IM冲突
                    binding.optionItemChat.visibility = View.VISIBLE
                    initChat()
                }
                RoomType.LARGE_CLASS -> {
                    binding.optionItemSetting.visibility = View.VISIBLE
                    binding.optionItemToolbox.visibility = View.GONE
                    binding.optionItemChat.visibility = View.GONE
                    binding.optionItemRoster.visibility = View.VISIBLE
                    binding.optionItemHandup.visibility = View.VISIBLE
                    binding.optionItemWhiteboardTool.visibility = VISIBLE
                }
            }
        } else{
            // 观众端
            when (roomType) {
                RoomType.ONE_ON_ONE -> {
                    binding.optionItemSetting.visibility = View.VISIBLE
                    binding.optionItemToolbox.visibility = View.GONE
                    binding.optionItemChat.visibility = View.GONE
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
        binding.root.removeAllViews()
    }
}





















