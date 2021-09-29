package io.agora.agoraeducore.core.internal.framework

import io.agora.agoraeducore.core.internal.framework.data.EduCallback
import io.agora.agoraeducore.core.internal.education.api.board.EduBoard
import io.agora.agoraeducore.core.internal.framework.data.EduMessage
import io.agora.agoraeducore.core.internal.education.api.record.EduRecord
import io.agora.agoraeducore.core.internal.education.api.room.data.*
import io.agora.agoraeducore.core.internal.education.api.statistics.ConnectionState
import io.agora.agoraeducore.core.internal.education.api.statistics.NetworkQuality
import io.agora.agoraeducore.core.internal.framework.data.EduStreamEvent
import io.agora.agoraeducore.core.internal.framework.data.EduStreamInfo
import io.agora.agoraeducore.core.internal.framework.data.EduChatMessage
import java.io.Serializable

interface EduRoom {
    var roomProperties: MutableMap<String, Any>
    var board: EduBoard?
    var record: EduRecord?
    var eventListener: EduRoomEventListener?
    var roomAudioMixingListener: EduRoomAudioMixingListener?

    /**
     * return code descriptions:
     * 1: illegal arguments
     * 2: internal error
     * 101: rtm error
     * 201: rtc error
     * 301: networking error
     */
    fun join(options: RoomJoinOptions, callback: EduCallback<EduLocalUser>)

    /**
     * return code descriptions
     * 1:you haven't joined the room
     */
    fun leave(callback: EduCallback<Unit>)

    /**
     * return code descriptions
     * 1: you haven't joined the room
     */
    fun getRoomInfo(callback: EduCallback<EduRoomInfo>)

    /**
     * return code descriptions
     * 1: you haven't joined the room
     */
    fun getRoomStatus(callback: EduCallback<EduRoomStatus>)

    /**
     * return code descriptions
     * 1: you haven't joined the room
     */
    fun getLocalUser(callback: EduCallback<EduLocalUser>)

    /**
     * return code descriptions
     * 1: you haven't joined the room
     */
    fun getStudentCount(callback: EduCallback<Int>)

    /**
     * return code descriptions
     * 1: you haven't joined the room
     */
    fun getTeacherCount(callback: EduCallback<Int>)

    /**
     * return code descriptions
     * 1: you haven't joined the room
     */
    fun getStudentList(callback: EduCallback<MutableList<EduUserInfo>>)

    /**
     * return code descriptions
     * 1: you haven't joined the room
     */
    fun getTeacherList(callback: EduCallback<MutableList<EduUserInfo>>)

    /**code:message
     * 1:you haven't joined the room
     */
    fun getFullUserList(callback: EduCallback<MutableList<EduUserInfo>>)

    /**
     * return code descriptions
     * 1: you haven't joined the room
     */
    fun getFullStreamList(callback: EduCallback<MutableList<EduStreamInfo>>)

    fun getRtcCallId(id: String): String

    fun getRtmSessionId(): String

    fun getRoomUuid(): String
}

class RoomCreateOptions(
        var roomUuid: String,
        var roomName: String,
        val roomType: Int) {

    val roomProperties: MutableMap<String, String> = mutableMapOf()

    init {
        roomProperties[Property.KEY_TEACHER_LIMIT] = when (roomType) {
            RoomType.ONE_ON_ONE.value -> "1"
            RoomType.SMALL_CLASS.value -> "1"
            RoomType.LARGE_CLASS.value -> "1"
            RoomType.BREAKOUT_CLASS_OBSOLETE.value -> "1"
            RoomType.MEDIUM_CLASS_OBSOLETE.value -> "1"
            else -> {
                // -1 means no limit
                "-1"
            }
        }

        roomProperties[Property.KEY_STUDENT_LIMIT] = when (roomType) {
            RoomType.ONE_ON_ONE.value -> "1"
            RoomType.SMALL_CLASS.value -> "16"
            RoomType.LARGE_CLASS.value -> "-1"
            RoomType.BREAKOUT_CLASS_OBSOLETE.value -> "-1"
            RoomType.MEDIUM_CLASS_OBSOLETE.value -> "-1"
            else -> "-1"
        }

        roomProperties[Property.KEY_ASSISTANT_LIMIT] = when (roomType) {
            RoomType.ONE_ON_ONE.value -> "0"
            RoomType.SMALL_CLASS.value -> "0"
            RoomType.LARGE_CLASS.value -> "0"
            RoomType.BREAKOUT_CLASS_OBSOLETE.value -> "1"
            RoomType.MEDIUM_CLASS_OBSOLETE.value -> "0"
            else -> "1"
        }
    }
}

data class Property(
        val key: String,
        val value: String
) : Serializable {
    companion object {
        const val CAUSE = "cause"
        const val KEY_TEACHER_LIMIT = "TeacherLimit"
        const val KEY_STUDENT_LIMIT = "StudentLimit"
        const val KEY_ASSISTANT_LIMIT = "AssistantLimit"
    }
}

interface EduRoomEventListener {
    fun onRemoteUsersInitialized(users: List<EduUserInfo>, classRoom: EduRoom)

    fun onRemoteUsersJoined(users: List<EduUserInfo>, classRoom: EduRoom)

    fun onRemoteUserLeft(userEvent: EduUserEvent, classRoom: EduRoom)

    fun onRemoteUserUpdated(userEvent: EduUserEvent, type: EduUserStateChangeType, classRoom: EduRoom)

    fun onRemoteUserPropertiesChanged(changedProperties: MutableMap<String, Any>, classRoom: EduRoom,
                                      userInfo: EduUserInfo, cause: MutableMap<String, Any>?,
                                      operator: EduBaseUserInfo?)

    fun onRoomMessageReceived(message: EduMessage, classRoom: EduRoom)

    fun onRoomChatMessageReceived(chatMsg: EduChatMessage, classRoom: EduRoom)

    fun onRemoteStreamsInitialized(streams: List<EduStreamInfo>, classRoom: EduRoom)

    fun onRemoteStreamsAdded(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom)

    fun onRemoteStreamUpdated(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom)

    fun onRemoteStreamsRemoved(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom)

    fun onRemoteRTCJoinedOfStreamId(streamUuid: String)

    fun onRemoteRTCOfflineOfStreamId(streamUuid: String)

    fun onRoomStatusChanged(type: EduRoomChangeType, operatorUser: EduUserInfo?, classRoom: EduRoom)

    fun onRoomPropertiesChanged(changedProperties: MutableMap<String, Any>, classRoom: EduRoom,
                                cause: MutableMap<String, Any>?, operator: EduBaseUserInfo?)

    fun onNetworkQualityChanged(quality: NetworkQuality, user: EduBaseUserInfo, classRoom: EduRoom)

    fun onConnectionStateChanged(state: ConnectionState, classRoom: EduRoom)
}

interface EduRoomAudioMixingListener {
    fun onAudioMixingFinished()

    fun onAudioMixingStateChanged(state: Int, errorCode: Int)
}

/**
 * Master and sub room of room type breakout class
 */
enum class ClassType(var value: Int) {
    Main(0),
    Sub(1)
}

enum class RoomType(var value: Int) {
    ONE_ON_ONE(0),

    // The old version of medium class
    SMALL_CLASS(4),

    LARGE_CLASS(2),

    BREAKOUT_CLASS_OBSOLETE(3),

    // The old version of small class
    MEDIUM_CLASS_OBSOLETE(1);

    companion object {
        fun roomTypeIsValid(value: Int): Boolean {
            return value == ONE_ON_ONE.value ||
                    value == SMALL_CLASS.value ||
                    value == LARGE_CLASS.value ||
                    value == BREAKOUT_CLASS_OBSOLETE.value ||
                    value == MEDIUM_CLASS_OBSOLETE.value
        }
    }
}