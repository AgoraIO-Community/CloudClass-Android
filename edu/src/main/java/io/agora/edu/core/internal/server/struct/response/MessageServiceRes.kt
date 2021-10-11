package io.agora.edu.core.internal.server.struct.response

class SendChatRes(
        val code: Int,
        val msg: String,
        val data: SendChatResData)

class SendChatResData(
        val messageId: Int,
        sensitiveWords: MutableList<String>)

class ConversationRes(
        val code: Int,
        val msg: String,
        val data: ConversationResData)

class ConversationResData(
        val peerMessageId: String)

data class ChatRecordRes(
        val total: Int,
        val list: MutableList<ChatRecordItem>,
        val nextId: Int?,
        val count: Int
)

data class ChatRecordItem(
        val messageId: Int,
        val message: String,
        val sensitiveWords: List<String>? = listOf(),
        val type: Int,
        val fromUser: ChatFromUser,
        val sendTime: Long
)

data class ChatFromUser(
        val role: String,
        val userName: String,
        val userUuid: String
)

data class ConversationRecordRes(
        val total: Int,
        val list: MutableList<ConversationRecordItem>,
        val nextId: String?,
        val count: Int?
)

data class ConversationRecordItem(
        val peerMessageId: String?,
        val message: String,
        val sensitiveWords: List<String>? = listOf(),
        val fromUserUuid: String,
        val type: Int,
        val fromUser: ChatFromUser,
        val sendTime: Long)

data class ChatTranslateRes(
        val translation: String?)