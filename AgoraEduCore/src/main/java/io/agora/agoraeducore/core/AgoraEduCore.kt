package io.agora.agoraeducore.core

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.google.gson.Gson
import com.herewhite.sdk.domain.SDKError
import com.herewhite.sdk.domain.SceneState
import io.agora.agoraeducontext.*
import io.agora.agoraeduextapp.*
import io.agora.agoraeducore.core.internal.edu.classroom.WhiteBoardManager
import io.agora.agoraeducore.core.internal.edu.classroom.WhiteBoardManagerEventListener
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.base.callback.Callback
import io.agora.agoraeducore.core.internal.edu.classroom.*
import io.agora.agoraeducore.core.internal.education.api.logger.DebugItem
import io.agora.agoraeducore.core.internal.education.api.room.data.*
import io.agora.agoraeducore.core.internal.education.api.statistics.ConnectionState
import io.agora.agoraeducore.core.internal.education.api.statistics.NetworkQuality
import io.agora.agoraeducore.core.internal.education.api.stream.data.*
import io.agora.agoraeducore.core.internal.framework.EduUserEventListener
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.education.impl.Constants.Companion.AgoraLog
import io.agora.agoraeducore.core.internal.extapp.AgoraExtAppManager
import io.agora.agoraeducore.core.internal.framework.*
import io.agora.agoraeducore.core.internal.framework.data.*
import io.agora.agoraeducore.core.internal.framework.data.EduError.Companion.internalError
import io.agora.agoraeducore.core.internal.launch.*
import io.agora.agoraeducore.core.internal.log.UploadManager
import io.agora.agoraeducore.core.internal.privatechat.PrivateChatManager
import io.agora.agoraeducore.core.internal.report.ReportManager.getAPaasReporter
import io.agora.agoraeducore.core.internal.report.reporters.APaasReporter
import io.agora.agoraeducore.core.internal.report.v2.reporter.ReporterV2
import io.agora.agoraeducore.core.internal.rte.data.RteLocalVideoStats
import io.agora.agoraeducore.core.internal.rte.data.RteRemoteVideoStats
import io.agora.agoraeducore.core.internal.server.requests.AgoraRequestClient
import io.agora.agoraeducore.core.internal.util.TimeUtil
import io.agora.agoraeducore.core.internal.whiteboard.netless.bean.AgoraBoardFitMode
import io.agora.agoraeducore.sdk.app.activities.BaseClassActivity
import io.agora.agoraeducontext.handlerimpl.DeViceHandler
import io.agora.agoraeducontext.handlerimpl.WhiteboardHandler
import io.agora.agoraeducore.BuildConfig
import io.agora.agoraeducore.core.DataConvert.convert
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcChannel

class AgoraEduCore(
        private val context: Context,
        private val config: AgoraEduCoreConfig,
        private val stateListener: AgoraEduCoreStateListener? = null)
    : EduRoomEventListener,
        EduUserEventListener,
        EduManagerEventListener,
        EduRoomAudioMixingListener {

    private val tag = "AgoraEduCore"

    @Volatile
    private var eduManager: EduManager? = null

    private var eduRoom: EduRoom? = null

    @Volatile
    private var eduManagerCreated = false

    @Volatile
    private var pendingJoinRoom = false

    @Volatile
    private var pendingJoinWhiteboard = false

    @Volatile
    private var roomJoined = false

    @Volatile
    private var whiteboardJoined = false

    private var localUser: EduLocalUser? = null

    // All managers are illegible to use after joining room successfully
    private var roomStateManager: RoomStateManager? = null
    private var deviceManager: DeviceManager? = null
    private var chatManager: ChatManager? = null
    private var easeImManager: IMManager? = null
    private var handsUpManager: HandsUpManager? = null
    private var oneToOneVideoManager: OneToOneVideoManager? = null
    private var teacherVideoManager: TeacherVideoManager? = null
    private var userListManager: UserListManager? = null
    private var screenShareManager: ScreenShareManager? = null
    private var privateChatManager: PrivateChatManager? = null
    private var extAppManager: AgoraExtAppManager? = null
    private var whiteBoardManager: WhiteBoardManager? = null
    private var flexPropsManager: FlexPropsManager? = null

    @Volatile
    private var rtmConnectionState = ConnectionState.DISCONNECTED.value

    private val agoraEduLog = AgoraEduLog()

    init {
        val option = EduManagerOptions(
                context,
                config.appId,
                config.rtmToken,
                config.userUuid,
                config.userName, config.rtcRegion, config.rtmRegion)
        option.logFileDir = config.logDir
        EduManager.init(option, object : EduCallback<EduManager> {
            override fun onSuccess(res: EduManager?) {
                res?.let {
                    synchronized(this@AgoraEduCore) {
                        eduManagerCreated = true
                        it.eduManagerEventListener = this@AgoraEduCore
                        eduManager = it
                        stateListener?.onCreated()

                        eduRoom = createEduRoom(it)
                        eduRoom!!.eventListener = this@AgoraEduCore
                        eduRoom!!.roomAudioMixingListener = this@AgoraEduCore

                        // Enable rtm request channel and those requests which are
                        // recommended to go rtm channel can find available server peers
                        AgoraRequestClient.enableRtmRequestChannel(config.boardRegion, true)

                        if (pendingJoinRoom) {
                            AgoraLog.i("$tag:pendingJoinRoom is $pendingJoinRoom, execute joinClassroom")
                            pendingJoinRoom = false
                            // join room request occurs when edu core
                            // instance is still initializing
                            eduContextPool().roomContext()?.joinClassroom()
                        }

                        if (pendingJoinWhiteboard) {
                            AgoraLog.i("$tag:pendingJoinWhiteboard is $pendingJoinWhiteboard, execute joinWhiteboard")
                            pendingJoinWhiteboard = false
                            // join whiteboard request occurs when edu core
                            // instance is still initializing
                            eduContextPool.whiteboardContext().joinWhiteboard()
                        }

                        // check uploadLog
                        AgoraLog.i("$tag:checkUploadLog")
                        agoraEduLog.checkUploadLog(res, UploadManager.UploadParamTag(
                                config.roomUuid,
                                config.roomName,
                                config.roomType,
                                config.userUuid,
                                config.userName,
                                config.roleType))
                    }
                }
            }

            override fun onFailure(error: EduError) {
                stateListener?.onError(error)
            }
        })
    }

    private fun createEduRoom(eduManager: EduManager): EduRoom? {
        AgoraLog.i("$tag:createEduRoom->roomUuid:${config.roomUuid}, roomName:${config.roomName}, " +
                "roomType:${config.roomType}")
        return eduManager.createEduRoom(
                RoomCreateOptions(
                        config.roomUuid,
                        config.roomName,
                        config.roomType))
    }

    private val widgetContext = object : WidgetContext {
        override fun getWidgetProperties(type: WidgetType): Map<String, Any>? {
            return when (type) {
                WidgetType.IM -> {
                    easeImManager?.parseEaseIMProperties(
                            eduRoom?.roomProperties?.get(IMManager.propertiesKey) as? Map<String, Any>)
                }
            }
        }
    }

    private val eduContextPool = object : EduContextPool {
        override fun chatContext(): ChatContext {
            return chatContextImpl
        }

        override fun handsUpContext(): HandsUpContext {
            return handsUpContextImpl
        }

        override fun roomContext(): RoomContext {
            return roomContextImpl
        }

        override fun mediaContext(): MediaContext {
            return mediaContextImpl
        }

        override fun deviceContext(): DeviceContext {
            return deviceContextImpl
        }

        override fun screenShareContext(): ScreenShareContext {
            return screenShareContextImpl
        }

        override fun userContext(): UserContext {
            return userContextImpl
        }

        override fun videoContext(): VideoContext {
            return videoContextImpl
        }

        override fun whiteboardContext(): WhiteboardContext {
            return whiteboardContextImpl
        }

        override fun privateChatContext(): PrivateChatContext {
            return privateChatContext
        }

        override fun extAppContext(): ExtAppContext {
            return extAppContext
        }

        override fun widgetContext(): WidgetContext? {
            return widgetContext
        }
    }

    private val chatContextImpl: ChatContext = object : ChatContext() {
        override fun sendLocalChannelMessage(message: String, timestamp: Long,
                                             callback: EduContextCallback<EduContextChatItemSendResult>): EduContextChatItem {
            AgoraLog.i("$tag:sendLocalChannelMessage->message:$message, timestamp:$timestamp")
            chatManager?.sendRoomChat(message, timestamp, callback)
            return EduContextChatItem(
                    name = config.userName,
                    uid = config.userUuid,
                    message = message,
                    source = EduContextChatSource.Local,
                    state = EduContextChatState.InProgress,
                    timestamp = timestamp)
        }

        override fun sendConversationMessage(message: String, timestamp: Long,
                                             callback: EduContextCallback<EduContextChatItemSendResult>): EduContextChatItem {
            AgoraLog.i("$tag:sendConversationMessage->message:$message, timestamp:$timestamp")
            chatManager?.conversation(message, timestamp, callback)
            return EduContextChatItem(
                    name = config.userName,
                    uid = config.userUuid,
                    message = message,
                    source = EduContextChatSource.Local,
                    state = EduContextChatState.InProgress,
                    timestamp = timestamp)
        }

        override fun fetchChannelHistory(startId: String?, count: Int?, callback: EduContextCallback<List<EduContextChatItem>>) {
            // channel message ids are actually integers, and we need to calculate the
            // previous id to pull channel history messages.
            var id: String? = null
            startId?.toIntOrNull()?.let {
                id = (it - 1).toString()
            }

            AgoraLog.i("$tag:fetchChannelHistory->nextId:$id, count:${count ?: 0}, reverse:true")
            chatManager?.pullChatRecords(id, count ?: 0, true, callback)
        }

        override fun fetchConversationHistory(startId: String?, callback: EduContextCallback<List<EduContextChatItem>>) {
            AgoraLog.i("$tag:fetchConversationHistory->startId:$startId")
            chatManager?.pullConversationRecords(startId, true, callback)
        }
    }

    private val handsUpContextImpl = object : HandsUpContext() {
        override fun performHandsUp(state: EduContextHandsUpState, callback: EduContextCallback<Boolean>?) {
            AgoraLog.i("$tag:performHandsUp->state:${state.name}")
            handsUpManager?.performHandsUp(state, callback)
        }
    }

    private val roomContextImpl: RoomContext = object : RoomContext() {
        override fun roomInfo(): EduContextRoomInfo {
            AgoraLog.i("$tag:roomInfo->roomUuid:${config.roomUuid}, roomName:${config.roomName}, " +
                    "roomType:${EduContextRoomType.fromValue(config.roomType)}")
            return EduContextRoomInfo(
                    config.roomUuid,
                    config.roomName,
                    EduContextRoomType.fromValue(config.roomType))
        }

        override fun leave(exit: Boolean) {
            this@AgoraEduCore.leave(exit)
        }

        override fun uploadLog(quiet: Boolean) {
            AgoraLog.i("$tag:uploadLog")
            eduManager?.uploadDebugItem(DebugItem.LOG, UploadManager.UploadParamTag(
                    config.roomUuid,
                    config.roomName,
                    config.roomType,
                    config.userUuid,
                    config.userName,
                    config.roleType), object : EduCallback<String> {
                override fun onSuccess(res: String?) {
                    AgoraLog.d(tag, "log uploaded ->$res")
                    if (res != null && !quiet) {
                        roomStateManager?.setUploadedLogMsg(res)
                    }
                }

                override fun onFailure(error: EduError) {
                    Constants.AgoraLog.e(tag, "log update failed ->${error.type}:${error.msg}")
                }

            })
        }

        override fun updateFlexRoomProps(properties: MutableMap<String, String>, cause: MutableMap<String, String>?) {
            AgoraLog.i("$tag:updateFlexRoomProps->properties:${Gson().toJson(properties)}, " +
                    "cause:${Gson().toJson(cause)}")
            roomStateManager?.updateFlexProps(properties, cause)
        }

        override fun joinClassroom() {
            synchronized(this@AgoraEduCore) {
                AgoraLog.i("$tag:joinClassroom")
                joinRoomAsStudent(config.userName, config.userUuid,
                        config.autoSubscribe, config.autoPublish,
                        config.needUserListener,
                        object : EduCallback<EduLocalUser?> {
                            override fun onSuccess(res: EduLocalUser?) {
                                res?.let { localUser ->
                                    Constants.AgoraLog.d("$tag-> join classroom success, " +
                                            "user:${localUser.userInfo.userName}, room: ${config.roomName}")
                                    initEduManagers()
                                    addInCoreHandlers()
                                    eduContextPool().roomContext()?.getHandlers()?.forEach {
                                        it.onClassroomJoinSuccess(config.roomUuid, System.currentTimeMillis())
                                    }
                                }
                            }

                            override fun onFailure(error: EduError) {
                                Constants.AgoraLog.d("$tag-> join classroom fail, " +
                                        "user:${config.userName}, room: ${config.roomName}, msg: ${error.msg}")
                                eduContextPool().roomContext()?.getHandlers()?.forEach {
                                    it.onClassroomJoinFail(config.roomUuid, error.type, error.msg, System.currentTimeMillis())
                                }
                            }
                        })
            }
        }
    }

    @Synchronized
    private fun joinRoomAsStudent(name: String?, uuid: String?,
                                  autoSubscribe: Boolean,
                                  autoPublish: Boolean,
                                  needUserListener: Boolean,
                                  callback: EduCallback<EduLocalUser?>) {
        if (roomJoined) {
            AgoraLog.i("$tag:already joined successfully, do not need to join again")
            return
        }

        if (!eduManagerCreated) {
            pendingJoinRoom = true
            return
        }

        val options = RoomJoinOptions(
                uuid!!, name, EduUserRole.STUDENT,
                RoomMediaOptions(autoSubscribe, autoPublish, config.mediaOptions?.encryptionConfigs),
                config.roomType, config.videoEncoderConfig, config.latencyLevel.convert())
        AgoraLog.i("$tag:eduRoom?.join->options:${Gson().toJson(options)}")
        eduRoom?.join(options, object : EduCallback<EduLocalUser> {
            override fun onSuccess(res: EduLocalUser?) {
                if (res != null) {
                    AgoraLog.i("$tag:eduRoom?.join success!")
                    config?.let {
                        val streamUid = res.userInfo.streamUuid.toLongOrNull() ?: return@let
                        reportJoinResult(
                                config = it,
                                streamUid = streamUid,
                                streamSuid = res.userInfo.streamUuid)
                    }

                    // For mobile clients, users will join the room
                    // as students, further requirements may vary
                    eduManager?.reportAppScenario(
                            config.roomType,
                            BuildConfig.SERVICE_APAAS,
                            BuildConfig.APAAS_VERSION)

                    if (needUserListener) {
                        res.eventListener = this@AgoraEduCore
                    }

                    // TODO may need to implement in a more consistent method
                    AgoraEduSDK.agoraEduLaunchCallback.onCallback(AgoraEduEvent.AgoraEduEventReady)

                    localUser = res
                    callback.onSuccess(localUser)

                    synchronized(this@AgoraEduCore) {
                        roomJoined = true
                    }
                    reportClassJoinResult("1", null, null)
                } else {
                    joinRoomFailed()
                    val error = EduError.internalError("join failed: localUser is null")
                    callback.onFailure(error)
                    AgoraLog.i("$tag:eduRoom?.join failed->${Gson().toJson(error)}")

                    reportClassJoinResult("0", error.type.toString() + "", error.httpError.toString() + "")
                    config?.let {
                        reportJoinResult(config = it, code = error.type)
                    }
                }
            }

            override fun onFailure(error: EduError) {
                joinRoomFailed()
                callback.onFailure(error)
                AgoraLog.i("$tag:eduRoom?.join failed->${Gson().toJson(error)}")

                reportClassJoinResult("0", error.type.toString() + "", error.httpError.toString() + "")
                config?.let {
                    reportJoinResult(config = it, code = error.type)
                }
            }
        })
    }

    private fun reportJoinResult(config: AgoraEduCoreConfig, streamUid: Long = 0,
                                 streamSuid: String = "", code: Int = 0) {
        val reporterV2 = ReporterV2.getReporterV2(config.vendorId)
        getMainRoomStatus(object : EduCallback<EduRoomStatus> {
            override fun onSuccess(res: EduRoomStatus?) {
                res?.let {
                    reporterV2.setRoomReportInfo(
                            BuildConfig.APAAS_VERSION, config.roomUuid,
                            config.userUuid, config.userName, streamUid, streamSuid,
                            AgoraEduRoleType.fromValue(config.roleType).toString(),
                            eduRoom!!.getRtcCallId(config.roomUuid),
                            eduRoom!!.getRtmSessionId(), it.createTime)
                    reporterV2.reportAPaaSUserJoined(code, System.currentTimeMillis())
                }
            }

            override fun onFailure(error: EduError) {
            }
        })
    }

    protected open fun getMainRoomStatus(callback: EduCallback<EduRoomStatus>) {
        eduRoom?.getRoomStatus(object : EduCallback<EduRoomStatus> {
            override fun onSuccess(res: EduRoomStatus?) {
                if (res == null) {
                    callback.onFailure(internalError("current eduRoom`s status is null"))
                } else {
                    callback.onSuccess(res)
                }
            }

            override fun onFailure(error: EduError) {
                callback.onFailure(error)
            }
        })
    }

    private fun reportClassJoinResult(result: String, errorCode: String?, httpCode: String?) {
        getReporter().reportRoomEntryEnd(result, errorCode, httpCode, null)
    }

    fun getReporter(): APaasReporter {
        return getAPaasReporter()
    }

    private fun joinRoomFailed() {
        // write join failure flag, check on next success
        agoraEduLog.writeLogSign(true)
        AgoraEduSDK.agoraEduLaunchCallback.onCallback(AgoraEduEvent.AgoraEduEventFailed)
    }

    private val whiteboardGranted = object : ((String) -> Boolean) {
        override fun invoke(p1: String): Boolean {
            return whiteBoardManager?.isGranted(p1) ?: false
        }
    }

    private fun initEduManagers() {
        roomStateManager = RoomStateManager(context,
                eduContextPool().roomContext(),
                config, eduRoom)
        roomStateManager?.let { it ->
            roomStateManager = it
            it.setClassName(config.roomName)
            it.initClassState()
        }

        localUser?.let { user ->
            chatManager = ChatManager(context, eduRoom, eduContextPool, config, user)
            chatManager!!.initChat()

            privateChatManager = PrivateChatManager(
                    context, eduContextPool, eduRoom, config, user)

            easeImManager = IMManager()

            TeacherVideoManager(context.applicationContext, config, eduRoom,
                    user, eduContextPool, whiteboardGranted).let { manager ->
                teacherVideoManager = manager
                manager.notifyUserDetailInfo(EduUserRole.STUDENT)
                manager.notifyUserDetailInfo(EduUserRole.TEACHER)
                manager.screenShareStarted = object : () -> Boolean {
                    override fun invoke(): Boolean {
                        return screenShareManager?.isScreenSharing() ?: false
                    }
                }
            }

            DeviceManager(context.applicationContext, config, eduRoom, user, eduContextPool()).let {
                deviceManager = it
                it.initDeviceConfig()
            }

            ScreenShareManager(context, eduContextPool(), config, eduRoom, user).let { manager ->
                screenShareManager = manager
                // onRemoteRTCJoinedOfStreamId maybe called before screenShareManager is instantiated
                // so, when screenShareManager is instantiated, try to get the missing rtcUid from the cache
                manager.updateRemoteOnlineUids(user.cacheRemoteOnlineUserIds)
                manager.screenShareStateChangedListener = object : (Boolean) -> Unit {
                    override fun invoke(p1: Boolean) {
                        teacherVideoManager?.container?.let { container ->
                            // now, renderView is TextureView, there is no render hierarchy issue
                            // so comment out the following code and del teacherVideoManager?.videoGroupListener
//                            teacherVideoManager?.videoGroupListener?.onRendererContainer(
//                                    container, teacherVideoManager!!.teacherCameraStreamUuid)
                        }
                    }
                }
                manager.getWhiteBoardCurScenePathListener = object : () -> String? {
                    override fun invoke(): String? {
                        return whiteBoardManager?.getCurScenePath()
                    }

                }
            }

            OneToOneVideoManager(context.applicationContext, config, eduRoom,
                    user, eduContextPool, whiteboardGranted).let { manager ->
                oneToOneVideoManager = manager
                manager.initLocalDeviceState(deviceManager!!.getDeviceConfig())
                manager.notifyUserDetailInfo(EduUserRole.STUDENT)
                manager.notifyUserDetailInfo(EduUserRole.TEACHER)
            }

            eduContextPool().handsUpContext()?.let { handsUpContext ->
                HandsUpManager(context.applicationContext, eduContextPool, config, eduRoom, user).let { manager ->
                    handsUpManager = manager
                    manager.initHandsUpData()
                }
            }

            UserListManager(context.applicationContext, config, eduRoom,
                    user, eduContextPool, whiteboardGranted).let { manager ->
                userListManager = manager
                manager.initLocalDeviceState(deviceManager!!.getDeviceConfig())
                manager.notifyUserList()
            }

            FlexPropsManager(context, eduContextPool(), config,
                    eduRoom, user, whiteboardGranted).let {
                flexPropsManager = it
                flexPropsManager?.initRoomFlexProps()
            }
        }
    }

    /**
     * Default handlers that handle in-core communications between
     * contexts.
     */
    private fun addInCoreHandlers() {
        eduContextPool().deviceContext()?.addHandler(object : DeViceHandler() {
            override fun onCameraDeviceEnableChanged(enabled: Boolean) {
                if (config.roomType == AgoraEduRoomType.AgoraEduRoomType1V1.value) {
                    oneToOneVideoManager?.updateLocalCameraSwitchState(!enabled)
                    oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.STUDENT)
                } else {
                    userListManager?.updateLocalCameraSwitchState(!enabled)
                    userListManager?.notifyUserList()
                }
            }

            override fun onMicDeviceEnabledChanged(enabled: Boolean) {
                if (config.roomType == AgoraEduRoomType.AgoraEduRoomType1V1.value) {
                    oneToOneVideoManager?.updateLocalMicSwitchState(!enabled)
                    oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.STUDENT)
                } else {
                    userListManager?.updateLocalMicSwitchState(!enabled)
                    userListManager?.notifyUserList()
                }
            }
        })

        eduContextPool().whiteboardContext()?.addHandler(object : WhiteboardHandler() {
            override fun onPermissionGranted(granted: Boolean) {
                if (config.roomType == AgoraEduRoomType.AgoraEduRoomType1V1.value) {
                    oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.STUDENT)
                } else {
                    userListManager?.notifyUserList()
                }
            }
        })
    }

    private val mediaContextImpl: MediaContext = object : MediaContext() {
        override fun startPreview(container: ViewGroup) {
            AgoraLog.i("$tag:startRenderLocalVideo->container:$container")
            val videoTrack = eduManager?.getEduMediaControl()?.createCameraVideoTrack()
            val b = videoTrack?.setRenderConfig(EduRenderConfig())
            AgoraLog.i("$tag:setRenderConfig->$b")
            val a = videoTrack?.setView(container)
            AgoraLog.i("$tag:startPreview->$a")
        }

        override fun stopPreview() {
            val videoTrack = eduManager?.getEduMediaControl()?.createCameraVideoTrack()
            val a = videoTrack?.setView(null)
            AgoraLog.i("$tag:stopPreview->$a")
        }

        override fun openCamera() {
            val videoTrack = eduManager?.getEduMediaControl()?.createCameraVideoTrack()
            val a = videoTrack?.start()
            AgoraLog.i("$tag:openCamera->$a")
        }

        override fun closeCamera() {
            val videoTrack = eduManager?.getEduMediaControl()?.createCameraVideoTrack()
            val a = videoTrack?.stop()
            AgoraLog.i("$tag:closeCamera->$a")
        }

        override fun openMicrophone() {
            val audioTrack = eduManager?.getEduMediaControl()?.createMicrophoneTrack()
            val a = audioTrack?.start()
            AgoraLog.i("$tag:openMicrophone->$a")
        }

        override fun closeMicrophone() {
            val audioTrack = eduManager?.getEduMediaControl()?.createMicrophoneTrack()
            val a = audioTrack?.stop()
            AgoraLog.i("$tag:closeMicrophone->$a")
        }

        override fun publishStream(type: EduContextMediaStreamType) {
            val hasVideo = type != EduContextMediaStreamType.Audio
            val hasAudio = type != EduContextMediaStreamType.Video

            localUser?.let { user ->
                eduRoom?.getFullStreamList(object : EduCallback<MutableList<EduStreamInfo>> {
                    override fun onSuccess(res: MutableList<EduStreamInfo>?) {
                        var stream = res?.find { it.publisher.userUuid == user.userInfo.userUuid }
                        if (stream == null) {
                            stream = EduStreamInfo(user.userInfo.streamUuid, null,
                                    VideoSourceType.CAMERA, hasVideo, hasAudio, user.userInfo)
                        } else {
                            if (type == EduContextMediaStreamType.Video) {
                                stream.hasVideo = true
                            }
                            if (type == EduContextMediaStreamType.Audio) {
                                stream.hasAudio = true
                            }
                            if (type == EduContextMediaStreamType.All) {
                                stream.hasVideo = hasVideo
                                stream.hasAudio = hasAudio
                            }
                        }
                        AgoraLog.i("$tag:publishStream->stream:${Gson().toJson(stream)}")
                        user.publishStream(stream, object : EduCallback<Boolean> {
                            override fun onSuccess(res: Boolean?) {

                            }

                            override fun onFailure(error: EduError) {

                            }
                        })
                    }

                    override fun onFailure(error: EduError) {

                    }
                })
            }
        }

        override fun unPublishStream(type: EduContextMediaStreamType) {
            localUser?.let { user ->
                eduRoom?.getFullStreamList(object : EduCallback<MutableList<EduStreamInfo>> {
                    override fun onSuccess(res: MutableList<EduStreamInfo>?) {
                        val stream = res?.find { it.publisher.userUuid == user.userInfo.userUuid }
                        stream?.let {
                            if (type == EduContextMediaStreamType.Video) {
                                it.hasVideo = false
                            }

                            if (type == EduContextMediaStreamType.Audio) {
                                it.hasAudio = false
                            }

                            if (type == EduContextMediaStreamType.All) {
                                it.hasVideo = false
                                it.hasAudio = false
                            }

                            AgoraLog.i("$tag:unPublishStream->stream:${Gson().toJson(it)}")
                            user.unPublishStream(it, object : EduCallback<Boolean> {
                                override fun onSuccess(res: Boolean?) {

                                }


                                override fun onFailure(error: EduError) {
                                }
                            })
                        }
                    }

                    override fun onFailure(error: EduError) {

                    }
                })
            }
        }

        override fun setVideoEncoderConfig(videoEncoderConfig: EduContextVideoEncoderConfig) {
            localUser?.resetVideoEncoderConfig(convert(videoEncoderConfig))
        }
    }

    private val deviceContextImpl: DeviceContext = object : DeviceContext() {
        override fun getDeviceConfig(): EduContextDeviceConfig {
            return deviceManager?.getDeviceConfig() ?: EduContextDeviceConfig()
        }

        override fun setCameraDeviceEnable(enable: Boolean) {
            AgoraLog.i("$tag:setCameraDeviceEnable->enable:$enable")
            deviceManager?.setCameraDeviceEnable(enable)
        }

        override fun switchCameraFacing() {
            deviceManager?.switchCameraFacing()
        }

        override fun setMicDeviceEnable(enable: Boolean) {
            AgoraLog.i("$tag:setMicDeviceEnable->enable:$enable")
            deviceManager?.setMicDeviceEnable(enable)
        }

        override fun setSpeakerEnable(enable: Boolean) {
            AgoraLog.i("$tag:setSpeakerEnable->enable:$enable")
            deviceManager?.setSpeakerEnable(enable)
        }

        override fun setDeviceLifecycle(lifecycle: EduContextDeviceLifecycle) {
            deviceManager?.setDeviceLifecycle(lifecycle)
        }
    }

    private val screenShareContextImpl: ScreenShareContext = object : ScreenShareContext() {
        override fun setScreenShareState(state: EduContextScreenShareState) {
            AgoraLog.i("$tag:setScreenShareState->state:${Gson().toJson(state)}")
            screenShareManager?.setScreenShareState(state)
        }

        override fun renderScreenShare(container: ViewGroup?, streamUuid: String) {
            AgoraLog.i("$tag:renderScreenShare->container:$container, streamUuid:$streamUuid")
            screenShareManager?.renderScreenShare(container, streamUuid)
        }
    }

    private val userContextImpl: UserContext = object : UserContext() {
        override fun localUserInfo(): EduContextUserInfo {
            var uid = ""
            var name = ""
            var properties: MutableMap<String, String>? = null

            eduRoom?.getLocalUser(object : EduCallback<EduLocalUser> {
                override fun onSuccess(res: EduLocalUser?) {
                    res?.let {
                        uid = it.userInfo.userUuid
                        name = it.userInfo.userName
                        properties = userListManager?.getUserFlexProps(uid)
                    }
                }

                override fun onFailure(error: EduError) {

                }
            })

            return EduContextUserInfo(
                    userUuid = uid,
                    userName = name,
                    properties = properties)
        }

        override fun muteVideo(muted: Boolean) {
            AgoraLog.i("$tag:userContextImpl-muteVideo->muted:$muted")
            userListManager?.muteLocalVideo(muted)
        }

        override fun muteAudio(muted: Boolean) {
            AgoraLog.i("$tag:userContextImpl-muteAudio->muted:$muted")
            userListManager?.muteLocalAudio(muted)
        }

        override fun renderVideo(container: ViewGroup?, streamUuid: String, renderConfig: EduContextRenderConfig) {
            AgoraLog.i("$tag:userContextImpl-renderVideo->container:$container, streamUuid:$streamUuid")
            userListManager?.renderContainer(container, streamUuid, convert(renderConfig))
        }

        override fun updateFlexUserProps(userUuid: String, properties: MutableMap<String, String>,
                                         cause: MutableMap<String, String>?) {
            AgoraLog.i("$tag:userContextImpl-updateFlexUserProps->userUuid:$userUuid, properties:${Gson().toJson(properties)}, " +
                    "cause:${Gson().toJson(cause)}")
            userListManager?.updateFlexProps(userUuid, properties, cause)
        }

        override fun setVideoEncoderConfig(config: EduContextVideoEncoderConfig) {
            AgoraLog.i("$tag:userContextImpl-setVideoEncoderConfig->config:${Gson().toJson(config)}")
            localUser?.resetVideoEncoderConfig(convert(config))
        }
    }

    private val videoContextImpl: VideoContext = object : VideoContext() {
        override fun updateVideo(enabled: Boolean) {
            AgoraLog.i("$tag:videoContextImpl-updateVideo->enabled:$enabled")
            when (config.roomType) {
                AgoraEduRoomType.AgoraEduRoomType1V1.value -> {
                    oneToOneVideoManager?.muteLocalVideo(!enabled)
                }
                AgoraEduRoomType.AgoraEduRoomTypeSmall.value -> {
                    teacherVideoManager?.muteLocalVideo(!enabled)
                }
                AgoraEduRoomType.AgoraEduRoomTypeBig.value -> {
                    teacherVideoManager?.muteLocalVideo(!enabled)
                }
            }
        }

        override fun updateAudio(enabled: Boolean) {
            AgoraLog.i("$tag:videoContextImpl-updateAudio->enabled:$enabled")
            when (config.roomType) {
                AgoraEduRoomType.AgoraEduRoomType1V1.value -> {
                    oneToOneVideoManager?.muteLocalAudio(!enabled)
                }
                AgoraEduRoomType.AgoraEduRoomTypeSmall.value -> {
                    teacherVideoManager?.muteLocalAudio(!enabled)
                }
                AgoraEduRoomType.AgoraEduRoomTypeBig.value -> {
                    teacherVideoManager?.muteLocalAudio(!enabled)
                }
            }
        }

        override fun renderVideo(viewGroup: ViewGroup?, streamUuid: String, renderConfig: EduContextRenderConfig) {
            AgoraLog.i("$tag:videoContextImpl-renderVideo->viewGroup:$viewGroup, streamUuid:$streamUuid")
            when (config.roomType) {
                AgoraEduRoomType.AgoraEduRoomType1V1.value -> {
                    oneToOneVideoManager?.renderVideo(viewGroup, streamUuid, convert(renderConfig))
                }
                AgoraEduRoomType.AgoraEduRoomTypeSmall.value -> {
                    teacherVideoManager?.renderVideo(viewGroup, streamUuid, convert(renderConfig))
                }
                AgoraEduRoomType.AgoraEduRoomTypeBig.value -> {
                    teacherVideoManager?.renderVideo(viewGroup, streamUuid, convert(renderConfig))
                }
            }
        }

        override fun setVideoEncoderConfig(config: EduContextVideoEncoderConfig) {
            AgoraLog.i("$tag:videoContextImpl-setVideoEncoderConfig->config:${Gson().toJson(config)}")
            localUser?.resetVideoEncoderConfig(convert(config))
        }
    }

    private val whiteboardContextImpl: WhiteboardContext = object : WhiteboardContext() {
        override fun initWhiteboard(container: ViewGroup) {
            AgoraLog.i("$tag:initWhiteboard->container:$container")
            initWhiteboardManager(container, this)
        }

        override fun joinWhiteboard() {
            AgoraLog.i("$tag:joinWhiteboard")
            synchronized(this@AgoraEduCore) {
                if (whiteboardJoined) {
                    return
                }

                if (!eduManagerCreated) {
                    pendingJoinWhiteboard = true
                    return
                }

                if (whiteBoardManager == null) {
                    Constants.AgoraLog.e("$tag -> whiteboard context needs to be initialized first")
                }

                whiteBoardManager?.initBoardWithRoomToken(
                        config.boardId, config.boardToken, config.userUuid)
            }
        }

        override fun isGranted(): Boolean {
            return whiteBoardManager?.isGranted(config.userUuid) ?: false
        }

        override fun leave() {
            AgoraLog.i("$tag:releaseBoard")
            whiteBoardManager?.releaseBoard()
        }

        override fun selectAppliance(type: WhiteboardApplianceType) {
            whiteBoardManager?.onApplianceSelected(type)
        }

        override fun selectColor(color: Int) {
            whiteBoardManager?.onColorSelected(color)
        }

        override fun selectFontSize(size: Int) {
            whiteBoardManager?.onFontSizeSelected(size)
        }

        override fun selectThickness(thick: Int) {
            whiteBoardManager?.onThicknessSelected(thick)
        }

        override fun selectRoster(anchor: View) {
            userContextImpl.getHandlers()?.forEach { handler ->
                handler.onRoster(context, anchor,
                        when (config.roomType) {
                            AgoraEduRoomType.AgoraEduRoomTypeSmall.value -> {
                                io.agora.agoraeduuikit.impl.users.AgoraUIRoster.RosterType.SmallClass.value()
                            }
                            AgoraEduRoomType.AgoraEduRoomTypeBig.value -> {
                                io.agora.agoraeduuikit.impl.users.AgoraUIRoster.RosterType.LargeClass.value()
                            }
                            else -> null
                        })
            }
        }

        override fun setBoardInputEnable(enable: Boolean) {
            whiteBoardManager?.onBoardInputEnabled(enable)
        }

        override fun skipDownload(url: String?) {
            whiteBoardManager?.onDownloadSkipped(url)
        }

        override fun cancelDownload(url: String?) {
            whiteBoardManager?.onDownloadCanceled(url)
        }

        override fun retryDownload(url: String?) {
            whiteBoardManager?.onDownloadRetry(url)
        }

        override fun setFullScreen(full: Boolean) {
            whiteBoardManager?.onBoardFullScreen(full)
        }

        override fun setZoomOut() {
            whiteBoardManager?.onBoardZoomOut()
        }

        override fun setZoomIn() {
            whiteBoardManager?.onBoardZoomIn()
        }

        override fun setPrevPage() {
            whiteBoardManager?.onBoardPrevPage()
        }

        override fun setNextPage() {
            whiteBoardManager?.onBoardNextPage()
        }
    }

    private fun initWhiteboardManager(container: ViewGroup, whiteboardContext: WhiteboardContext) {
        synchronized(this@AgoraEduCore) {
            // The initialization of whiteboard sdk needs a whiteboard container
            // from outside world to display ppt docs, for example.
            // Thus it has to be triggered explicitly by users when
            // they can find an appropriate container.
            // Only after the initialization successes could we continue
            // the rest of whiteboard operations like join/leave
            if (whiteBoardManager == null) {
                whiteBoardManager = WhiteBoardManager(context, config, container, whiteboardContext)
                whiteBoardManager!!.whiteBoardManagerEventListener = object : WhiteBoardManagerEventListener {
                    override fun onWhiteBoardJoinSuccess(config: WhiteboardDrawingConfig) {
                        AgoraLog.i("$tag:onWhiteBoardJoinSuccess->config:${Gson().toJson(config)}")
                        eduContextPool().whiteboardContext()?.getHandlers()?.forEach {
                            it.onWhiteboardJoinSuccess(config)
                        }

                        synchronized(this@AgoraEduCore) {
                            whiteboardJoined = true
                        }
                    }

                    override fun onWhiteBoardJoinFail(error: SDKError?) {
                        AgoraLog.i("$tag:onWhiteBoardJoinFail->error:${Gson().toJson(error)}")
                        eduContextPool().whiteboardContext()?.getHandlers()?.forEach {
                            it.onWhiteboardJoinFail(error?.message ?: "")
                        }
                    }

                    override fun onSceneChanged(state: SceneState) {
                        //TODO may need callback this event
                        screenShareManager?.checkAndNotifyScreenShareByScene(state)
                    }

                    override fun onGrantedChanged() {
                        AgoraLog.i("$tag:onGrantedChanged")
                        userListManager?.notifyUserList()
                    }

                    override fun onStartAudioMixing(filepath: String, loopback: Boolean, replace: Boolean, cycle: Int) {
                        eduRoom?.getLocalUser(object : EduCallback<EduLocalUser> {
                            override fun onSuccess(res: EduLocalUser?) {
                                res?.startAudioMixing(filepath, loopback, replace, cycle)
                            }

                            override fun onFailure(error: EduError) {
                            }
                        })
                    }

                    override fun onStopAudioMixing() {
                        eduRoom?.getLocalUser(object : EduCallback<EduLocalUser> {
                            override fun onSuccess(res: EduLocalUser?) {
                                res?.stopAudioMixing()
                            }

                            override fun onFailure(error: EduError) {
                            }
                        })
                    }

                    override fun onSetAudioMixingPosition(position: Int) {
                        eduRoom?.getLocalUser(object : EduCallback<EduLocalUser> {
                            override fun onSuccess(res: EduLocalUser?) {
                                res?.setAudioMixingPosition(position)
                            }

                            override fun onFailure(error: EduError) {
                            }
                        })
                    }
                }

                //TODO refactor, obtain course ware from an inner module,
                // instead of from outside edu sdk
                var ware: AgoraEduCourseware? = null
                if (AgoraEduSDK.COURSEWARES.size > 0) {
                    ware = AgoraEduSDK.COURSEWARES[0]
                }
                whiteBoardManager!!.initData(config.roomUuid, config.boardAppId,
                        config.boardRegion, ware)

                Constants.AgoraLog.w("$tag, initialize whiteboard with container, room uid ${config.roomUuid}")
            } else {
                Constants.AgoraLog.w("$tag, repeatedly initialize whiteboard")
            }
        }
    }

    private val privateChatContext: PrivateChatContext = object : PrivateChatContext() {
        override fun getLocalUserInfo(): EduContextUserInfo {
            return EduContextUserInfo(
                    config.userUuid,
                    config.userName,
                    EduContextUserRole.Student,
                    privateChatManager?.getUserFlexProps(config.userUuid))
        }

        override fun startPrivateChat(peerId: String, callback: EduContextCallback<EduContextPrivateChatInfo>?) {
            AgoraLog.i("$tag:startPrivateChat->peerId:$peerId")
            privateChatManager?.startPrivateChat(peerId, Callback { response ->
                response?.let { _ ->
                    eduRoom?.getFullUserList(object : EduCallback<MutableList<EduUserInfo>> {
                        override fun onSuccess(res: MutableList<EduUserInfo>?) {
                            res?.find { it.userUuid == peerId }?.let { user ->
                                val toUserInfo = EduContextUserInfo(
                                        user.userUuid,
                                        user.userName,
                                        toEduContextUserInfo(user.role),
                                        privateChatManager?.getUserFlexProps(user.userUuid))

                                callback?.onSuccess(EduContextPrivateChatInfo(getLocalUserInfo(), toUserInfo))
                            }
                        }

                        override fun onFailure(error: EduError) {
                            callback?.onFailure(EduContextError(error.type, error.msg))
                        }
                    })
                }
            })
        }

        private fun toEduContextUserInfo(role: EduUserRole): EduContextUserRole {
            return when (role) {
                EduUserRole.TEACHER -> EduContextUserRole.Teacher
                EduUserRole.STUDENT -> EduContextUserRole.Student
                EduUserRole.ASSISTANT -> EduContextUserRole.Assistant
                EduUserRole.EduRoleTypeInvalid -> EduContextUserRole.Assistant
            }
        }

        override fun endPrivateChat(callback: EduContextCallback<Boolean>?) {
            AgoraLog.i("$tag:endPrivateChat")
            privateChatManager?.stopSideVoiceChat(Callback {
                it?.let {
                    callback?.onSuccess(true)
                }
            })
        }
    }

    private val extAppContext: ExtAppContext = object : ExtAppContext {
        override fun init(container: RelativeLayout) {
            initExtAppManager(container, config)
        }

        private fun initExtAppManager(layout: RelativeLayout, config: AgoraEduCoreConfig) {
            AgoraLog.i("$tag:initExtAppManager")
            extAppManager = object : AgoraExtAppManager(config.appId, context, layout, config.roomUuid) {
                override fun getRoomInfo(): AgoraExtAppRoomInfo {
                    return AgoraExtAppRoomInfo(
                            config.roomUuid,
                            config.roomName,
                            config.roomType)
                }

                override fun getLocalUserInfo(): AgoraExtAppUserInfo {
                    return AgoraExtAppUserInfo(config.userUuid, config.userName,
                            AgoraExtAppUserRole.toType(config.roleType))
                }
            }

            // Explicitly call extension manager to search for
            // extension app information.
            AgoraLog.i("$tag:handleExtAppPropertyInitialized")
            extAppManager?.handleExtAppPropertyInitialized(eduRoom?.roomProperties)
        }

        override fun launchExtApp(appIdentifier: String): Int {
            AgoraLog.i("$tag:launchExtApp->appIdentifier:$appIdentifier")
            return extAppManager?.launchExtApp(appIdentifier, TimeUtil.currentTimeMillis())
                    ?: AgoraExtAppErrorCode.ExtAppEngineError
        }

        override fun getRegisteredExtApps(): List<AgoraExtAppInfo> {
            return extAppManager?.getRegisteredApps() ?: mutableListOf()
        }
    }

    fun eduContextPool(): EduContextPool {
        return eduContextPool
    }

    private fun leave(exit: Boolean) {
        AgoraLog.i("$tag:leave")
        eduRoom?.leave(object : EduCallback<Unit> {
            override fun onSuccess(res: Unit?) {
                Constants.AgoraLog.e("$tag:leave EduRoom ${config.roomName} success")
                eduContextPool().roomContext()?.getHandlers()?.forEach {
                    it.onClassroomLeft(config.roomUuid, System.currentTimeMillis(), exit)
                }

                eduRoom = null
                ReporterV2.getReporterV2(config.vendorId)
                        .reportAPaaSUserQuit(0, System.currentTimeMillis())
                ReporterV2.deleteReporterV2(config.vendorId)
            }

            override fun onFailure(error: EduError) {
                Constants.AgoraLog.e("$tag:leave EduRoom error-> " +
                        "code: ${error.type}, reason: ${error.msg}")
                ReporterV2.getReporterV2(config.vendorId)
                        .reportAPaaSUserQuit(error.type, System.currentTimeMillis())
            }
        })
    }

    fun release() {
        AgoraLog.i("$tag:release")
        // Stop rtm request channel for current region.
        AgoraRequestClient.enableRtmRequestChannel(config.boardRegion, false)

        roomStateManager?.dispose()
        privateChatManager?.dispose()
        extAppManager?.dispose()
        teacherVideoManager?.dispose()
        screenShareManager?.dispose()
        userListManager?.dispose()
        handsUpManager?.dispose()

        eduManager?.let {
            it.eduManagerEventListener = null
            it.release()
        }

        AgoraEduSDK.agoraEduLaunchCallback.onCallback(AgoraEduEvent.AgoraEduEventDestroyed)
    }

    companion object {

        fun setAgoraEduSDKConfig(mConfig: AgoraEduSDKConfig) {
            AgoraEduSDK.setConfig(mConfig)
        }

        fun launch(context: Context, config: AgoraEduLaunchConfig, callback: AgoraEduLaunchCallback) {
            // Every time launch is called, the authentication information should
            // be reset (as request headers) according to different room id, user
            // id and region, so on.
            AgoraRequestClient.setAuthInfo(config.rtmToken, config.userUuid)
            AgoraEduSDK.launch(context, config, callback)
        }
    }

    // Agora Edu Manager Event Listener callbacks
    override fun onUserMessageReceived(message: EduMessage) {
        AgoraLog.i("$tag:onUserMessageReceived->message:${Gson().toJson(message)}")
    }

    override fun onUserChatMessageReceived(chatMsg: EduPeerChatMessage) {
        AgoraLog.i("$tag:onUserChatMessageReceived->chatMsg:${Gson().toJson(chatMsg)}")
        chatManager?.receiveConversationMessage(chatMsg)
    }

    override fun onUserActionMessageReceived(actionMessage: EduActionMessage) {
        AgoraLog.i("$tag:onUserActionMessageReceived->actionMessage:${Gson().toJson(actionMessage)}")
    }

    // Edu Room Event Listener callbacks
    override fun onRemoteUsersInitialized(users: List<EduUserInfo>, classRoom: EduRoom) {
        AgoraLog.i("$tag:onRemoteUsersInitialized->users:${Gson().toJson(users)}, room: ${config.roomUuid}")
        // Nothing done
    }

    override fun onRemoteUsersJoined(users: List<EduUserInfo>, classRoom: EduRoom) {
        AgoraLog.e("$tag:Receive a callback when the remote user joined, " +
                "users:${Gson().toJson(users)}, room: ${config.roomUuid}")
        if (config.roomType != AgoraEduRoomType.AgoraEduRoomType1V1.value) {
            userListManager?.notifyUserList()
        }
    }

    override fun onRemoteUserLeft(userEvent: EduUserEvent, classRoom: EduRoom) {
        AgoraLog.e("$tag:Receive a callback when the remote user left, " +
                "userEvent:${Gson().toJson(userEvent)}, room: ${config.roomUuid}")
        if (config.roomType != AgoraEduRoomType.AgoraEduRoomType1V1.value) {
            userListManager?.notifyUserList()
        }
    }

    override fun onRemoteUserUpdated(userEvent: EduUserEvent, type: EduUserStateChangeType, classRoom: EduRoom) {
        AgoraLog.e("$tag:Receive a callback when the remote user modified, " +
                "userEvent:${Gson().toJson(userEvent)}, type: $type, room: ${config.roomUuid}")
        // Nothing done
    }

    override fun onRoomMessageReceived(message: EduMessage, classRoom: EduRoom) {
        AgoraLog.i("$tag:onRoomMessageReceived->message:${Gson().toJson(message)}, " +
                "room: ${config.roomUuid}")
        // Nothing done
    }

    override fun onRoomChatMessageReceived(chatMsg: EduChatMessage, classRoom: EduRoom) {
        AgoraLog.i("$tag:onRoomChatMessageReceived->chatMsg:${Gson().toJson(chatMsg)}, " +
                "room: ${config.roomUuid}")
        chatManager?.receiveRemoteChatMessage(chatMsg)
    }

    override fun onRemoteStreamsInitialized(streams: List<EduStreamInfo>, classRoom: EduRoom) {
        AgoraLog.i("$tag:onRemoteStreamsInitialized->streams:${Gson().toJson(streams)}, " +
                "room: ${config.roomUuid}")
        screenShareManager?.checkAndNotifyScreenShareRestored()
    }

    override fun onRemoteStreamsAdded(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        AgoraLog.i("$tag:onRemoteStreamsAdded->streamEvents:${Gson().toJson(streamEvents)}, " +
                "room: ${config.roomUuid}")
        notifyRemoteStreamChanged(streamEvents)
    }

    override fun onRemoteStreamUpdated(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        AgoraLog.i("$tag:onRemoteStreamUpdated->streamEvents:${Gson().toJson(streamEvents)}, " +
                "room: ${config.roomUuid}")
        notifyRemoteStreamChanged(streamEvents)
    }

    override fun onRemoteStreamsRemoved(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        AgoraLog.i("$tag:onRemoteStreamsRemoved->streamEvents:${Gson().toJson(streamEvents)}, " +
                "room: ${config.roomUuid}")
        notifyRemoteStreamChanged(streamEvents)
    }

    private fun notifyRemoteStreamChanged(streamEvents: MutableList<EduStreamEvent>) {
        when (config.roomType) {
            AgoraEduRoomType.AgoraEduRoomType1V1.value -> {
                oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
            }
            AgoraEduRoomType.AgoraEduRoomTypeSmall.value,
            AgoraEduRoomType.AgoraEduRoomTypeBig.value -> {
                teacherVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
                if (streamEvents.find { it.modifiedStream.publisher.role == EduUserRole.TEACHER } == null) {
                    userListManager?.notifyUserList()
                }
            }
        }

        screenShareManager?.checkAndNotifyScreenShareStarted(streamEvents)
    }

    override fun onRemoteRTCJoinedOfStreamId(streamUuid: String) {
        AgoraLog.i("$tag:onRemoteRTCJoinedOfStreamId->streamUuid:$streamUuid")
        screenShareManager?.updateRemoteOnlineUids(streamUuid, true)
        screenShareManager?.checkAndNotifyScreenShareByRTC(streamUuid)
    }

    override fun onRemoteRTCOfflineOfStreamId(streamUuid: String) {
        AgoraLog.i("$tag:onRemoteRTCOfflineOfStreamId->streamUuid:$streamUuid")
        screenShareManager?.updateRemoteOnlineUids(streamUuid, false)
        screenShareManager?.checkAndNotifyScreenShareByRTC(streamUuid)
    }

    override fun onRoomStatusChanged(type: EduRoomChangeType, operatorUser: EduUserInfo?, classRoom: EduRoom) {
        AgoraLog.i("$tag:onRoomStatusChanged->type:$type, operatorUser:${Gson().toJson(operatorUser)}, " +
                "room: ${config.roomUuid}")
        chatManager?.notifyMuteChatStatus(type)
        roomStateManager?.updateClassState(type)
    }

    override fun onRoomPropertiesChanged(changedProperties: MutableMap<String, Any>,
                                         classRoom: EduRoom, cause: MutableMap<String, Any>?,
                                         operator: EduBaseUserInfo?) {
        AgoraLog.i("$tag:onRoomPropertiesChanged->changedProperties:${Gson().toJson(changedProperties)}, " +
                "cause:${Gson().toJson(cause)}, operator:${Gson().toJson(operator)}, room: ${config.roomUuid}")
        if (config.roomType != AgoraEduRoomType.AgoraEduRoomType1V1.value) {
            handsUpManager?.notifyHandsUpEnable(cause)
            handsUpManager?.notifyHandsUpState(cause)
            userListManager?.notifyListByPropertiesChanged(cause)
        }

        extAppManager?.handleRoomPropertiesChange(classRoom.roomProperties, cause)
        screenShareManager?.checkAndNotifyScreenShareByProperty(cause)
        flexPropsManager?.notifyRoomFlexProps(changedProperties, cause, operator)
    }

    override fun onNetworkQualityChanged(quality: NetworkQuality, user: EduBaseUserInfo, classRoom: EduRoom) {
//        AgoraLog.i("$tag:onNetworkQualityChanged->quality:${quality.name}, user:${Gson().toJson(user)}, " +
//                "room: ${config.roomUuid}")
        if (user.userUuid == config.userUuid) {
            roomStateManager?.updateNetworkState(quality)
        }
    }

    override fun onConnectionStateChanged(state: ConnectionState, classRoom: EduRoom) {
        Constants.AgoraLog.e("$tag:onConnectionStateChanged-> " +
                "${state.name}, room: ${config.roomUuid}")

        val s = EduContextConnectionState.convert(state.value)
        roomStateManager?.updateConnectionState(s)

        // when reconnected, need sync deviceConfig to remote.
        if (roomStateManager?.isReconnected(s) == true) {
            deviceManager?.syncDeviceConfig()
        }

        if (s == EduContextConnectionState.Aborted) {
            rtmConnectionState = state.value
            leave(true)
            return
        } else if (s == EduContextConnectionState.Connected) {
            if (rtmConnectionState == EduContextConnectionState.Reconnecting.value) {
                ReporterV2.getReporterV2(config.vendorId)
                        .reportAPpaSUserReconnect(0, System.currentTimeMillis())
            }
        }
        rtmConnectionState = state.value
    }

    // Edu User Event Listener callbacks
    override fun onRemoteVideoStateChanged(rtcChannel: RtcChannel?, uid: Int, state: Int, reason: Int, elapsed: Int) {
        AgoraLog.i("tag:onRemoteVideoStateChanged->rtcChannel:${rtcChannel?.channelId()}, uid:$uid, " +
                "state:$state, reason:$reason, elapsed:$elapsed")
        when (config.roomType) {
            AgoraEduRoomType.AgoraEduRoomType1V1.value -> {
                oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
            }
            AgoraEduRoomType.AgoraEduRoomTypeSmall.value,
            AgoraEduRoomType.AgoraEduRoomTypeBig.value -> {
                teacherVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
                userListManager?.notifyUserList()
            }
        }
    }

    override fun onRemoteAudioStateChanged(rtcChannel: RtcChannel?, uid: Int, state: Int, reason: Int, elapsed: Int) {
        AgoraLog.i("tag:onRemoteAudioStateChanged->rtcChannel:${rtcChannel?.channelId()}, uid:$uid, " +
                "state:$state, reason:$reason, elapsed:$elapsed")
        when (config.roomType) {
            AgoraEduRoomType.AgoraEduRoomType1V1.value -> {
                oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
            }
            AgoraEduRoomType.AgoraEduRoomTypeSmall.value,
            AgoraEduRoomType.AgoraEduRoomTypeBig.value -> {
                teacherVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
                userListManager?.notifyUserList()
            }
        }
    }

    override fun onRemoteVideoStats(stats: RteRemoteVideoStats) {

    }

    override fun onLocalVideoStateChanged(localVideoState: Int, error: Int) {
        when (config.roomType) {
            AgoraEduRoomType.AgoraEduRoomType1V1.value -> {
                oneToOneVideoManager?.updateLocalCameraAvailableState(localVideoState)
            }
            AgoraEduRoomType.AgoraEduRoomTypeSmall.value,
            AgoraEduRoomType.AgoraEduRoomTypeBig.value -> {
                userListManager?.updateLocalCameraAvailableState(localVideoState)
            }
        }
    }

    override fun onLocalAudioStateChanged(localAudioState: Int, error: Int) {
        when (config.roomType) {
            AgoraEduRoomType.AgoraEduRoomType1V1.value -> {
                oneToOneVideoManager?.updateLocalMicAvailableState(localAudioState)
            }
            AgoraEduRoomType.AgoraEduRoomTypeSmall.value,
            AgoraEduRoomType.AgoraEduRoomTypeBig.value -> {
                userListManager?.updateLocalMicAvailableState(localAudioState)
            }
        }
    }

    override fun onLocalVideoStats(stats: RteLocalVideoStats) {

    }

    override fun onAudioVolumeIndicationOfLocalSpeaker(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?, totalVolume: Int) {
        updateVolumes(speakers)
    }

    override fun onAudioVolumeIndicationOfRemoteSpeaker(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?, totalVolume: Int) {
        updateVolumes(speakers)
    }

    private fun updateVolumes(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?) {
        when (config.roomType) {
            AgoraEduRoomType.AgoraEduRoomType1V1.value -> {
                oneToOneVideoManager?.updateAudioVolume(speakers)
            }
            AgoraEduRoomType.AgoraEduRoomTypeSmall.value,
            AgoraEduRoomType.AgoraEduRoomTypeBig.value -> {
                teacherVideoManager?.updateAudioVolume(speakers)
                userListManager?.updateAudioVolumeIndication(speakers)
            }
        }
    }

    override fun onLocalUserUpdated(userEvent: EduUserEvent, type: EduUserStateChangeType) {
        Constants.AgoraLog.e("$tag:onLocalUserUpdated->userEvent:${Gson().toJson(userEvent)}, " +
                "type: ${type.name}")
    }

    override fun onLocalStreamAdded(streamEvent: EduStreamEvent) {
        Constants.AgoraLog.e("$tag:onLocalStreamAdded->streamEvent:${Gson().toJson(streamEvent)}")
        when (config.roomType) {
            AgoraEduRoomType.AgoraEduRoomType1V1.value -> {
                oneToOneVideoManager?.addLocalStream(streamEvent)
                oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.STUDENT)
            }
            AgoraEduRoomType.AgoraEduRoomTypeSmall.value,
            AgoraEduRoomType.AgoraEduRoomTypeBig.value -> {
                userListManager?.addLocalStream(streamEvent)
            }
        }

        // 如果上次有业务流 (而且音视频都都打开，但是关闭了cameraDevice/micDevice),
        // 当退出重新进入教室的时候， RTE发现存在一条本地流，就会重新enableLocalMedia、
        // publish并回调此函数，故在此做拦截(检查设备是否被关闭并调用enableLocalMedia)
        deviceManager?.checkDeviceConfig()
    }

    override fun onLocalStreamUpdated(streamEvent: EduStreamEvent) {
        Constants.AgoraLog.e("$tag:onLocalStreamUpdated->streamEvent:${Gson().toJson(streamEvent)}")
        when (config.roomType) {
            AgoraEduRoomType.AgoraEduRoomType1V1.value -> {
                oneToOneVideoManager?.updateLocalStream(streamEvent)
                oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.STUDENT)
            }
            AgoraEduRoomType.AgoraEduRoomTypeSmall.value,
            AgoraEduRoomType.AgoraEduRoomTypeBig.value -> {
                userListManager?.updateLocalStream(streamEvent)
                userListManager?.notifyUserList()
            }
        }

        // 如果后台已存在本地用户的业务流 (而且音视频都都打开，但是关闭了cameraDevice/micDevice),
        // 当退出重新进入教室的时候， RTE发现存在一条本地流，就会重新enableLocalMedia、
        // publish并回调此函数，故在此做拦截(检查设备是否被关闭并调用enableLocalMedia)
        deviceManager?.checkDeviceConfig()
    }

    override fun onLocalStreamRemoved(streamEvent: EduStreamEvent) {
        Constants.AgoraLog.e("$tag:onLocalStreamRemoved->streamEvent:${Gson().toJson(streamEvent)}")
        when (config.roomType) {
            AgoraEduRoomType.AgoraEduRoomType1V1.value -> {
                oneToOneVideoManager?.removeLocalStream(streamEvent)
            }
            AgoraEduRoomType.AgoraEduRoomTypeSmall.value,
            AgoraEduRoomType.AgoraEduRoomTypeBig.value -> {
                userListManager?.removeLocalStream(streamEvent)
                userListManager?.notifyUserList()
            }
        }
    }

    override fun onLocalUserLeft(userEvent: EduUserEvent, leftType: EduUserLeftType) {
        Constants.AgoraLog.e("$tag:onLocalUserLeft->userEvent:${Gson().toJson(userEvent)}, " +
                "leftType:${leftType.name}")
        if (leftType == EduUserLeftType.KickOff) {
            userListManager?.kickOut()
        }
    }

    override fun onRemoteUserPropertiesChanged(changedProperties: MutableMap<String, Any>,
                                               classRoom: EduRoom, userInfo: EduUserInfo,
                                               cause: MutableMap<String, Any>?,
                                               operator: EduBaseUserInfo?) {
        AgoraLog.i("$tag:onRemoteUserPropertiesChanged->changedProperties:${Gson().toJson(changedProperties)}, " +
                "userInfo:${userInfo.userUuid}, cause:${Gson().toJson(cause)}, " +
                "operator:${Gson().toJson(operator)}, room: ${config.roomUuid}")
        when (config.roomType) {
            AgoraEduRoomType.AgoraEduRoomType1V1.value -> {
                oneToOneVideoManager?.updateRemoteDeviceState(userInfo, cause)
                oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
            }
            AgoraEduRoomType.AgoraEduRoomTypeSmall.value,
            AgoraEduRoomType.AgoraEduRoomTypeBig.value -> {
                teacherVideoManager?.updateRemoteDeviceState(userInfo, cause)
                teacherVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
                userListManager?.updateRemoteDeviceState(userInfo, cause)
                userListManager?.notifyListByPropertiesChanged(cause)
                chatManager?.notifyUserChatMuteStatus(userInfo, cause, operator)
            }
        }

        flexPropsManager?.notifyUserFlexProps(userInfo, changedProperties, cause, operator)
    }

    override fun onLocalUserPropertiesChanged(changedProperties: MutableMap<String, Any>,
                                              userInfo: EduUserInfo, cause: MutableMap<String, Any>?,
                                              operator: EduBaseUserInfo?) {
        AgoraLog.i("$tag:onLocalUserPropertiesChanged->changedProperties:${Gson().toJson(changedProperties)}, " +
                "cause:${Gson().toJson(cause)}, operator:${Gson().toJson(operator)}, room: ${config.roomUuid}")
        if (config.roomType != AgoraEduRoomType.AgoraEduRoomType1V1.value) {
            userListManager?.notifyListByPropertiesChanged(cause)
            chatManager?.notifyUserChatMuteStatus(userInfo, cause, operator)
        }

        flexPropsManager?.notifyUserFlexProps(userInfo, changedProperties, cause, operator)
    }

    override fun onAudioMixingFinished() {
    }

    override fun onAudioMixingStateChanged(state: Int, errorCode: Int) {
        whiteBoardManager?.changeMixingState(state, errorCode)
    }
}

internal object DataConvert {
    fun convert(config: EduContextRenderConfig): EduRenderConfig {
        val render = when (config.renderMode) {
            EduContextRenderMode.HIDDEN -> EduRenderMode.HIDDEN
            EduContextRenderMode.FIT -> EduRenderMode.FIT
        }
        val mirror = if (config.mirrorMode) EduMirrorMode.ENABLED else EduMirrorMode.DISABLED
        return EduRenderConfig(render, mirror)
    }

    fun convert(config: EduContextVideoEncoderConfig): EduVideoEncoderConfig {
        return EduVideoEncoderConfig(config.videoDimensionWidth, config.videoDimensionHeight,
                config.frameRate, config.bitRate, config.mirrorMode.value)
    }
}

data class AgoraEduCoreConfig(
        val appId: String = "",
        val userName: String = "",
        val userUuid: String = "",
        val roomName: String = "",
        val roomUuid: String = "",
        val roleType: Int = AgoraEduRoleType.AgoraEduRoleTypeStudent.value,
        val roomType: Int = AgoraEduRoomType.AgoraEduRoomType1V1.value,
        val rtmToken: String = "",
        var startTime: Long = 0,
        val duration: Long = 0,
        val rtcRegion: String = "",
        val rtmRegion: String = "",
        val mediaOptions: AgoraEduMediaOptions? = null,
        val userProperties: MutableMap<String, String>? = null,
        val widgetConfigs: MutableList<io.agora.agoraeduwidget.UiWidgetConfig>? = null,

        /**
         * {@link io.agora.edu.core.internal.education.api.room.data.EduRoomState}
         */
        val state: Int = EduRoomState.INIT.value,

        val closeDelay: Long,
        val lastMessageId: Long,

        /**
         * {@link io.agora.edu.core.internal.education.api.room.data.EduMuteState}
         */
        val muteChat: Int = EduMuteState.Disable.value,

        val boardAppId: String = "",
        val boardId: String = "",
        val boardToken: String = "",
        val boardRegion: String = "",
        var videoEncoderConfig: EduVideoEncoderConfig? = null,
        val boardFitMode: AgoraBoardFitMode = AgoraBoardFitMode.Auto,
        val streamState: StreamState?,
        val latencyLevel: AgoraEduLatencyLevel = AgoraEduLatencyLevel.AgoraEduLatencyLevelUltraLow,
        val vendorId: Int = 0,
        val logDir: String?,

        val autoSubscribe: Boolean = false,
        val autoPublish: Boolean = false,
        val needUserListener: Boolean = true
)

interface AgoraEduCoreStateListener {
    fun onCreated()
    fun onError(error: EduError)
}

/**
 * Cache class type data for multiple purposes.
 * Usually to reduce coupling between classes or modules
 */
object ClassInfoCache {
    private const val tag = "ClassInfoCache"
    private val roomClassMapDefault = mutableMapOf<Int, Class<out BaseClassActivity>>()
    private val roomClassMapReplace = mutableMapOf<Int, Class<out BaseClassActivity>>()

    fun addRoomActivityDefault(type: Int, clz: Class<out BaseClassActivity>) {
        if (roomClassMapDefault.containsKey(type) || roomClassMapDefault[type] == null) {
            Log.i(tag, "reset class info, type:$type, class:$clz")
        }
        roomClassMapDefault.remove(type)
        roomClassMapDefault[type] = clz
    }

    fun getRoomActivityDefault(type: Int): Class<out BaseClassActivity>? {
        return roomClassMapDefault[type]
    }

    fun replaceRoomActivity(type: Int, clz: Class<out BaseClassActivity>) {
        if (roomClassMapReplace.containsKey(type) || roomClassMapReplace[type] == null) {
            Log.i(tag, "reset class info, type:$type, class:$clz")
        }

        roomClassMapReplace.remove(type)
        roomClassMapReplace[type] = clz
    }

    fun getRoomActivityReplaced(type: Int): Class<out BaseClassActivity>? {
        return roomClassMapReplace[type]
    }
}