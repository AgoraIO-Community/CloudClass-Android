package io.agora.education.impl.room.data.request

class EduUpsertRoomPropertyReq(
        val properties: MutableMap<String, Any>,
        val cause: MutableMap<String, String>
) {
}

class EduRemoveRoomPropertyReq(
        val properties: MutableList<String>,
        val cause: MutableMap<String, String>
) {
}