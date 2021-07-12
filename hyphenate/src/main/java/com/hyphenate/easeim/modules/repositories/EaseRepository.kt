package com.hyphenate.easeim.modules.repositories

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

    /**
     * 加载本地消息
     */
    fun loadMessages(conversationId: String) {
        val conversation = EMClient.getInstance().chatManager()
                .getConversation(conversationId, EMConversation.EMConversationType.ChatRoom, true)
        val msgList = conversation.allMessages
        val norMsgList = mutableListOf<EMMessage>()
        for (message in msgList) {
            if (message.type == EMMessage.Type.TXT || message.type == EMMessage.Type.CUSTOM)
                norMsgList.add(message)
        }
        for (listener in listeners) {
            listener.loadMessageFinish(norMsgList)
        }
    }

    /**
     * 漫游50条历史消息
     */
    fun loadHistoryMessages(conversationId: String) {
        EMLog.e("test:", "loadHistoryMessages")
        EMClient.getInstance().chatManager().asyncFetchHistoryMessage(conversationId, EMConversation.EMConversationType.ChatRoom, 50, "", object : EMValueCallBack<EMCursorResult<EMMessage>> {
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
                                deleteMessage(conversationId, msgId)
                                notifyBody.params = mutableMapOf(Pair(EaseConstant.OPERATION, body.action()))
                            }
                        }
                        notifyMessage.body = notifyBody
                        notifyMessage.to = conversationId
                        notifyMessage.chatType = EMMessage.ChatType.ChatRoom
                        notifyMessage.setStatus(EMMessage.Status.SUCCESS)
                        notifyMessage.msgTime = message.msgTime
                        notifyMessage.msgId = message.msgId
                        notifyMessage.setAttribute(EaseConstant.NICK_NAME, message.getStringAttribute(EaseConstant.NICK_NAME, message.from))
                        EMClient.getInstance().chatManager().saveMessage(notifyMessage)
                    }
                }
                ThreadManager.instance.runOnMainThread {
                    val conversation = EMClient.getInstance().chatManager().getConversation(conversationId, EMConversation.EMConversationType.ChatRoom, true)
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

    fun refreshLastMessageId(conversationId: String) {
        val conversation = EMClient.getInstance().chatManager().getConversation(conversationId, EMConversation.EMConversationType.ChatRoom, true)
        if (conversation.allMessages.size != 0)
            brokenMsgId = conversation.lastMessage?.msgId.toString()
    }

    /**
     * 重连之后拉取消息
     */
    @Synchronized
    fun reconnectionLoadMessages(conversationId: String) {
        if (brokenMsgId.isNotEmpty()) {
            EMClient.getInstance().chatManager().asyncFetchHistoryMessage(conversationId, EMConversation.EMConversationType.ChatRoom, 50, lastMsgId, object : EMValueCallBack<EMCursorResult<EMMessage>> {
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
                                    deleteMessage(conversationId, msgId)
                                    notifyBody.params = mutableMapOf(Pair(EaseConstant.OPERATION, body.action()))
                                }
                            }
                            notifyMessage.body = notifyBody
                            notifyMessage.to = conversationId
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
                            loadMoreMsgFromDB(conversationId, fetchMsgNum)
                            return@onSuccess
                        }
                    }
                    reconnectionLoadMessages(conversationId)
                }

                override fun onError(error: Int, errorMsg: String?) {
                    EMLog.e(TAG, "loadHistoryMessages failed: $error = $errorMsg")
                }
            })
        } else {
            loadHistoryMessages(conversationId)
        }
    }

    fun loadMoreMsgFromDB(conversationId: String, msgNum: Int) {
        ThreadManager.instance.runOnMainThread {
            val conversation = EMClient.getInstance().chatManager().getConversation(conversationId, EMConversation.EMConversationType.ChatRoom, true)
            conversation.loadMoreMsgFromDB("", msgNum)
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
    fun fetchAnnouncement(chatRoomId: String) {
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
    fun fetchChatRoomSingleMutedStatus(chatRoomId: String) {
        EMClient.getInstance().chatroomManager().checkIfInChatRoomWhiteList(
                chatRoomId, object : EMValueCallBack<Boolean> {
            override fun onSuccess(value: Boolean?) {
                ThreadManager.instance.runOnMainThread {
                    for (listener in listeners) {
                        value?.let { listener.fetchChatRoomMutedStatus(it) }
                    }
                }
            }

            override fun onError(error: Int, errorMsg: String?) {
                EMLog.e(TAG, "fetchChatRoomSingleMutedStatus failed: $error = $errorMsg")
            }
        })
    }

    /**
     * 获取聊天室全员禁言状态
     */
    @Synchronized
    fun fetchChatRoomMutedStatus(chatRoomId: String) {
        EMClient.getInstance().chatroomManager().asyncFetchChatRoomFromServer(chatRoomId, object : EMValueCallBack<EMChatRoom> {
            override fun onSuccess(value: EMChatRoom?) {
                value?.isAllMemberMuted?.let {
                    if (it) {
                        ThreadManager.instance.runOnMainThread {
                            for (listener in listeners) {
                                listener.fetchChatRoomMutedStatus(it)
                            }
                        }
                    } else {
                        fetchChatRoomSingleMutedStatus(chatRoomId)
                    }
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
        ThreadManager.instance.runOnMainThread {
            val conversation = EMClient.getInstance().chatManager().getConversation(conversationId, EMConversation.EMConversationType.ChatRoom, true)
            conversation.removeMessage(messageId)
        }
    }

    fun addOperationListener(operationListener: EaseOperationListener) {
        listeners.add(operationListener)
    }

    fun removeOperationListener(operationListener: EaseOperationListener) {
        listeners.remove(operationListener)
    }

}