package io.agora.education.api.room.listener

import io.agora.education.api.message.EduChatMsg
import io.agora.education.api.message.EduMsg
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.EduRoomChangeType
import io.agora.education.api.statistics.ConnectionState
import io.agora.education.api.statistics.NetworkQuality
import io.agora.education.api.stream.data.EduStreamEvent
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.user.data.EduUserEvent
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.api.user.data.EduUserStateChangeType

interface EduRoomEventListener {

    fun onRemoteUsersInitialized(users: List<EduUserInfo>, classRoom: EduRoom)

    fun onRemoteUsersJoined(users: List<EduUserInfo>, classRoom: EduRoom)

    fun onRemoteUserLeft(userEvent: EduUserEvent, classRoom: EduRoom)

    fun onRemoteUserUpdated(userEvent: EduUserEvent, type: EduUserStateChangeType, classRoom: EduRoom)

    fun onRoomMessageReceived(message: EduMsg, classRoom: EduRoom)

    fun onRoomChatMessageReceived(chatMsg: EduChatMsg, classRoom: EduRoom)

    fun onRemoteStreamsInitialized(streams: List<EduStreamInfo>, classRoom: EduRoom)

    fun onRemoteStreamsAdded(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom)

    fun onRemoteStreamUpdated(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom)

    fun onRemoteStreamsRemoved(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom)

    fun onRoomStatusChanged(type: EduRoomChangeType, operatorUser: EduUserInfo?, classRoom: EduRoom)

    /**引起roomProperty发生改变的原因
     * 对应EduUser中的updateRoomProperty*/
    fun onRoomPropertiesChanged(classRoom: EduRoom, cause: MutableMap<String, Any>?)

    fun onNetworkQualityChanged(quality: NetworkQuality, user: EduUserInfo, classRoom: EduRoom)

    fun onConnectionStateChanged(state: ConnectionState, classRoom: EduRoom)
}
