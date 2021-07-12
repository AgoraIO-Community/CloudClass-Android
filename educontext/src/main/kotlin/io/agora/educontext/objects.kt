package io.agora.educontext

import android.graphics.Color

data class EduContextChatItem(
        var name: String = "",
        var uid: String = "",
        var role: Int = EduContextUserRole.Student.value,
        var message: String = "",
        var messageId: String = "",
        var type: EduContextChatItemType = EduContextChatItemType.Text,
        var source: EduContextChatSource = EduContextChatSource.Remote,
        var state: EduContextChatState = EduContextChatState.Default,
        var timestamp: Long = 0) {
}

data class EduContextChatItemSendResult(
        val fromUserId: String,
        val messageId: String,
        val timestamp: Long)

enum class EduContextChatItemType {
    Text
}

enum class EduContextChatSource {
    Local, Remote, System
}

enum class EduContextChatState {
    Default, InProgress, Success, Fail
}

enum class EduContextHandsUpState(val value: Int) {
    Init(0),
    HandsUp(1),
    HandsDown(2)
}

enum class EduContextClassState {
    Init, Start, End, Destroyed
}

enum class EduContextNetworkState {
    Good, Medium, Bad, Unknown;
}

enum class EduContextConnectionState(val value: Int) {
    Disconnected(1),
    Connecting(2),
    Connected(3),
    Reconnecting(4),
    Aborted(5);

    companion object {
        fun convert(state: Int): EduContextConnectionState {
            return when (state) {
                Disconnected.value -> {
                    Disconnected
                }
                Connecting.value -> {
                    Connecting
                }
                Connected.value -> {
                    Connected
                }
                Reconnecting.value -> {
                    Reconnecting
                }
                Aborted.value -> {
                    Aborted
                }
                else -> {
                    Disconnected
                }
            }
        }
    }
}

data class EduContextUserDetailInfo(val user: EduContextUserInfo, val streamUuid: String) {
    var isSelf: Boolean = true
    var onLine: Boolean = false
    var coHost: Boolean = false
    var boardGranted: Boolean = false
    var cameraState: EduContextDeviceState = EduContextDeviceState.UnAvailable
    var microState: EduContextDeviceState = EduContextDeviceState.UnAvailable
    var enableVideo: Boolean = false
    var enableAudio: Boolean = false
    var silence: Boolean = false
    var rewardCount: Int = -1

    constructor(user: EduContextUserInfo, streamUuid: String, isSelf: Boolean = true, onLine: Boolean = false, coHost: Boolean,
                boardGranted: Boolean, cameraStateEduContext: EduContextDeviceState, microStateEduContext: EduContextDeviceState,
                enableVideo: Boolean, enableAudio: Boolean, silence: Boolean, rewardCount: Int) : this(user, streamUuid) {
        this.isSelf = isSelf
        this.onLine = onLine
        this.coHost = coHost
        this.boardGranted = boardGranted
        this.cameraState = cameraStateEduContext
        this.microState = microStateEduContext
        this.enableVideo = enableVideo
        this.enableAudio = enableAudio
        this.silence = silence
        this.rewardCount = rewardCount
    }

    fun copy(): EduContextUserDetailInfo {
        return EduContextUserDetailInfo(user, streamUuid, isSelf, onLine, coHost, boardGranted, cameraState,
                microState, enableVideo, enableAudio, silence, rewardCount)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is EduContextUserDetailInfo) {
            return false
        }
        return other.user == user && other.streamUuid == streamUuid && other.isSelf == isSelf
                && other.onLine == onLine && other.coHost == coHost && other.boardGranted == boardGranted
                && other.cameraState == cameraState && other.microState == microState
                && other.enableVideo == enableVideo && other.enableAudio == enableAudio
                && other.silence == silence && other.rewardCount == rewardCount
    }

    override fun hashCode(): Int {
        var result = user.hashCode()
        result = 31 * result + streamUuid.hashCode()
        result = 31 * result + isSelf.hashCode()
        result = 31 * result + onLine.hashCode()
        result = 31 * result + coHost.hashCode()
        result = 31 * result + boardGranted.hashCode()
        result = 31 * result + cameraState.hashCode()
        result = 31 * result + microState.hashCode()
        result = 31 * result + enableVideo.hashCode()
        result = 31 * result + enableAudio.hashCode()
        result = 31 * result + silence.hashCode()
        result = 31 * result + rewardCount
        return result
    }
}

data class EduContextUserInfo(
        val userUuid: String,
        val userName: String,
        val role: EduContextUserRole = EduContextUserRole.Student,
        val properties: MutableMap<String, String>?
) {
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is EduContextUserInfo) {
            return false
        }
        return other.userUuid == userUuid && other.userName == userName && other.role == role &&
                other.properties == properties
    }

    override fun hashCode(): Int {
        var result = userUuid.hashCode()
        result = 31 * result + userName.hashCode()
        result = 31 * result + role.hashCode()
        result = 31 * result + properties.hashCode()
        return result
    }
}

enum class EduContextUserRole(val value: Int) {
    Teacher(1),
    Student(2),
    Assistant(3);

    companion object {
        fun fromValue(value: Int): EduContextUserRole {
            return when (value) {
                Teacher.value -> Teacher
                Student.value -> Student
                Assistant.value -> Assistant
                else -> Student
            }
        }
    }
}

enum class EduContextDeviceState(val value: Int) {
    UnAvailable(0),
    Available(1),
    Closed(2),

    // only use in local(not sync to remote)
    Open(30)
}

object DeviceType {
    const val CAMERA = "camera"
    const val MIC = "mic"
    const val SPEAKER = "speaker"
}

enum class EduContextVideoMode(val value: Int) {
    Single(0),
    Pair(1)
}

data class WhiteboardDrawingConfig(
        var activeAppliance: WhiteboardApplianceType = WhiteboardApplianceType.Clicker,
        var color: Int = Color.WHITE,
        var fontSize: Int = 22,
        var thick: Int = 4) {

    fun set(config: WhiteboardDrawingConfig) {
        this.activeAppliance = config.activeAppliance
        this.color = config.color
        this.fontSize = config.fontSize
        this.thick = config.thick
    }
}

enum class WhiteboardApplianceType {
    Select, Pen, Rect, Circle, Line, Eraser, Text, Clicker;
}

enum class WhiteboardToolType {
    All, Whiteboard
}

data class EduContextPrivateChatInfo(
        val fromUser: EduContextUserInfo,
        val toUser: EduContextUserInfo)

enum class EduContextScreenShareState(val value: Int) {
    Start(0),
    Pause(1),
    Stop(2)
}

data class EduContextRoomInfo(
        val roomUuid: String,
        val roomName: String,
        val roomType: EduContextRoomType
)

enum class EduContextRoomType(val value: Int) {
    OneToOne(0),
    LargeClass(2),
    SmallClass(4);

    companion object {
        fun fromValue(value: Int): EduContextRoomType {
            return when (value) {
                LargeClass.value -> LargeClass
                SmallClass.value -> SmallClass
                else -> OneToOne
            }
        }
    }
}

enum class EduContextCameraFacing(val value: Int) {
    Front(0),
    Back(1);
}

data class EduContextDeviceConfig(
        var cameraEnabled: Boolean = true,
        var cameraFacing: EduContextCameraFacing = EduContextCameraFacing.Front,
        var micEnabled: Boolean = true,
        var speakerEnabled: Boolean = true)

enum class EduBoardRoomPhase(val value: Int) {
    connecting(0),
    connected(1),
    reconnecting(2),
    disconnecting(3),
    disconnected(4);

    companion object {
        fun convert(name: String?): EduBoardRoomPhase {
            return when (name) {
                connecting.name -> connecting
                connected.name -> connected
                reconnecting.name -> reconnecting
                disconnecting.name -> disconnecting
                disconnected.name -> disconnected
                else -> disconnected
            }
        }
    }
}

enum class EduContextMediaStreamType(val value: Int) {
    Audio(0),
    Video(1),
    All(2)
}

enum class State(val value: Int) {
    NO(0),
    YES(1)
}

enum class WidgetType {
    IM
}