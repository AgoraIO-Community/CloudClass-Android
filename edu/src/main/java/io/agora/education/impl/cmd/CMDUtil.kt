package io.agora.education.impl.cmd

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.agora.education.impl.util.Convert
import io.agora.education.api.message.EduChatMsg
import io.agora.education.api.message.EduFromUserInfo
import io.agora.education.api.message.EduMsg
import io.agora.education.api.room.EduRoom
import io.agora.education.api.user.data.EduUserRole
import io.agora.education.impl.cmd.bean.CMDResponseBody
import io.agora.education.impl.cmd.bean.RtmMsg
import io.agora.education.impl.room.EduRoomImpl
import io.agora.education.impl.room.data.response.EduFromUserRes
import io.agora.education.impl.user.data.EduUserInfoImpl

internal object CMDUtil {

    fun buildEduMsg(text: String, eduRoom: EduRoom?): EduMsg {
        val cmdResponseBody = Gson().fromJson<CMDResponseBody<RtmMsg>>(text, object :
                TypeToken<CMDResponseBody<RtmMsg>>() {}.type)
        val rtmMsg = cmdResponseBody.data
        val fromUser = if (eduRoom == null) {
            EduFromUserInfo(rtmMsg.fromUser.userUuid, rtmMsg.fromUser.userName, EduUserRole.EduRoleTypeInvalid)
        } else {
            Convert.convertFromUserInfo(rtmMsg.fromUser, (eduRoom as EduRoomImpl).getCurRoomType())
        }
        return if (rtmMsg.type != null) {
            EduChatMsg(fromUser, rtmMsg.message, cmdResponseBody.timestamp, rtmMsg.type)
        } else {
            EduMsg(fromUser, rtmMsg.message, cmdResponseBody.timestamp)
        }
    }
}