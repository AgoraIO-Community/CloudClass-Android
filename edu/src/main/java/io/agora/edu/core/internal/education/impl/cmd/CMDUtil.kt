package io.agora.edu.core.internal.education.impl.cmd

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.agora.edu.core.internal.education.impl.util.Convert
import io.agora.edu.core.internal.framework.data.EduMessage
import io.agora.edu.core.internal.framework.EduRoom
import io.agora.edu.core.internal.framework.EduUserRole
import io.agora.edu.core.internal.education.impl.cmd.bean.CMDResponseBody
import io.agora.edu.core.internal.education.impl.cmd.bean.RtmMsg
import io.agora.edu.core.internal.education.impl.cmd.bean.RtmPeerMsg
import io.agora.edu.core.internal.education.impl.room.EduRoomImpl
import io.agora.edu.core.internal.framework.EduBaseUserInfo
import io.agora.edu.core.internal.framework.data.EduChatMessage
import io.agora.edu.core.internal.framework.data.EduPeerChatMessage
import java.lang.Exception

internal object CMDUtil {

    fun buildEduMsg(text: String, eduRoom: EduRoom? = null): EduMessage {
        val cmdResponseBody = Gson().fromJson<CMDResponseBody<RtmMsg>>(text, object :
                TypeToken<CMDResponseBody<RtmMsg>>() {}.type)
        val rtmMsg = cmdResponseBody.data
        val fromUser = if (eduRoom == null) {
            EduBaseUserInfo(rtmMsg.fromUser.userUuid, rtmMsg.fromUser.userName, EduUserRole.EduRoleTypeInvalid)
        } else {
            Convert.convertFromUserInfo(rtmMsg.fromUser, (eduRoom as EduRoomImpl).getCurRoomType())
        }
        return if (rtmMsg.type != null) {
            EduChatMessage(fromUser, rtmMsg.message, rtmMsg.sensitiveWords, cmdResponseBody.timestamp, cmdResponseBody.sequence, rtmMsg.type)
        } else {
            EduMessage(fromUser, rtmMsg.message, rtmMsg.sensitiveWords, cmdResponseBody.timestamp, cmdResponseBody.sequence)
        }
    }

    fun buildPeerEduMsg(text: String, eduRoom: EduRoom? = null): EduPeerChatMessage? {
        try {
            val cmdResponseBody = Gson().fromJson<CMDResponseBody<RtmPeerMsg>>(
                    text, object : TypeToken<CMDResponseBody<RtmPeerMsg>>() {}.type)
            val rtmMsg = cmdResponseBody.data
            return EduPeerChatMessage(
                    EduBaseUserInfo(
                            rtmMsg.fromUser.userUuid,
                            rtmMsg.fromUser.userName,
                            Convert.convertUserRole(rtmMsg.fromUser.role)),
                    null,
                    rtmMsg.message,
                    rtmMsg.sensitiveWords,
                    cmdResponseBody.timestamp,
                    rtmMsg.peerMessageId,
                    rtmMsg.type
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}