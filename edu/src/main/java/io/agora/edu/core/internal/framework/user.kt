package io.agora.edu.core.internal.framework

import android.view.ViewGroup
import io.agora.edu.core.internal.education.api.room.data.EduRoomState
import io.agora.edu.core.internal.education.api.stream.data.*
import io.agora.edu.core.internal.framework.data.*
import io.agora.edu.core.internal.rte.data.RteLocalVideoStats
import io.agora.edu.core.internal.rte.data.RteRemoteVideoStats
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcChannel

enum class EduUserRole(var value: Int) {
    EduRoleTypeInvalid(0),
    TEACHER(1),
    STUDENT(2),
    ASSISTANT(3);
}

/**
 * The information that all users should have
 */
open class EduBaseUserInfo(
        val userUuid: String,
        val userName: String,
        val role: EduUserRole) {

    override fun equals(other: Any?): Boolean {
        (other as? EduBaseUserInfo)?.let {
            return (other.userUuid == this.userUuid &&
                    other.userName == this.userName &&
                    other.role == this.role)
        }

        return false
    }

    fun copy(): EduBaseUserInfo {
        return EduBaseUserInfo(this.userUuid, this.userName, this.role)
    }

    override fun hashCode(): Int {
        var result = userUuid.hashCode()
        result = 31 * result + userName.hashCode()
        result = 31 * result + role.hashCode()
        return result
    }
}

/**
 * The information a user should have when he has joined a room
 */
open class EduUserInfo(
        userUuid: String,
        userName: String,
        role: EduUserRole,
        var isChatAllowed: Boolean,
        var streamUuid: String = "",
        var userProperties: MutableMap<String, Any> = mutableMapOf()
) : EduBaseUserInfo(userUuid, userName, role)

/**
 * The information that only local user should have
 */
open class EduLocalUserInfo(
        userUuid: String,
        userName: String,
        role: EduUserRole,
        isChatAllowed: Boolean,
        var userToken: String = "",
        @Transient var streams: MutableList<EduStreamEvent>
) : EduUserInfo(userUuid, userName, role, isChatAllowed)

enum class EduChatState(var value: Int) {
    NotAllow(1),
    Allow(0)
}

enum class EduUserStateChangeType {
    Chat,
}

enum class EduUserLeftType(var value: Int) {
    Normal(1),
    KickOff(2)
}

/**
 * Callback return codes for user APIs:
 * 1: illegal arguments
 * 201: rtm or rtc error code
 * 301 :networking error
 */
interface EduLocalUser {
    var userInfo: EduLocalUserInfo

    var videoEncoderConfig: EduVideoEncoderConfig

    var eventListener: EduUserEventListener?

    var cachedRemoteVideoStates: MutableMap<String, Int>

    var cachedRemoteAudioStates: MutableMap<String, Int>

    var cacheRemoteOnlineUserIds: MutableList<String>

    fun initOrUpdateLocalStream(options: LocalStreamInitOptions, callback: EduCallback<EduStreamInfo>)

    fun switchCamera(): EduError?

    fun subscribeStream(stream: EduStreamInfo, options: StreamSubscribeOptions, callback: EduCallback<Unit>)

    fun unSubscribeStream(stream: EduStreamInfo, options: StreamSubscribeOptions, callback: EduCallback<Unit>)

    fun publishStream(stream: EduStreamInfo, callback: EduCallback<Boolean>)

    fun muteStream(stream: EduStreamInfo, callback: EduCallback<Boolean>)

    fun unPublishStream(stream: EduStreamInfo, callback: EduCallback<Boolean>)

    fun sendRoomMessage(message: String, callback: EduCallback<EduMessage>)

    fun sendUserMessage(message: String, user: EduUserInfo, callback: EduCallback<EduMessage>)

    fun sendRoomChatMessage(message: String, callback: EduCallback<EduChatMessage>)

    fun sendUserChatMessage(message: String, remoteUser: EduUserInfo, callback: EduCallback<EduChatMessage>)

    fun startActionWithConfig(config: EduActionConfig, callback: EduCallback<Unit>)

    fun stopActionWithConfig(config: EduActionConfig, callback: EduCallback<Unit>)

    fun setStreamView(stream: EduStreamInfo, channelId: String, viewGroup: ViewGroup?,
                      configEdu: EduRenderConfig = EduRenderConfig(), top: Boolean = false): EduError

    fun setStreamView(stream: EduStreamInfo, channelId: String, viewGroup: ViewGroup?, top: Boolean = false): EduError

    fun setRoomProperties(properties: MutableMap<String, Any>,
                          cause: MutableMap<String, String>, callback: EduCallback<Unit>)

    fun removeRoomProperties(properties: MutableList<String>,
                             cause: MutableMap<String, String>, callback: EduCallback<Unit>)

    fun resetVideoEncoderConfig(videoEncoderConfig: EduVideoEncoderConfig): EduError

    fun startAudioMixing(filepath: String, loopback: Boolean, replace: Boolean, cycle: Int)

    fun stopAudioMixing()

    fun setAudioMixingPosition(position: Int)
}

interface EduAssistant : EduLocalUser {
    fun setEventListener(eventListener: EduAssistantEventListener?)

    fun createOrUpdateTeacherStream(streamInfo: EduStreamInfo, callback: EduCallback<Unit>)

    fun createOrUpdateStudentStream(streamInfo: EduStreamInfo, callback: EduCallback<Unit>)
}

interface EduTeacher : EduLocalUser {
    fun setEventListener(eventListener: EduTeacherEventListener?)

    fun updateCourseState(roomState: EduRoomState, callback: EduCallback<Unit>)

    fun allowAllStudentChat(isAllow: Boolean, callback: EduCallback<Unit>)

    fun allowStudentChat(isAllow: Boolean, studentInfo: EduUserInfo, callback: EduCallback<Unit>)

    fun startShareScreen(options: ScreenStreamInitOptions, callback: EduCallback<EduStreamInfo>)

    fun stopShareScreen(callback: EduCallback<Unit>)

    fun upsertStudentStreams(streams: MutableList<EduStreamInfo>, callback: EduCallback<Unit>)

    fun deleteStudentStreams(streams: MutableList<EduStreamInfo>, callback: EduCallback<Unit>)
}

interface EduUserEventListener {
    fun onRemoteVideoStateChanged(rtcChannel: RtcChannel?, uid: Int, state: Int, reason: Int, elapsed: Int)

    fun onRemoteAudioStateChanged(rtcChannel: RtcChannel?, uid: Int, state: Int, reason: Int, elapsed: Int)

    fun onRemoteVideoStats(stats: RteRemoteVideoStats)

    fun onLocalVideoStateChanged(localVideoState: Int, error: Int)

    fun onLocalAudioStateChanged(localAudioState: Int, error: Int)

    fun onLocalVideoStats(stats: RteLocalVideoStats)

    fun onAudioVolumeIndicationOfLocalSpeaker(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?, totalVolume: Int)

    fun onAudioVolumeIndicationOfRemoteSpeaker(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?, totalVolume: Int)

    fun onLocalUserUpdated(userEvent: EduUserEvent, type: EduUserStateChangeType)

    fun onLocalStreamAdded(streamEvent: EduStreamEvent)

    fun onLocalStreamUpdated(streamEvent: EduStreamEvent)

    fun onLocalStreamRemoved(streamEvent: EduStreamEvent)

    fun onLocalUserLeft(userEvent: EduUserEvent, leftType: EduUserLeftType)

    fun onLocalUserPropertiesChanged(changedProperties: MutableMap<String, Any>,
                                     userInfo: EduUserInfo, cause: MutableMap<String, Any>?,
                                     operator: EduBaseUserInfo?)
}

interface EduTeacherEventListener : EduUserEventListener {
    fun onCourseStateUpdated(roomState: EduRoomState, error: EduError)

    fun onAllStudentChaAllowed(enable: Boolean, error: EduError)

    fun onStudentChatAllowed(studentInfo: EduUserInfo, enable: Boolean, error: EduError)

    fun onCreateOrUpdateStudentStreamCompleted(streamInfo: EduStreamInfo, error: EduError)
}

interface EduAssistantEventListener : EduUserEventListener {
    fun onCreateOrUpdateTeacherStreamCompleted(streamInfo: EduStreamInfo, error: EduError)

    fun onCreateOrUpdateStudentStreamCompleted(streamInfo: EduStreamInfo, error: EduError)
}

class EduUserEvent(val modifiedUser: EduUserInfo, val operatorUser: EduBaseUserInfo?)

class EduActionConfig(
        val processUuid: String,
        val action: AgoraActionType,
        val toUserUuid: String,
        val fromUserUuid: String?,
        val limit: Int,
        val payload: Map<String, Any>?) {
    var timeout: Long = 60

    constructor(processUuid: String, action: AgoraActionType, toUserUuid: String,
                fromUserUuid: String?, timeout: Long, limit: Int, payload: Map<String, Any>?)
            : this(processUuid, action, toUserUuid, fromUserUuid, limit, payload) {
        this.timeout = timeout
    }
}

class EduStopActionConfig(
        val processUuid: String,
        val action: AgoraActionType,
        var payload: Map<String, Any>?)