package io.agora.edu.common.bean.flexpropes

data class RoomFlexPropsReq(
        val properties: MutableMap<String, String>,
        val cause: MutableMap<String, String>?
) {
}

data class UserFlexPropsReq(
        val properties: MutableMap<String, String>,
        val cause: MutableMap<String, String>?
) {
}