package io.agora.educontext

data class EduContextError(
        val code: Int,
        val msg: String)

object EduContextErrors {
    val DefaultError = EduContextError(-1, "default error")
}
