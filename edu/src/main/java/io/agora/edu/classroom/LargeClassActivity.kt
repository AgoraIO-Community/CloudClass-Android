package io.agora.edu.classroom

import android.os.Bundle
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import com.herewhite.sdk.domain.SDKError
import io.agora.edu.launch.AgoraEduCourseware
import io.agora.edu.launch.AgoraEduSDK
import io.agora.education.api.base.EduError
import io.agora.education.api.message.EduChatMsg
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.EduRoomChangeType
import io.agora.education.api.stream.data.EduStreamEvent
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.user.EduStudent
import io.agora.education.api.user.data.*
import io.agora.educontext.EduContextError
import io.agora.educontext.WhiteboardDrawingConfig
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcChannel
import io.agora.uikit.impl.container.AgoraContainerType
import io.agora.uikit.interfaces.protocols.AgoraUIContainer

class LargeClassActivity : BaseClassActivity() {
    private val tag = "LargeClassActivity"

    private val whiteBoardManagerEventListener = object : WhiteBoardManagerEventListener {
        override fun onWhiteBoardJoinSuccess(config: WhiteboardDrawingConfig) {
            getReporter().reportWhiteBoardResult("1", null, null)
            setWhiteboardJoinSuccess()
            checkProcessSuccess()
        }

        override fun onWhiteBoardJoinFail(error: SDKError?) {
            if (error != null) {
                container?.showError(EduContextError(-1, error.toString()))
            }
            getReporter().reportWhiteBoardResult("0", "White board join room fail", null)
        }

        override fun onGrantedChanged() {
            userListManager?.notifyUserList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        contentLayout?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (contentLayout!!.width > 0 && contentLayout!!.height > 0) {
                    contentLayout!!.viewTreeObserver.removeOnGlobalLayoutListener(this)

                    container = AgoraUIContainer.create(contentLayout!!,
                            0, 0, contentLayout!!.width,
                            contentLayout!!.height,
                            AgoraContainerType.LargeClass, eduContext)

                    whiteboardContext.getHandlers()?.forEach {
                        it.getBoardContainer()?.let { viewGroup ->
                            whiteBoardContainer = viewGroup
                            whiteBoardManager = WhiteBoardManager(this@LargeClassActivity,
                                    launchConfig!!, viewGroup, whiteboardContext)
                            return@forEach
                        }
                    }

                    whiteBoardManager?.let {
                        launchConfig?.let { config ->
                            var ware: AgoraEduCourseware? = null
                            if (AgoraEduSDK.COURSEWARES.size > 0) {
                                ware = AgoraEduSDK.COURSEWARES[0]
                            }
                            it.initData(config.roomUuid, config.whiteBoardAppId,
                                    preCheckData?.board?.boardRegion, ware)
                        }
                        it.whiteBoardManagerEventListener = whiteBoardManagerEventListener
                    }
                }
            }
        })
    }

    override fun onContentViewLayout(): ViewGroup {
        contentLayout = FrameLayout(this)
        return contentLayout!!
    }

    override fun onRoomJoinConfig(): JoinRoomConfiguration {
        return JoinRoomConfiguration(
                autoPublish = false,
                autoSubscribe = true,
                needUserListener = true)
    }

    override fun onRoomJoined(success: Boolean, student: EduStudent?, error: EduError?) {

    }

    override fun onDestroy() {
        super.onDestroy()
        teacherVideoManager?.dispose()
        screenShareManager?.dispose()
        userListManager?.dispose()
        handsUpManager?.dispose()
        roomStatusManager?.dispose()
    }

    override fun onRemoteUsersInitialized(users: List<EduUserInfo>, classRoom: EduRoom) {
        super.onRemoteUsersInitialized(users, classRoom)
        whiteBoardManager!!.initBoardWithRoomToken(preCheckData!!.board.boardId,
                preCheckData!!.board.boardToken, launchConfig!!.userUuid)
    }

    override fun onRemoteUsersJoined(users: List<EduUserInfo>, classRoom: EduRoom) {
        super.onRemoteUsersJoined(users, classRoom)
        userListManager?.notifyUserList()
    }

    override fun onRemoteUserUpdated(userEvent: EduUserEvent, type: EduUserStateChangeType, classRoom: EduRoom) {
        super.onRemoteUserUpdated(userEvent, type, classRoom)
    }

    override fun onRemoteUserLeft(userEvent: EduUserEvent, classRoom: EduRoom) {
        super.onRemoteUserLeft(userEvent, classRoom)
        userListManager?.notifyUserList()
    }

    override fun onRoomChatMessageReceived(chatMsg: EduChatMsg, classRoom: EduRoom) {
        super.onRoomChatMessageReceived(chatMsg, classRoom)
        chatManager?.receiveRemoteChatMessage(chatMsg)
    }

    override fun onRemoteStreamsInitialized(streams: List<EduStreamInfo>, classRoom: EduRoom) {
        super.onRemoteStreamsInitialized(streams, classRoom)
        screenShareManager?.checkAndNotifyScreenShareRestored()
    }

    override fun onRemoteStreamsAdded(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        super.onRemoteStreamsAdded(streamEvents, classRoom)
        teacherVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
        screenShareManager?.checkAndNotifyScreenShareStarted(streamEvents)
        if (streamEvents.find { it.modifiedStream.publisher.role == EduUserRole.TEACHER } == null) {
            userListManager?.notifyUserList()
        }
    }

    override fun onRemoteStreamUpdated(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        super.onRemoteStreamUpdated(streamEvents, classRoom)
        teacherVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
        screenShareManager?.checkAndNotifyScreenShareStarted(streamEvents)
        if (streamEvents.find { it.modifiedStream.publisher.role == EduUserRole.TEACHER } == null) {
            userListManager?.notifyUserList()
        }
    }

    override fun onRemoteStreamsRemoved(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        super.onRemoteStreamsRemoved(streamEvents, classRoom)
        teacherVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
        screenShareManager?.checkAndNotifyScreenShareRemoved(streamEvents)
        if (streamEvents.find { it.modifiedStream.publisher.role == EduUserRole.TEACHER } == null) {
            userListManager?.notifyUserList()
        }
    }

    override fun onLocalStreamAdded(streamEvent: EduStreamEvent) {
        super.onLocalStreamAdded(streamEvent)
        userListManager?.addLocalStream(streamEvent)
    }

    override fun onLocalStreamUpdated(streamEvent: EduStreamEvent) {
        super.onLocalStreamUpdated(streamEvent)
        userListManager?.updateLocalStream(streamEvent)
        userListManager?.notifyUserList()
    }

    override fun onLocalStreamRemoved(streamEvent: EduStreamEvent) {
        super.onLocalStreamRemoved(streamEvent)
        userListManager?.removeLocalStream(streamEvent)
        userListManager?.notifyUserList()
    }

    override fun onRoomStatusChanged(type: EduRoomChangeType, operatorUser: EduUserInfo?, classRoom: EduRoom) {
        super.onRoomStatusChanged(type, operatorUser, classRoom)
        roomStatusManager?.updateClassState(type)
    }

    override fun onRoomPropertiesChanged(classRoom: EduRoom, cause: MutableMap<String, Any>?) {
        super.onRoomPropertiesChanged(classRoom, cause)
        handsUpManager?.notifyHandsUpEnable(cause)
        handsUpManager?.notifyHandsUpState(cause)
        userListManager?.notifyListByPropertiesChanged(cause)
    }

    override fun onRemoteVideoStateChanged(rtcChannel: RtcChannel?, uid: Int, state: Int, reason: Int, elapsed: Int) {
        super.onRemoteVideoStateChanged(rtcChannel, uid, state, reason, elapsed)
        teacherVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
    }

    override fun onLocalVideoStateChanged(localVideoState: Int, error: Int) {
        super.onLocalVideoStateChanged(localVideoState, error)
        teacherVideoManager?.updateLocalCameraState(localVideoState)
        userListManager?.updateLocalCameraState(localVideoState)
    }

    override fun onAudioVolumeIndicationOfLocalSpeaker(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?, totalVolume: Int) {
        super.onAudioVolumeIndicationOfLocalSpeaker(speakers, totalVolume)
        teacherVideoManager?.updateAudioVolume(speakers)
        userListManager?.updateAudioVolumeIndication(speakers)

    }

    override fun onAudioVolumeIndicationOfRemoteSpeaker(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?, totalVolume: Int) {
        super.onAudioVolumeIndicationOfRemoteSpeaker(speakers, totalVolume)
        teacherVideoManager?.updateAudioVolume(speakers)
        userListManager?.updateAudioVolumeIndication(speakers)
    }

    override fun onRemoteUserPropertiesChanged(classRoom: EduRoom, userInfo: EduUserInfo, cause: MutableMap<String, Any>?) {
        super.onRemoteUserPropertiesChanged(classRoom, userInfo, cause)
        teacherVideoManager?.updateRemoteCameraState(userInfo, cause)
        teacherVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
        userListManager?.updateRemoteCameraState(userInfo, cause)
    }
}