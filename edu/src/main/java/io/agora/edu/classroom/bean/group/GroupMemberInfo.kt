package io.agora.edu.classroom.bean.group


class GroupMemberInfo(
        var uuid: String,
        val userName: String,
        val avatar: String,
        var reward: Int,
        var enableAudio: Boolean,
        var enableVideo: Boolean,
        var streamUuid: String?,
        var streamName: String?
) {

    var online: Boolean = false
    var onStage: Boolean = false

    fun online() {
        online = true
    }

    fun offLine() {
        online = false
    }

    fun onStage() {
        onStage = true
    }

    fun offStage() {
        onStage = false
    }

}