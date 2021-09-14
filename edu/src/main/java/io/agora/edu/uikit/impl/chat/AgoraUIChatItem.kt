package io.agora.edu.uikit.impl.chat

import io.agora.edu.core.context.EduContextUserRole

data class AgoraUIChatItem(
        var name: String = "",
        var uid: String = "",
        var role: Int = EduContextUserRole.Student.value,
        var message: String = "",
        var sensitiveWords: List<String>? = listOf(),
        var messageId: String = "",
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
        fun fromContextItem(item: io.agora.edu.core.context.EduContextChatItem): AgoraUIChatItem {
            return AgoraUIChatItem(
                    item.name,
                    item.uid,
                    item.role,
                    item.message,
                    item.sensitiveWords,
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
        fun fromContextChatItemType(type: io.agora.edu.core.context.EduContextChatItemType) : AgoraUIChatItemType {
            return when (type) {
                io.agora.edu.core.context.EduContextChatItemType.Text -> Text
            }
        }
    }
}

enum class AgoraUIChatSource {
    Local, Remote, System;

    companion object {
        fun fromContextChatSource(source: io.agora.edu.core.context.EduContextChatSource) : AgoraUIChatSource {
            return when (source) {
                io.agora.edu.core.context.EduContextChatSource.Local -> Local
                io.agora.edu.core.context.EduContextChatSource.Remote -> Remote
                io.agora.edu.core.context.EduContextChatSource.System -> System
            }
        }
    }
}

enum class AgoraUIChatState {
    Default, InProgress, Success, Fail;

    companion object {
        fun fromContextChatState(source: io.agora.edu.core.context.EduContextChatState) : AgoraUIChatState {
            return when (source) {
                io.agora.edu.core.context.EduContextChatState.Default -> Default
                io.agora.edu.core.context.EduContextChatState.InProgress -> InProgress
                io.agora.edu.core.context.EduContextChatState.Success -> Success
                io.agora.edu.core.context.EduContextChatState.Fail -> Fail
            }
        }
    }
}