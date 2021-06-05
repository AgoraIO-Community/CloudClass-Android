package io.agora.education.impl.cmd.bean

import io.agora.education.api.user.data.EduBaseUserInfo

open class CMDRoomPropertyRes(
        val action: Int,
        val changeProperties: MutableMap<String, Any>,
        val cause: MutableMap<String, Any>?,
        val operator: EduBaseUserInfo?
) {
}

enum class PropertyChangeType(val value: Int) {
    Upsert(1),
    Delete(2);
}