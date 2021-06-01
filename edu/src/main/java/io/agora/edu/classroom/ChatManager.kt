package io.agora.edu.classroom

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import io.agora.edu.R
import io.agora.edu.common.api.Chat
import io.agora.edu.common.bean.request.ChatTranslateReq
import io.agora.edu.common.bean.response.ChatRecordItem
import io.agora.edu.common.bean.response.ChatTranslateRes
import io.agora.edu.common.bean.response.ConversationRecordItem
import io.agora.edu.common.impl.ChatImpl
import io.agora.edu.launch.AgoraEduLaunchConfig
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.base.EduError.Companion.internalError
import io.agora.education.api.message.EduChatMsg
import io.agora.education.api.message.EduPeerChatMsg
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.EduRoomChangeType
import io.agora.education.api.room.data.EduRoomStatus
import io.agora.education.api.user.EduUser
import io.agora.education.api.user.data.EduBaseUserInfo
import io.agora.education.api.user.data.EduUserInfo
import io.agora.educontext.*
import io.agora.educontext.context.ChatContext
import java.util.*

class ChatManager(
        context: Context,
        eduRoom: EduRoom?,
        private var chatContext: ChatContext?,
        launchConfig: AgoraEduLaunchConfig,
        eduUser: EduUser) : BaseManager(context, launchConfig, eduRoom, eduUser) {

    override var tag = "ChatManager"
    private val chat: Chat = ChatImpl(launchConfig.appId, launchConfig.roomUuid)
    private var chatAllowed = true

    fun initChat() {
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

        eduRoom?.getLocalUser(object : EduCallback<EduUser> {
            override fun onSuccess(res: EduUser?) {
                res?.userInfo?.let { info ->
                    val properties = info.userProperties
                    (properties["mute"] as? Map<String, Any?>)?.let { data ->
                        (data["muteChat"] as? Double)?.let { double ->
                            val mute = double.toInt() == 1
                            chatContext?.getHandlers()?.forEach { h ->
                                h.onChatAllowed(!mute,
                                        EduContextUserInfo(
                                                launchConfig.userUuid,
                                                launchConfig.userName,
                                                EduContextUserRole.fromValue(launchConfig.roleType),
                                                getAgoraCustomProps(launchConfig.userUuid)),
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

    fun receiveRemoteChatMessage(chatMsg: EduChatMsg) {
        val item = EduContextChatItem(
                chatMsg.fromUser.userName ?: "",
                chatMsg.fromUser.userUuid ?: "",
                chatMsg.message,
                "${chatMsg.messageId}",
                EduContextChatItemType.Text,
                EduContextChatSource.Remote,
                EduContextChatState.Success,
                chatMsg.timestamp)

        chatContext?.getHandlers()?.forEach {
            it.onReceiveMessage(item)
        }
    }

    fun receiveConversationMessage(msg: EduPeerChatMsg) {
        val item = EduContextChatItem(
                msg.fromUser.userName ?: "",
                msg.fromUser.userUuid ?: "",
                msg.message,
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
        chat.roomChat(launchConfig.userUuid, message, object : EduCallback<Int?> {
            override fun onSuccess(res: Int?) {
                if (res != null) {
                    callback?.onSuccess(EduContextChatItemSendResult(launchConfig.userUuid, "$res", timestamp))
                } else {
                    callback?.onFailure(EduContextErrors.DefaultError)
                }
            }

            override fun onFailure(error: EduError) {
                Log.e(tag, "onSendLocalMessage fail: ${error.type}, ${error.msg}")
                callback?.onFailure(EduContextError(error.type, error.msg))
            }
        })
    }

    fun conversation(message: String, timestamp: Long,
                     callback: EduContextCallback<EduContextChatItemSendResult>? = null) {
        chat.conversation(launchConfig.userUuid, message, object : EduCallback<String> {
            override fun onSuccess(res: String?) {
                if (res != null) {
                    callback?.onSuccess(EduContextChatItemSendResult(launchConfig.userUuid, res, timestamp))
                } else {
                    callback?.onFailure(EduContextErrors.DefaultError)
                }
            }

            override fun onFailure(error: EduError) {
                Log.e(tag, "onSendLocalMessage fail: ${error.type}, ${error.msg}")
                callback?.onFailure(EduContextError(error.type, error.msg))
            }
        })
    }

    fun pullChatRecords(nextId: String?, count: Int, reverse: Boolean,
                        callback: EduContextCallback<List<EduContextChatItem>>? = null) {
        chat.pullRoomChatRecords(nextId, count, reverse, object : EduCallback<MutableList<ChatRecordItem>> {
            override fun onSuccess(res: MutableList<ChatRecordItem>?) {
                if (res != null) {
                    Log.i(tag, "pullChatRecords result->" + Gson().toJson(res))
                    val result: MutableList<EduContextChatItem> = ArrayList()
                    res.forEach { item -> result.add(toEduContextChatItem(item)) }
                    callback?.onSuccess(result.toList())
                } else {
                    Log.e(tag, "pullChatRecords failed!")
                    callback?.onFailure(EduContextErrors.DefaultError)
                }
            }

            override fun onFailure(error: EduError) {
                Log.e(tag, "pullChatRecords failed->" + Gson().toJson(error))
                callback?.onFailure(EduContextError(error.type, error.msg))
            }
        })
    }

    private fun toEduContextChatItem(item: ChatRecordItem): EduContextChatItem {
        return EduContextChatItem(
                name = item.fromUser.userName,
                uid = item.fromUser.userUuid,
                message = item.message,
                messageId = "${item.messageId}",
                type = EduContextChatItemType.Text,
                source = if (item.fromUser.userUuid == launchConfig.userUuid)
                    EduContextChatSource.Local
                else EduContextChatSource.Remote,
                timestamp = item.sendTime)
    }

    fun pullConversationRecords(nextId: String?, reverse: Boolean,
                                callback: EduContextCallback<List<EduContextChatItem>>?) {
        chat.pullConversationRecords(nextId, launchConfig.userUuid, reverse,
                object : EduCallback<List<ConversationRecordItem>> {
                    override fun onSuccess(res: List<ConversationRecordItem>?) {
                        if (res != null) {
                            Log.i(tag, "pullPeerChatRecords result->" + Gson().toJson(res))
                            val result: MutableList<EduContextChatItem> = ArrayList()
                            res.forEach { item -> result.add(toEduContextChatItem(item)) }
                            callback?.onSuccess(result.toList())
                        } else {
                            Log.e(tag, "pullPeerChatRecords failed!")
                            callback?.onFailure(EduContextErrors.DefaultError)
                        }
                    }

                    override fun onFailure(error: EduError) {
                        Log.e(tag, "pullPeerChatRecords failed->" + Gson().toJson(error))
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
                source = if (item.fromUser.userUuid == launchConfig.userUuid)
                    EduContextChatSource.Local
                else EduContextChatSource.Remote,
                timestamp = item.sendTime)
    }

    fun translate(msg: String?, to: String?, callback: EduCallback<String?>) {
        val req = ChatTranslateReq(msg!!, to!!)
        chat.translate(req, object : EduCallback<ChatTranslateRes?> {
            override fun onSuccess(res: ChatTranslateRes?) {
                if (res != null) {
                    Log.i(tag, "translate result->" + res.translation)
                    callback.onSuccess(res.translation)
                } else {
                    Log.e(tag, "translate failed!")
                    callback.onFailure(internalError("no translate result found"))
                }
            }

            override fun onFailure(error: EduError) {
                Log.e(tag, "translate failed->" + Gson().toJson(error))
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
                            getAgoraCustomProps(userInfo.userUuid))

                    val teacher = if (operator != null) {
                        EduContextUserInfo(
                                operator.userUuid,
                                operator.userName,
                                EduContextUserRole.Teacher,
                                getAgoraCustomProps(operator.userUuid))
                    } else null

                    chatContext?.getHandlers()?.forEach { h ->
                        h.onChatAllowed(!mute, stu, teacher, userInfo.userUuid == launchConfig.userUuid)
                    }
                }
            }
        }
    }
}