package com.hyphenate.easeimgroup.modules.repositories

import com.hyphenate.easeimgroup.modules.constant.EaseConstant
import com.hyphenate.easeimgroup.modules.exception.ChatError
import com.hyphenate.easeimgroup.modules.manager.ThreadManager
import com.hyphenate.easeimgroup.modules.view.`interface`.EaseOperationListener
import io.agora.CallBack
import io.agora.ValueCallBack
import io.agora.chat.*
import io.agora.util.EMLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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
    var recvRoomIds: List<String> = emptyList()
    /**
     * 加载本地消息
     */
    fun loadMessages() {
        if (isInit) {
            val norMsgList = mutableListOf<ChatMessage>()
            recvRoomIds.forEach { roomId->
                val conversation = ChatClient.getInstance().chatManager()
                    .getConversation(roomId, Conversation.ConversationType.ChatRoom, true)
                val msgList = conversation?.allMessages
                msgList?.forEach { message ->
                    val msgType = message.getIntAttribute(EaseConstant.MSG_TYPE, EaseConstant.NORMAL_MSG)
                    if (msgType == EaseConstant.NORMAL_MSG)
                        norMsgList.add(message)
                }
            }
            norMsgList.sortBy { msg-> msg.msgTime}
            for (listener in listeners) {
                listener.loadMessageFinish(norMsgList)
            }
        }
    }




    suspend fun getHistoryMsgs(roomId:String):List<ChatMessage> = suspendCoroutine { cont ->
        ChatClient.getInstance().chatManager().asyncFetchHistoryMessage(roomId, Conversation.ConversationType.ChatRoom, 50, "", object : ValueCallBack<CursorResult<ChatMessage>> {
            override fun onSuccess(value: CursorResult<ChatMessage>?) {
                cont.resume(value?.data ?: emptyList())
            }

            override fun onError(error: Int, errorMsg: String?) {
                val message = "loadHistoryMessages failed: $error = $errorMsg"
                cont.resumeWithException(ChatError(error, message))
                EMLog.e(TAG, message)
            }

        })
    }

    private suspend fun batchGetHistoryMsgs(roomIds:List<String>):List<ChatMessage> {
        val list = mutableListOf<ChatMessage>()
        roomIds.forEach { i->
            val item = getHistoryMsgs(i)
            item.forEach { j->
                list.add(j)
            }
        }
        return list
    }

    /**
     * 漫游50条历史消息
     */
    fun loadHistoryMessages() {
        EMLog.e(TAG, "loadHistoryMessages")

        CoroutineScope(Dispatchers.Main).launch {
            val msgs = batchGetHistoryMsgs(recvRoomIds)
            msgs.forEach { message->
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

            val lastCmdMsg = msgs.sortedBy { it.msgTime }.filter { msg ->
                if(msg.type != ChatMessage.Type.CMD){
                    return@filter false
                }
                val body = msg.body as CmdMessageBody
                return@filter body.action() == EaseConstant.SET_ALL_MUTE || body.action() == EaseConstant.REMOVE_ALL_MUTE || body.action() == EaseConstant.MUTE || body.action() == EaseConstant.UN_MUTE
            }.lastOrNull()

            if(lastCmdMsg != null){
                val body = lastCmdMsg.body as CmdMessageBody
                when (body.action()) {
                    EaseConstant.SET_ALL_MUTE, EaseConstant.REMOVE_ALL_MUTE -> {
                        val isMuted = body.action() == EaseConstant.SET_ALL_MUTE
                        allMuted = isMuted
                        for (listener in listeners) {
                            listener.fetchChatRoomMutedStatus(isMuted)
                        }
                    }
                    EaseConstant.MUTE, EaseConstant.UN_MUTE -> {
                        val member = lastCmdMsg.getStringAttribute(EaseConstant.MUTE_MEMEBER, "")
                        if (member.equals(userName)){
                            val isMuted = body.action() == EaseConstant.MUTE
                            singleMuted = isMuted

                            for (listener in listeners) {
                                listener.fetchChatRoomMutedStatus(isMuted)
                            }
                        }

                    }
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