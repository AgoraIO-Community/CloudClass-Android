package io.agora.uikit.impl.chat

import io.agora.educontext.EduContextChatItem
import io.agora.educontext.EduContextChatItemType
import io.agora.educontext.EduContextChatSource
import io.agora.educontext.EduContextChatState

data class AgoraUIChatItem(
        var name: String = "",
        var uid: String = "",
        var message: String = "",
        var messageId: String = "",
        var type: AgoraUIChatItemType = AgoraUIChatItemType.Text,
        var source: AgoraUIChatSource = AgoraUIChatSource.Remote,
        var state: AgoraUIChatState = AgoraUIChatState.Default,
        var timestamp: Long = 0) {

    companion object {
        fun fromContextItem(item: EduContextChatItem): AgoraUIChatItem {
            return AgoraUIChatItem(
                    item.name,
                    item.uid,
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
        fun fromContextChatItemType(type: EduContextChatItemType) : AgoraUIChatItemType {
            return when (type) {
                EduContextChatItemType.Text -> Text
            }
        }
    }
}

enum class AgoraUIChatSource {
    Local, Remote, System;

    companion object {
        fun fromContextChatSource(source: EduContextChatSource) : AgoraUIChatSource {
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
        fun fromContextChatState(source: EduContextChatState) : AgoraUIChatState {
            return when (source) {
                EduContextChatState.Default -> Default
                EduContextChatState.InProgress -> InProgress
                EduContextChatState.Success -> Success
                EduContextChatState.Fail -> Fail
            }
        }
    }
}