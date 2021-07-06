package io.agora.educontext

import io.agora.educontext.context.*

interface EduContextPool {
    fun chatContext(): ChatContext?

    fun handsUpContext(): HandsUpContext?

    fun roomContext(): RoomContext?

    fun mediaContext(): MediaContext?

    fun deviceContext(): DeviceContext?

    fun screenShareContext(): ScreenShareContext?

    fun userContext(): UserContext?

    fun videoContext(): VideoContext?

    fun whiteboardContext(): WhiteboardContext?

    fun privateChatContext(): PrivateChatContext?

    fun extAppContext(): ExtAppContext?
}