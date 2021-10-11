package io.agora.edu.core.internal.education.impl.cmd

import io.agora.edu.core.internal.education.impl.Constants.Companion.AgoraLog
import io.agora.edu.core.internal.education.api.room.data.EduRoomChangeType
import io.agora.edu.core.internal.framework.*
import io.agora.edu.core.internal.framework.data.*

internal class CMDCallbackManager {

    fun onRoomStatusChanged(eventEdu: EduRoomChangeType, operatorUser: EduUserInfo?, classRoom: EduRoom) {
        classRoom.eventListener?.onRoomStatusChanged(eventEdu, operatorUser, classRoom)
    }

    fun onRoomPropertyChanged(changedProperties: MutableMap<String, Any>, classRoom: EduRoom,
                              cause: MutableMap<String, Any>?, operator: EduBaseUserInfo?) {
        classRoom.eventListener?.onRoomPropertiesChanged(changedProperties, classRoom, cause, operator)
    }

    fun onRoomChatMessageReceived(chatMsg: EduChatMessage, classRoom: EduRoom) {
        classRoom.eventListener?.onRoomChatMessageReceived(chatMsg, classRoom)
    }

    fun onRoomMessageReceived(message: EduMessage, classRoom: EduRoom) {
        classRoom.eventListener?.onRoomMessageReceived(message, classRoom)
    }

    fun onRemoteUsersJoined(users: List<EduUserInfo>, classRoom: EduRoom) {
        classRoom.eventListener?.onRemoteUsersJoined(users, classRoom)
    }

    fun onRemoteStreamsAdded(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        classRoom.eventListener?.onRemoteStreamsAdded(streamEvents, classRoom)
    }

    fun onRemoteUsersLeft(userEvents: MutableList<EduUserEvent>, classRoom: EduRoom) {
        userEvents.forEach {
            classRoom.eventListener?.onRemoteUserLeft(it, classRoom)
        }
    }

    fun onRemoteStreamsRemoved(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        classRoom.eventListener?.onRemoteStreamsRemoved(streamEvents, classRoom)
    }

    fun onRemoteStreamsUpdated(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        classRoom.eventListener?.onRemoteStreamUpdated(streamEvents, classRoom)
    }

    fun onRemoteUserUpdated(userEvent: EduUserEvent, type: EduUserStateChangeType, classRoom: EduRoom) {
        classRoom.eventListener?.onRemoteUserUpdated(userEvent, type, classRoom)
    }

    fun onRemoteUserPropertiesUpdated(changedProperties: MutableMap<String, Any>, classRoom: EduRoom,
                                      userInfo: EduUserInfo, cause: MutableMap<String, Any>?,
                                      operator: EduBaseUserInfo?) {
        classRoom.eventListener?.onRemoteUserPropertiesChanged(changedProperties, classRoom,
                userInfo, cause, operator)
    }

    fun onLocalUserAdded(userInfo: EduUserInfo, eduUser: EduLocalUser) {
        /**本地用户的online数据不回调出去，仅内部处理*/
    }

    fun onLocalUserUpdated(userEvent: EduUserEvent, type: EduUserStateChangeType, eduUser: EduLocalUser) {
        eduUser.eventListener?.onLocalUserUpdated(userEvent, type)
    }

    fun onLocalUserRemoved(userEvent: EduUserEvent, eduUser: EduLocalUser, type: Int) {
        /**本地用户的offline数据暂不回调出去，后期会在EduUserEventListener中添加
         * onLocalUserLeft回调来处理此消息(为踢人功能预备)*/
        if (type == 2) {
            AgoraLog.i("Local User was removed from classroom by teacher!")
        }
        eduUser.eventListener?.onLocalUserLeft(userEvent, if (type == 1) EduUserLeftType.Normal else EduUserLeftType.KickOff)
    }

    fun onLocalUserPropertiesUpdated(changedProperties: MutableMap<String, Any>,
                                     cause: MutableMap<String, Any>?, user: EduLocalUser,
                                     operator: EduBaseUserInfo?) {
        user.eventListener?.onLocalUserPropertiesChanged(changedProperties, user.userInfo, cause, operator)
    }

    fun onLocalStreamAdded(streamEvent: EduStreamEvent, eduUser: EduLocalUser) {
        eduUser.eventListener?.onLocalStreamAdded(streamEvent)
    }

    fun onLocalStreamUpdated(streamEvent: EduStreamEvent, eduUser: EduLocalUser) {
        eduUser.eventListener?.onLocalStreamUpdated(streamEvent)
    }

    fun onLocalStreamRemoved(streamEvent: EduStreamEvent, eduUser: EduLocalUser) {
        eduUser.eventListener?.onLocalStreamRemoved(streamEvent)
    }

    fun onUserChatMessageReceived(chatMsg: EduPeerChatMessage, listener: EduManagerEventListener?) {
        listener?.onUserChatMessageReceived(chatMsg)
    }

    fun onUserMessageReceived(message: EduMessage, listener: EduManagerEventListener?) {
        listener?.onUserMessageReceived(message)
    }

    fun onUserActionMessageReceived(actionMsg: EduActionMessage, listener: EduManagerEventListener?) {
        listener?.onUserActionMessageReceived(actionMsg)
    }
}