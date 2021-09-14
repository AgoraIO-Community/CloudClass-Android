package io.agora.edu.core.internal.education.impl.cmd.bean

open class CMDRoomPropertyRes(
        val action: Int,
        val changeProperties: MutableMap<String, Any>,
        val cause: MutableMap<String, Any>?,
        val operator: OperatorUserInfo?
) {
}

data class OperatorUserInfo(
        val userUuid: String,
        val userName: String,
        val role: String
)

enum class PropertyChangeType(val value: Int) {
    Upsert(1),
    Delete(2);
}