package io.agora.edu.common.bean.sidechat

class SideChatData(
        val actionType: Int
)

class SideChatConfig(
        val users: MutableList<SideChatUser>,
        val streams: MutableList<SideChatStream>
) {
    companion object {
        const val KeyPrefix = "streamGroups"
    }
}

data class SideChatUser(
        val userUuid: String) {
}

data class SideChatStream(
        val streamUuid: String,
        val audio: Int,
        val video: Int) {
}
