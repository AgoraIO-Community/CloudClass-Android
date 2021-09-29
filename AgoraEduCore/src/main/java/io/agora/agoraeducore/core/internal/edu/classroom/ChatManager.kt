package io.agora.agoraeducore.core.internal.edu.classroom

import android.content.Context
import com.google.gson.Gson
import io.agora.agoraeducontext.*
import io.agora.agoraeducore.core.AgoraEduCoreConfig
import io.agora.agoraeducore.core.internal.edu.common.api.Chat
import io.agora.agoraeducore.core.internal.edu.common.impl.ChatImpl
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.framework.data.EduCallback
import io.agora.agoraeducore.core.internal.framework.data.EduError
import io.agora.agoraeducore.core.internal.framework.data.EduError.Companion.internalError
import io.agora.agoraeducore.core.internal.framework.EduRoom
import io.agora.agoraeducore.core.internal.education.api.room.data.EduRoomChangeType
import io.agora.agoraeducore.core.internal.education.api.room.data.EduRoomStatus
import io.agora.agoraeducore.core.internal.education.impl.Constants.Companion.AgoraLog
import io.agora.agoraeducore.core.internal.framework.EduLocalUser
import io.agora.agoraeducore.core.internal.framework.EduBaseUserInfo
import io.agora.agoraeducore.core.internal.framework.EduUserInfo
import io.agora.agoraeducore.core.internal.framework.data.EduChatMessage
import io.agora.agoraeducore.core.internal.framework.data.EduPeerChatMessage
import io.agora.agoraeducore.core.internal.server.struct.request.ChatTranslateReq
import io.agora.agoraeducore.core.internal.server.struct.response.ChatRecordItem
import io.agora.agoraeducore.core.internal.server.struct.response.ChatTranslateRes
import io.agora.agoraeducore.core.internal.server.struct.response.ConversationRecordItem
import java.util.*
import io.agora.agoraeducore.R

internal class ChatManager(
        context: Context,
        eduRoom: EduRoom?,
        eduContextPool: EduContextPool,
        config: AgoraEduCoreConfig,
        eduUser: EduLocalUser) : BaseManager(context, config, eduRoom, eduUser, eduContextPool) {

    override var tag = "ChatManager"
    private val chat: Chat = ChatImpl(config.appId, config.roomUuid)
    private var chatAllowed = true
    private val chatContext = eduContextPool.chatContext()

    fun initChat() {
        AgoraLog.i("$tag:initChat")
        notifyMuteChatStatus(EduRoomChangeType.AllStudentsChat)
        pullChatRecords(nextId = null, count = 50, reverse = true,
                callback = object : EduContextCallback<List<EduContextChatItem>> {
                    override fun onSuccess(target: List<EduContextChatItem>?) {
                        target?.let {
                            chatContext?.getHandlers()?.forEach { h ->
                                h.onReceiveChatHistory(target)
                            }
                        }
                    }

                    override fun onFailure(error: EduContextError?) {

                    }
                })

        pullConversationRecords(nextId = null, reverse = true,
                callback = object : EduContextCallback<List<EduContextChatItem>> {
                    override fun onSuccess(target: List<EduContextChatItem>?) {
                        target?.let { list ->
                            chatContext?.getHandlers()?.forEach { h ->
                                h.onReceiveConversationHistory(list)
                            }
                        }
                    }

                    override fun onFailure(error: EduContextError?) {

                    }
                })

        eduRoom?.getLocalUser(object : EduCallback<EduLocalUser> {
            override fun onSuccess(res: EduLocalUser?) {
                res?.userInfo?.let { info ->
                    val properties = info.userProperties
                    (properties["mute"] as? Map<String, Any?>)?.let { data ->
                        (data["muteChat"] as? Double)?.let { double ->
                            val mute = double.toInt() == 1
                            chatContext?.getHandlers()?.forEach { h ->
                                h.onChatAllowed(!mute,
                                        EduContextUserInfo(
                                                config.userUuid,
                                                config.userName,
                                                EduContextUserRole.fromValue(config.roleType),
                                                getUserFlexProps(config.userUuid)),
                                        null, true)
                            }
                        }
                    }
                }
            }

            override fun onFailure(error: EduError) {

            }
        })
    }

    fun receiveRemoteChatMessage(chatMsg: EduChatMessage) {
        val item = EduContextChatItem(
                chatMsg.fromUser.userName ?: "",
                chatMsg.fromUser.userUuid ?: "",
                chatMsg.fromUser.role?.value ?: EduContextUserRole.Student.value,
                chatMsg.message,
                chatMsg.sensitiveWords,
                "${chatMsg.messageId}",
                EduContextChatItemType.Text,
                EduContextChatSource.Remote,
                EduContextChatState.Success,
                chatMsg.timestamp)

        chatContext?.getHandlers()?.forEach {
            it.onReceiveMessage(item)
        }
    }

    fun receiveConversationMessage(msg: EduPeerChatMessage) {
        val item = EduContextChatItem(
                msg.fromUser.userName ?: "",
                msg.fromUser.userUuid ?: "",
                msg.fromUser.role?.value ?: EduContextUserRole.Student.value,
                msg.message,
                msg.sensitiveWords,
                msg.peerMessageId,
                EduContextChatItemType.Text,
                EduContextChatSource.Remote,
                EduContextChatState.Success,
                msg.timestamp)

        chatContext?.getHandlers()?.forEach { h ->
            h.onReceiveConversationMessage(item)
        }
    }

    fun sendRoomChat(message: String, timestamp: Long, callback: EduContextCallback<EduContextChatItemSendResult>? = null) {
        chat.roomChat(config.userUuid, message, object : EduCallback<Int?> {
            override fun onSuccess(res: Int?) {
                if (res != null) {
                    callback?.onSuccess(EduContextChatItemSendResult(config.userUuid, "$res", timestamp))
                } else {
                    callback?.onFailure(EduContextErrors.DefaultError)
                }
            }

            override fun onFailure(error: EduError) {
                AgoraLog.e("$tag:onSendLocalMessage fail: ${error.type}, ${error.msg}")
                callback?.onFailure(EduContextError(error.type, error.msg))
            }
        })
    }

    fun conversation(message: String, timestamp: Long,
                     callback: EduContextCallback<EduContextChatItemSendResult>? = null) {
        chat.conversation(config.userUuid, message, object : EduCallback<String> {
            override fun onSuccess(res: String?) {
                AgoraLog.i("$tag:conversation result->" + Gson().toJson(res))
                if (res != null) {
                    callback?.onSuccess(EduContextChatItemSendResult(config.userUuid, res, timestamp))
                } else {
                    callback?.onFailure(EduContextErrors.DefaultError)
                }
            }

            override fun onFailure(error: EduError) {
                AgoraLog.e("$tag:onSendLocalMessage fail: ${error.type}, ${error.msg}")
                callback?.onFailure(EduContextError(error.type, error.msg))
            }
        })
    }

    fun pullChatRecords(nextId: String?, count: Int, reverse: Boolean,
                        callback: EduContextCallback<List<EduContextChatItem>>? = null) {
        chat.pullRoomChatRecords(nextId, count, reverse, object : EduCallback<MutableList<ChatRecordItem>> {
            override fun onSuccess(res: MutableList<ChatRecordItem>?) {
                if (res != null) {
                    AgoraLog.i("$tag:pullChatRecords result->" + Gson().toJson(res))
                    val result: MutableList<EduContextChatItem> = ArrayList()
                    res.forEach { item -> result.add(toEduContextChatItem(item)) }
                    callback?.onSuccess(result.toList())
                } else {
                    AgoraLog.e("$tag:pullChatRecords failed!")
                    callback?.onFailure(EduContextErrors.DefaultError)
                }
            }

            override fun onFailure(error: EduError) {
                AgoraLog.e("$tag:pullChatRecords failed->" + Gson().toJson(error))
                callback?.onFailure(EduContextError(error.type, error.msg))
            }
        })
    }

    private fun toEduContextChatItem(item: ChatRecordItem): EduContextChatItem {
        return EduContextChatItem(
                name = item.fromUser.userName,
                uid = item.fromUser.userUuid,
                role = item.fromUser.role.toInt(),
                message = item.message,
                sensitiveWords = item.sensitiveWords,
                messageId = "${item.messageId}",
                type = EduContextChatItemType.Text,
                source = if (item.fromUser.userUuid == config.userUuid)
                    EduContextChatSource.Local
                else EduContextChatSource.Remote,
                timestamp = item.sendTime)
    }

    fun pullConversationRecords(nextId: String?, reverse: Boolean,
                                callback: EduContextCallback<List<EduContextChatItem>>?) {
        chat.pullConversationRecords(nextId, config.userUuid, reverse,
                object : EduCallback<List<ConversationRecordItem>> {
                    override fun onSuccess(res: List<ConversationRecordItem>?) {
                        if (res != null) {
                            AgoraLog.i("$tag:pullPeerChatRecords result->" + Gson().toJson(res))
                            val result: MutableList<EduContextChatItem> = ArrayList()
                            res.forEach { item -> result.add(toEduContextChatItem(item)) }
                            callback?.onSuccess(result.toList())
                        } else {
                            AgoraLog.e("$tag:pullPeerChatRecords failed!")
                            callback?.onFailure(EduContextErrors.DefaultError)
                        }
                    }

                    override fun onFailure(error: EduError) {
                        AgoraLog.e("$tag:pullPeerChatRecords failed->" + Gson().toJson(error))
                        callback?.onFailure(EduContextError(error.type, error.msg))
                    }
                })
    }

    private fun toEduContextChatItem(item: ConversationRecordItem): EduContextChatItem {
        return EduContextChatItem(
                name = item.fromUser.userName,
                uid = item.fromUser.userUuid,
                message = item.message,
                messageId = item.peerMessageId ?: "",
                type = EduContextChatItemType.Text,
                source = if (item.fromUser.userUuid == config.userUuid)
                    EduContextChatSource.Local
                else EduContextChatSource.Remote,
                timestamp = item.sendTime)
    }

    fun translate(msg: String?, to: String?, callback: EduCallback<String?>) {
        val req = ChatTranslateReq(msg!!, to!!)
        chat.translate(req, object : EduCallback<ChatTranslateRes?> {
            override fun onSuccess(res: ChatTranslateRes?) {
                if (res != null) {
                    AgoraLog.e("$tag:translate result->" + res.translation)
                    callback.onSuccess(res.translation)
                } else {
                    AgoraLog.e("$tag:translate failed!")
                    callback.onFailure(internalError("no translate result found"))
                }
            }

            override fun onFailure(error: EduError) {
                AgoraLog.e("$tag:translate failed->" + Gson().toJson(error))
                callback.onFailure(error)
            }
        })
    }

    fun notifyMuteChatStatus(type: EduRoomChangeType) {
        if (type == EduRoomChangeType.AllStudentsChat) {
            eduRoom?.getRoomStatus(object : EduCallback<EduRoomStatus> {
                override fun onSuccess(res: EduRoomStatus?) {
                    res?.let { status ->
                        chatContext?.getHandlers()?.forEach { handler ->
                            handler.onChatAllowed(status.isStudentChatAllowed)
                            if (chatAllowed != status.isStudentChatAllowed) {
                                handler.onChatTips(context.getString(
                                        if (status.isStudentChatAllowed) R.string.chat_window_chat_enable
                                        else R.string.chat_window_chat_disable)
                                )
                            }
                        }
                        chatAllowed = status.isStudentChatAllowed
                    }
                }

                override fun onFailure(error: EduError) {

                }
            })
        }
    }

    fun notifyUserChatMuteStatus(userInfo: EduUserInfo, cause: MutableMap<String, Any>?, operator: EduBaseUserInfo?) {
        cause?.let {
            if (!it.containsKey("data")) {
                return
            }

            (it["data"] as? Map<String, Any?>)?.get("muteChat")?.let { data ->
                (data as? Double)?.let { status ->
                    val mute = status.toInt() == 1
                    val stu = EduContextUserInfo(
                            userInfo.userUuid,
                            userInfo.userName,
                            EduContextUserRole.fromValue(userInfo.role.value),
                            getUserFlexProps(userInfo.userUuid))

                    val teacher = if (operator != null) {
                        EduContextUserInfo(
                                operator.userUuid,
                                operator.userName,
                                EduContextUserRole.Teacher,
                                getUserFlexProps(operator.userUuid))
                    } else null

                    chatContext?.getHandlers()?.forEach { h ->
                        h.onChatAllowed(!mute, stu, teacher, userInfo.userUuid == config.userUuid)
                    }
                }
            }
        }
    }
}