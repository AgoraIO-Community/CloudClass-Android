package io.agora.agoraeducore.core.internal.framework.data

import io.agora.agoraeducore.core.internal.framework.EduBaseUserInfo

open class EduMessage(
        val fromUser: EduBaseUserInfo,
        val message: String,
        val sensitiveWords: List<String>? = listOf(),
        val timestamp: Long,
        val messageId: Int)

open class EduChatMessage(
        fromUser: EduBaseUserInfo,
        message: String,
        sensitiveWords: List<String>? = listOf(),
        timestamp: Long,
        messageId: Int,
        val type: Int
) : EduMessage(fromUser, message, sensitiveWords, timestamp, messageId)

enum class EduChatMessageType(var value: Int) {
    Text(1)
}

enum class EduRoomBaseInfo(
        val roomUuid: String,
        val roomName: String)

/**
 * Peer chat message has a different message id
 * type of String, so it cannot be extended from
 * Basic edu chat message
 */
class EduPeerChatMessage(
        val fromUser: EduBaseUserInfo,
        val fromRoom: EduRoomBaseInfo?,
        val message: String,
        var sensitiveWords: List<String>? = listOf(),
        val timestamp: Long,
        val peerMessageId: String,
        val type: Int)

enum class AgoraActionType(val value: Int) {
    AgoraActionTypeApply(1),
    AgoraActionTypeInvitation(2),
    AgoraActionTypeAccept(3),
    AgoraActionTypeReject(4),
    AgoraActionTypeCancel(5)
}

class EduActionMessage(
        private val action: Int,
        val fromUser: AgoraActionFromUser,
        val fromRoom: AgoraActionFromRoom) {

    fun getCurAction(): AgoraActionType {
        return when (action) {
            AgoraActionType.AgoraActionTypeApply.value -> {
                AgoraActionType.AgoraActionTypeApply
            }
            AgoraActionType.AgoraActionTypeInvitation.value -> {
                AgoraActionType.AgoraActionTypeInvitation
            }
            AgoraActionType.AgoraActionTypeAccept.value -> {
                AgoraActionType.AgoraActionTypeAccept
            }
            AgoraActionType.AgoraActionTypeReject.value -> {
                AgoraActionType.AgoraActionTypeReject
            }
            AgoraActionType.AgoraActionTypeCancel.value -> {
                AgoraActionType.AgoraActionTypeCancel
            }
            else -> AgoraActionType.AgoraActionTypeCancel
        }
    }
}

class AgoraActionFromUser(
        val uuid: String,
        val name: String,
        val role: String
)

class AgoraActionFromRoom(
        val uuid: String,
        val name: String
)