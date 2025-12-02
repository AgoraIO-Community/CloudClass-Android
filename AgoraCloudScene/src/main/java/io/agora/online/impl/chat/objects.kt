package io.agora.online.impl.chat

data class AgoraChatInteractionPacket(val signal: AgoraChatInteractionSignal, val body: Any)

enum class AgoraChatInteractionSignal(val value: Int) {
    // unread tips
    UnreadTips(0)
}