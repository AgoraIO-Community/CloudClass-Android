package io.agora.covideo

import androidx.annotation.Keep
import io.agora.base.bean.MapBean

@Keep
class AgoraCoVideoAction(
        val action: Int,
        val fromRoom: AgoraCoVideoFromRoom) : MapBean() {
}

@Keep
class AgoraCoVideoFromRoom(
        val uuid: String,
        val name: String
)