package io.agora.uikit.impl.chat

import io.agora.educontext.*

data class AgoraUIChatItem(
        var name: String = "",
        var uid: String = "",
        var role: Int = EduContextUserRole.Student.value,
        var message: String = "",
        var messageId: Int = 0,
        var type: AgoraUIChatItemType = AgoraUIChatItemType.Text,
        var source: AgoraUIChatSource = AgoraUIChatSource.Remote,
        var state: AgoraUIChatState = AgoraUIChatState.Default,
        var timestamp: Long = 0) {

    fun copyValue(item: AgoraUIChatItem) {
        this.name = item.name
        this.uid = item.uid
        this.role = item.role
        this.message = message
        this.messageId = messageId
        this.type = type
        this.source = item.source
        this.state = item.state
        this.timestamp = item.timestamp
    }

    companion object {
        fun fromContextItem(item: EduContextChatItem): AgoraUIChatItem {
            return AgoraUIChatItem(
                    item.name,
                    item.uid,
                    item.role,
                    item.message,
                    item.messageId,
                    AgoraUIChatItemType.fromContextChatItemType(item.type),
                    AgoraUIChatSource.fromContextChatSource(item.source),
                    AgoraUIChatState.fromContextChatState(item.state),
                    item.timestamp)
        }
    }
}

enum class AgoraUIChatItemType {
    Text;

    companion object {
        fun fromContextChatItemType(type: EduContextChatItemType): AgoraUIChatItemType {
            return when (type) {
                EduContextChatItemType.Text -> Text
            }
        }
    }
}

enum class AgoraUIChatSource {
    Local, Remote, System;

    companion object {
        fun fromContextChatSource(source: EduContextChatSource): AgoraUIChatSource {
            return when (source) {
                EduContextChatSource.Local -> Local
                EduContextChatSource.Remote -> Remote
                EduContextChatSource.System -> System
            }
        }
    }
}

enum class AgoraUIChatState {
    Default, InProgress, Success, Fail;

    companion object {
        fun fromContextChatState(source: EduContextChatState): AgoraUIChatState {
            return when (source) {
                EduContextChatState.Default -> Default
                EduContextChatState.InProgress -> InProgress
                EduContextChatState.Success -> Success
                EduContextChatState.Fail -> Fail
            }
        }
    }
}