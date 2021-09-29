package io.agora.agoraeducore.core.internal.education.impl.cmd.bean

import io.agora.agoraeducore.core.internal.education.impl.room.data.response.EduFromUserRes

/**rtm传送消息时，返回数据的数据结构*/
class RtmMsg(
        val fromUser: EduFromUserRes,
        val message: String,
        val sensitiveWords: List<String>,
        val type: Int?)

class RtmPeerMsg(
        val fromUser: EduFromUserRes,
        val message: String,
        val sensitiveWords: List<String>,
        val peerMessageId: String,
        val type: Int)