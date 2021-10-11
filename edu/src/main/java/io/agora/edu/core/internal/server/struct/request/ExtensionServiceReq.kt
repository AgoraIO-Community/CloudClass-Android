package io.agora.edu.core.internal.server.struct.request

data class RoomFlexPropsReq(
        val properties: MutableMap<String, String>,
        val cause: MutableMap<String, String>?
)

data class UserFlexPropsReq(
        val properties: MutableMap<String, String>,
        val cause: MutableMap<String, String>?
)