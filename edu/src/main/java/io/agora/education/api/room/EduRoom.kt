package io.agora.education.api.room

import io.agora.education.api.EduCallback
import io.agora.education.api.board.EduBoard
import io.agora.education.api.record.EduRecord
import io.agora.education.api.room.data.EduRoomInfo
import io.agora.education.api.room.data.EduRoomStatus
import io.agora.education.api.room.data.RoomJoinOptions
import io.agora.education.api.room.listener.EduRoomEventListener
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.user.EduStudent
import io.agora.education.api.user.EduTeacher
import io.agora.education.api.user.EduUser
import io.agora.education.api.user.data.EduUserInfo

abstract class EduRoom {

    var roomProperties: MutableMap<String, Any> = mutableMapOf()

    lateinit var board: EduBoard
    lateinit var record: EduRecord

    var eventListener: EduRoomEventListener? = null

    /**code:message
     * 1:parameter XXX is invalid
     * 2:internal error：可以内部订阅具体什么错误
     * 101:communication error:code，透传rtm错误code或者message。
     * 201:media error:code，透传rtc错误code或者message。
     * 301:network error，透传后台错误msg字段*/
    abstract fun joinClassroom(options: RoomJoinOptions, callback: EduCallback<EduUser>)

    /**code:message
     * 1:you haven't joined the room*/
    abstract fun getLocalUser(callback: EduCallback<EduUser>)

    /**code:message
     * 1:you haven't joined the room*/
    abstract fun getRoomInfo(callback: EduCallback<EduRoomInfo>)

    /**code:message
     * 1:you haven't joined the room*/
    abstract fun getRoomStatus(callback: EduCallback<EduRoomStatus>)

    /**code:message
     * 1:you haven't joined the room*/
    abstract fun getStudentCount(callback: EduCallback<Int>)

    /**code:message
     * 1:you haven't joined the room*/
    abstract fun getTeacherCount(callback: EduCallback<Int>)

    /**code:message
     * 1:you haven't joined the room*/
    abstract fun getStudentList(callback: EduCallback<MutableList<EduUserInfo>>)

    /**code:message
     * 1:you haven't joined the room*/
    abstract fun getTeacherList(callback: EduCallback<MutableList<EduUserInfo>>)

    /**code:message
     * 1:you haven't joined the room*/
    abstract fun getFullStreamList(callback: EduCallback<MutableList<EduStreamInfo>>)

    /**code:message
     * 1:you haven't joined the room*/
    abstract fun getFullUserList(callback: EduCallback<MutableList<EduUserInfo>>)

    abstract fun clearData()

    /**code:message
     * 1:you haven't joined the room*/
    abstract fun leave(callback: EduCallback<Unit>)

    protected abstract fun getRoomUuid(): String

    override fun equals(other: Any?): Boolean {
        if(other == null) {
            return false
        }
        return if (other !is EduRoom) {
            false
        } else {
            getRoomUuid() == other.getRoomUuid()
        }
    }
}
