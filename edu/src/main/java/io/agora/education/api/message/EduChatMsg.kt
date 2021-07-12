package io.agora.education.api.message

open class EduChatMsg(
        fromUser: EduFromUserInfo,
        message: String,
        timestamp: Long,
        messageId: Int,
        val type: Int
) : EduMsg(fromUser, message, timestamp, messageId)

enum class EduChatMsgType(var value: Int) {
    Text(1)
}

enum class EduChatMsgFromRoom(
        val roomUuid: String,
        val roomName: String
)

open class EduPeerChatMsg(
        val fromUser: EduFromUserInfo,
        val fromRoom: EduChatMsgFromRoom?,
        val message: String,
        val timestamp: Long,
        val peerMessageId: String,
        val type: Int
)
