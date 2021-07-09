package com.hyphenate.easeim.modules.view.ui.widget

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.hyphenate.*
import com.hyphenate.chat.*
import com.hyphenate.easeim.R
import com.hyphenate.easeim.modules.constant.EaseConstant
import com.hyphenate.easeim.modules.manager.ThreadManager
import com.hyphenate.easeim.modules.repositories.EaseRepository
import com.hyphenate.easeim.modules.view.`interface`.ChatPagerListener
import com.hyphenate.easeim.modules.view.`interface`.ViewClickListener
import com.hyphenate.easeim.modules.view.adapter.ChatViewPagerAdapter
import com.hyphenate.exceptions.HyphenateException
import com.hyphenate.util.EMLog
import org.json.JSONObject


class ChatViewPager(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : LinearLayout(context, attributeSet, defStyleAttr), EMMessageListener, EMChatRoomChangeListener, ViewClickListener, EMConnectionListener {

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    companion object {
        private const val TAG = "ChatViewPager"
    }

    private lateinit var viewPager: ViewPager
    private lateinit var tabLayout: TabLayout
    private lateinit var iconHidden: ImageView
    lateinit var chatView: ChatView
    private lateinit var announcementView: AnnouncementView
    private var container: RelativeLayout
    private var chooseTab = 0
    private var loginLimit = 0
    private var joinLimit = 0

    private var roomUuid = ""
    private var chatRoomId = ""
    private var nickName = ""
    private var avatarUrl = ""
    private var userName = ""
    private var userUuid = ""

    var viewClickListener: ViewClickListener? = null
    var chatPagerListener: ChatPagerListener? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.chat_total_layout, this)
        container = findViewById(R.id.total_layout)
    }

    /**
     * 初始化view
     */
    fun initView() {
        iconHidden = findViewById(R.id.hidden)
        viewPager = findViewById(R.id.viewPager)
        chatView = ChatView(chatRoomId, context)
        announcementView = AnnouncementView(context)
        val pagerList = listOf<View>(chatView, announcementView)
        val titleList = listOf<String>(
                context.getString(R.string.chat),
                context.getString(R.string.announcement),
        )
        val viewPagerAdapter = ChatViewPagerAdapter(pagerList)
        viewPager.adapter = viewPagerAdapter
//        viewPager.offscreenPageLimit = 2
        tabLayout = findViewById(R.id.tab_layout)
        for (index in pagerList.indices)
            tabLayout.addTab(
                    tabLayout.newTab().setCustomView(context?.let {
                        getTabView(
                                it.applicationContext,
                                titleList[index]
                        )
                    })
            )

        recoverItem()
        chooseTab(tabLayout.getTabAt(0))
        initListener()
    }

    /**
     * 注册监听
     */
    private fun initListener() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                recoverItem()
                chooseTab(tab)
                tab?.position?.let {
                    chooseTab = it
                    viewPager.setCurrentItem(it, true)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }
        })
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))

        iconHidden.setOnClickListener {
            chatPagerListener?.onIconHideenClick()
        }

        chatView.viewClickListener = this

        EMClient.getInstance().chatManager().addMessageListener(this)
        EMClient.getInstance().chatroomManager().addChatRoomChangeListener(this)
        EMClient.getInstance().addConnectionListener(this)
    }

    private fun getTabView(context: Context, title:String) : View {
        val view: View =
                LayoutInflater.from(context).inflate(R.layout.re_tab_item_layout, null)
        val text = view.findViewById<TextView>(R.id.title)
        text.text = title
        return view
    }

    /**
     * 重置状态
     */
    private fun recoverItem() {
        for (i in 0..2) {
            val title = tabLayout.getTabAt(i)?.view?.findViewById<TextView>(R.id.title)
            title?.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
            title?.setTextColor(Color.BLACK)
        }
    }

    /**
     * 选中状态
     */
    private fun chooseTab(tab: TabLayout.Tab?) {
        val title = tab?.view?.findViewById<TextView>(R.id.title)
        val unread = tab?.view?.findViewById<ImageView>(R.id.iv_tips)
        title?.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        title?.setTextColor(Color.BLUE)
        unread?.visibility = View.INVISIBLE
    }

    override fun onMessageReceived(messages: MutableList<EMMessage>?) {
        messages?.let {
            for (message in messages) {
                if (message.type == EMMessage.Type.TXT) {
                    chatPagerListener?.onMessageReceived()
                    if (chooseTab != 0) {
                        showUnread()
                    }
                    refreshUI()
                }
            }
        }


    }

    override fun onCmdMessageReceived(messages: MutableList<EMMessage>?) {
        messages?.forEach { message ->
            if (message.chatType == EMMessage.ChatType.ChatRoom && message.to == chatRoomId) {
                val body = message.body as EMCmdMessageBody
                val notifyMessage = EMMessage.createSendMessage(EMMessage.Type.CUSTOM)
                val notifyBody = EMCustomMessageBody(EaseConstant.NOTIFY)
                when (body.action()) {
                    EaseConstant.SET_ALL_MUTE, EaseConstant.REMOVE_ALL_MUTE -> {
                        notifyBody.params = mutableMapOf(Pair(EaseConstant.OPERATION, body.action()))
                    }
                    EaseConstant.DEL -> {
                        val msgId = message.getStringAttribute(EaseConstant.MSG_ID, "")
                        EaseRepository.instance.deleteMessage(chatRoomId, msgId)
                        notifyBody.params = mutableMapOf(Pair(EaseConstant.OPERATION, body.action()))
                    }
                }
                notifyMessage.body = notifyBody
                notifyMessage.to = chatRoomId
                notifyMessage.chatType = EMMessage.ChatType.ChatRoom
                notifyMessage.setStatus(EMMessage.Status.SUCCESS)
                notifyMessage.msgTime = message.msgTime
                notifyMessage.setAttribute(EaseConstant.NICK_NAME, message.getStringAttribute(EaseConstant.NICK_NAME, message.from))
                EMClient.getInstance().chatManager().saveMessage(notifyMessage)
                chatPagerListener?.onMessageReceived()
            }
        }
        refreshUI()
    }

    override fun onMessageRead(messages: MutableList<EMMessage>?) {
    }

    override fun onMessageDelivered(messages: MutableList<EMMessage>?) {
    }

    override fun onMessageRecalled(messages: MutableList<EMMessage>?) {
    }

    override fun onMessageChanged(message: EMMessage?, change: Any?) {
    }

    override fun onChatRoomDestroyed(roomId: String?, roomName: String?) {

    }

    override fun onMemberJoined(roomId: String?, participant: String?) {

    }

    override fun onMemberExited(roomId: String?, roomName: String?, participant: String?) {

    }

    override fun onRemovedFromChatRoom(
            reason: Int,
            roomId: String?,
            roomName: String?,
            participant: String?
    ) {

    }

    override fun onMuteListAdded(
            chatRoomId: String?,
            mutes: MutableList<String>?,
            expireTime: Long
    ) {

    }

    override fun onMuteListRemoved(chatRoomId: String?, mutes: MutableList<String>?) {

    }

    override fun onWhiteListAdded(chatRoomId: String?, whitelist: MutableList<String>?) {
        whitelist?.forEach {
            if (it == userName) {
                ThreadManager.instance.runOnMainThread {
                    chatView.showMutedView()
                    chatPagerListener?.onSingleMuted(true)
                }
            }
        }
    }

    override fun onWhiteListRemoved(chatRoomId: String?, whitelist: MutableList<String>?) {
        whitelist?.forEach {
            if (it == userName) {
                ThreadManager.instance.runOnMainThread {
                    chatView.hideMutedView()
                    chatPagerListener?.onSingleMuted(false)
                }
            }
        }
    }

    override fun onAllMemberMuteStateChanged(chatRoomId: String?, isMuted: Boolean) {
        ThreadManager.instance.runOnMainThread {
            chatPagerListener?.onAllMemberMuted(isMuted)
            if (isMuted)
                chatView.showMutedView()
            else
                chatView.hideMutedView()
        }
    }

    override fun onAdminAdded(chatRoomId: String?, admin: String?) {

    }

    override fun onAdminRemoved(chatRoomId: String?, admin: String?) {

    }

    override fun onOwnerChanged(chatRoomId: String?, newOwner: String?, oldOwner: String?) {

    }

    override fun onAnnouncementChanged(chatRoomId: String?, announcement: String?) {
        ThreadManager.instance.runOnMainThread {
            announcement?.let {
                chatView.announcementChange(announcement)
                announcementView.announcementChange(announcement)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacksAndMessages(null)
        EMClient.getInstance().chatManager().removeMessageListener(this)
        EMClient.getInstance().chatroomManager().removeChatRoomListener(this)
        EMClient.getInstance().chatroomManager().leaveChatRoom(chatRoomId)
        EMClient.getInstance().chatManager().deleteConversation(chatRoomId, true)
        EMClient.getInstance().logout(false)
    }


    fun setRoomUuid(roomUuid: String) {
        this.roomUuid = roomUuid
    }

    fun setChatRoomId(chatRoomId: String) {
        this.chatRoomId = chatRoomId
    }

    fun setNickName(nickName: String) {
        this.nickName = nickName
    }

    fun setAvatarUrl(avatarUrl: String) {
        this.avatarUrl = avatarUrl
    }

    fun setUserName(userName: String) {
        this.userName = userName
    }

    fun setUserUuid(userUuid: String) {
        this.userUuid = userUuid
    }

    /**
     * 登录环信
     */
    fun loginIM() {
        loginLimit++
        EMClient.getInstance().login(userName, userUuid, object : EMCallBack {
            override fun onSuccess() {
                val info = EMUserInfo()
                info.nickName = nickName
                info.avatarUrl = avatarUrl
                val extJson = JSONObject()
                extJson.put(EaseConstant.ROLE, EaseConstant.ROLE_STUDENT)
                info.ext = extJson.toString()
                EaseRepository.instance.updateOwnInfo(info)
                joinChatRoom()
            }

            override fun onError(code: Int, error: String) {
                EMLog.e(TAG, "login failed:$code:$error")
                if (loginLimit == 2) {
                    ThreadManager.instance.runOnMainThread{
                        Toast.makeText(context, context.getString(R.string.login_chat_failed), Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                // 判断不存在去注册再登录
                if (code == EMError.USER_NOT_FOUND) {
                    loginLimit = 0
                    createIM()
                } else {
                    loginIM()
                }
            }

            override fun onProgress(progress: Int, status: String) {}
        })
    }

    /**
     * 加入聊天室
     */
    private fun joinChatRoom() {
        joinLimit++
        EMClient.getInstance().chatroomManager().joinChatRoom(chatRoomId, object : EMValueCallBack<EMChatRoom?> {
            override fun onSuccess(value: EMChatRoom?) {
                EMLog.e("Login:", "join success")
                ThreadManager.instance.runOnMainThread {
                    initView()
                }
            }

            override fun onError(error: Int, errorMsg: String) {
                EMLog.e(TAG, "join failed: $error:$errorMsg")
                if(error == EMError.CHATROOM_ALREADY_JOINED) {
                    ThreadManager.instance.runOnMainThread {
                        initView()
                    }
                    return
                }
                if (joinLimit == 2) {
                    ThreadManager.instance.runOnMainThread{
                        Toast.makeText(context, context.getString(R.string.join_chat_room_failed), Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                joinChatRoom()
            }
        })
    }

    /**
     * 创建环信账号
     */
    private fun createIM() {
        ThreadManager.instance.runOnIOThread {
            try {
                EMClient.getInstance().createAccount(userName, userUuid)
                loginIM()
            } catch (e: HyphenateException) {
                e.printStackTrace()
                EMLog.e(TAG, "create failed:" + e.errorCode + ":" + e.description)
            }
        }
    }

    override fun onAnnouncementClick() {
        chooseTab = 1
        chooseTab(tabLayout.getTabAt(1))
        tabLayout.getTabAt(1)?.let { viewPager.setCurrentItem(it.position, true) }
    }

    override fun onMsgContentClick() {
        viewClickListener?.onMsgContentClick()
    }

    override fun onFaceIconClick() {
        viewClickListener?.onFaceIconClick()
    }

    /**
     * 展示未读标识
     */
    private fun showUnread() {
        ThreadManager.instance.runOnMainThread {
            val unread = tabLayout.getTabAt(0)?.view?.findViewById<ImageView>(R.id.iv_tips)
            unread?.visibility = View.VISIBLE
        }
    }

    /**
     * 刷新聊天页
     */
    fun refreshUI() {
        ThreadManager.instance.runOnMainThread {
            chatView.refresh()
        }
    }

    override fun onConnected() {
        EMClient.getInstance().chatroomManager().joinChatRoom(chatRoomId, object : EMValueCallBack<EMChatRoom> {
            override fun onSuccess(value: EMChatRoom?) {
                EaseRepository.instance.reconnectionLoadMessages(chatRoomId)
            }

            override fun onError(error: Int, errorMsg: String?) {
                if(error == EMError.CHATROOM_ALREADY_JOINED)
                    EaseRepository.instance.reconnectionLoadMessages(chatRoomId)
            }

        })

    }

    override fun onDisconnected(errorCode: Int) {
        EaseRepository.instance.refreshLastMessageId(chatRoomId)
    }
}