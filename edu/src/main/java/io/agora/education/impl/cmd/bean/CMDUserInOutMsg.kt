package io.agora.education.impl.cmd.bean

import io.agora.education.impl.room.data.response.EduBaseStreamRes
import io.agora.education.impl.room.data.response.EduUserRes

/**人员进出时，RTM回调出来的数据结构*/
class RtmUserInOutMsg(val total: Int, var onlineUsers: MutableList<OnlineUserInfo>,
                      var offlineUsers: MutableList<OfflineUserInfo>) {
}

class OnlineUserInfo(userUuid: String, userName: String, role: String,
                     muteChat: Int, updateTime: Long?, state: Int,
                     val type: Int,
                     val streamUuid: String, val streams: MutableList<EduBaseStreamRes>,
                     val userProperties: MutableMap<String, Any>)
    : EduUserRes(userUuid, userName, role, muteChat, updateTime, state)

class OfflineUserInfo(userUuid: String, userName: String, role: String,
                      muteChat: Int, updateTime: Long?, state: Int,
                      val type: Int,
                      val operator: EduUserRes?, val streamUuid: String,
                      val streams: MutableList<EduBaseStreamRes>,
                      val userProperties: MutableMap<String, Any>)
    : EduUserRes(userUuid, userName, role, muteChat, updateTime, state) {

}