package io.agora.education.impl.cmd.bean

open class CMDRoomPropertyRes(
        val action: Int,
        val changeProperties: MutableMap<String, Any>,
        val cause: MutableMap<String, Any>?
) {
}

enum class PropertyChangeType(val value: Int) {
    Upsert(1),
    Delete(2);
}