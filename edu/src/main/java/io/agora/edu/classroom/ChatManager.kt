package io.agora.edu.classroom

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import io.agora.edu.R
import io.agora.edu.common.api.Chat
import io.agora.edu.common.bean.request.ChatTranslateReq
import io.agora.edu.common.bean.response.ChatRecordItem
import io.agora.edu.common.bean.response.ChatTranslateRes
import io.agora.edu.common.impl.ChatImpl
import io.agora.edu.launch.AgoraEduLaunchConfig
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.base.EduError.Companion.internalError
import io.agora.education.api.message.EduChatMsg
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.EduRoomChangeType
import io.agora.education.api.room.data.EduRoomStatus
import io.agora.educontext.*
import io.agora.educontext.context.ChatContext
import java.util.*

class ChatManager(
        private var context: Context,
        private var eduRoom: EduRoom?,
        private var chatContext: ChatContext?,
        private val launchConfig: AgoraEduLaunchConfig) {

    private val tag = "ChatManager"
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
    }

    fun receiveRemoteChatMessage(chatMsg: EduChatMsg) {
        val item = EduContextChatItem(
                chatMsg.fromUser.userName ?: "",
                chatMsg.fromUser.userUuid ?: "",
                chatMsg.fromUser.role?.value ?: EduContextUserRole.Student.value,
                chatMsg.message,
                chatMsg.messageId,
                EduContextChatItemType.Text,
                EduContextChatSource.Remote,
                EduContextChatState.Success,
                chatMsg.timestamp)

        chatContext?.getHandlers()?.forEach {
            it.onReceiveMessage(item)
        }
    }

    fun sendRoomChat(message: String, timestamp: Long, callback: EduContextCallback<EduContextChatItemSendResult>? = null) {
        chat.roomChat(launchConfig.userUuid, message, object : EduCallback<Int?> {
            override fun onSuccess(res: Int?) {
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
        chat.pullRecords(nextId, count, reverse, object : EduCallback<MutableList<ChatRecordItem>> {
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
                role = item.fromUser.role.toInt(),
                message = item.message,
                messageId = item.messageId,
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
}