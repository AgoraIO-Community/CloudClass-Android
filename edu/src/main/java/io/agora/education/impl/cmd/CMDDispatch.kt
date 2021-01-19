package io.agora.education.impl.cmd

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.agora.education.impl.Constants.Companion.AgoraLog
import io.agora.education.api.manager.listener.EduManagerEventListener
import io.agora.education.api.message.EduChatMsg
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.EduRoomChangeType
import io.agora.education.api.room.data.RoomType
import io.agora.education.api.stream.data.EduStreamEvent
import io.agora.education.api.user.data.EduChatState
import io.agora.education.api.user.data.EduUserEvent
import io.agora.education.impl.cmd.bean.*
import io.agora.education.impl.room.EduRoomImpl
import io.agora.education.impl.util.Convert
import io.agora.rtc.Constants
import io.agora.rte.RteEngineImpl


internal class CMDDispatch(private val eduRoom: EduRoom) {
    private val TAG = CMDDispatch::class.java.simpleName

    private val cmdCallbackManager: CMDCallbackManager = CMDCallbackManager()

    fun dispatchMsg(cmdResponseBody: CMDResponseBody<Any>?) {
        val text = Gson().toJson(cmdResponseBody)
        cmdResponseBody?.let {
            dispatchChannelMsg(text)
        }
    }

    private fun dispatchChannelMsg(text: String) {
        val cmdResponseBody = Gson().fromJson<CMDResponseBody<Any>>(text, object :
                TypeToken<CMDResponseBody<Any>>() {}.type)
        when (cmdResponseBody.cmd) {
            CMDId.RoomStateChange.value -> {
                /**课堂状态发生改变*/
                val rtmRoomState = Gson().fromJson<CMDResponseBody<CMDRoomState>>(text, object :
                        TypeToken<CMDResponseBody<CMDRoomState>>() {}.type).data
                val roomStatus = (eduRoom as EduRoomImpl).getCurRoomStatus()
                roomStatus.courseState = Convert.convertRoomState(rtmRoomState.state)
                AgoraLog.i("$TAG->Course state changed to :${roomStatus.courseState.value}")
                roomStatus.startTime = rtmRoomState.startTime
                val operator = Convert.convertUserInfo(rtmRoomState.operator, eduRoom.getCurRoomType())
                cmdCallbackManager.onRoomStatusChanged(EduRoomChangeType.CourseState, operator, eduRoom)
            }
            CMDId.RoomMuteStateChange.value -> {
                val rtmRoomMuteState = Gson().fromJson<CMDResponseBody<CMDRoomMuteState>>(text, object :
                        TypeToken<CMDResponseBody<CMDRoomMuteState>>() {}.type).data
                val roomStatus = (eduRoom as EduRoomImpl).getCurRoomStatus()
                when (eduRoom.getCurRoomType()) {
                    RoomType.ONE_ON_ONE, RoomType.SMALL_CLASS -> {
                        /**判断本次更改是否包含针对学生的全部禁聊;*/
                        val broadcasterMuteChat = rtmRoomMuteState.muteChat?.broadcaster
                        broadcasterMuteChat?.let {
                            roomStatus.isStudentChatAllowed =
                                    broadcasterMuteChat.toFloat().toInt() == EduChatState.Allow.value
                        }
                        /**
                         * roomStatus中仅定义了isStudentChatAllowed来标识是否全员禁聊；
                         * 没有属性来标识是否全员禁摄像头和麦克风；
                         * 需要确定
                         * */
                    }
                    RoomType.LARGE_CLASS -> {
                        /**判断本次更改是否包含针对学生的全部禁聊;*/
                        val audienceMuteChat = rtmRoomMuteState.muteChat?.audience
                        audienceMuteChat?.let {
                            roomStatus.isStudentChatAllowed =
                                    audienceMuteChat.toFloat().toInt() == EduChatState.Allow.value
                        }
                        val broadcasterMuteChat = rtmRoomMuteState.muteChat?.broadcaster
                        broadcasterMuteChat?.let {
                            roomStatus.isStudentChatAllowed =
                                    broadcasterMuteChat.toFloat().toInt() == EduChatState.Allow.value
                        }
                    }
                }
                val operator = Convert.convertUserInfo(rtmRoomMuteState.operator, eduRoom.getCurRoomType())
                cmdCallbackManager.onRoomStatusChanged(EduRoomChangeType.AllStudentsChat, operator, eduRoom)
            }
            CMDId.RoomPropertyChanged.value -> {
                AgoraLog.i("$TAG->Received RTM message for roomProperty change:${text}")
                val propertyChangeEvent = Gson().fromJson<CMDResponseBody<CMDRoomPropertyRes>>(
                        text, object : TypeToken<CMDResponseBody<CMDRoomPropertyRes>>() {}.type).data
                /**把变化(update or delete)的属性更新到本地*/
                CMDDataMergeProcessor.updateRoomProperties(eduRoom, propertyChangeEvent)
                /**通知用户房间属性发生改变*/
                AgoraLog.i("$TAG->Callback the received roomProperty to upper layer")
                cmdCallbackManager.onRoomPropertyChanged(eduRoom, propertyChangeEvent.cause)
            }
            CMDId.RoomPropertiesChanged.value -> {
                AgoraLog.i("$TAG->Received RTM message for roomProperties change:${text}")
                val propertyChangeEvent = Gson().fromJson<CMDResponseBody<CMDRoomPropertyRes>>(
                        text, object : TypeToken<CMDResponseBody<CMDRoomPropertyRes>>() {}.type).data
                /**把变化(update or delete)的属性更新到本地*/
                CMDDataMergeProcessor.updateRoomProperties2(eduRoom, propertyChangeEvent)
                /**通知用户房间属性发生改变*/
                AgoraLog.i("$TAG->Callback the received roomProperties to upper layer")
                cmdCallbackManager.onRoomPropertyChanged(eduRoom, propertyChangeEvent.cause)
            }
            CMDId.ChannelMsgReceived.value -> {
                /**频道内的聊天消息*/
                AgoraLog.i("$TAG->Receive channel chat message")
                val eduMsg = CMDUtil.buildEduMsg(text, eduRoom) as EduChatMsg
                AgoraLog.i("$TAG->Build eduChatMsg1:${Gson().toJson(eduMsg)}")
                if (eduMsg.fromUser.userUuid == (eduRoom as EduRoomImpl).getCurLocalUserInfo().userUuid) {
                    AgoraLog.i("$TAG->In channel msg sent by localUser，auto shield1")
                } else {
                    AgoraLog.i("$TAG->In channel msg sent by remoteUser，callback to upper layer1")
                    cmdCallbackManager.onRoomChatMessageReceived(eduMsg, eduRoom)
                }
            }
            CMDId.ChannelCustomMsgReceived.value -> {
                /**频道内自定义消息(可以是用户的自定义的信令)*/
                AgoraLog.i("$TAG->Receive channel custom message")
                val eduMsg = CMDUtil.buildEduMsg(text, eduRoom)
                AgoraLog.i("$TAG->Build eduMsg2:${Gson().toJson(eduMsg)}")
                if (eduMsg.fromUser.userUuid == (eduRoom as EduRoomImpl).getCurLocalUserInfo().userUuid) {
                    AgoraLog.i("$TAG->In channel msg sent by localUser，auto shield2")
                } else {
                    AgoraLog.i("$TAG->In channel msg sent by remoteUser，callback to upper layer2")
                    cmdCallbackManager.onRoomMessageReceived(eduMsg, eduRoom)
                }
            }
            CMDId.UserJoinOrLeave.value -> {
                val rtmInOutMsg = Gson().fromJson<CMDResponseBody<RtmUserInOutMsg>>(text, object :
                        TypeToken<CMDResponseBody<RtmUserInOutMsg>>() {}.type).data
                AgoraLog.i("$TAG->Receive RTM of user online or offline->" +
                        "${(eduRoom as EduRoomImpl).getCurRoomUuid()}:${text}")
                Log.w(TAG, " -- get user join leave message!")
                if (rtmInOutMsg.onlineUsers == null) {
                    rtmInOutMsg.onlineUsers = mutableListOf<OnlineUserInfo>()
                }
                /**根据回调数据，维护本地存储的流列表，并返回有效数据(可能同时包含local和remote数据)*/
                val validOnlineUsers = CMDDataMergeProcessor.addUserWithOnline(rtmInOutMsg.onlineUsers,
                        eduRoom.getCurUserList(), eduRoom.getCurRoomType())
                val validOfflineUsers = CMDDataMergeProcessor.removeUserWithOffline(rtmInOutMsg.offlineUsers,
                        eduRoom.getCurUserList(), eduRoom.getCurRoomType())

                /**从online和offline数据中剥离出本地用户的数据*/
                val validOnlineLocalUser = CMDProcessor.filterLocalUserInfo(
                        eduRoom.getCurLocalUserInfo(), validOnlineUsers)
                Log.w(TAG, " -- start fetch user offline status!")
                val validOfflineLocalUser = CMDProcessor.filterLocalUserEvent(
                        eduRoom.getCurLocalUserInfo(), validOfflineUsers)
                Log.w(TAG, " -- start fetch user offline status:" + validOfflineUsers.size)

                /**提取出online和offline携带的流信息(可能同时包含local和remote数据)*/
                val validAddedStreams = CMDDataMergeProcessor.addStreamWithUserOnline(rtmInOutMsg.onlineUsers,
                        eduRoom.getCurStreamList(), eduRoom.getCurRoomType())
                val validRemovedStreams = CMDDataMergeProcessor.removeStreamWithUserOffline(rtmInOutMsg.offlineUsers,
                        eduRoom.getCurStreamList(), eduRoom.getCurRoomType())

                /**从有效的流数据中剥离出本地用户的流数据*/
                val validAddedLocalStream = CMDProcessor.filterLocalStreamInfo(
                        eduRoom.getCurLocalUserInfo(), validAddedStreams)
                val validRemovedLocalStream = CMDProcessor.filterLocalStreamInfo(
                        eduRoom.getCurLocalUserInfo(), validRemovedStreams)

                if (validOnlineUsers.size > 0) {
                    AgoraLog.i("$TAG->onRemoteUsersJoined:${Gson().toJson(validOnlineUsers)}")
                    cmdCallbackManager.onRemoteUsersJoined(validOnlineUsers, eduRoom)
                }
                if (validAddedStreams.size > 0) {
                    AgoraLog.i("$TAG->onRemoteStreamsAdded:${Gson().toJson(validAddedStreams)}")
                    cmdCallbackManager.onRemoteStreamsAdded(validAddedStreams, eduRoom)
                }
                validOnlineLocalUser?.let {
                    AgoraLog.i("$TAG->onLocalUserAdded:${Gson().toJson(it)}")
                    cmdCallbackManager.onLocalUserAdded(it, eduRoom.getCurLocalUser())
                }
                validOfflineLocalUser?.let {
                    AgoraLog.i("$TAG->onLocalUserRemoved:${Gson().toJson(it)}")
                    Log.w(TAG, " -- get user offline message!")

                    cmdCallbackManager.onLocalUserRemoved(it, eduRoom.getCurLocalUser(), rtmInOutMsg.offlineUsers.find { item ->
                        item.userUuid == eduRoom.getCurLocalUser().userInfo.userUuid
                    }!!.type)
                }
                if(validOfflineUsers.size > 0) {
                    AgoraLog.i("$TAG->onRemoteUsersLeft:${Gson().toJson(validOfflineUsers)}")
                    cmdCallbackManager.onRemoteUsersLeft(validOfflineUsers, eduRoom)
                }
                if (validRemovedStreams.size > 0) {
                    AgoraLog.i("$TAG->onRemoteStreamsRemoved:${Gson().toJson(validRemovedStreams)}")
                    cmdCallbackManager.onRemoteStreamsRemoved(validRemovedStreams, eduRoom)
                }
                validAddedLocalStream?.let {
                    AgoraLog.i("$TAG->错误onLocalStreamAdded:${Gson().toJson(it)}")
                    cmdCallbackManager.onLocalStreamAdded(it, eduRoom.getCurLocalUser())
                }
                validRemovedLocalStream?.let {
                    AgoraLog.i("$TAG->onLocalStreamRemoved:${Gson().toJson(it)}")
                    cmdCallbackManager.onLocalStreamRemoved(it, eduRoom.getCurLocalUser())
                }
            }
            CMDId.UserStateChange.value -> {
                val cmdUserStateMsg = Gson().fromJson<CMDResponseBody<CMDUserStateMsg>>(text, object :
                        TypeToken<CMDResponseBody<CMDUserStateMsg>>() {}.type).data
                val changeEvents = CMDDataMergeProcessor.updateUserWithUserStateChange(cmdUserStateMsg,
                        (eduRoom as EduRoomImpl).getCurUserList(), eduRoom.getCurRoomType())

                /**判断有效的数据中是否有本地用户的数据,有则处理并回调*/
                val iterable = changeEvents.iterator()
                while (iterable.hasNext()) {
                    val element = iterable.next()
                    val event = element.event
                    if (event.modifiedUser.userUuid == eduRoom.getCurLocalUserInfo().userUuid) {
                        AgoraLog.e(TAG, "onLocalUserUpdated:${event.modifiedUser.userUuid}")
                        cmdCallbackManager.onLocalUserUpdated(EduUserEvent(event.modifiedUser,
                                event.operatorUser), element.type, eduRoom.getCurLocalUser())
                        iterable.remove()
                    }
                }
                /**把剩余的远端数据回调出去*/
                changeEvents.forEach {
                    cmdCallbackManager.onRemoteUserUpdated(it.event, it.type, eduRoom)
                }
            }
//            CMDId.UserPropertyChanged.value -> {
//                AgoraLog.e(TAG, "Receive RTM of userProperty change: ${text}")
//                val cmdUserPropertyRes = Gson().fromJson<CMDResponseBody<CMDUserPropertyRes>>(text, object :
//                        TypeToken<CMDResponseBody<CMDUserPropertyRes>>() {}.type).data
//                val updatedUserInfo = CMDDataMergeProcessor.updateUserPropertyWithChange(cmdUserPropertyRes,
//                        (eduRoom as EduRoomImpl).getCurUserList())
//                updatedUserInfo?.let {
//                    if (updatedUserInfo == eduRoom.getCurLocalUserInfo()) {
//                        cmdCallbackManager.onLocalUserPropertyUpdated(it, cmdUserPropertyRes.cause,
//                                eduRoom.getCurLocalUser())
//                    } else {
//                        /**远端用户property发生改变如何回调出去*/
//                        cmdCallbackManager.onRemoteUserPropertiesUpdated(it, eduRoom,
//                                cmdUserPropertyRes.cause)
//                    }
//                }
//            }
            CMDId.StreamStateChange.value, CMDId.StreamsStateChange.value -> {
                var cmdStreamsActionMsg: CMDStreamsActionMsg? = null
                if (cmdResponseBody.cmd == CMDId.StreamStateChange.value) {
                    val cmdStreamActionMsg = Gson().fromJson<CMDResponseBody<CMDStreamActionMsg>>(text, object :
                            TypeToken<CMDResponseBody<CMDStreamActionMsg>>() {}.type).data
                    val streams: MutableList<CMDStreamRes> = mutableListOf()
                    val operator = cmdStreamActionMsg.operator
                    val streamRes = Convert.convertCMDStreamRes(cmdStreamActionMsg)
                    streams.add(streamRes)
                    cmdStreamsActionMsg = CMDStreamsActionMsg(streams, operator)
                } else {
                    cmdStreamsActionMsg = Gson().fromJson<CMDResponseBody<CMDStreamsActionMsg>>(text, object :
                            TypeToken<CMDResponseBody<CMDStreamsActionMsg>>() {}.type).data
                }
                val operator = Convert.convertUserInfo(cmdStreamsActionMsg.operator,
                        (eduRoom as EduRoomImpl).getCurRoomType())
                val validAddedStreams = mutableListOf<EduStreamEvent>()
                val validUpdatedStreams = mutableListOf<EduStreamEvent>()
                val validRemovedStreams = mutableListOf<EduStreamEvent>()
                /**根据回调数据，维护本地存储的流列表;并获取有效的数据*/
                cmdStreamsActionMsg?.streams?.forEach {
                    when (it.action) {
                        CMDStreamAction.Add.value -> {
                            val streamInfo = CMDDataMergeProcessor.addStreamWithAction(it,
                                    eduRoom.getCurStreamList(), eduRoom.getCurRoomType())
                            streamInfo?.let { stream ->
                                val event = EduStreamEvent(stream, operator)
                                validAddedStreams.add(event)
                            }
                        }
                        CMDStreamAction.Modify.value -> {
                            val streamInfo = CMDDataMergeProcessor.updateStreamWithAction(it,
                                    eduRoom.getCurStreamList(), eduRoom.getCurRoomType())
                            streamInfo?.let { stream ->
                                val event = EduStreamEvent(stream, operator)
                                validUpdatedStreams.add(event)
                            }
                        }
                        CMDStreamAction.Remove.value -> {
                            val streamInfo = CMDDataMergeProcessor.removeStreamWithAction(it,
                                    eduRoom.getCurStreamList(), eduRoom.getCurRoomType())
                            streamInfo?.let { stream ->
                                val event = EduStreamEvent(stream, operator)
                                validRemovedStreams.add(event)
                            }
                        }
                    }
                }
                /**检测并处理本地数据*/
                val validAddedLocalStreams = mutableListOf<EduStreamEvent>()
                val validUpdatedLocalStreams = mutableListOf<EduStreamEvent>()
                val validRemovedLocalStreams = mutableListOf<EduStreamEvent>()
                if (validAddedStreams.size > 0) {
                    AgoraLog.e(TAG, "Effective newly added streams is: ${Gson().toJson(validAddedStreams)}")
                    val iterable = validAddedStreams.iterator()
                    while (iterable.hasNext()) {
                        val element = iterable.next()
                        val streamInfo = element.modifiedStream
                        if (streamInfo.publisher == eduRoom.getCurLocalUserInfo()) {
                            RteEngineImpl.updateLocalStream(streamInfo.hasAudio, streamInfo.hasVideo)
                            RteEngineImpl.setClientRole(eduRoom.getCurRoomUuid(),
                                    Constants.CLIENT_ROLE_BROADCASTER)
                            RteEngineImpl.publish(eduRoom.getCurRoomUuid())
                            val event = EduStreamEvent(streamInfo, operator)
                            validAddedLocalStreams.add(event)
                            iterable.remove()
                        }
                    }
                }
                if (validUpdatedStreams.size > 0) {
                    AgoraLog.e(TAG, "Effective updated streams is: ${Gson().toJson(validUpdatedStreams)}")
                    val iterable = validUpdatedStreams.iterator()
                    while (iterable.hasNext()) {
                        val element = iterable.next()
                        val streamInfo = element.modifiedStream
                        if (streamInfo.publisher == eduRoom.getCurLocalUserInfo()) {
                            RteEngineImpl.updateLocalStream(streamInfo.hasAudio, streamInfo.hasVideo)
                            RteEngineImpl.setClientRole(eduRoom.getCurRoomUuid(),
                                    Constants.CLIENT_ROLE_BROADCASTER)
                            RteEngineImpl.publish(eduRoom.getCurRoomUuid())
                            val event = EduStreamEvent(streamInfo, operator)
                            validUpdatedLocalStreams.add(event)
                            iterable.remove()
                        }
                    }
                }
                if (validRemovedStreams.size > 0) {
                    AgoraLog.e(TAG, "Effective deleted streams is: ${Gson().toJson(validRemovedStreams)}")
                    val iterable = validRemovedStreams.iterator()
                    while (iterable.hasNext()) {
                        val element = iterable.next()
                        val streamInfo = element.modifiedStream
                        if (streamInfo.publisher == eduRoom.getCurLocalUserInfo()) {
                            RteEngineImpl.updateLocalStream(streamInfo.hasAudio, streamInfo.hasVideo)
                            RteEngineImpl.unpublish(eduRoom.getCurRoomUuid())
                            val event = EduStreamEvent(streamInfo, operator)
                            validRemovedLocalStreams.add(event)
                            iterable.remove()
                        }
                    }
                }
                /**回调有效数据*/
                if (validAddedLocalStreams.size > 0) {
                    AgoraLog.e(TAG, "Callback the newly added localStream to upper layer")
                    AgoraLog.e(TAG, "错误onLocalStreamAdded:${Gson().toJson(validAddedLocalStreams)}")
                    validAddedLocalStreams.forEach {
                        cmdCallbackManager.onLocalStreamAdded(it, eduRoom.getCurLocalUser())
                    }
                }
                if (validAddedStreams.size > 0) {
                    AgoraLog.e(TAG, "Callback the newly added remoteStream to upper layer")
                    cmdCallbackManager.onRemoteStreamsAdded(validAddedStreams, eduRoom)
                }
                if (validUpdatedLocalStreams.size > 0) {
                    AgoraLog.e(TAG, "Callback the updated localStream to upper layer")
                    validUpdatedLocalStreams.forEach {
                        cmdCallbackManager.onLocalStreamUpdated(it, eduRoom.getCurLocalUser())
                    }
                }
                if (validUpdatedStreams.size > 0) {
                    AgoraLog.e(TAG, "Callback the updated remoteStream to upper layer")
                    cmdCallbackManager.onRemoteStreamsUpdated(validUpdatedStreams, eduRoom)
                }
                if (validRemovedLocalStreams.size > 0) {
                    AgoraLog.e(TAG, "Callback the deleted localStream to upper layer")
                    validRemovedLocalStreams.forEach {
                        cmdCallbackManager.onLocalStreamRemoved(it, eduRoom.getCurLocalUser())
                    }
                }
                if (validRemovedStreams.size > 0) {
                    AgoraLog.e(TAG, "Callback the deleted remoteStream to upper layer")
                    cmdCallbackManager.onRemoteStreamsRemoved(validRemovedStreams, eduRoom)
                }
            }
            CMDId.BoardRoomStateChange.value -> {
            }
            CMDId.BoardUserStateChange.value -> {
            }
        }
    }

    fun dispatchPeerMsg(text: String, listener: EduManagerEventListener?) {
        val cmdResponseBody = Gson().fromJson<CMDResponseBody<Any>>(text, object :
                TypeToken<CMDResponseBody<Any>>() {}.type)
        when (cmdResponseBody.cmd) {
            CMDId.PeerMsgReceived.value -> {
                /**点对点消息*/
                val eduMsg = CMDUtil.buildEduMsg(text, eduRoom) as EduChatMsg
                cmdCallbackManager.onUserChatMessageReceived(eduMsg, listener)
            }
//            CMDId.ActionMsgReceived.value -> {
//                /**邀请申请动作消息*/
//                val actionMsg = Convert.convertEduActionMsg(text)
//                cmdCallbackManager.onUserActionMessageReceived(actionMsg, listener)
//            }
            CMDId.PeerCustomMsgReceived.value -> {
                /**点对点的自定义消息(可以是用户自定义的信令)*/
                val eduMsg = CMDUtil.buildEduMsg(text, eduRoom)
                cmdCallbackManager.onUserMessageReceived(eduMsg, listener)
            }
        }
    }

}