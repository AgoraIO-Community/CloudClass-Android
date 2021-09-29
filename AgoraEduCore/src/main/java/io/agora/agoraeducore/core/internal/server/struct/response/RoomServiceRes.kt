package io.agora.agoraeducore.core.internal.server.struct.response

import android.os.Parcel
import android.os.Parcelable
import io.agora.agoraeducore.core.internal.edu.common.bean.board.BoardInfo
import io.agora.agoraeducore.core.internal.education.api.room.data.EduRoomInfo
import io.agora.agoraeducore.core.internal.education.impl.cmd.bean.OnlineUserInfo
import io.agora.agoraeducore.core.internal.server.struct.request.RoleMuteConfig

class EduRemoteConfigRes(var theme: Any, var netless: NetLessConfig) {
    var vid = 0
}

class NetLessConfig(var appId: String, var oss: Any)

class RoomPreCheckRes() : Parcelable {
    var state = 0
    var startTime: Long = 0
    var duration: Long = 0
    var closeDelay: Long = 0
    var lastMessageId: Long = 0
    var muteChat = 0
    var board: BoardInfo? = null
    var rtcRegion: String? = null
    var rtmRegion: String? = null

    constructor(state: Int, startTime: Long, duration: Long, closeDelay: Long,
                lastMessageId: Long, muteChat: Int, board: BoardInfo?, rtcRegion: String, rtmRegion: String) : this() {
        this.state = state
        this.startTime = startTime
        this.duration = duration
        this.closeDelay = closeDelay
        this.lastMessageId = lastMessageId
        this.muteChat = muteChat
        this.board = board
        this.rtcRegion = rtcRegion
        this.rtmRegion = rtmRegion

    }

    constructor(target: Parcel) : this() {
        state = target.readInt()
        startTime = target.readLong()
        duration = target.readLong()
        closeDelay = target.readLong()
        lastMessageId = target.readLong()
        muteChat = target.readInt()
        board = target.readParcelable(BoardInfo::class.java.classLoader)
        rtcRegion = target.readString()
        rtmRegion = target.readString()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(state)
        dest.writeLong(startTime)
        dest.writeLong(duration)
        dest.writeLong(closeDelay)
        dest.writeLong(lastMessageId)
        dest.writeInt(muteChat)
        dest.writeParcelable(board, flags)
        dest.writeString(rtcRegion)
        dest.writeString(rtmRegion)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RoomPreCheckRes> {
        override fun createFromParcel(parcel: Parcel): RoomPreCheckRes {
            return RoomPreCheckRes(parcel)
        }

        override fun newArray(size: Int): Array<RoomPreCheckRes?> {
            return arrayOfNulls(size)
        }
    }
}

class EduEntryRes(
        val room: EduEntryRoomRes,
        val user: EduEntryUserRes)

class EduEntryRoomRes(
        val roomInfo: EduRoomInfo,
        val roomState: EduEntryRoomStateRes,
        val roomProperties: MutableMap<String, Any>?)

class EduEntryRoomStateRes(
        val state: Int,
        val startTime: Long,
        val muteChat: RoleMuteConfig?,
        val muteVideo: RoleMuteConfig?,
        val muteAudio: RoleMuteConfig?, val createTime: Long)

class EduEntryUserRes(
        val userUuid: String,
        val userName: String,
        val role: String,
        val streamUuid: String,
        val userToken: String,
        val rtmToken: String,
        val rtcToken: String,
        val muteChat: Int,
        val userProperties: MutableMap<String, Any>,
        val streams: MutableList<EduEntryStreamRes>?,
        val updateTime: Long,
        val state: Int)

class EduEntryStreamRes(
        var streamUuid: String,
        var streamName: String,
        var videoSourceType: Int,
        var audioSourceType: Int,
        var videoState: Int,
        var audioState: Int,
        var updateTime: Long,
        var rtcToken: String)

class EduSequenceSnapshotRes(
        val sequence: Int,
        val snapshot: EduSnapshotRes)

class EduSnapshotRes(
        val room: EduSnapshotRoomRes,
        val users: MutableList<OnlineUserInfo>)

class EduSnapshotRoomRes(
        val roomInfo: EduSnapshotRoomInfoRes,
        val roomState: EduSnapshotRoomStateRes,
        val roomProperties: MutableMap<String, Any>?)

class EduSnapshotRoomInfoRes(
        val roomName: String,
        val roomUuid: String)

class EduSnapshotRoomStateRes(
        val state: Int,
        val startTime: Long,
        val muteChat: RoleMuteConfig?,
        val muteVideo: RoleMuteConfig?,
        val muteAudio: RoleMuteConfig?,
        val createTime: Long)

class EduSequenceListRes<T>(
        val total: Int,
        val nextId: Int,
        val list: MutableList<EduSequenceRes<T>>)

class EduSequenceRes<T>(
        val sequence: Int,
        val cmd: Int,
        val version: Int,
        val data: T)