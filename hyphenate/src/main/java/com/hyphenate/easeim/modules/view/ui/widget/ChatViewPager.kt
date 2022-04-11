package com.hyphenate.easeim.modules.view.ui.widget

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.annotation.UiThread
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.hyphenate.*
import com.hyphenate.chat.*
import com.hyphenate.easeim.R
import com.hyphenate.easeim.modules.constant.EaseConstant
import com.hyphenate.easeim.modules.manager.ThreadManager
import com.hyphenate.easeim.modules.repositories.EaseRepository
import com.hyphenate.easeim.modules.utils.CommonUtil
import com.hyphenate.easeim.modules.utils.ScreenUtil
import com.hyphenate.easeim.modules.view.`interface`.ChatPagerListener
import com.hyphenate.easeim.modules.view.`interface`.ViewClickListener
import com.hyphenate.easeim.modules.view.adapter.ChatViewPagerAdapter
import com.hyphenate.exceptions.HyphenateException
import com.hyphenate.util.EMFileHelper
import com.hyphenate.util.EMLog
import com.hyphenate.util.VersionUtils
import org.json.JSONObject
import java.io.File


class ChatViewPager(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : LinearLayout(context, attributeSet, defStyleAttr), EMMessageListener, EMChatRoomChangeListener, ViewClickListener, EMConnectionListener {

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    companion object {
        private const val TAG = "ChatViewPager"
    }

    private lateinit var viewPager: ViewPager
    private var tabLayout: TabLayout? = null
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
    private var pagerList = mutableListOf<View>()
    var chatPagerListener: ChatPagerListener? = null

    private lateinit var receiver:BroadcastReceiver
    private val selectImageResultCode = 78

    init {
        LayoutInflater.from(context).inflate(R.layout.chat_total_layout, this)
        container = findViewById(R.id.total_layout)
        initView()
        initReceiver()
    }

    /**
     * 初始化view
     */
    fun initView() {
        iconHidden = findViewById(R.id.hidden)
        viewPager = findViewById(R.id.viewPager)
        chatView = ChatView(context)
        announcementView = AnnouncementView(context)
        pagerList = mutableListOf(chatView, announcementView)
        val titleList = listOf<String>(
                context.getString(R.string.chat),
                context.getString(R.string.announcement),
        )
        val viewPagerAdapter = ChatViewPagerAdapter(pagerList)
        viewPager.adapter = viewPagerAdapter
//        viewPager.offscreenPageLimit = 2
        tabLayout = findViewById(R.id.tab_layout)
        for (index in pagerList.indices)
            tabLayout?.newTab()?.let {
                tabLayout?.addTab(
                        it.setCustomView(context?.let {
                            getTabView(
                                    it.applicationContext,
                                    titleList[index]
                            )
                        })
                )
            }

        recoverItem()
        chooseFirstTab()
        initListener()
        container.post {
            ScreenUtil.instance.init(context!!)
            ScreenUtil.instance.screenWidth = container.width
            ScreenUtil.instance.screenHeight = container.height
        }
    }

    /**
     * 注册监听
     */
    private fun initListener() {
        tabLayout?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
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
    }

    private fun initIMListener(){
        EMClient.getInstance().chatManager().addMessageListener(this)
        EMClient.getInstance().chatroomManager().addChatRoomChangeListener(this)
        EMClient.getInstance().addConnectionListener(this)
    }

    private fun getTabView(context: Context, title: String): View {
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
            val title = tabLayout?.getTabAt(i)?.view?.findViewById<TextView>(R.id.title)
            title?.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
            title?.setTextColor(Color.BLACK)
        }
    }

    /**
     * 默认选中第一个tab
     */
    private fun chooseFirstTab() {
        val tab = tabLayout?.getTabAt(0)
        val title = tab?.view?.findViewById<TextView>(R.id.title)
        val unread = tab?.view?.findViewById<ImageView>(R.id.iv_tips)
        title?.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        title?.setTextColor(ContextCompat.getColor(context, R.color.blue))
        unread?.visibility = View.INVISIBLE
    }

    /**
     * 选中状态
     */
    private fun chooseTab(tab: TabLayout.Tab?) {
        val title = tab?.view?.findViewById<TextView>(R.id.title)
        val unread = tab?.view?.findViewById<ImageView>(R.id.iv_tips)
        title?.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        title?.setTextColor(ContextCompat.getColor(context, R.color.blue))
        unread?.visibility = View.INVISIBLE
        showOuterLayerUnread()
    }

    override fun onMessageReceived(messages: MutableList<EMMessage>?) {
        messages?.let {
            for (message in messages) {
                if (message.getIntAttribute(EaseConstant.MSG_TYPE, EaseConstant.NORMAL_MSG) == EaseConstant.NORMAL_MSG) {
                    ThreadManager.instance.runOnMainThread {
                        if (chooseTab != 0) {
                            showUnread(0)
                        }
                        showOuterLayerUnread()
                        refreshUI()
                    }
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
                    EaseConstant.MUTE, EaseConstant.UN_MUTE -> {
                        val member = message.getStringAttribute(EaseConstant.MUTE_MEMEBER, "")
                        if (!member.equals(userName))
                            return@forEach
                        notifyBody.params = mutableMapOf(Pair(EaseConstant.OPERATION, body.action()))
                    }
                }
                notifyMessage.body = notifyBody
                notifyMessage.to = chatRoomId
                notifyMessage.chatType = EMMessage.ChatType.ChatRoom
                notifyMessage.setStatus(EMMessage.Status.SUCCESS)
                notifyMessage.msgTime = message.msgTime
                notifyMessage.msgId = message.msgId
                notifyMessage.setAttribute(EaseConstant.NICK_NAME, message.getStringAttribute(EaseConstant.NICK_NAME, message.from))
                EMClient.getInstance().chatManager().saveMessage(notifyMessage)
                ThreadManager.instance.runOnMainThread {
                    if (chooseTab != 0) {
                        showUnread(0)
                    }
                    showOuterLayerUnread()
                }
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
                EaseRepository.instance.singleMuted = true
                ThreadManager.instance.runOnMainThread {
                    chatView.showMutedView()
                    chatPagerListener?.onMuted(true)
                }
            }
        }
    }

    override fun onWhiteListRemoved(chatRoomId: String?, whitelist: MutableList<String>?) {
        whitelist?.forEach {
            if (it == userName) {
                EaseRepository.instance.singleMuted = false
                ThreadManager.instance.runOnMainThread {
                    if (!EaseRepository.instance.allMuted) {
                        chatView.hideMutedView()
                        chatPagerListener?.onMuted(false)
                    }
                }
            }
        }
    }

    override fun onAllMemberMuteStateChanged(chatRoomId: String?, isMuted: Boolean) {
        EaseRepository.instance.allMuted = isMuted
        ThreadManager.instance.runOnMainThread {
            if (isMuted) {
                chatView.showMutedView()
                chatPagerListener?.onMuted(isMuted)
            } else {
                if (EaseRepository.instance.singleMuted) {
                    chatView.showMutedView()
                } else {
                    chatView.hideMutedView()
                    chatPagerListener?.onMuted(isMuted)
                }
            }
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
                ThreadManager.instance.runOnMainThread {
                    if (chooseTab != 1)
                        showUnread(1)
                    showOuterLayerUnread()
                }
            }
        }
    }

    fun logout(){
        handler?.removeCallbacksAndMessages(null)
        if(EaseRepository.instance.isLogin){
            EMClient.getInstance().chatManager().removeMessageListener(this)
            EMClient.getInstance().chatroomManager().removeChatRoomListener(this)
            EMClient.getInstance().removeConnectionListener(this)
            EMClient.getInstance().chatroomManager().leaveChatRoom(chatRoomId)
            EMClient.getInstance().chatManager().deleteConversation(chatRoomId, true)
            EMClient.getInstance().logout(false)
        }
        EaseRepository.instance.reset()
        context.unregisterReceiver(receiver)
    }

    fun setRoomUuid(roomUuid: String) {
        this.roomUuid = roomUuid
        EaseRepository.instance.roomUuid = roomUuid
    }

    fun setChatRoomId(chatRoomId: String) {
        this.chatRoomId = chatRoomId
        EaseRepository.instance.chatRoomId = chatRoomId
    }

    fun setNickName(nickName: String) {
        this.nickName = nickName
        EaseRepository.instance.nickName = nickName
    }

    fun setAvatarUrl(avatarUrl: String) {
        this.avatarUrl = avatarUrl
        EaseRepository.instance.avatarUrl = avatarUrl
    }

    fun setUserName(userName: String) {
        this.userName = userName.toLowerCase()
        EaseRepository.instance.userName = userName
    }

    fun setUserUuid(userUuid: String) {
        this.userUuid = userUuid
        EaseRepository.instance.userUuid = userUuid
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
                    ThreadManager.instance.runOnMainThread {
                        Toast.makeText(context, context.getString(R.string.login_chat_failed)+":$code:$error", Toast.LENGTH_SHORT).show()
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
                    EaseRepository.instance.isLogin = true
                    initIMListener()
                    chatView.initData()
                }
            }

            override fun onError(error: Int, errorMsg: String) {
                EMLog.e(TAG, "join failed: $error:$errorMsg")
                if (error == EMError.CHATROOM_ALREADY_JOINED) {
                    ThreadManager.instance.runOnMainThread {
                        EaseRepository.instance.isLogin = true
                        initIMListener()
                        chatView.initData()
                    }
                    return
                }
                if (joinLimit == 2) {
                    ThreadManager.instance.runOnMainThread {
                        Toast.makeText(context, context.getString(R.string.login_chat_failed)+"--"+context.getString(R.string.join_chat_room_failed)+":$error:$errorMsg", Toast.LENGTH_SHORT).show()
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
                ThreadManager.instance.runOnMainThread {
                    Toast.makeText(context, context.getString(R.string.login_chat_failed)+"--"+context.getString(R.string.create_failed)+":" + e.errorCode + ":" + e.description, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onAnnouncementClick() {
        chooseTab = 1
        chooseTab(tabLayout?.getTabAt(1))
        tabLayout?.getTabAt(1)?.let { viewPager.setCurrentItem(it.position, true) }
    }

    override fun onMsgContentClick() {
        chatPagerListener?.onMsgContentClick()
    }

    override fun onFaceIconClick() {
        chatPagerListener?.onFaceIconClick()
    }

    override fun onPicIconClick() {
        selectPicFromLocal()
    }

    override fun onImageClick(message: EMMessage) {
        chatPagerListener?.onImageClick(message)
    }

    /**
     * 展示未读标识
     */
    private fun showUnread(index: Int) {
        val unread = tabLayout?.getTabAt(index)?.view?.findViewById<ImageView>(R.id.iv_tips)
        unread?.visibility = View.VISIBLE
    }

    /**
     * 刷新聊天页
     */
    private fun refreshUI() {
        ThreadManager.instance.runOnMainThread {
            chatView.refresh()
        }
    }

    override fun onConnected() {
        EMLog.e(TAG, "onConnected")
        if(EaseRepository.instance.isInit && EaseRepository.instance.isLogin){
            EMClient.getInstance().chatroomManager().joinChatRoom(chatRoomId, object : EMValueCallBack<EMChatRoom> {
                override fun onSuccess(value: EMChatRoom?) {
                    EaseRepository.instance.reconnectionLoadMessages()
                    EaseRepository.instance.fetchChatRoomMutedStatus()
                }

                override fun onError(error: Int, errorMsg: String?) {
                    if (error == EMError.CHATROOM_ALREADY_JOINED) {
                        EaseRepository.instance.reconnectionLoadMessages()
                        EaseRepository.instance.fetchChatRoomMutedStatus()
                    }
                }
            })
        }

    }

    override fun onDisconnected(errorCode: Int) {
        EMLog.e(TAG, "onDisconnected:$errorCode")
        if(EaseRepository.instance.isInit && EaseRepository.instance.isLogin) {
            EaseRepository.instance.refreshLastMessageId()
        }
    }

    fun setInputContent(content: String) {
        chatView.setInputContent(content)
    }

    private fun showOuterLayerUnread() {
        val chatUnread = tabLayout?.getTabAt(0)?.view?.findViewById<ImageView>(R.id.iv_tips)
        val noticeUnread = tabLayout?.getTabAt(1)?.view?.findViewById<ImageView>(R.id.iv_tips)
        chatPagerListener?.onShowUnread(chatUnread?.visibility == VISIBLE || noticeUnread?.visibility == VISIBLE)
        chatPagerListener?.onShowUnread(chatUnread?.visibility == VISIBLE || noticeUnread?.visibility == VISIBLE)
    }

    @UiThread
    fun setCloseable(closable: Boolean) {
        iconHidden.visibility = if (closable) VISIBLE else GONE
    }

    @UiThread
    fun setTabLayoutCloseable(closable: Boolean) {
        var params = tabLayout?.layoutParams
        params?.height = 0
        tabLayout?.layoutParams = params
        tabLayout?.removeAllTabs()
        pagerList.removeLast()
        val viewPagerAdapter = ChatViewPagerAdapter(pagerList)
        viewPager.adapter = viewPagerAdapter
    }

    fun sendTextMessage(content: String) {
        val message = EMMessage.createTxtSendMessage(content, chatRoomId)
        sendMessage(message)
    }

    fun sendImageMessage(uri: Uri) {
        val message = EMMessage.createImageSendMessage(uri, false, chatRoomId)
        sendMessage(message)
    }

    private fun sendMessage(message: EMMessage) {
        if (!(EaseRepository.instance.isInit && EaseRepository.instance.isLogin)) {
            Toast.makeText(context, context.getString(R.string.send_message_failed) + ":" + context.getString(R.string.login_chat_failed), Toast.LENGTH_SHORT).show()
            return
        }
        setExtBeforeSend(message)
        message.chatType = EMMessage.ChatType.ChatRoom
        message.setMessageStatusCallback(object : EMCallBack {
            override fun onSuccess() {

            }

            override fun onError(code: Int, error: String?) {
                if (code == EMError.MESSAGE_INCLUDE_ILLEGAL_CONTENT) {
                    ThreadManager.instance.runOnMainThread {
                        Toast.makeText(context, context.getString(R.string.message_incloud_illegal_content), Toast.LENGTH_SHORT).show()
                    }
                }
                EMLog.e(TAG, "onMessageError:$code = $error")
            }

            override fun onProgress(progress: Int, status: String?) {

            }
        })
        EMClient.getInstance().chatManager().sendMessage(message)
        refreshUI()
    }

    private fun setExtBeforeSend(message: EMMessage) {
        message.setAttribute(EaseConstant.ROLE, EaseConstant.ROLE_STUDENT)
        message.setAttribute(EaseConstant.MSG_TYPE, EaseConstant.NORMAL_MSG)
        message.setAttribute(EaseConstant.ROOM_UUID, roomUuid)
        message.setAttribute(EaseConstant.NICK_NAME, nickName)
        message.setAttribute(EaseConstant.AVATAR_URL, avatarUrl)
    }

    /**
     * 选择本地相册
     */
    fun selectPicFromLocal() {
        val intent: Intent?
        if (VersionUtils.isTargetQ(context)) {
            intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
        } else {
            intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        }
        intent.type = "image/*"
        val activity = context as Activity
        activity.startActivityForResult(intent, selectImageResultCode)
    }

    private fun initReceiver() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(con: Context?, intent: Intent?) {
                val data = intent?.getParcelableExtra<Uri>(context.resources
                        .getString(R.string.chat_window_select_image_key))
                data?.let {
                    val filePath = EMFileHelper.getInstance().getFilePath(data)
                    if (filePath.isNotEmpty() && File(filePath).exists()) {
                        sendImageMessage(Uri.parse(filePath))
                    } else {
                        CommonUtil.takePersistableUriPermission(context, it)
                        sendImageMessage(it)
                    }
                }
            }
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(context.packageName.plus(
                context.resources.getString(R.string.chat_window_select_image_action)))
        context.registerReceiver(receiver, intentFilter)
    }
}