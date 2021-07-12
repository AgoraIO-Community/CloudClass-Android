package io.agora.educontext.eventHandler

import io.agora.educontext.EduContextPrivateChatInfo

interface IPrivateChatHandler {
    fun onPrivateChatStarted(info: EduContextPrivateChatInfo)

    fun onPrivateChatEnded()
}