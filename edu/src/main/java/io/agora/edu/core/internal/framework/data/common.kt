package io.agora.edu.core.internal.framework.data

interface EduCallback<T>{
    fun onSuccess(res: T?)

    fun onFailure(error: EduError)
}

class EduError(
        val type: Int,
        val msg: String) {
    var httpError: Int? = 0

    constructor(type: Int, msg: String, httpError: Int?) : this(type, msg) {
        this.httpError = httpError
    }

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