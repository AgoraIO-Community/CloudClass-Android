package io.agora.online.impl.chat.rtm

data class AgoraChatItem(
    var name: String = "",
    var uid: String = "",
    var role: Int = AgoraChatUserRole.Student.value,
    var message: String = "",
    var messageId: String = "",
    var type: AgoraChatItemType = AgoraChatItemType.Text,
    var source: AgoraUIChatSource = AgoraUIChatSource.Remote,
    var state: AgoraUIChatState = AgoraUIChatState.Default,
    var timestamp: Long = 0) {
}

enum class AgoraChatUserRole(val value: Int) {
    Teacher(1), Student(2), Assistant(3)
}

enum class AgoraChatItemType {
    Text, Unknown;
}

enum class AgoraUIChatSource {
    Local, Remote, System;
}

enum class AgoraUIChatState {
    Default, InProgress, Success, Fail;
}