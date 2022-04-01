package com.agora.edu.component.options

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.agora.edu.component.AgoraEduChatComponent
import com.agora.edu.component.AgoraEduSettingComponent
import com.agora.edu.component.common.AbsAgoraEduComponent
import com.agora.edu.component.common.IAgoraUIProvider
import com.agora.edu.component.helper.GsonUtils
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.EduContextRoomType
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.launch.AgoraEduRoleType
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.databinding.AgoraEduOptionsComponentBinding
import io.agora.agoraeduuikit.impl.chat.ChatPopupWidgetListener
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
    private var settingIcon = binding.optionItemSetting
    private var rosterIcon = binding.optionItemRoster
    private var chatIcon = binding.optionItemChat

    private lateinit var agroSettingWidget: AgoraEduSettingComponent
    private lateinit var popupViewRoster: AgoraEduRosterComponent
    private lateinit var popupViewChat: AgoraEduChatComponent

    fun initView(uuid: String, rootContainer: ViewGroup, itemContainer: ViewGroup, agoraUIProvider: IAgoraUIProvider) {
        this.uuid = uuid
        this.itemContainer = itemContainer
        this.rootContainer = rootContainer
        initView(agoraUIProvider)
    }

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        initWhiteboardTools()
        initViewItem() // 需要提前init，不然无法监听到加入教室的回调

        settingIcon.setOnClickListener {
            if (!settingIcon.isActivated) {
                showItem(agroSettingWidget)
                setIconActivated(settingIcon)
            } else {
                hiddenItem()
                settingIcon.isActivated = false
            }
        }
        rosterIcon.setOnClickListener {
            if (!rosterIcon.isActivated) {
                if (eduContext?.roomContext()?.getRoomInfo()?.roomType?.value == EduContextRoomType.SmallClass.value) {
                    showItem(popupViewRoster, R.dimen.agora_userlist_dialog_w, R.dimen.agora_userlist_dialog_h)
                } else {
                    showItem(popupViewRoster, R.dimen.agora_userlist_dialog_large_w, R.dimen.agora_userlist_dialog_large_h)
                }
                setIconActivated(rosterIcon)
            } else {
                hiddenItem()
                rosterIcon.isActivated = false
            }
        }
        chatIcon.setOnClickListener {
            if (!chatIcon.isActivated) {
                showItem(popupViewChat, R.dimen.agora_edu_chat_width, R.dimen.agora_edu_chat_height)
                setIconActivated(chatIcon)
            } else {
                hiddenItem()
                chatIcon.isActivated = false
            }
        }
    }

    fun initViewItem() {
        binding.optionItemHandup.initView(agoraUIProvider)

        agroSettingWidget = AgoraEduSettingComponent(context)
        agroSettingWidget.initView(agoraUIProvider)

        popupViewRoster = AgoraEduRosterComponent(context)
        popupViewRoster.initView(agoraUIProvider)
    }

    fun initWhiteboardTools() {
        binding.optionItemWhiteboardTool.initView(uuid, itemContainer, agoraUIProvider)

        eduContext?.widgetContext()?.addWidgetMessageObserver(object : AgoraWidgetMessageObserver {
            override fun onMessageReceived(msg: String, id: String) {
                val packet = GsonUtils.mGson.fromJson(msg, AgoraBoardInteractionPacket::class.java)
                if (packet.signal == AgoraBoardInteractionSignal.BoardGrantDataChanged) {
                    eduContext?.userContext()?.getLocalUserInfo()?.let { localUser ->
                        if (localUser.role == AgoraEduContextUserRole.Student) {
                            val granted = (packet.body as? ArrayList<String>)?.contains(localUser.userUuid) ?: false
                            uiHandler.post {
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
            settingIcon -> {
                rosterIcon.isActivated = false
                chatIcon.isActivated = false
            }
            rosterIcon -> {
                settingIcon.isActivated = false
                chatIcon.isActivated = false
            }
            chatIcon -> {
                rosterIcon.isActivated = false
                settingIcon.isActivated = false
            }
        }
    }

    fun initChat() {
        //初始化聊天组件
        popupViewChat = AgoraEduChatComponent(context)
        popupViewChat.initView(rootContainer, agoraUIProvider)
        popupViewChat.setTabDisplayed(true)
        popupViewChat.chatListener = object : ChatPopupWidgetListener {
            override fun onShowUnread(show: Boolean) {
                when {
                    chatIcon.isActivated -> {
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
            }
        } else {
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
        }
    }
}





















