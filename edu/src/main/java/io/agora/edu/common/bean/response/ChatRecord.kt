package io.agora.edu.common.bean.response

data class ChatRecordRes(
        val total: Int,
        val list: MutableList<ChatRecordItem>,
        val nextId: Int?,
        val count: Int
)

data class ChatRecordItem(
        val messageId: Int,
        val message: String,
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
        val fromUserUuid: String,
        val type: Int,
        val fromUser: ChatFromUser,
        val sendTime: Long)