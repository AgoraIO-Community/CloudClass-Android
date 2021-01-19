package io.agora.education.impl.cmd

import android.util.Log
import com.google.gson.Gson
import io.agora.education.impl.util.Convert
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.EduRoomState
import io.agora.education.api.room.data.RoomType
import io.agora.education.api.stream.data.*
import io.agora.education.api.user.data.*
import io.agora.education.impl.cmd.bean.*
import io.agora.education.impl.room.EduRoomImpl
import io.agora.education.impl.room.data.response.EduSnapshotRes
import io.agora.education.impl.stream.EduStreamInfoImpl
import io.agora.education.impl.user.data.EduUserInfoImpl
import io.agora.education.impl.user.data.base.EduUserStateChangeEvent

internal class CMDDataMergeProcessor : CMDProcessor() {
    companion object {
        const val TAG = "CMDDataMergeProcessor"

        /**从 {@param userInfoList} 中移除 离开课堂的用户 {@param offLineUserList}*/
        fun removeUserWithOffline(offlineUserList: MutableList<OfflineUserInfo>,
                                  userInfoList: MutableList<EduUserInfo>, roomType: RoomType):
                MutableList<EduUserEvent> {
            val validUserInfoList = mutableListOf<EduUserEvent>()
            synchronized(userInfoList) {
                for (element in offlineUserList) {
                    val role = Convert.convertUserRole(element.role, roomType)
                    val userInfo1: EduUserInfo = EduUserInfoImpl(element.userUuid, element.userName, role,
                            element.muteChat == EduChatState.Allow.value, element.updateTime)
                    userInfo1.streamUuid = element.streamUuid
                    userInfo1.userProperties = element.userProperties
                    if (userInfoList.contains(userInfo1)) {
                        val index = userInfoList.indexOf(userInfo1)

                        /**剔除掉被过滤掉的用户*/
                        userInfoList.removeAt(index)
                        /**构造userEvent并返回*/
                        val operator = getOperator(element.operator, userInfo1, roomType)
                        val userEvent = EduUserEvent(userInfo1, operator)
                        validUserInfoList.add(userEvent)
                    }
                }
                return validUserInfoList
            }
        }

        fun addUserWithOnline(onlineUserList: MutableList<OnlineUserInfo>,
                              userInfoList: MutableList<EduUserInfo>, roomType: RoomType):
                MutableList<EduUserInfo> {
            val validUserInfoList = mutableListOf<EduUserInfo>()
            synchronized(userInfoList) {
                for (element in onlineUserList) {
                    val role = Convert.convertUserRole(element.role, roomType)
                    val userInfo1 = EduUserInfoImpl(element.userUuid, element.userName, role,
                            element.muteChat == EduChatState.Allow.value, element.updateTime)
                    userInfo1.streamUuid = element.streamUuid
                    userInfo1.userProperties = element.userProperties
                    if (userInfoList.contains(userInfo1)) {
                        val index = userInfoList.indexOf(userInfo1)

                        /**更新用户的数据为最新数据*/
                        userInfoList[index] = userInfo1
//                            validUserInfoList.add(userInfo1)
                    } else {
                        userInfoList.add(userInfo1)
                        validUserInfoList.add(userInfo1)
                    }
                }
                return validUserInfoList
            }
        }

        fun updateUserWithUserStateChange(cmdUserStateMsg: CMDUserStateMsg,
                                          eduUserInfos: MutableList<EduUserInfo>, roomType: RoomType)
                : MutableList<EduUserStateChangeEvent> {
            val userStateChangedList = mutableListOf<EduUserInfo>()
            userStateChangedList.add(Convert.convertUserInfo(cmdUserStateMsg, roomType))
            val validUserEventList = mutableListOf<EduUserStateChangeEvent>()
            synchronized(eduUserInfos) {
                var type = EduUserStateChangeType.Chat
                for (element in userStateChangedList) {
                    if (eduUserInfos.contains(element)) {
                        val index = eduUserInfos.indexOf(element)

                        /**获取已存在于集合中的用户*/
                        val userInfo2 = eduUserInfos[index]
                        /**更新用户的数据为最新数据*/
                        eduUserInfos[index] = element
                        /**构造userEvent并返回*/
                        val operator = getOperator(cmdUserStateMsg.operator, element, roomType)
                        val userEvent = EduUserEvent(element, operator)
                        validUserEventList.add(EduUserStateChangeEvent(userEvent, type))
                    } else {
                        /**用户信息不存在于本地，需要先把此用户信息同步至本地*/
                        eduUserInfos.add(element)
                        /**构造userEvent并返回*/
                        val operator = getOperator(cmdUserStateMsg.operator, element, roomType)
                        val userEvent = EduUserEvent(element, operator)
                        validUserEventList.add(EduUserStateChangeEvent(userEvent, type))
                    }
                }
                return validUserEventList
            }
        }

        fun updateUserPropertyWithChange(cmdUserPropertyRes: CMDUserPropertyRes,
                                         eduUserInfos: MutableList<EduUserInfo>): EduUserInfo? {
            for (element in eduUserInfos) {
                if (cmdUserPropertyRes.fromUser.userUuid == element.userUuid) {
                    val properties = cmdUserPropertyRes.changeProperties
                    val sets = properties.entries
                    sets?.forEach {
                        if (cmdUserPropertyRes.action == PropertyChangeType.Upsert.value) {
                            element.userProperties[it.key] = it.value
                        } else if (cmdUserPropertyRes.action == PropertyChangeType.Delete.value) {
                            element.userProperties.remove(it.key)
                        }
                    }
                    element.userProperties = cmdUserPropertyRes.changeProperties
                    return element
                }
            }
            return null
        }

        fun addStreamWithUserOnline(onlineUserList: MutableList<OnlineUserInfo>,
                                    streamInfoList: MutableList<EduStreamInfo>, roomType: RoomType): MutableList<EduStreamEvent> {
            val validStreamList = mutableListOf<EduStreamEvent>()
            synchronized(streamInfoList) {
                for (element in onlineUserList) {
                    val role = Convert.convertUserRole(element.role, roomType)
                    val publisher = EduBaseUserInfo(element.userUuid, element.userName, role)
                    element.streams?.forEach {
                        val videoSourceType = Convert.convertVideoSourceType(it.videoSourceType)
                        val streamInfo = EduStreamInfoImpl(it.streamUuid, it.streamName, videoSourceType,
                                it.videoState == EduVideoState.Open.value, it.audioState == EduAudioState.Open.value,
                                publisher, it.updateTime)
//                        if (streamInfoList.contains(streamInfo)) {
                        val index = Convert.streamExistsInList(streamInfo, streamInfoList)
                        Log.e(TAG, "index的值:$index, 数组长度:${streamInfoList.size}")
                        if (index > -1) {
                            /**更新本地缓存为最新数据;因为onlineUserList经过了有效判断，所以此处不再比较updateTime，直接remove*/
                            streamInfoList[index] = streamInfo
//                            validStreamList.add(EduStreamEvent(streamInfo, null))
                        } else {
                            streamInfoList.add(streamInfo)
                            validStreamList.add(EduStreamEvent(streamInfo, null))
                        }
                    }
                }
            }
            return validStreamList
        }

        fun removeStreamWithUserOffline(offlineUserList: MutableList<OfflineUserInfo>,
                                        streamInfoList: MutableList<EduStreamInfo>, roomType: RoomType): MutableList<EduStreamEvent> {
            val validStreamList = mutableListOf<EduStreamEvent>()
            synchronized(streamInfoList) {
                for (element in offlineUserList) {
                    val role = Convert.convertUserRole(element.role, roomType)
                    val publisher = EduBaseUserInfo(element.userUuid, element.userName, role)
                    val operator = getOperator(element.operator, publisher, roomType)
                    element.streams?.forEach {
                        val videoSourceType = Convert.convertVideoSourceType(it.videoSourceType)
                        val streamInfo = EduStreamInfoImpl(it.streamUuid, it.streamName, videoSourceType,
                                it.audioState == EduAudioState.Open.value, it.videoState == EduVideoState.Open.value,
                                publisher, it.updateTime)
//                        if (streamInfoList.contains(streamInfo)) {
                        val index = Convert.streamExistsInList(streamInfo, streamInfoList);
                        if (index > -1) {
                            /**更新本地缓存为最新数据;因为offlineUserList经过了有效判断，所以此处不再比较updateTime，直接remove*/
                            streamInfoList.removeAt(index)
                            validStreamList.add(EduStreamEvent(streamInfo, operator))
                        }
                    }
                }
            }
            return validStreamList
        }


        /**调用此函数之前须确保first和second代表的是同一个流
         *
         * 比较first的数据是否比second的更为接近当前时间(即找出一个最新数据)
         * @return > 0（first > second）
         *         !(> 0) first <= second*/
        private fun compareStreamInfoTime(first: EduStreamInfo, second: EduStreamInfo): Long {
            /**判断更新时间是否为空(为空的有可能是原始数据)*/
            if ((first as EduStreamInfoImpl).updateTime == null) {
                return -1
            }
            if ((second as EduStreamInfoImpl).updateTime == null) {
                return first.updateTime!!
            }
            /**最终判断出最新数据*/
            return first.updateTime!!.minus(second.updateTime!!)
        }


        fun addStreamWithAction(streamRes: CMDStreamRes,
                                streamInfoList: MutableList<EduStreamInfo>, roomType: RoomType):
                EduStreamInfo {
            val validStream: EduStreamInfo
            val streamInfo = Convert.convertStreamInfo(streamRes, roomType)
            synchronized(streamInfoList) {
                val index = Convert.streamExistsInList(streamInfo, streamInfoList)
                Log.e(TAG, "index的值:$index, 数组长度:${streamInfoList.size}")
                if (index > -1) {
                    /**更新用户的数据为最新数据*/
                    streamInfoList[index] = streamInfo
                    validStream = streamInfo
                } else {
                    streamInfoList.add(streamInfo)
                    validStream = streamInfo
                }
                return validStream
            }
        }

        fun updateStreamWithAction(streamRes: CMDStreamRes,
                                   streamInfoList: MutableList<EduStreamInfo>, roomType: RoomType):
                EduStreamInfo {
            val validStream: EduStreamInfo
            val streamInfo = Convert.convertStreamInfo(streamRes, roomType)
            Log.e(TAG, "本地流缓存:" + Gson().toJson(streamInfoList))
            synchronized(streamInfoList) {
                val index = Convert.streamExistsInList(streamInfo, streamInfoList)
                Log.e(TAG, "index的值:$index, 数组长度:${streamInfoList.size}")
                if (index > -1) {
                    /**更新用户的数据为最新数据*/
                    streamInfoList[index] = streamInfo
                    validStream = streamInfo
                } else {
                    /**发现是修改流而且本地又没有那么直接添加到本地并作为有效数据*/
                    streamInfoList.add(streamInfo)
                    validStream = streamInfo
                }
                return validStream
            }
        }

        fun removeStreamWithAction(streamRes: CMDStreamRes,
                                   streamInfoList: MutableList<EduStreamInfo>, roomType: RoomType):
                EduStreamInfo? {
            val validStream: EduStreamInfo?
            val streamInfo = Convert.convertStreamInfo(streamRes, roomType)
            synchronized(streamInfoList) {
                val index = Convert.streamExistsInList(streamInfo, streamInfoList)
                Log.e(TAG, "index的值:$index, 数组长度:${streamInfoList.size}")
                validStream = if (index > -1) {
                    /**更新用户的数据为最新数据*/
                    streamInfoList.removeAt(index)
                    streamInfo
                } else {
                    /*流不存在与本地缓存中*/
                    null
                }
                return validStream
            }
        }

        /**同步房间的快照信息*/
        fun syncSnapshotToRoom(eduRoom: EduRoom, snapshotRes: EduSnapshotRes) {
            val snapshotRoomRes = snapshotRes.room
            val roomInfo = (eduRoom as EduRoomImpl).getCurRoomInfo()
            val roomStatus = eduRoom.getCurRoomStatus()
            roomInfo.roomName = snapshotRoomRes.roomInfo.roomName
            roomInfo.roomUuid = snapshotRoomRes.roomInfo.roomUuid
            val status = snapshotRoomRes.roomState
            roomStatus.isStudentChatAllowed = Convert.extractStudentChatAllowState(
                    status.muteChat, eduRoom.getCurRoomType())
            roomStatus.courseState = Convert.convertRoomState(status.state)
            if (status.state == EduRoomState.START.value) {
                roomStatus.startTime = status.startTime
            }
            snapshotRoomRes.roomProperties?.let {
                eduRoom.roomProperties = it
            }
            val snapshotUserRes = snapshotRes.users
            val validAddedUserList = addUserWithOnline(snapshotUserRes, eduRoom.getCurUserList(),
                    eduRoom.getCurRoomType())
            val validAddedStreamList = addStreamWithUserOnline(snapshotUserRes, eduRoom.getCurStreamList(),
                    eduRoom.getCurRoomType())
            roomStatus.onlineUsersCount = validAddedUserList.size
        }

        fun updateRoomProperties(eduRoom: EduRoom, event: CMDRoomPropertyRes) {
            val properties = event.changeProperties
            val sets = properties.entries
            sets?.forEach {
                if (event.action == PropertyChangeType.Upsert.value) {
                    eduRoom.roomProperties[it.key] = it.value
                } else if (event.action == PropertyChangeType.Delete.value) {
                    eduRoom.roomProperties.remove(it.key)
                }
            }
        }

        fun updateRoomProperties2(eduRoom: EduRoom, event: CMDRoomPropertyRes) {
            val roomProperties = eduRoom.roomProperties
            val properties = event.changeProperties
            val sets = properties.entries
            sets?.forEach {
                if (event.action == PropertyChangeType.Upsert.value) {
                    val keys = it.key.split(".")
                    val iterable = keys.iterator()
                    var map: MutableMap<String, Any> = roomProperties
                    val notExistsKeys = mutableListOf<String>()
                    while (iterable.hasNext()) {
                        val elementKey = iterable.next()
                        val data = map[elementKey]
                        if (data != null && data is MutableMap<*, *>) {
                            if (!iterable.hasNext()) {
                                map[elementKey] = it.value
                            } else {
                                map = data as MutableMap<String, Any>
                            }
                        } else if (data != null && data !is MutableMap<*, *>) {
                            map[elementKey] = it.value
                        } else if (data == null) {
                            notExistsKeys.add(elementKey)
                        }
                    }
                    if (notExistsKeys.isNotEmpty()) {
                        notExistsKeys.reverse()
                        var value = it.value
                        if (notExistsKeys.size > 1) {
                            for ((index, element) in notExistsKeys.withIndex()) {
                                val map = mutableMapOf<String, Any>()
                                map[element] = value
                                value = map
                                if (index == notExistsKeys.size - 2) {
                                    break
                                }
                            }
                        }
                        map[notExistsKeys[notExistsKeys.size - 1]] = value
                    }
                } else if (event.action == PropertyChangeType.Delete.value) {
                }
            }
        }
    }
}
