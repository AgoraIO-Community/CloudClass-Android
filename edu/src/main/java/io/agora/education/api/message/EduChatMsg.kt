package io.agora.education.api.message

open class EduChatMsg(
        fromUser: EduFromUserInfo,
        message: String,
        timestamp: Long,
        val type: Int
) : EduMsg(fromUser, message, timestamp)

enum class EduChatMsgType(var value: Int) {
    Text(1)
}
