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
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.api.user.data.EduUserRole
import io.agora.educontext.*
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcChannel
import io.agora.uikit.impl.container.AgoraContainerType
import io.agora.uikit.interfaces.protocols.AgoraUIContainer

class OneToOneClassActivity : BaseClassActivity() {
    private val tag = "OneToOneClassActivity"

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
            // notify student`s grantedIc
            oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.STUDENT)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        contentLayout?.viewTreeObserver?.addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (contentLayout!!.width > 0 && contentLayout!!.height > 0) {
                            contentLayout!!.viewTreeObserver.removeOnGlobalLayoutListener(this)

                            container = AgoraUIContainer.create(
                                    contentLayout!!,
                                    0, 0,
                                    contentLayout!!.width,
                                    contentLayout!!.height,
                                    AgoraContainerType.OneToOne,
                                    eduContext)

                            whiteboardContext.getHandlers()?.forEach {
                                it.getBoardContainer()?.let { viewGroup ->
                                    whiteBoardContainer = viewGroup
                                    whiteBoardManager = WhiteBoardManager(this@OneToOneClassActivity,
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

    override fun onRoomJoinConfig(): JoinRoomConfiguration {
        return JoinRoomConfiguration(
                autoPublish = true,
                autoSubscribe = true,
                needUserListener = true)
    }

    override fun onContentViewLayout(): ViewGroup {
        contentLayout = FrameLayout(this)
        return contentLayout!!
    }

    override fun onRoomJoined(success: Boolean, student: EduStudent?, error: EduError?) {

    }

    override fun onRemoteUsersInitialized(users: List<EduUserInfo>, classRoom: EduRoom) {
        super.onRemoteUsersInitialized(users, classRoom)
        whiteBoardManager!!.initBoardWithRoomToken(preCheckData!!.board.boardId,
                preCheckData!!.board.boardToken, launchConfig!!.userUuid)
    }

    override fun onRoomChatMessageReceived(chatMsg: EduChatMsg, classRoom: EduRoom) {
        super.onRoomChatMessageReceived(chatMsg, classRoom)
        val item = EduContextChatItem(
                chatMsg.fromUser.userName ?: "",
                chatMsg.fromUser.userUuid ?: "",
                chatMsg.fromUser.role?.value ?: EduContextUserRole.Student.value,
                chatMsg.message,
                chatMsg.messageId,
                EduContextChatItemType.Text,
                EduContextChatSource.Remote,
                EduContextChatState.Success,
                chatMsg.timestamp)

        eduContext.chatContext()?.getHandlers()?.forEach { handler ->
            handler.onReceiveMessage(item)
        }
    }

    override fun onRemoteStreamsInitialized(streams: List<EduStreamInfo>, classRoom: EduRoom) {
        super.onRemoteStreamsInitialized(streams, classRoom)
        screenShareManager?.checkAndNotifyScreenShareRestored()
    }

    override fun onRemoteStreamsAdded(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        super.onRemoteStreamsAdded(streamEvents, classRoom)
        oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
        screenShareManager?.checkAndNotifyScreenShareStarted(streamEvents)
    }

    override fun onRemoteStreamUpdated(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        super.onRemoteStreamUpdated(streamEvents, classRoom)
        oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
        screenShareManager?.checkAndNotifyScreenShareStarted(streamEvents)
    }

    override fun onRemoteStreamsRemoved(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        super.onRemoteStreamsRemoved(streamEvents, classRoom)
        oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
        screenShareManager?.checkAndNotifyScreenShareRemoved(streamEvents)
    }

    override fun onLocalStreamAdded(streamEvent: EduStreamEvent) {
        super.onLocalStreamAdded(streamEvent)
        oneToOneVideoManager?.addLocalStream(streamEvent)
        oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.STUDENT)
    }

    override fun onLocalStreamUpdated(streamEvent: EduStreamEvent) {
        super.onLocalStreamUpdated(streamEvent)
        oneToOneVideoManager?.updateLocalStream(streamEvent)
        oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.STUDENT)
    }

    override fun onLocalStreamRemoved(streamEvent: EduStreamEvent) {
        super.onLocalStreamRemoved(streamEvent)
        oneToOneVideoManager?.removeLocalStream(streamEvent)
    }

    override fun onRoomStatusChanged(type: EduRoomChangeType, operatorUser: EduUserInfo?, classRoom: EduRoom) {
        super.onRoomStatusChanged(type, operatorUser, classRoom)
        roomStatusManager?.updateClassState(type)
    }

    override fun onRemoteVideoStateChanged(rtcChannel: RtcChannel?, uid: Int, state: Int, reason: Int, elapsed: Int) {
        super.onRemoteVideoStateChanged(rtcChannel, uid, state, reason, elapsed)
        oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
    }

    override fun onLocalVideoStateChanged(localVideoState: Int, error: Int) {
        super.onLocalVideoStateChanged(localVideoState, error)
        oneToOneVideoManager?.updateLocalCameraState(localVideoState)
    }

    override fun onAudioVolumeIndicationOfLocalSpeaker(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?, totalVolume: Int) {
        super.onAudioVolumeIndicationOfLocalSpeaker(speakers, totalVolume)
        oneToOneVideoManager?.updateAudioVolume(speakers)
    }

    override fun onAudioVolumeIndicationOfRemoteSpeaker(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?, totalVolume: Int) {
        super.onAudioVolumeIndicationOfRemoteSpeaker(speakers, totalVolume)
        oneToOneVideoManager?.updateAudioVolume(speakers)
    }

    override fun onRemoteUserPropertiesChanged(classRoom: EduRoom, userInfo: EduUserInfo, cause: MutableMap<String, Any>?) {
        super.onRemoteUserPropertiesChanged(classRoom, userInfo, cause)
        oneToOneVideoManager?.updateRemoteCameraState(userInfo, cause)
        oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
    }
}