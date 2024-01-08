package io.agora.online.easeim.repositories

import io.agora.online.easeim.constant.EaseConstant
import io.agora.online.easeim.manager.ThreadManager
import io.agora.online.easeim.view.`interface`.EaseOperationListener
import io.agora.CallBack
import io.agora.ValueCallBack
import io.agora.chat.*
import io.agora.util.EMLog

class EaseRepository {
    companion object {
        private const val TAG = "EaseRepository"
        val instance by lazy(LazyThreadSafetyMode.NONE) {
            EaseRepository()
        }
    }

    var listeners = mutableListOf<EaseOperationListener>()
    var brokenMsgId = ""
    var lastMsgId = ""
    var fetchMsgNum = 0
    var singleMuted = false
    var allMuted = false
    var isInit = false
    var isLogin = false

    var role = EaseConstant.ROLE_STUDENT
    var nickName = ""
    var avatarUrl = ""
    var chatRoomId = ""
    var roomUuid = ""
    var userName = ""
    var userUuid = ""

    /**
     * 加载本地消息
     */
    fun loadMessages() {
        if (isInit) {
            // Load chatroom chatting messages and add to list
            val conversation = ChatClient.getInstance().chatManager()
                .getConversation(chatRoomId, Conversation.ConversationType.ChatRoom, true)
            val msgList = conversation?.allMessages

            val norMsgList = mutableListOf<ChatMessage>()

            msgList?.forEach { message ->
                val msgType =
                    message.getIntAttribute(EaseConstant.MSG_TYPE, EaseConstant.NORMAL_MSG)
                if (msgType == EaseConstant.NORMAL_MSG)
                    norMsgList.add(message)
            }

            for (listener in listeners) {
                listener.loadMessageFinish(norMsgList)
            }
        }
    }

    /**
     * 漫游50条历史消息
     */
    fun loadHistoryMessages() {
        EMLog.e(TAG, "loadHistoryMessages")
        ChatClient.getInstance().chatManager().asyncFetchHistoryMessage(chatRoomId, Conversation.ConversationType.ChatRoom, 50, "", object : ValueCallBack<CursorResult<ChatMessage>> {
            override fun onSuccess(value: CursorResult<ChatMessage>?) {
                value?.data?.forEach { message ->
                    if (message.type == ChatMessage.Type.CMD) {
                        val body = message.body as CmdMessageBody
                        val notifyMessage = ChatMessage.createSendMessage(ChatMessage.Type.CUSTOM)
                        val notifyBody = CustomMessageBody(EaseConstant.NOTIFY)
                        when (body.action()) {
                            EaseConstant.SET_ALL_MUTE, EaseConstant.REMOVE_ALL_MUTE -> {
                                notifyBody.params = mutableMapOf(Pair(EaseConstant.OPERATION, body.action()))

                            }
                            EaseConstant.DEL -> {
                                val msgId = message.getStringAttribute(EaseConstant.MSG_ID, "")
                                deleteMessage(chatRoomId, msgId)
                                notifyBody.params = mutableMapOf(Pair(EaseConstant.OPERATION, body.action()))
                            }
                            EaseConstant.MUTE, EaseConstant.UN_MUTE -> {
                                val member = message.getStringAttribute(EaseConstant.MUTE_MEMEBER, "")
                                if (!member.equals(ChatClient.getInstance().currentUser))
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
                    }
                }
                ThreadManager.instance.runOnMainThread {
                    val conversation = ChatClient.getInstance().chatManager().getConversation(chatRoomId, Conversation.ConversationType.ChatRoom, true)
                    conversation.loadMoreMsgFromDB("", 50)
                    for (listener in listeners) {
                        listener.loadHistoryMessageFinish()
                    }
                }
            }

            override fun onError(error: Int, errorMsg: String?) {
                EMLog.e(TAG, "loadHistoryMessages failed: $error = $errorMsg")
            }

        })
    }

    fun refreshLastMessageId() {
        val conversation = ChatClient.getInstance().chatManager().getConversation(chatRoomId, Conversation.ConversationType.ChatRoom, true)
        if (conversation.allMessages.size != 0)
            brokenMsgId = conversation.lastMessage?.msgId.toString()
        EMLog.e(TAG, "brokenMsgId=$brokenMsgId")
    }

    /**
     * 重连之后拉取消息
     */
    @Synchronized
    fun reconnectionLoadMessages() {
        EMLog.e(TAG, "reconnectionLoadMessages:lastMsgId=$lastMsgId")
        if (brokenMsgId.isNotEmpty()) {
            ChatClient.getInstance().chatManager().asyncFetchHistoryMessage(chatRoomId, Conversation.ConversationType.ChatRoom, 50, lastMsgId, object : ValueCallBack<CursorResult<ChatMessage>> {
                override fun onSuccess(value: CursorResult<ChatMessage>?) {
                    value?.data?.forEach { message ->
                        if (message.type == ChatMessage.Type.CMD) {
                            val body = message.body as CmdMessageBody
                            val notifyMessage = ChatMessage.createSendMessage(ChatMessage.Type.CUSTOM)
                            val notifyBody = CustomMessageBody(EaseConstant.NOTIFY)
                            when (body.action()) {
                                EaseConstant.SET_ALL_MUTE, EaseConstant.REMOVE_ALL_MUTE -> {
                                    notifyBody.params = mutableMapOf(Pair(EaseConstant.OPERATION, body.action()))

                                }
                                EaseConstant.DEL -> {
                                    val msgId = message.getStringAttribute(EaseConstant.MSG_ID, "")
                                    deleteMessage(chatRoomId, msgId)
                                    notifyBody.params = mutableMapOf(Pair(EaseConstant.OPERATION, body.action()))
                                }
                                EaseConstant.MUTE, EaseConstant.UN_MUTE -> {
                                    val member = message.getStringAttribute(EaseConstant.MUTE_MEMEBER, "")
                                    if (!member.equals(ChatClient.getInstance().currentUser))
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
                        }
                        fetchMsgNum++
                        lastMsgId = message.msgId
                    }
                    value?.data?.forEach { message ->
                        if (message.msgId == brokenMsgId) {
                            loadMoreMsgFromDB()
                            return@onSuccess
                        }
                    }
                    reconnectionLoadMessages()
                }

                override fun onError(error: Int, errorMsg: String?) {
                    EMLog.e(TAG, "loadHistoryMessages failed: $error = $errorMsg")
                }
            })
        } else {
            loadHistoryMessages()
        }
    }

    fun loadMoreMsgFromDB() {
        ThreadManager.instance.runOnMainThread {
            val conversation = ChatClient.getInstance().chatManager().getConversation(chatRoomId, Conversation.ConversationType.ChatRoom, true)
            conversation.loadMoreMsgFromDB("", fetchMsgNum)
            brokenMsgId = ""
            lastMsgId = ""
            fetchMsgNum = 0
            for (listener in listeners) {
                listener.loadHistoryMessageFinish()
            }
        }
    }

    /**
     * 获取聊天室公告
     */
    fun fetchAnnouncement() {
        ChatClient.getInstance().chatroomManager()
                .asyncFetchChatRoomAnnouncement(chatRoomId, object : ValueCallBack<String> {
                    override fun onSuccess(value: String?) {
                        ThreadManager.instance.runOnMainThread {
                            value?.let {
                                for (listener in listeners) {
                                    listener.fetchAnnouncementFinish(it)
                                }
                            }
                        }
                    }

                    override fun onError(error: Int, errorMsg: String?) {
                        EMLog.e(TAG, "fetchAnnouncement failed: $error = $errorMsg")
                    }

                })
    }

    /**
     * 获取聊天室自己是否被禁言
     */
    @Synchronized
    fun fetchChatRoomSingleMutedStatus() {
        ChatClient.getInstance().chatroomManager().asyncCheckIfInMuteList(
            chatRoomId, object : ValueCallBack<Boolean> {
            override fun onSuccess(value: Boolean?) {
                value?.let {
                    singleMuted = it
                    ThreadManager.instance.runOnMainThread {
                        for (listener in listeners) {
                            if (allMuted || singleMuted)
                                listener.fetchChatRoomMutedStatus(true)
                            else
                                listener.fetchChatRoomMutedStatus(false)
                        }
                    }
                }
            }

            override fun onError(error: Int, errorMsg: String?) {
                EMLog.e(TAG, "asyncCheckIfInMuteList failed: $error = $errorMsg")
            }
        })
    }

    /**
     * chat 的 userUuid 和 room 里面的 userUuid 是不一样的
     * 1、chat（userUuid） = md5（room（userUuid））
     * 2、room 里面的 userUuid，存放在chat的User的 ext里面，进行关联
     */
    fun fetchUserChat() {
        // http://docs-im-beta.easemob.com/document/android/userprofile.html#%E8%8E%B7%E5%8F%96%E7%94%A8%E6%88%B7%E5%B1%9E%E6%80%A7
        // 1、获取所有成员，需要开启线程获取
        val fetchChatRoomFromServer = ChatClient.getInstance().chatroomManager().fetchChatRoomFromServer(chatRoomId)
        val memberList = fetchChatRoomFromServer.memberList  // 不包含管理员 , userId list
        val memberArray = memberList?.toTypedArray()

        // 2、获取成员的ext属性，查询这个用户的教室 userUuid
        ChatClient.getInstance().userInfoManager()
            .fetchUserInfoByUserId(memberArray, object : ValueCallBack<Map<String, UserInfo>> {
                override fun onSuccess(map: Map<String, UserInfo>?) {

                }

                override fun onError(p0: Int, p1: String?) {

                }
            })
    }

    /**
     * 获取聊天室禁言状态
     */
    @Synchronized
    fun fetchChatRoomMutedStatus() {
        ChatClient.getInstance().chatroomManager().asyncFetchChatRoomFromServer(chatRoomId, object : ValueCallBack<ChatRoom> {
            override fun onSuccess(value: ChatRoom?) {
                value?.isAllMemberMuted?.let {
                    allMuted = it
                    fetchChatRoomSingleMutedStatus()
                }
            }

            override fun onError(error: Int, errorMsg: String?) {
                EMLog.e(TAG, "fetchChatRoomAllMutedStatus failed: $error = $errorMsg")
            }

        })
    }

    /**
     * 更新用户属性
     */
    fun updateOwnInfo(emUserInfo: UserInfo) {
        ChatClient.getInstance().userInfoManager().updateOwnInfo(emUserInfo, object : ValueCallBack<String> {
            override fun onSuccess(value: String?) {

            }

            override fun onError(error: Int, errorMsg: String?) {
                EMLog.e(TAG, "updateOwnInfo failed: $error = $errorMsg")
            }
        })
    }

    /**
     * 删除消息
     */
    fun deleteMessage(conversationId: String, messageId: String) {
        EMLog.e(TAG, "deleteMessage")
        ThreadManager.instance.runOnMainThread {
            val conversation = ChatClient.getInstance().chatManager().getConversation(conversationId, Conversation.ConversationType.ChatRoom, true)
            conversation.removeMessage(messageId)
        }
    }

    fun reset() {
        EMLog.e(TAG, "reset")
        brokenMsgId = ""
        lastMsgId = ""
        fetchMsgNum = 0
        singleMuted = false
        allMuted = false
        isInit = false
        isLogin = false
    }

    fun addOperationListener(operationListener: EaseOperationListener) {
        listeners.add(operationListener)
    }

    fun removeOperationListener(operationListener: EaseOperationListener) {
        listeners.remove(operationListener)
    }

    fun isStudentRole(): Boolean {
        return role == EaseConstant.ROLE_STUDENT
    }

    fun sendOperationMessage(action: String, userId: String, messageId: String, callback: CallBack?) {
        val message = ChatMessage.createSendMessage(ChatMessage.Type.CMD)
        message.addBody(CmdMessageBody(action))
        message.setAttribute(EaseConstant.ROLE, role)
        message.setAttribute(EaseConstant.MSG_TYPE, EaseConstant.NORMAL_MSG)
        message.setAttribute(EaseConstant.ROOM_UUID, roomUuid)
        message.setAttribute(EaseConstant.NICK_NAME, nickName)
        message.setAttribute(EaseConstant.AVATAR_URL, avatarUrl)
        message.chatType = ChatMessage.ChatType.ChatRoom
        message.to = chatRoomId
        message.setMessageStatusCallback(object : CallBack {
            override fun onSuccess() {

            }

            override fun onError(code: Int, error: String?) {

            }

            override fun onProgress(progress: Int, status: String?) {
                callback?.onError(progress, status)
            }

        })
        when(action){
            EaseConstant.SET_ALL_MUTE, EaseConstant.REMOVE_ALL_MUTE ->{

            }
            EaseConstant.DEL -> {
                message.setAttribute(EaseConstant.MSG_ID, messageId)
            }
            EaseConstant.MUTE, EaseConstant.UN_MUTE -> {
                message.setAttribute(EaseConstant.MUTE_MEMEBER, userId)
            }
        }

        ChatClient.getInstance().chatManager().sendMessage(message)
        saveOperationMessage(action, message, callback)
    }

    private fun saveOperationMessage(operation: String, message: ChatMessage, callback: CallBack?) {
        val notifyMessage = ChatMessage.createSendMessage(ChatMessage.Type.CUSTOM)
        val notifyBody = CustomMessageBody(EaseConstant.NOTIFY)
        notifyBody.params = mutableMapOf(Pair(EaseConstant.OPERATION, operation))
        notifyMessage.body = notifyBody
        notifyMessage.to = chatRoomId
        notifyMessage.chatType = ChatMessage.ChatType.ChatRoom
        notifyMessage.setStatus(ChatMessage.Status.SUCCESS)
        notifyMessage.msgTime = message.msgTime
        notifyMessage.setAttribute(EaseConstant.NICK_NAME, message.getStringAttribute(EaseConstant.NICK_NAME, message.from))
        ChatClient.getInstance().chatManager().saveMessage(notifyMessage)
        callback?.onSuccess()
        ThreadManager.instance.runOnMainThread {
            for (listener in listeners) {
                listener.loadHistoryMessageFinish()
            }
        }
    }

}