package io.agora.educontext

import android.graphics.Color

data class EduContextChatItem(
        var name: String = "",
        var uid: String = "",
        var message: String = "",
        var messageId: Int = 0,
        var type: EduContextChatItemType = EduContextChatItemType.Text,
        var source: EduContextChatSource = EduContextChatSource.Remote,
        var state: EduContextChatState = EduContextChatState.Default,
        var timestamp: Long = 0) {

    fun copyValue(item: EduContextChatItem) {
        this.name = item.name
        this.uid = item.uid
        this.message = message
        this.messageId = messageId
        this.type = type
        this.source = item.source
        this.state = item.state
        this.timestamp = item.timestamp
    }
}

data class EduContextChatItemSendResult(val userId: String, val messageId: Int, val timestamp: Long)

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

enum class EduContextConnectionState {
    Disconnected,
    Connecting,
    Connected,
    Reconnecting,
    Aborted
}

data class EduContextUserDetailInfo(val user: EduContextUserInfo, val streamUuid: String) {
    var isSelf: Boolean = true
    var onLine: Boolean = false
    var coHost: Boolean = false
    var boardGranted: Boolean = false
    var cameraState: DeviceState = DeviceState.UnAvailable
    var microState: DeviceState = DeviceState.UnAvailable
    var enableVideo: Boolean = false
    var enableAudio: Boolean = false
    var rewardCount: Int = -1

    constructor(user: EduContextUserInfo, streamUuid: String, isSelf: Boolean = true, onLine: Boolean = false, coHost: Boolean,
                boardGranted: Boolean, cameraState: DeviceState, microState: DeviceState,
                enableVideo: Boolean, enableAudio: Boolean, rewardCount: Int) : this(user, streamUuid) {
        this.isSelf = isSelf
        this.onLine = onLine
        this.coHost = coHost
        this.boardGranted = boardGranted
        this.cameraState = cameraState
        this.microState = microState
        this.enableVideo = enableVideo
        this.enableAudio = enableAudio
        this.rewardCount = rewardCount
    }

    fun copy(): EduContextUserDetailInfo {
        return EduContextUserDetailInfo(user, streamUuid, isSelf, onLine, coHost, boardGranted, cameraState,
                microState, enableVideo, enableAudio, rewardCount)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is EduContextUserDetailInfo) {
            return false
        }
        return other.user == user && other.streamUuid == streamUuid && other.isSelf == isSelf
                && other.onLine == onLine && other.coHost == coHost && other.boardGranted == boardGranted
                && other.cameraState == cameraState && other.microState == microState
                && other.enableVideo == enableVideo && other.enableAudio == enableAudio
                && other.rewardCount == rewardCount
    }
}

data class EduContextUserInfo(
        val userUuid: String,
        val userName: String,
        val role: EduContextUserRole = EduContextUserRole.Student
) {
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is EduContextUserInfo) {
            return false
        }
        return other.userUuid == userUuid && other.userName == userName && other.role == role
    }
}

enum class EduContextUserRole(val value: Int) {
    Teacher(1),
    Student(2),
    Assistant(3)
}

enum class DeviceState(val value: Int) {
    UnAvailable(0),
    Available(1),
    Closed(2)
}

enum class EduContextVideoMode(val value: Int) {
    Single(0),
    Pair(1)
}

data class WhiteboardDrawingConfig(
        var activeAppliance: WhiteboardApplianceType = WhiteboardApplianceType.Select,
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
    Select, Pen, Rect, Circle, Line, Eraser, Text;
}

enum class WhiteboardToolType {
    All, Whiteboard
}

data class EduContextPrivateChatInfo(
        val fromUser: EduContextUserInfo,
        val toUser: EduContextUserInfo)