package com.hyphenate.easeim.modules.repositories

import com.hyphenate.EMCallBack
import com.hyphenate.EMValueCallBack
import com.hyphenate.chat.*
import com.hyphenate.easeim.modules.constant.EaseConstant
import com.hyphenate.easeim.modules.manager.ThreadManager
import com.hyphenate.easeim.modules.view.`interface`.EaseOperationListener
import com.hyphenate.util.EMLog

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
            val conversation = EMClient.getInstance().chatManager()
                    .getConversation(chatRoomId, EMConversation.EMConversationType.ChatRoom, true)
            val msgList = conversation?.allMessages
            val norMsgList = mutableListOf<EMMessage>()
            msgList?.forEach { message ->
                val msgType = message.getIntAttribute(EaseConstant.MSG_TYPE, EaseConstant.NORMAL_MSG)
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
        EMClient.getInstance().chatManager().asyncFetchHistoryMessage(chatRoomId, EMConversation.EMConversationType.ChatRoom, 50, "", object : EMValueCallBack<EMCursorResult<EMMessage>> {
            override fun onSuccess(value: EMCursorResult<EMMessage>?) {
                value?.data?.forEach { message ->
                    if (message.type == EMMessage.Type.CMD) {
                        val body = message.body as EMCmdMessageBody
                        val notifyMessage = EMMessage.createSendMessage(EMMessage.Type.CUSTOM)
                        val notifyBody = EMCustomMessageBody(EaseConstant.NOTIFY)
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
                                if (!member.equals(EMClient.getInstance().currentUser))
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
                    }
                }
                ThreadManager.instance.runOnMainThread {
                    val conversation = EMClient.getInstance().chatManager().getConversation(chatRoomId, EMConversation.EMConversationType.ChatRoom, true)
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
        val conversation = EMClient.getInstance().chatManager().getConversation(chatRoomId, EMConversation.EMConversationType.ChatRoom, true)
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
            EMClient.getInstance().chatManager().asyncFetchHistoryMessage(chatRoomId, EMConversation.EMConversationType.ChatRoom, 50, lastMsgId, object : EMValueCallBack<EMCursorResult<EMMessage>> {
                override fun onSuccess(value: EMCursorResult<EMMessage>?) {
                    value?.data?.forEach { message ->
                        if (message.type == EMMessage.Type.CMD) {
                            val body = message.body as EMCmdMessageBody
                            val notifyMessage = EMMessage.createSendMessage(EMMessage.Type.CUSTOM)
                            val notifyBody = EMCustomMessageBody(EaseConstant.NOTIFY)
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
                                    if (!member.equals(EMClient.getInstance().currentUser))
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
            val conversation = EMClient.getInstance().chatManager().getConversation(chatRoomId, EMConversation.EMConversationType.ChatRoom, true)
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
        EMClient.getInstance().chatroomManager()
                .asyncFetchChatRoomAnnouncement(chatRoomId, object : EMValueCallBack<String> {
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
        EMClient.getInstance().chatroomManager().checkIfInChatRoomWhiteList(
                chatRoomId, object : EMValueCallBack<Boolean> {
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
                EMLog.e(TAG, "fetchChatRoomSingleMutedStatus failed: $error = $errorMsg")
            }
        })
    }

    /**
     * 获取聊天室禁言状态
     */
    @Synchronized
    fun fetchChatRoomMutedStatus() {
        EMClient.getInstance().chatroomManager().asyncFetchChatRoomFromServer(chatRoomId, object : EMValueCallBack<EMChatRoom> {
            override fun onSuccess(value: EMChatRoom?) {
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
    fun updateOwnInfo(emUserInfo: EMUserInfo) {
        EMClient.getInstance().userInfoManager().updateOwnInfo(emUserInfo, object : EMValueCallBack<String> {
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
            val conversation = EMClient.getInstance().chatManager().getConversation(conversationId, EMConversation.EMConversationType.ChatRoom, true)
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

    fun sendOperationMessage(action: String, userId: String, messageId: String, callback: EMCallBack?) {
        val message = EMMessage.createSendMessage(EMMessage.Type.CMD)
        message.addBody(EMCmdMessageBody(action))
        message.setAttribute(EaseConstant.ROLE, role)
        message.setAttribute(EaseConstant.MSG_TYPE, EaseConstant.NORMAL_MSG)
        message.setAttribute(EaseConstant.ROOM_UUID, roomUuid)
        message.setAttribute(EaseConstant.NICK_NAME, nickName)
        message.setAttribute(EaseConstant.AVATAR_URL, avatarUrl)
        message.chatType = EMMessage.ChatType.ChatRoom
        message.to = chatRoomId
        message.setMessageStatusCallback(object : EMCallBack {
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

        EMClient.getInstance().chatManager().sendMessage(message)
        saveOperationMessage(action, message, callback)
    }

    private fun saveOperationMessage(operation: String, message: EMMessage, callback: EMCallBack?) {
        val notifyMessage = EMMessage.createSendMessage(EMMessage.Type.CUSTOM)
        val notifyBody = EMCustomMessageBody(EaseConstant.NOTIFY)
        notifyBody.params = mutableMapOf(Pair(EaseConstant.OPERATION, operation))
        notifyMessage.body = notifyBody
        notifyMessage.to = chatRoomId
        notifyMessage.chatType = EMMessage.ChatType.ChatRoom
        notifyMessage.setStatus(EMMessage.Status.SUCCESS)
        notifyMessage.msgTime = message.msgTime
        notifyMessage.setAttribute(EaseConstant.NICK_NAME, message.getStringAttribute(EaseConstant.NICK_NAME, message.from))
        EMClient.getInstance().chatManager().saveMessage(notifyMessage)
        callback?.onSuccess()
        ThreadManager.instance.runOnMainThread {
            for (listener in listeners) {
                listener.loadHistoryMessageFinish()
            }
        }
    }

}