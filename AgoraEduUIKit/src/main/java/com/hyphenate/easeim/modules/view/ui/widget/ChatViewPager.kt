package com.hyphenate.easeim.modules.view.ui.widget

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import io.agora.agoraeduuikit.R
import com.hyphenate.easeim.modules.constant.EaseConstant
import com.hyphenate.easeim.modules.manager.ThreadManager
import com.hyphenate.easeim.modules.repositories.EaseRepository
import com.hyphenate.easeim.modules.utils.CommonUtil
import com.hyphenate.easeim.modules.utils.ScreenUtil
import com.hyphenate.easeim.modules.view.`interface`.ChatPagerListener
import com.hyphenate.easeim.modules.view.`interface`.ViewEventListener
import com.hyphenate.easeim.modules.view.adapter.ChatViewPagerAdapter
import io.agora.*
import io.agora.chat.*
import io.agora.util.EMLog
import io.agora.util.FileHelper
import io.agora.util.VersionUtils
import io.agora.Error
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors


class ChatViewPager(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : LinearLayout(context, attributeSet, defStyleAttr), MessageListener, ChatRoomChangeListener, ViewEventListener, ConnectionListener {
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
    private lateinit var container: RelativeLayout
    private var chooseTab = 0
    private var loginLimit = 0
    private var joinLimit = 0

    private var chatTab = 0
    private var announcementTab = 1

    private var roomType = 4
    private var roomUuid = ""
    private var chatRoomId = ""
    private var nickName = ""
    private var avatarUrl = ""
    private var userName = ""
    private var userUuid = ""
    private var userToken = ""
    private var appId = ""
    private var baseUrl = ""
    private var imToken = ""
    private var pagerList = mutableListOf<View>()
    var chatPagerListener: ChatPagerListener? = null

    private lateinit var receiver:BroadcastReceiver
    private val selectImageResultCode = 78
    private var executor = Executors.newScheduledThreadPool(1)

    init {
        LayoutInflater.from(context).inflate(R.layout.fcr_chat_total_layout, this)
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
            context.getString(R.string.fcr_hyphenate_im_chat),
            context.getString(R.string.fcr_hyphenate_im_announcement)
        )
        announcementTab = 1
        val viewPagerAdapter = ChatViewPagerAdapter(pagerList)
        viewPager.adapter = viewPagerAdapter
        viewPager.offscreenPageLimit = 2
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
        container.post {
            ScreenUtil.instance.screenWidth = container.width
            ScreenUtil.instance.screenHeight = container.height
        }
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
        chatView.viewEventListener = this
        announcementView.viewEventListener = this
    }

    private fun initIMListener(){
        ChatClient.getInstance().chatManager().addMessageListener(this)
        ChatClient.getInstance().chatroomManager().addChatRoomChangeListener(this)
        ChatClient.getInstance().addConnectionListener(this)
    }

    private fun getTabView(context: Context, title: String): View {
        val view: View =
            LayoutInflater.from(context).inflate(R.layout.fcr_view_pager_tab_item_layout, null)
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
            title?.setTextColor(ContextCompat.getColor(context, R.color.fcr_text_level2_color))
        }
    }

    /**
     * 选中状态
     */
    private fun chooseTab(tab: TabLayout.Tab?) {
        val title = tab?.view?.findViewById<TextView>(R.id.title)
        val unread = tab?.view?.findViewById<ImageView>(R.id.iv_tips)
        title?.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        title?.setTextColor(ContextCompat.getColor(context, R.color.fcr_icon_fill_color))
        unread?.visibility = View.INVISIBLE
        showOuterLayerUnread()
    }

    override fun onMessageReceived(messages: MutableList<ChatMessage>?) {
        messages?.let {
            for (message in messages) {
                if (message.getIntAttribute(EaseConstant.MSG_TYPE, EaseConstant.NORMAL_MSG) == EaseConstant.NORMAL_MSG) {
                    ThreadManager.instance.runOnMainThread {
                        if (chooseTab != chatTab) {
                            showUnread(chatTab)
                        }
                        showOuterLayerUnread()
                        refreshUI()
                    }
                }
            }
        }


    }

    override fun onCmdMessageReceived(messages: MutableList<ChatMessage>?) {
        messages?.forEach { message ->
            if (message.chatType == ChatMessage.ChatType.ChatRoom && message.to == chatRoomId) {
                val body = message.body as CmdMessageBody
                val notifyMessage = ChatMessage.createSendMessage(ChatMessage.Type.CUSTOM)
                val notifyBody = CustomMessageBody(EaseConstant.NOTIFY)
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
                notifyMessage.chatType = ChatMessage.ChatType.ChatRoom
                notifyMessage.setStatus(ChatMessage.Status.SUCCESS)
                notifyMessage.msgTime = message.msgTime
                notifyMessage.msgId = message.msgId
                notifyMessage.setAttribute(EaseConstant.NICK_NAME, message.getStringAttribute(EaseConstant.NICK_NAME, message.from))
                ChatClient.getInstance().chatManager().saveMessage(notifyMessage)
                ThreadManager.instance.runOnMainThread {
                    if (chooseTab != chatTab) {
                        showUnread(chatTab)
                    }
                    showOuterLayerUnread()
                }
            }
        }
        refreshUI()
    }

    override fun onMessageRead(messages: MutableList<ChatMessage>?) {
    }

    override fun onMessageDelivered(messages: MutableList<ChatMessage>?) {
    }

    override fun onMessageRecalled(messages: MutableList<ChatMessage>?) {
    }

    override fun onMessageChanged(message: ChatMessage?, change: Any?) {
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
        mutes?.forEach {
            if (it == userName) {
                EaseRepository.instance.singleMuted = true
                ThreadManager.instance.runOnMainThread {
                    chatView.showMutedView()
                    chatPagerListener?.onMuted(true)
                }
            }
        }
    }

    override fun onMuteListRemoved(chatRoomId: String?, mutes: MutableList<String>?) {
        mutes?.forEach {
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

    override fun onWhiteListAdded(chatRoomId: String?, whitelist: MutableList<String>?) {

    }

    override fun onWhiteListRemoved(chatRoomId: String?, whitelist: MutableList<String>?) {

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
                    if (chooseTab != announcementTab)
                        showUnread(announcementTab)
                    showOuterLayerUnread()
                }
            }
        }
    }

    fun logout(){
        handler?.removeCallbacksAndMessages(null)
        if(EaseRepository.instance.isLogin){
            ChatClient.getInstance().chatManager().removeMessageListener(this)
            ChatClient.getInstance().chatroomManager().removeChatRoomListener(this)
            ChatClient.getInstance().removeConnectionListener(this)
            ChatClient.getInstance().chatroomManager().leaveChatRoom(chatRoomId)
            ChatClient.getInstance().chatManager().deleteConversation(chatRoomId, true)
            ChatClient.getInstance().logout(false, object : CallBack {
                override fun onSuccess() {
                    EMLog.e(TAG, "onSuccess")

                }

                override fun onError(code: Int, error: String) {
                    EMLog.e(TAG, "onError:$code:$error")
                }

                override fun onProgress(progress: Int, status: String) {}
            })
        }
        EaseRepository.instance.reset()

        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            EMLog.e(TAG, "Exception:$e")
        }
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

    fun setUserToken(userToken: String) {
        this.userToken = userToken
    }

    fun setBaseUrl(baseUrl: String) {
        this.baseUrl = baseUrl
    }

    fun setAppId(appId: String){
        this.appId = appId
    }

    fun fetchIMToken(){
        val urlPath = "edu/apps/$appId/v2/rooms/$roomUuid/widgets/easemobIM/users/$userUuid/token"
        val client = OkHttpClient.Builder().build()
        val request = Request.Builder()
            .url(baseUrl + urlPath)
            .header("Authorization", "agora token=\"$userToken\"")
            .header("x-agora-token", userToken)
            .header("x-agora-uid", userUuid)
            .get()
            .build()
        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                EMLog.e(TAG, "fetchIMToken failed: ${e.message}")
                ThreadManager.instance.runOnMainThread {
                    Toast.makeText(context, context.getString(R.string.fcr_hyphenate_im_fetch_token_failed)+":${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val repBody = response.body?.string()
                val code = response.code
                EMLog.e(TAG, "fetchIMToken: $repBody")
                ThreadManager.instance.runOnMainThread {
                    if (code == 200 && !repBody.isNullOrEmpty()) {
                        try {
                            val json = JSONObject(repBody)
                            val data = json.optJSONObject("data")
                            imToken = data?.optString("token").toString()
                            loginIM()
                        } catch (e: JSONException) {
                            Toast.makeText(context, context.getString(R.string.fcr_hyphenate_im_fetch_token_failed)+": $code : ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.fcr_hyphenate_im_fetch_token_failed)+": $code : $repBody", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    /**
     * 登录环信
     */
    private fun loginIM() {
        loginLimit++
        ChatClient.getInstance().loginWithToken(userName, imToken, object : CallBack {
            override fun onSuccess() {
                val info = UserInfo()
                info.nickname = nickName
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
                        Toast.makeText(context, context.getString(R.string.fcr_hyphenate_im_login_chat_failed)+":$code:$error", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                if (code == Error.USER_ALREADY_LOGIN) {
                    joinChatRoom()
                    return
                }
                loginIM()
            }
        })
    }

    /**
     * 加入聊天室
     */
    private fun joinChatRoom() {
        joinLimit++
        ChatClient.getInstance().chatroomManager().joinChatRoom(chatRoomId, object : ValueCallBack<ChatRoom?> {
            override fun onSuccess(value: ChatRoom?) {
                EMLog.e("Login:", "join success")
                ThreadManager.instance.runOnMainThread {
                    EaseRepository.instance.isLogin = true
                    initIMListener()
                    chatView.initData()
                }
            }

            override fun onError(error: Int, errorMsg: String) {
                EMLog.e(TAG, "join failed: $error:$errorMsg")
                if (error == Error.CHATROOM_ALREADY_JOINED) {
                    ThreadManager.instance.runOnMainThread {
                        EaseRepository.instance.isLogin = true
                        initIMListener()
                        chatView.initData()
                    }
                    return
                }
                if (joinLimit == 2) {
                    ThreadManager.instance.runOnMainThread {
                        Toast.makeText(context, context.getString(R.string.fcr_hyphenate_im_login_chat_failed)+"--"+context.getString(R.string.fcr_hyphenate_im_join_chat_room_failed)+":$error:$errorMsg", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                joinChatRoom()
            }
        })
    }

    override fun onAnnouncementClick() {
        chooseTab = announcementTab
        chooseTab(tabLayout.getTabAt(announcementTab))
        tabLayout.getTabAt(announcementTab)?.let { viewPager.setCurrentItem(it.position, true) }
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

    override fun onImageClick(message: ChatMessage) {
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
            ChatClient.getInstance().chatroomManager().joinChatRoom(chatRoomId, object : ValueCallBack<ChatRoom> {
                override fun onSuccess(value: ChatRoom?) {
                    EaseRepository.instance.reconnectionLoadMessages()
                    EaseRepository.instance.fetchChatRoomMutedStatus()
                }

                override fun onError(error: Int, errorMsg: String?) {
                    if (error == Error.CHATROOM_ALREADY_JOINED) {
                        EaseRepository.instance.reconnectionLoadMessages()
                        EaseRepository.instance.fetchChatRoomMutedStatus()
                    }
                }
            })
        }

    }

    override fun onAnnouncementChange(content: String) {
        ThreadManager.instance.runOnMainThread {
            chatView.announcementChange(content)
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
        val chatUnread = tabLayout?.getTabAt(chatTab)?.view?.findViewById<ImageView>(R.id.iv_tips)
        val noticeUnread = tabLayout?.getTabAt(announcementTab)?.view?.findViewById<ImageView>(R.id.iv_tips)
        if (this.isAttachedToWindow) {
            chatPagerListener?.onShowUnread(chatUnread?.visibility == VISIBLE || noticeUnread?.visibility == VISIBLE)
        } else { // 页面不可见的时候
            chatPagerListener?.onShowUnread(true)
        }
    }

    @UiThread
    fun setCloseable(closable: Boolean) {
        iconHidden.visibility = if (closable) VISIBLE else GONE
    }

    @UiThread
    fun setMuteViewVisibility(visibility: Boolean) {
        chatView.setMuteViewVisibility(visibility)
    }

    @UiThread
    fun setChatLayoutBackground(background: Int) {
        container.setBackgroundResource(background)
    }

    @UiThread
    fun setTabLayoutCloseable(closable: Boolean) {
        var params = tabLayout?.layoutParams
        params?.height = 0
        tabLayout?.layoutParams = params
        tabLayout?.removeAllTabs()
        pagerList.removeLastOrNull()
        val viewPagerAdapter = ChatViewPagerAdapter(pagerList)
        viewPager.adapter = viewPagerAdapter
    }

    @UiThread
    fun setInputViewCloseable(closable: Boolean) {
        chatView.tvInputView?.visibility  = if (closable) VISIBLE else GONE
    }

    fun sendTextMessage(content: String) {
        val message = ChatMessage.createTxtSendMessage(content, chatRoomId)
        sendMessage(message)
    }

    fun sendImageMessage(uri: Uri) {
        val message = ChatMessage.createImageSendMessage(uri, false, chatRoomId)
        sendMessage(message)
    }

    private fun sendMessage(message: ChatMessage) {
        if (!(EaseRepository.instance.isInit && EaseRepository.instance.isLogin)) {
            Toast.makeText(context, context.getString(R.string.fcr_hyphenate_im_send_message_failed) + ":" + context.getString(R.string.fcr_hyphenate_im_login_chat_failed), Toast.LENGTH_SHORT).show()
            return
        }
        setExtBeforeSend(message)
        message.chatType = ChatMessage.ChatType.ChatRoom
        message.setMessageStatusCallback(object : CallBack {
            override fun onSuccess() {

            }

            override fun onError(code: Int, error: String?) {
                if (code == Error.MESSAGE_INCLUDE_ILLEGAL_CONTENT) {
                    ThreadManager.instance.runOnMainThread {
                        Toast.makeText(context, context.getString(R.string.fcr_hyphenate_im_message_incloud_illegal_content), Toast.LENGTH_SHORT).show()
                    }
                }
                EMLog.e(TAG, "onMessageError:$code = $error")
            }

            override fun onProgress(progress: Int, status: String?) {

            }
        })
        ChatClient.getInstance().chatManager().sendMessage(message)
        refreshUI()
    }

    private fun setExtBeforeSend(message: ChatMessage) {
        message.setAttribute(EaseConstant.ROLE, EaseRepository.instance.role)
        message.setAttribute(EaseConstant.MSG_TYPE, EaseConstant.NORMAL_MSG)
        message.setAttribute(EaseConstant.ROOM_UUID, roomUuid)
        message.setAttribute(EaseConstant.NICK_NAME, nickName)
        message.setAttribute(EaseConstant.AVATAR_URL, avatarUrl)
    }

    fun setRoomType(roomType: Int){
        this.roomType = roomType
        chatView.setRoomType(roomType)
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
                    .getString(R.string.fcr_chat_window_select_image_key))
                data?.let {
                    val filePath = FileHelper.getInstance().getFilePath(data)
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
            context.resources.getString(R.string.fcr_chat_window_select_image_action)))
        context.registerReceiver(receiver, intentFilter)
    }
}