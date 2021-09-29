package io.agora.agoraeducore.core.internal.education.impl.sync

import io.agora.agoraeducore.core.internal.framework.data.EduCallback
import io.agora.agoraeducore.core.internal.education.api.room.data.EduRoomInfo
import io.agora.agoraeducore.core.internal.education.api.room.data.EduRoomStatus
import io.agora.agoraeducore.core.internal.framework.data.EduStreamInfo
import io.agora.agoraeducore.core.internal.framework.EduLocalUser
import io.agora.agoraeducore.core.internal.framework.EduUserInfo
import io.agora.agoraeducore.core.internal.education.impl.cmd.bean.CMDResponseBody
import java.util.*

internal open abstract class RoomSyncSession(val roomInfo: EduRoomInfo, val roomStatus: EduRoomStatus) {

    lateinit var localUser: EduLocalUser

    /**本地缓存的人流数据*/
    private val eduUserInfoList = Collections.synchronizedList(mutableListOf<EduUserInfo>())
    private val eduStreamInfoList = Collections.synchronizedList(mutableListOf<EduStreamInfo>())

    @Synchronized
    fun getFullUserInfoList(): MutableList<EduUserInfo> {
        return eduUserInfoList
    }

    @Synchronized
    fun getFullStreamInfoList(): MutableList<EduStreamInfo> {
        return eduStreamInfoList
    }

    abstract fun updateSequenceId(cmdResponseBody: CMDResponseBody<Any>): Pair<Int, Int>?

    abstract fun fetchLostSequence(callback: EduCallback<Unit>)

    abstract fun fetchLostSequence(nextId: Int, count: Int?, callback: EduCallback<Unit>)

    abstract fun fetchSnapshot(callback: EduCallback<Unit>)
}