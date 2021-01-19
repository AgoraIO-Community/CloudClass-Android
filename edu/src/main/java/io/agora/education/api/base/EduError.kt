package io.agora.education.api.base

class EduError(
        val type: Int,
        val msg: String
) {
    companion object {
        fun noError(): EduError {
            return EduError(-1, "")
        }

        fun customMsgError(msg: String?): EduError {
            return EduError(1, msg ?: "")
        }

        fun parameterError(parameterName: String): EduError {
            return EduError(1, "Parameter $parameterName is invalid")
        }

        fun notJoinedRoomError(): EduError {
            return EduError(1, "You haven't joined the room")
        }

        fun internalError(msg: String?): EduError {
            return EduError(2, msg ?: "")
        }

        fun communicationError(code: Int, msg: String?): EduError {
            return EduError(101, "Communication error->$code,reason->$msg")
        }

        fun mediaError(code: Int, msg: String?): EduError {
            return EduError(201, "Media error->$code,reason->$msg")
        }

        fun httpError(code: Int, msg: String?): EduError {
            return EduError(301, "Http error->$code,reason->$msg")
        }
    }
}