package com.hyphenate.easeim.modules.repositories

import android.text.TextUtils
import android.widget.Toast
import com.hyphenate.easeim.modules.constant.EaseConstant
import com.hyphenate.easeim.modules.manager.ThreadManager
import com.hyphenate.easeim.modules.view.`interface`.EaseOperationListener
import com.hyphenate.easeim.modules.view.ui.widget.ChatViewPager
import io.agora.CallBack
import io.agora.ValueCallBack
import io.agora.agoraeduuikit.R
import io.agora.chat.*
import io.agora.util.EMLog
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

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
    var appId = ""
    var baseUrl = ""

    /**
     * 加载本地消息
     */
    fun loadMessages() {
        if (isInit) {
            val conversation = ChatClient.getInstance().chatManager()
                    .getConversation(chatRoomId, Conversation.ConversationType.ChatRoom, true)
            val msgList = conversation?.allMessages
            val norMsgList = mutableListOf<ChatMessage>()
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
    private fun fetchChatRoomSingleMutedStatus() {
        val urlPath = "edu/apps/$appId/v2/rooms/$roomUuid/users/$userUuid"
        val client = OkHttpClient.Builder().build()
        val request = Request.Builder()
            .url(baseUrl + urlPath)
            .header("Content-Type", "application/json")
            .get()
            .build()
        val call = client.newCall(request)
        call.enqueue(object : Callback{
            override fun onFailure(call: Call, e: IOException) {
                EMLog.e(TAG, "fetchUserMuteState failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val repBody = response.body?.string()
                val code = response.code
                EMLog.e(TAG, "fetchUserMuteState: $repBody")
                if (code == 200 && !repBody.isNullOrEmpty()) {
                    try {
                        val json = JSONObject(repBody)
                        val state = json.optString("msg")
                        if(TextUtils.equals("Success", state)){
                            val data = json.optJSONObject("data")
                            val userProperties = data?.optJSONObject("userProperties")
                            val flexProps = userProperties?.optJSONObject("flexProps")
                            val mute = flexProps?.optInt("mute")
                            singleMuted = mute == 1
                            ThreadManager.instance.runOnMainThread {
                                for (listener in listeners) {
                                    if (allMuted || singleMuted)
                                        listener.fetchChatRoomMutedStatus(true)
                                    else
                                        listener.fetchChatRoomMutedStatus(false)
                                }
                            }
                        } else {
                            EMLog.e(TAG, "fetchUserMuteState failed: $repBody")
                        }
                    } catch (e: JSONException) {
                        EMLog.e(TAG, "fetchUserMuteState parse failed: $repBody")
                    }
                } else {
                    EMLog.e(TAG, "fetchUserMuteState failed: ${response.message}")
                }
            }
        })
    }

    /**
     * 获取聊天室禁言状态
     */
    @Synchronized
    fun fetchChatRoomMutedStatus() {
        if(role == EaseConstant.ROLE_STUDENT){
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

    /**
     * 设置用户属性用于保存禁言状态
     */
    fun setUserProperties(mute:Boolean){
        val urlPath = "edu/apps/$appId/v2/rooms/$roomUuid/users/properties/batch"
        val json = JSONObject()
        val array = JSONArray()
        val user = JSONObject()
        val properties = JSONObject()
        val cause = JSONObject()
        cause.put("mute", "mute")
        if(mute){
            properties.put("mute", 1)
        } else {
            properties.put("mute", 0)
        }
        user.put("userUuid", userUuid)
        user.put("properties", properties)
        user.put("cause", cause)
        array.put(user)
        json.put("users", array)
        val body =
            json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val client = OkHttpClient.Builder().build()
        val request = Request.Builder()
            .url(baseUrl + urlPath)
            .header("Content-Type", "application/json")
            .put(body)
            .build()
        val call = client.newCall(request)
        call.enqueue(object : Callback{
            override fun onFailure(call: Call, e: IOException) {
                EMLog.e(TAG, "setUserProperties failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val repBody = response.body?.string()
                val code = response.code
                EMLog.e(TAG, "setUserProperties: $repBody")
            }
        })
    }

}