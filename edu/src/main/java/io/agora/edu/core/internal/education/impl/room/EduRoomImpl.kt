package io.agora.edu.core.internal.education.impl.room

import android.text.TextUtils
import android.util.Log
import androidx.annotation.NonNull
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.agora.edu.core.internal.base.callback.ThrowableCallback
import io.agora.edu.core.internal.base.network.BusinessException
import io.agora.edu.core.internal.launch.AgoraEduSDK
import io.agora.edu.core.internal.framework.data.EduCallback
import io.agora.edu.core.internal.framework.data.EduError
import io.agora.edu.core.internal.framework.data.EduError.Companion.communicationError
import io.agora.edu.core.internal.framework.data.EduError.Companion.httpError
import io.agora.edu.core.internal.framework.data.EduError.Companion.mediaError
import io.agora.edu.core.internal.framework.data.EduError.Companion.notJoinedRoomError
import io.agora.edu.core.internal.framework.data.EduError.Companion.parameterError
import io.agora.edu.core.internal.education.api.board.EduBoard
import io.agora.edu.core.internal.education.api.logger.LogLevel
import io.agora.edu.core.internal.education.api.record.EduRecord
import io.agora.edu.core.internal.education.api.room.data.*
import io.agora.edu.core.internal.education.api.statistics.NetworkQuality
import io.agora.edu.core.internal.education.api.stream.data.*
import io.agora.edu.core.internal.education.api.stream.data.VideoDimensions.VideoDimensions_160X120
import io.agora.edu.core.internal.education.api.stream.data.VideoDimensions.VideoDimensions_320X240
import io.agora.edu.core.internal.education.impl.Constants.Companion.APPID
import io.agora.edu.core.internal.education.impl.Constants.Companion.AgoraLog
import io.agora.edu.core.internal.education.impl.Constants.Companion.rtcConfigKey
import io.agora.edu.core.internal.education.impl.board.EduBoardImpl
import io.agora.edu.core.internal.education.impl.cmd.CMDDispatch
import io.agora.edu.core.internal.education.impl.cmd.bean.CMDResponseBody
import io.agora.edu.core.internal.framework.impl.EduManagerImpl
import io.agora.edu.core.internal.education.impl.network.RetrofitManager
import io.agora.edu.core.internal.education.impl.record.EduRecordImpl
import io.agora.edu.core.internal.education.impl.role.data.EduUserRoleStr
import io.agora.edu.core.internal.education.impl.room.data.EduRoomInfoImpl
import io.agora.edu.core.internal.server.struct.request.EduJoinClassroomReq
import io.agora.edu.core.internal.education.impl.room.data.response.*
import io.agora.edu.core.internal.education.impl.sync.RoomSyncHelper
import io.agora.edu.core.internal.education.impl.sync.RoomSyncSession
import io.agora.edu.core.internal.education.impl.user.EduAssistantImpl
import io.agora.edu.core.internal.education.impl.user.EduStudentImpl
import io.agora.edu.core.internal.education.impl.user.EduTeacherImpl
import io.agora.edu.core.internal.education.impl.user.EduUserImpl
import io.agora.edu.core.internal.education.impl.user.data.EduLocalUserInfoImpl
import io.agora.edu.core.internal.server.requests.http.retrofit.services.UserService
import io.agora.edu.core.internal.education.impl.util.CommonUtil
import io.agora.edu.core.internal.education.impl.util.Convert
import io.agora.edu.core.internal.framework.*
import io.agora.edu.core.internal.framework.data.EduStreamEvent
import io.agora.edu.core.internal.framework.data.EduStreamInfo
import io.agora.edu.core.internal.report.ReportManager
import io.agora.rtc.Constants.*
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcChannel
import io.agora.rtc.models.ChannelMediaOptions
import io.agora.edu.core.internal.rte.RteCallback
import io.agora.edu.core.internal.rte.RteEngineImpl
import io.agora.edu.core.internal.rte.RteEngineImpl.OK
import io.agora.edu.core.internal.rte.data.*
import io.agora.edu.core.internal.rte.listener.RteAudioMixingListener
import io.agora.edu.core.internal.rte.listener.RteChannelEventListener
import io.agora.edu.core.internal.server.struct.response.DataResponseBody
import io.agora.edu.core.internal.server.struct.response.EduEntryRes
import io.agora.rtc.internal.EncryptionConfig
import io.agora.rtm.*
import java.util.ArrayList
import kotlin.math.max

internal class EduRoomImpl(
        roomInfo: EduRoomInfo,
        roomStatus: EduRoomStatus
) : EduRoom, RteChannelEventListener, RteAudioMixingListener {
    private val TAG = EduRoomImpl::class.java.simpleName

    internal var syncSession: RoomSyncSession

    internal var cmdDispatch: CMDDispatch

    override var roomProperties: MutableMap<String, Any> = mutableMapOf()

    override var board: EduBoard? = null

    override var record: EduRecord? = null

    override var eventListener: EduRoomEventListener? = null

    override var roomAudioMixingListener: EduRoomAudioMixingListener? = null

    init {
        AgoraLog.i("$TAG->Init $TAG")
        RteEngineImpl.enableAudioVolumeIndication(500, 3, false)
        RteEngineImpl.createChannel(roomInfo.roomUuid, this, this)
        syncSession = RoomSyncHelper(this, roomInfo, roomStatus, 3)
        record = EduRecordImpl()
        board = EduBoardImpl()
        cmdDispatch = CMDDispatch(this)
        /**管理当前room*/
        EduManagerImpl.addRoom(this)
    }

    lateinit var rtcToken: String

    // The user monitors the callback of whether the join is successful
    private var joinCallback: EduCallback<EduLocalUser>? = null
    private lateinit var roomEntryRes: EduEntryRes
    lateinit var mediaOptions: RoomMediaOptions

    // Sign whether to exit the room
    private var leaveRoom: Boolean = false

    // Identify whether the join process is completely successful
    @Volatile
    var joinSuccess: Boolean = false

    // Identifies whether the join process is in progress
    @Volatile
    var joining = false

    // The stream information returned by the entry api(may be left over from the last time or this autoPublish stream)
    var defaultStreams: MutableList<EduStreamEvent> = mutableListOf()

    lateinit var defaultUserName: String

    internal fun getCurRoomUuid(): String {
        return syncSession.roomInfo.roomUuid
    }

    internal fun getCurRoomInfo(): EduRoomInfo {
        return syncSession.roomInfo
    }

    internal fun getCurRoomStatus(): EduRoomStatus {
        return syncSession.roomStatus
    }

    internal fun getCurLocalUser(): EduLocalUser {
        return syncSession.localUser
    }

    internal fun getCurLocalUserInfo(): EduUserInfo {
        return syncSession.localUser.userInfo
    }

    internal fun getCurRoomType(): RoomType {
        return (syncSession.roomInfo as EduRoomInfoImpl).roomType
    }

    internal fun getCurStudentList(): MutableList<EduUserInfo> {
        val studentList = mutableListOf<EduUserInfo>()
        for (element in getCurUserList()) {
            if (element.role == EduUserRole.STUDENT) {
                studentList.add(element)
            }
        }
        return studentList
    }

    internal fun getCurTeacherList(): MutableList<EduUserInfo> {
        val teacherList = mutableListOf<EduUserInfo>()
        for (element in getCurUserList()) {
            if (element.role == EduUserRole.TEACHER) {
                teacherList.add(element)
            }
        }
        return teacherList
    }

    internal fun getCurUserList(): MutableList<EduUserInfo> {
        return syncSession.getFullUserInfoList()
    }

    internal fun getCurRemoteUserList(): MutableList<EduUserInfo> {
        val list = mutableListOf<EduUserInfo>()
        syncSession.getFullUserInfoList().forEach {
            if (it != syncSession.localUser.userInfo) {
                list.add(it)
            }
        }
        return list
    }

    internal fun getCurStreamList(): MutableList<EduStreamInfo> {
        return syncSession.getFullStreamInfoList()
    }

    internal fun getCurRemoteStreamList(): MutableList<EduStreamInfo> {
        val list = mutableListOf<EduStreamInfo>()
        syncSession.getFullStreamInfoList().forEach {
            if (it.publisher != syncSession.localUser.userInfo) {
                list.add(it)
            }
        }
        return list
    }

    /**上课过程中，学生的角色目前不发生改变;
     * join流程包括请求加入classroom的API接口、加入rte、同步roomInfo、同步、本地流初始化成功，任何一步出错即视为join失败*/
    override fun join(options: RoomJoinOptions, callback: EduCallback<EduLocalUser>) {
        val reporter = ReportManager.getRteReporter()
        reporter.reportJoinRoomStart()

        if (TextUtils.isEmpty(options.userUuid)) {
            callback.onFailure(parameterError("userUuid"))
            return
        }

        AgoraLog.i("$TAG->User[${options.userUuid}]is ready to join the eduRoom:${getCurRoomUuid()}")
        this.joining = true
        this.joinCallback = callback

        /**判断是否指定了用户名*/
        if (options.userName == null) {
            AgoraLog.i("$TAG->roomJoinOptions.userName is null,user default userName:$defaultUserName")
            options.userName = defaultUserName
        }

        val localUserInfo = EduLocalUserInfoImpl(
                options.userUuid, options.userName!!,
                options.roleType, true,

                // Take care that user token will be calculated after
                // room entry, so we pass an empty String here
                "",
                mutableListOf(), System.currentTimeMillis())

        /**此处需要把localUserInfo设置进localUser中*/
        when (options.roleType) {
            EduUserRole.STUDENT -> {
                syncSession.localUser = EduStudentImpl(localUserInfo)
            }
            EduUserRole.TEACHER -> {
                syncSession.localUser = EduTeacherImpl(localUserInfo)
            }
            EduUserRole.ASSISTANT -> {
                syncSession.localUser = EduAssistantImpl(localUserInfo)
            }
            else -> {
                callback.onFailure(parameterError("roleType"))
            }
        }

        /**Set the video resolution according to the classType*/
        when (getCurRoomType()) {
            RoomType.ONE_ON_ONE -> {
                syncSession.localUser.videoEncoderConfig.videoDimensionWidth = VideoDimensions_320X240[0]
                syncSession.localUser.videoEncoderConfig.videoDimensionHeight = VideoDimensions_320X240[1]
            }
            RoomType.SMALL_CLASS -> {
                syncSession.localUser.videoEncoderConfig.videoDimensionWidth = VideoDimensions_160X120[0]
                syncSession.localUser.videoEncoderConfig.videoDimensionHeight = VideoDimensions_160X120[1]
                syncSession.localUser.videoEncoderConfig.bitrate = 65
            }
            RoomType.LARGE_CLASS -> {
                syncSession.localUser.videoEncoderConfig.videoDimensionWidth = VideoDimensions_320X240[0]
                syncSession.localUser.videoEncoderConfig.videoDimensionHeight = VideoDimensions_320X240[1]
            }
            else -> {
                /**default is 360 * 360*/
            }
        }

        (syncSession.localUser as EduUserImpl).eduRoom = this
        /**大班课强制不自动发流*/
        if (getCurRoomType() == RoomType.LARGE_CLASS) {
            AgoraLog.logMsg("LargeClass force not autoPublish", LogLevel.WARN.value)
            //TODO separate Large class and middle class logic here
            options.closeAutoPublish()
        }

        mediaOptions = options.mediaOptions
        /**根据classroomType和用户传的角色值转化出一个角色字符串来和后端交互*/
        val role = Convert.convertUserRole(localUserInfo.role, getCurRoomType())
        val eduJoinClassroomReq = EduJoinClassroomReq(localUserInfo.userName, role,
                mediaOptions.primaryStreamId.toString(), mediaOptions.getPublishType().value)

        reporter.reportRoomEntryApiStart()
        RetrofitManager.instance()!!.getService(AgoraEduSDK.baseUrl(), UserService::class.java)
                .joinClassroom(APPID, getCurRoomUuid(), localUserInfo.userUuid, eduJoinClassroomReq)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<DataResponseBody<EduEntryRes>> {
                    override fun onSuccess(res: DataResponseBody<EduEntryRes>?) {
                        reporter.reportRoomEntryApiResult("1", null, null, null)

                        roomEntryRes = res?.data!!
                        /**解析返回的user相关数据*/
                        localUserInfo.userToken = roomEntryRes.user.userToken
                        rtcToken = roomEntryRes.user.rtcToken
                        /**RTE中的API需要的userToken*/
                        RetrofitManager.instance()!!.addHeader("token", roomEntryRes.user.userToken)
                        localUserInfo.isChatAllowed = roomEntryRes.user.muteChat == EduChatState.Allow.value
                        localUserInfo.userProperties = roomEntryRes.user.userProperties
                        localUserInfo.streamUuid = roomEntryRes.user.streamUuid
                        /**把本地用户信息合并到本地缓存中(需要转换类型)*/
                        syncSession.getFullUserInfoList().add(Convert.convertUserInfo(localUserInfo))
                        /**获取用户可能存在的流信息待join成功后进行处理;*/
                        roomEntryRes.user.streams?.let {
                            /**转换并合并流信息到本地缓存*/
                            val streamEvents = Convert.convertStreamInfo(it, this@EduRoomImpl)
                            defaultStreams.addAll(streamEvents)
                        }
                        /**解析返回的room相关数据并同步保存至本地*/
                        getCurRoomStatus().startTime = roomEntryRes.room.roomState.startTime
                        getCurRoomStatus().createTime = roomEntryRes.room.roomState.createTime
                        getCurRoomStatus().courseState = Convert.convertRoomState(roomEntryRes.room.roomState.state)
                        getCurRoomStatus().isStudentChatAllowed = Convert.extractStudentChatAllowState(
                                roomEntryRes.room.roomState.muteChat, getCurRoomType())
                        roomEntryRes.room.roomProperties?.let {
                            roomProperties = it
                        }
                        /**加入rte(包括rtm和rtc)*/
                        // get the privateParams
                        val rtcMode = roomEntryRes.room.roomProperties?.get(rtcConfigKey[0]) as? Map<String, Any>
                        //rtcMode is the enum value of AgoraRTCMode, default is CHANNEL_PROFILE_LIVE_BROADCASTING
                        val rtcModeValue: Double = rtcMode?.get(rtcConfigKey[1])?.toString()?.toDouble()
                                ?: CHANNEL_PROFILE_LIVE_BROADCASTING.toDouble()
                        val privateParamsMap: Map<String, Any>? = roomEntryRes.room.roomProperties?.get(rtcConfigKey[2]) as? Map<String, Any>
                        //get android related params
                        val paramList = (privateParamsMap?.filter { (key, value) ->
                            key.startsWith(rtcConfigKey[3])
                        }?.let { it[rtcConfigKey[4]] as? Map<String, Any> }.let { param ->
                            param?.get(rtcConfigKey[0])
                        } ?: mutableListOf<String>()) as ArrayList<String>
                        AgoraLog.i("$TAG->call joinRte function")
                        joinRte(rtcToken, roomEntryRes.user.streamUuid.toLong(), mediaOptions.convert(),
                                mediaOptions.rteEncryptionConfig(), options, rtcModeValue.toInt(),
                                paramList, object : RteCallback<Void> {
                            override fun onSuccess(p0: Void?) {
                                AgoraLog.i("$TAG->joinRte success")
                                /**拉取全量数据*/
                                syncSession.fetchSnapshot(object : EduCallback<Unit> {
                                    override fun onSuccess(res: Unit?) {
                                        reporter.reportJoinRoomEnd("1", null)
                                        ReportManager.getHeartbeat()?.startHeartbeat(
                                                roomEntryRes.room.roomInfo.roomUuid,
                                                roomEntryRes.user.userUuid)

                                        AgoraLog.i("$TAG->Full data pull and merge successfully,init localStream")
                                        setVideoEncoderConfig(options.videoEncoderConfig)
                                        joinSuccess(syncSession.localUser, joinCallback as EduCallback<EduLocalUser>)
                                    }

                                    override fun onFailure(error: EduError) {
                                        AgoraLog.i("$TAG->Full data pull failed")
                                        joinFailed(error, callback)
                                    }
                                })
                            }

                            override fun onFailure(error: RteError) {
                                AgoraLog.i("$TAG->joinRte failed")
                                var eduError = if (error.type == ErrorType.RTC) {
                                    mediaError(error.errorCode, error.errorDesc)
                                } else {
                                    communicationError(error.errorCode, error.errorDesc)
                                }
                                joinFailed(eduError, callback)
                            }
                        })
                    }

                    override fun onFailure(throwable: Throwable?) {
                        AgoraLog.i("$TAG->Calling the entry API failed")
                        var error = throwable as? BusinessException
                        error = error ?: BusinessException(throwable?.message)
                        joinFailed(httpError(error.code, error.message
                                ?: throwable?.message), callback)

                        throwable?.let {
                            if (it is BusinessException) {
                                val t: BusinessException = it
                                reporter.reportRoomEntryApiResult("1", t.code.toString(), t.httpCode.toString(), null)
                            } else {
                                reporter.reportRoomEntryApiResult("0", error.code.toString(), null, null)
                            }
                        }
                    }
                }))
    }

    private fun setVideoEncoderConfig(videoEncoderConfig: EduVideoEncoderConfig?) {
        val config = videoEncoderConfig ?: syncSession.localUser.videoEncoderConfig
        val a = RteEngineImpl.setVideoEncoderConfiguration(Convert.convertVideoEncoderConfig(config))
        if (a != OK()) {
            AgoraLog.e(TAG, "Media error->$a,reason-> set video encoder config failed")
        } else {
            AgoraLog.i(TAG, "set video encoder config successfully")
        }
    }

    private fun joinRte(rtcToken: String, rtcUid: Long, options: ChannelMediaOptions,
                        encryptionConfig: EncryptionConfig, joinOptions: RoomJoinOptions,
                        rtcMode: Int, rtcParams: ArrayList<String>, @NonNull callback: RteCallback<Void>) {
        AgoraLog.i("$TAG->join Rtc and Rtm")
        RteEngineImpl.setChannelMode(rtcMode)
        rtcParams.forEach {
            RteEngineImpl.setPrivateParam(it)
        }
        RteEngineImpl.setLatencyLevel(getCurRoomUuid(), joinOptions.latencyLevel.value)
        RteEngineImpl.setClientRole(getCurRoomUuid(), CLIENT_ROLE_AUDIENCE)
        val rtcOptionalInfo: String = CommonUtil.buildRtcOptionalInfo(joinOptions.tag)
        RteEngineImpl[getCurRoomUuid()]?.join(rtcOptionalInfo, rtcToken, rtcUid, options, encryptionConfig, callback)
    }

    override fun getRtcCallId(id: String): String {
        return RteEngineImpl[getCurRoomUuid()]?.getRtcCallId() ?: ""
    }

    override fun getRtmSessionId(): String {
        return RteEngineImpl.getRtmSessionId()
    }

    private fun handleLocalStream(stream: EduStreamInfo, callback: EduCallback<Unit>?) {
        val localStreamInitOptions = LocalStreamInitOptions(stream.streamUuid, stream.hasVideo,
                stream.hasAudio, stream.hasVideo, stream.hasAudio)
        AgoraLog.i("$TAG->initOrUpdateLocalStream for localUser:${Gson().toJson(localStreamInitOptions)}")
        syncSession.localUser.initOrUpdateLocalStream(localStreamInitOptions, object : EduCallback<EduStreamInfo> {
            override fun onSuccess(res: EduStreamInfo?) {
                AgoraLog.i("$TAG->initOrUpdateLocalStream success")
                RteEngineImpl.setClientRole(getCurRoomUuid(), CLIENT_ROLE_BROADCASTER)
                RteEngineImpl.muteLocalStream(getCurRoomUuid(), !stream.hasAudio, !stream.hasVideo)
                RteEngineImpl.publish(getCurRoomUuid())
                callback?.onSuccess(Unit)
            }

            override fun onFailure(error: EduError) {
                AgoraLog.e("$TAG->Failed to initOrUpdateLocalStream for localUser")
                callback?.onFailure(error)
            }
        })
    }

    fun onRemoteInitialized() {
        /**本地缓存的远端人流数据为空，则不走initialized回调*/
        if (getCurRemoteUserList().size > 0) {
            eventListener?.onRemoteUsersInitialized(getCurRemoteUserList(), this@EduRoomImpl)
        }
        if (getCurRemoteStreamList().size > 0) {
            eventListener?.onRemoteStreamsInitialized(getCurRemoteStreamList(), this@EduRoomImpl)
        }
    }

    // Judge the joining state to prevent multiple calls
    private fun joinSuccess(eduUser: EduLocalUser, callback: EduCallback<EduLocalUser>) {
        if (joining) {
            joining = false
            synchronized(joinSuccess) {
                Log.e(TAG, "Join the eduRoom successfully:${getCurRoomUuid()}")
                getCurRemoteUserList().forEach {
                    if (!eduUser.cachedRemoteVideoStates.containsKey(it.streamUuid)) {
                        eduUser.cachedRemoteVideoStates[it.streamUuid] = RteRemoteVideoState.REMOTE_VIDEO_STATE_FROZEN.value
                    }
                    if (!eduUser.cachedRemoteAudioStates.containsKey(it.streamUuid)) {
                        eduUser.cachedRemoteAudioStates[it.streamUuid] == RteRemoteAudioState.REMOTE_AUDIO_STATE_FROZEN.value
                    }
                }
                /**维护本地存储的在线人数*/
                getCurRoomStatus().onlineUsersCount = getCurUserList().size
                joinSuccess = true
                callback.onSuccess(eduUser)
                /**把本地缓存的远端人流数据回调出去*/
                onRemoteInitialized()
                /**检查是否有默认流信息(直接处理数据)*/
                val addedStreamsIterable = defaultStreams.iterator()
                while (addedStreamsIterable.hasNext()) {
                    val element = addedStreamsIterable.next()
                    val streamInfo = element.modifiedStream
                    /**判断是否推本地流*/
                    if (streamInfo.publisher == syncSession.localUser.userInfo) {
                        /**本地流维护在本地用户信息中和全局集合中*/
                        syncSession.localUser.userInfo.streams.add(element)
                        /**根据流信息，更新本地媒体*/
                        handleLocalStream(streamInfo, null)
                        AgoraLog.i("$TAG->Join success，callback the added localStream to upper layer")
                        AgoraLog.i("$TAG->onLocalStreamAdded:${Gson().toJson(element)}")
                        syncSession.localUser.eventListener?.onLocalStreamAdded(element)
                        /**把本地流*/
                        addedStreamsIterable.remove()
                    }
                }
                /**检查并处理缓存数据(处理CMD消息)*/
                (syncSession as RoomSyncHelper).handleCache(object : EduCallback<Unit> {
                    override fun onSuccess(res: Unit?) {
                    }

                    override fun onFailure(error: EduError) {
                    }
                })
            }
        }
    }

    /**join失败的情况下，清楚所有本地已存在的缓存数据；判断joining状态防止多次调用
     * 并退出rtm和rtc*/
    private fun joinFailed(error: EduError, callback: EduCallback<EduLocalUser>) {
        AgoraLog.i("$TAG->JoinClassRoom failed, code:${error.type},msg:${error.msg}")
        if (joining) {
            joining = false
            synchronized(joinSuccess) {
                joinSuccess = false
                clearData()
                callback.onFailure(error)
            }
        }
    }

    // Clear the local cache; leave the current channel of RTM; exit RTM
    fun clearData() {
        AgoraLog.w("$TAG->Clean up local cached people and stream data")
        getCurUserList().clear()
        getCurStreamList().clear()
    }

    override fun getLocalUser(callback: EduCallback<EduLocalUser>) {
        if (!joinSuccess) {
            val error = notJoinedRoomError()
            AgoraLog.e("$TAG->EduRoom[${getCurRoomUuid()}] getLocalUser error:${error.msg}")
            callback.onFailure(error)
        } else {
            callback.onSuccess(syncSession.localUser)
        }
    }

    override fun getRoomInfo(callback: EduCallback<EduRoomInfo>) {
        if (!joinSuccess) {
            val error = notJoinedRoomError()
            AgoraLog.e("$TAG->EduRoom[${getCurRoomUuid()}] getRoomInfo error:${error.msg}")
            callback.onFailure(error)
        } else {
            callback.onSuccess(syncSession.roomInfo)
        }
    }

    override fun getRoomStatus(callback: EduCallback<EduRoomStatus>) {
        if (!joinSuccess) {
            val error = notJoinedRoomError()
            AgoraLog.e("$TAG->EduRoom[${getCurRoomUuid()}] getRoomStatus error:${error.msg}")
            callback.onFailure(error)
        } else {
            callback.onSuccess(syncSession.roomStatus)
        }
    }

    override fun getStudentCount(callback: EduCallback<Int>) {
        if (!joinSuccess) {
            val error = notJoinedRoomError()
            AgoraLog.e("$TAG->EduRoom[${getCurRoomUuid()}] getStudentCount error:${error.msg}")
            callback.onFailure(error)
        } else {
            callback.onSuccess(getCurStudentList().size)
        }
    }

    override fun getTeacherCount(callback: EduCallback<Int>) {
        if (!joinSuccess) {
            val error = notJoinedRoomError()
            AgoraLog.e("$TAG->EduRoom[${getCurRoomUuid()}] getTeacherCount error:${error.msg}")
            callback.onFailure(error)
        } else {
            callback.onSuccess(getCurTeacherList().size)
        }
    }

    override fun getStudentList(callback: EduCallback<MutableList<EduUserInfo>>) {
        if (!joinSuccess) {
            val error = notJoinedRoomError()
            AgoraLog.e("$TAG->EduRoom[${getCurRoomUuid()}] getStudentList error:${error.msg}")
            callback.onFailure(error)
        } else {
            val studentList = mutableListOf<EduUserInfo>()
            for (element in getCurUserList()) {
                if (element.role == EduUserRole.STUDENT) {
                    studentList.add(element)
                }
            }
            callback.onSuccess(studentList)
        }
    }

    override fun getTeacherList(callback: EduCallback<MutableList<EduUserInfo>>) {
        if (!joinSuccess) {
            val error = notJoinedRoomError()
            AgoraLog.e("$TAG->EduRoom[${getCurRoomUuid()}] getTeacherList error:${error.msg}")
            callback.onFailure(error)
        } else {
            val teacherList = mutableListOf<EduUserInfo>()
            for (element in getCurUserList()) {
                if (element.role == EduUserRole.TEACHER) {
                    teacherList.add(element)
                }
            }
            callback.onSuccess(teacherList)
        }
    }

    override fun getFullStreamList(callback: EduCallback<MutableList<EduStreamInfo>>) {
        if (!joinSuccess) {
            val error = notJoinedRoomError()
            AgoraLog.e("$TAG->EduRoom[${getCurRoomUuid()}] getFullStreamList error:${error.msg}")
            callback.onFailure(error)
        } else {
            callback.onSuccess(syncSession.getFullStreamInfoList())
        }
    }

    // Get all user data cached locally
    override fun getFullUserList(callback: EduCallback<MutableList<EduUserInfo>>) {
        if (!joinSuccess) {
            val error = notJoinedRoomError()
            AgoraLog.e("$TAG->EduRoom[${getCurRoomUuid()}] getFullUserList error:${error.msg}")
            callback.onFailure(error)
        } else {
            callback.onSuccess(syncSession.getFullUserInfoList())
        }
    }

    // Before exiting the room, you must call
    override fun leave(callback: EduCallback<Unit>) {
        if (!joinSuccess) {
            val error = notJoinedRoomError()
            AgoraLog.e("$TAG->Leave eduRoom[${getCurRoomUuid()}] error:${error.msg}")
//            callback.onFailure(error)
        }
        clearData()
        if (!leaveRoom) {
            AgoraLog.w("$TAG->Ready to leave the RTE channel:${getCurRoomUuid()}")
            RteEngineImpl[getCurRoomUuid()]?.leave(object : RteCallback<Unit> {
                override fun onSuccess(res: Unit?) {
                    Log.e(TAG, "Successfully left RTE channel")
                }

                override fun onFailure(error: RteError) {
                    Log.e(TAG, "Failed left RTE channel:code:${error.errorCode},msg:${error.errorDesc}")
                }
            })
            leaveRoom = true
        }
        RteEngineImpl[getCurRoomUuid()]?.release()
        eventListener = null
        syncSession.localUser.eventListener = null
        joinCallback = null
        (getCurLocalUser() as EduUserImpl).removeAllTextureView()
        AgoraLog.w("$TAG->Leave eduRoom[${getCurRoomUuid()}] success")
        /*移除掉当前room*/
        val rtn = EduManagerImpl.removeRoom(this)
        AgoraLog.w("$TAG->Remove this eduRoom from eduManager:$rtn")
        ReportManager.getHeartbeat()?.stopHeartbeat()
        callback.onSuccess(Unit)
    }

    override fun getRoomUuid(): String {
        return syncSession.roomInfo.roomUuid
    }

    override fun onChannelMsgReceived(p0: RtmMessage?, p1: RtmChannelMember?) {
        p0?.text?.let {
            val cmdResponseBody = Gson().fromJson<CMDResponseBody<Any>>(p0.text, object :
                    TypeToken<CMDResponseBody<Any>>() {}.type)

//            if(cmdResponseBody.cmd == 3) {
//                return
//            }

            val pair = syncSession.updateSequenceId(cmdResponseBody)
            if (pair != null) {
                /*count设为null,请求所有丢失的数据*/
                syncSession.fetchLostSequence(pair.first, pair.second, object : EduCallback<Unit> {
                    override fun onSuccess(res: Unit?) {
                    }

                    override fun onFailure(error: EduError) {
                    }
                })
            }
        }
    }

    override fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {
        /*The worst one for the upstream and downstream*/
        val value = max(txQuality, rxQuality)
        val quality: NetworkQuality = Convert.convertNetworkQuality(value)
        if (uid == 0) {
            eventListener?.onNetworkQualityChanged(quality, getCurLocalUserInfo() as EduBaseUserInfo, this)
        } else {
            val longUid = uid.toLong() and 0xffffffffL
            getCurStreamList().forEach {
                if (it.streamUuid.toLong() == longUid) {
                    eventListener?.onNetworkQualityChanged(quality, it.publisher, this)
                }
            }
        }
    }

    override fun onUserJoined(uid: Int) {
        val uuid = (uid.toLong() and 0xffffffffL).toString()
        if (!getCurLocalUser().cacheRemoteOnlineUserIds.contains(uuid)) {
            getCurLocalUser().cacheRemoteOnlineUserIds.add(uuid)
        }
        eventListener?.onRemoteRTCJoinedOfStreamId(uuid)
    }

    override fun onUserOffline(uid: Int) {
        val uuid = (uid.toLong() and 0xffffffffL).toString()
        getCurLocalUser().cacheRemoteOnlineUserIds.remove(uuid)
        eventListener?.onRemoteRTCOfflineOfStreamId(uuid)
    }

    override fun onRemoteVideoStateChanged(rtcChannel: RtcChannel?, uid: Int, state: Int, reason: Int, elapsed: Int) {
        val longStreamUuid = uid.toLong() and 0xffffffffL
        getCurLocalUser().cachedRemoteVideoStates[longStreamUuid.toString()] = state
        getCurLocalUser().eventListener?.onRemoteVideoStateChanged(rtcChannel, uid, state, reason, elapsed)
    }

    override fun onRemoteAudioStateChanged(rtcChannel: RtcChannel?, uid: Int, state: Int, reason: Int, elapsed: Int) {
        val longStreamUuid = uid.toLong() and 0xffffffffL
        getCurLocalUser().cachedRemoteAudioStates[longStreamUuid.toString()] = state
        getCurLocalUser().eventListener?.onRemoteAudioStateChanged(rtcChannel, uid, state, reason, elapsed)
    }

    override fun onRemoteVideoStats(stats: RteRemoteVideoStats) {
        getCurLocalUser().eventListener?.onRemoteVideoStats(stats)
    }

    override fun onLocalVideoStateChanged(localVideoState: Int, error: Int) {
        getCurLocalUser().eventListener?.onLocalVideoStateChanged(localVideoState, error)
    }

    override fun onLocalAudioStateChanged(localAudioState: Int, error: Int) {
        getCurLocalUser().eventListener?.onLocalAudioStateChanged(localAudioState, error)
    }

    override fun onLocalVideoStats(stats: RteLocalVideoStats) {
        getCurLocalUser().eventListener?.onLocalVideoStats(stats)
    }

    override fun onAudioVolumeIndicationOfLocalSpeaker(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?, totalVolume: Int) {
        getCurLocalUser().eventListener?.onAudioVolumeIndicationOfLocalSpeaker(speakers, totalVolume)
    }

    override fun onAudioVolumeIndicationOfRemoteSpeaker(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?, totalVolume: Int) {
        getCurLocalUser().eventListener?.onAudioVolumeIndicationOfRemoteSpeaker(speakers, totalVolume)
    }

    override fun onAudioMixingFinished() {
        roomAudioMixingListener?.onAudioMixingFinished()
    }

    override fun onAudioMixingStateChanged(state: Int, errorCode: Int) {
        roomAudioMixingListener?.onAudioMixingStateChanged(state, errorCode)
    }
}
