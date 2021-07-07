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
