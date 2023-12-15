package io.agora.online.impl.chat.rtm

import io.agora.agoraeducore.core.internal.base.http.AppRetrofitManager
import io.agora.agoraeducore.core.internal.framework.data.EduChatMessageType
import io.agora.agoraeducore.core.internal.server.requests.service.ChatService
import io.agora.agoraeducore.core.internal.server.struct.request.EduRoomChatMsgReq
import io.agora.agoraeducore.core.internal.server.struct.response.ChatRecordItem
import io.agora.agoraeducore.core.internal.server.struct.response.ChatRecordRes
import io.agora.agoraeducore.core.internal.server.struct.response.DataResponseBody
import io.agora.agoraeducore.core.internal.server.struct.response.SendChatRes
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatProxy(private val appId: String,
                private val roomId: String, 
                private val listener: AgoraChatProxyListener? = null) {

    companion object {
        const val DEFAULT_PULL_COUNT = 50
    }

    private val service = AppRetrofitManager.getService(ChatService::class.java)

    private val chatCache = ChatCache()

    fun sendLocalMessage(userId: String, userName: String,
                         userRole: Int, message: String, timestamp: Long) {
        chatCache.pushSentLocalMessage(userId, userName, userRole, message, timestamp)
        listener?.onLocalMessageSendBeforeResponse()
        sendLocalMessageToServer(userId, message, timestamp)
    }

    fun retrySendMessage(messageId: String, userId: String, timestamp: Long) {
        chatCache.pushRetryLocalMessage(messageId)?.let { item ->
            listener?.onLocalMessageSendBeforeResponse()
            sendLocalMessageToServer(userId, item.message, timestamp)
        }
    }

    private fun sendLocalMessageToServer(userId: String, message: String, timestamp: Long) {
        service.roomChat(appId, roomId, userId,
            EduRoomChatMsgReq(message, EduChatMessageType.Text.value))
            .enqueue(object : Callback<SendChatRes> {
                override fun onResponse(call: Call<SendChatRes>,
                                        response: Response<SendChatRes>) {
                    response.body()?.let { res ->
                        if (res.code == 0) {
                            callbackSendResult(res.data.messageId.toString(), timestamp, true)
                        } else {
                            callbackSendResult(res.data.messageId.toString(), timestamp, false)
                        }
                    } ?: Runnable {
                        callbackSendResult("", timestamp, false)
                    }
                    listener?.onLocalMessageSendResult()
                }

                override fun onFailure(call: Call<SendChatRes>, t: Throwable) {
                    callbackSendResult("", timestamp, false)
                }

                private fun callbackSendResult(messageId: String, timestamp: Long, success: Boolean) {
                    chatCache.updateLocalMessageResult(messageId, timestamp, success)
                    listener?.onLocalMessageSendResult()
                }
            })
    }

    fun addNewMessageToList(item: AgoraChatItem) {
        chatCache.pushMessageItem(item)
    }

    fun pullMessageRecord(localUserId: String, count: Int?, nextId: String?,
                          reverse: Boolean, callback: ChatProxyCallback<Int>? = null) {
        service.pullChatRecords(appId, roomId, count ?: DEFAULT_PULL_COUNT, nextId, if (reverse) 1 else 0)
                .enqueue(object : Callback<DataResponseBody<ChatRecordRes>> {
                    override fun onResponse(call: Call<DataResponseBody<ChatRecordRes>>,
                                            response: Response<DataResponseBody<ChatRecordRes>>) {
                        response.body()?.let {
                            if (it.code == 0) {
                                callbackPullRecordResult(it.data, localUserId, true)
                            } else {
                                callbackPullRecordResult(null, localUserId,false)
                            }
                        } ?: Runnable {
                            callbackPullRecordResult(null, localUserId,false)
                        }
                    }

                    override fun onFailure(call: Call<DataResponseBody<ChatRecordRes>>, t: Throwable) {
                        callbackPullRecordResult(null, localUserId,false)
                    }

                    private fun callbackPullRecordResult(res: ChatRecordRes?,
                                                         localUserId: String, success: Boolean) {
                        if (success) {
                            res?.list?.forEach { item ->
                                val agoraChatItem = toAgoraChatItem(item)
                                agoraChatItem.source =
                                    if (agoraChatItem.uid == localUserId)
                                        AgoraUIChatSource.Local
                                    else AgoraUIChatSource.Remote
                                chatCache.pushMessageItem(agoraChatItem)
                            }

                            callback?.onSuccess("", localUserId, 0, res?.count ?: 0)
                        }

                        listener?.onMessageRecordPulled()
                    }
        })
    }

    private fun toAgoraChatItem(record: ChatRecordItem): AgoraChatItem {
        val item = AgoraChatItem()
        item.name = record.fromUser.userName
        item.uid = record.fromUser.userUuid
        item.role = toAgoraChatUserRole(record.fromUser.role).value
        item.message = record.message
        item.messageId = record.messageId.toString()
        item.timestamp = record.sendTime
        item.type = toAgoraChatType(record.type)
        return item
    }

    private fun toAgoraChatUserRole(role: String): AgoraChatUserRole {
        return when (role) {
            "host" -> AgoraChatUserRole.Teacher
            else -> AgoraChatUserRole.Student
        }
    }

    private fun toAgoraChatType(type: Int): AgoraChatItemType {
        return when (type) {
            AgoraChatItemType.Text.ordinal -> AgoraChatItemType.Text
            else -> AgoraChatItemType.Unknown
        }
    }

    fun getMessageList(): List<AgoraChatItem> {
        return chatCache.getMessageList()
    }
}

interface AgoraChatProxyListener {
    fun onLocalMessageSendBeforeResponse()

    fun onLocalMessageSendResult()

    fun onMessageRecordPulled()
}

interface ChatProxyCallback<T> {
    fun onSuccess(id: String, userId: String, elapsed: Long, data: T?)

    fun onFailure(reason: Int, message: String)
}

internal class ChatCache {
    companion object {
        const val CACHE_MAX = 200
    }

    private val messageList = arrayListOf<AgoraChatItem>()
    private val messageMap = mutableMapOf<String, AgoraChatItem>()

    // When local message has just sent, the message id is fake.
    // When the server returns a real server message id,
    // the hat item's message id will be replaced
    private val sendingMap = mutableMapOf<Long, AgoraChatItem>()

    @Synchronized
    fun pushSentLocalMessage(userId: String, userName: String,
                             userRole: Int, message: String, timestamp: Long) {
        val item = AgoraChatItem(
            name = userName,
            uid = userId,
            role = userRole,
            message = message,
            messageId = timestamp.toString(),
            timestamp = timestamp,
            source = AgoraUIChatSource.Local,
            state = AgoraUIChatState.InProgress)
        messageList.add(item)
        messageMap[item.messageId] = item
        sendingMap[item.timestamp] = item
        ensureCacheMax()
    }

    @Synchronized
    fun pushRetryLocalMessage(messageId: String): AgoraChatItem? {
        return messageMap[messageId]?.let { item ->
            item.state = AgoraUIChatState.InProgress
            sendingMap[item.timestamp] = item
            item
        }
    }

    @Synchronized
    fun updateLocalMessageResult(messageId: String, timestamp: Long, success: Boolean) {
        // Once the server returns a result, we need to
        // remove this item from the temporary maps
        handleLocalMessageResult(sendingMap, messageId, timestamp, success)
    }

    private fun handleLocalMessageResult(map: MutableMap<Long, AgoraChatItem>,
                                         messageId: String, timestamp: Long, success: Boolean) {
        map.remove(timestamp)?.let { item ->
            if (success) {
                messageMap.remove(item.messageId)
                item.messageId = messageId
                item.state = AgoraUIChatState.Default
                messageMap[item.messageId] = item
            } else {
                item.state = AgoraUIChatState.Fail
            }
        }
    }

    @Synchronized
    fun pushMessageItem(record: AgoraChatItem) {
        messageList.add(record)
        messageMap[record.messageId] = record
        ensureCacheMax()
    }

    @Synchronized
    fun getMessageList(): List<AgoraChatItem> {
        return messageList
    }

    @Synchronized
    private fun ensureCacheMax() {
        if (messageList.size > CACHE_MAX) {
            val deleteCount = messageList.size - CACHE_MAX
            for (i in 0 until deleteCount) {
                val item = messageList.removeFirst()
                messageMap.remove(item.messageId)
                sendingMap.remove(item.timestamp)
            }
        }
    }
}