package io.agora.education.api.message

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