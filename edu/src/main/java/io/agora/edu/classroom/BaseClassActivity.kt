package io.agora.edu.classroom

import android.app.Activity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.core.view.ViewCompat
import io.agora.agoraactionprocess.AgoraActionListener
import io.agora.agoraactionprocess.AgoraActionMsgRes
import io.agora.base.callback.Callback
import io.agora.edu.BuildConfig
import io.agora.edu.common.bean.response.RoomPreCheckRes
import io.agora.edu.launch.AgoraEduEvent
import io.agora.edu.launch.AgoraEduLaunchConfig
import io.agora.edu.launch.AgoraEduSDK
import io.agora.edu.util.AppUtil
import io.agora.edu.util.AppUtil.getNavigationBarHeight
import io.agora.edu.widget.EyeProtection
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.base.EduError.Companion.internalError
import io.agora.education.api.manager.EduManager
import io.agora.education.api.manager.listener.EduManagerEventListener
import io.agora.education.api.message.EduActionMessage
import io.agora.education.api.message.EduChatMsg
import io.agora.education.api.message.EduMsg
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.*
import io.agora.education.api.room.listener.EduRoomEventListener
import io.agora.education.api.statistics.NetworkQuality
import io.agora.education.api.stream.data.EduStreamEvent
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.stream.data.VideoSourceType
import io.agora.education.api.user.EduStudent
import io.agora.education.api.user.EduUser
import io.agora.education.api.user.data.*
import io.agora.education.api.user.listener.EduUserEventListener
import io.agora.education.impl.Constants
import io.agora.educontext.*
import io.agora.educontext.context.*
import io.agora.extapp.AgoraExtAppManager
import io.agora.extension.*
import io.agora.privatechat.PrivateChatManager
import io.agora.report.ReportManager.getAPaasReporter
import io.agora.report.reporters.APaasReporter
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcChannel
import io.agora.rte.data.RteLocalVideoStats
import io.agora.rte.data.RteRemoteVideoStats
import io.agora.uikit.impl.container.AgoraUI1v1Container
import io.agora.uikit.impl.container.AgoraUILargeClassContainer
import io.agora.uikit.impl.container.AgoraUISmallClassContainer
import io.agora.uikit.impl.users.AgoraUIRoster
import io.agora.uikit.interfaces.protocols.IAgoraUIContainer

abstract class BaseClassActivity : BaseActivity(),
        EduRoomEventListener, EduUserEventListener,
        EduManagerEventListener, AgoraActionListener {

    object Data {
        const val launchConfig = "LAUNCHCONFIG"

        const val precheckData = "PRECHECKDATA"

        const val resultCode = 808
    }

    private val tag = "BaseClassActivity"

    protected var launchConfig: AgoraEduLaunchConfig? = null
    protected var preCheckData: RoomPreCheckRes? = null
    protected var eduRoom: EduRoom? = null

    protected var contentLayout: ViewGroup? = null

    protected var container: IAgoraUIContainer? = null
    protected var roomStatusManager: RoomStatusManager? = null
    protected var chatManager: ChatManager? = null
    protected var handsUpManager: HandsUpManager? = null
    protected var oneToOneVideoManager: OneToOneVideoManager? = null
    protected var teacherVideoManager: TeacherVideoManager? = null
    protected var userListManager: UserListManager? = null
    protected var screenShareManager: ScreenShareManager? = null
    protected var privateChatManager: PrivateChatManager? = null
    protected var extAppManager: AgoraExtAppManager? = null
    protected var whiteBoardManager: WhiteBoardManager? = null
    protected var whiteBoardContainer: ViewGroup? = null

    protected data class JoinRoomConfiguration(
            val autoPublish: Boolean = false,
            val autoSubscribe: Boolean = false,
            val needUserListener: Boolean = false)

    private var joinConfig = JoinRoomConfiguration()

    @Volatile
    var isJoining = false

    @Volatile
    var joinSuccess = false

    // The entire room entry should be said complete when
    // both room and white board join successfully
    private var roomJoinSuccess = false
    private var whiteboardJoinSuccess = false

    @Synchronized
    protected fun processJoinSuccess(): Boolean {
        return roomJoinSuccess && whiteboardJoinSuccess
    }

    @Synchronized
    protected fun setRoomJoinSuccess() {
        roomJoinSuccess = true
    }

    @Synchronized
    protected fun setWhiteboardJoinSuccess() {
        whiteboardJoinSuccess = true
    }

    protected fun checkProcessSuccess() {
        if (processJoinSuccess()) {
            getReporter().reportRoomEntryEnd("1", null, null, null)
        }
    }

    companion object EduManagerDelegate {
        private var eduManager: EduManager? = null

        fun setEduManager(manager: EduManager) {
            eduManager = manager
        }

        fun getEduManager(): EduManager? {
            return eduManager
        }
    }

    private val userListManagerEventListener = object : UserListManagerEventListener {
        override fun onGranted(userId: String): Boolean {
            return whiteBoardManager?.isGranted(userId) ?: false
        }

        override fun onKickOut() {
            forceLeave(false)
        }

        override fun onMediaMsgUpdate(msg: String) {
            container?.showTips(msg)
        }
    }

    private val oneToOneVideoManagerEventListener = object : OneToOneVideoManagerEventListener {
        override fun onMediaMsgUpdate(msg: String) {
            container?.showTips(msg)
        }

        override fun onGranted(userId: String): Boolean {
            return whiteBoardManager?.isGranted(userId) ?: false
        }
    }

    private val teacherVideoManagerEventListener = object : TeacherVideoManagerEventListener {
        override fun onMediaMsgUpdate(msg: String) {
            container?.showTips(msg)
        }

        override fun onGranted(userId: String): Boolean {
            return whiteBoardManager?.isGranted(userId) ?: false
        }
    }

    private val chatContext = object : ChatContext() {
        override fun sendLocalMessage(message: String, timestamp: Long,
                                      callback: EduContextCallback<EduContextChatItemSendResult>): EduContextChatItem {
            chatManager?.sendRoomChat(message, timestamp, callback)
            return EduContextChatItem(
                    name = launchConfig?.userName ?: "",
                    uid = launchConfig?.userUuid ?: "",
                    message = message,
                    source = EduContextChatSource.Local,
                    state = EduContextChatState.InProgress,
                    timestamp = timestamp)
        }

        override fun fetchHistory(startId: Int?, count: Int?, callback: EduContextCallback<List<EduContextChatItem>>) {
            chatManager?.pullChatRecords(if (startId != null) "$startId" else null,
                    count ?: 0, true, callback)
        }
    }

    private val handsUpContext = object : HandsUpContext() {
        override fun performHandsUp(state: EduContextHandsUpState, callback: EduContextCallback<Boolean>?) {
            handsUpManager?.performHandsUp(state, callback)
        }
    }

    private val roomContext = object : RoomContext() {
        override fun leave() {
            forceLeave(true)
        }
    }

    private val screenShareContext = object : ScreenShareContext() {
        override fun setScreenShareState(sharing: Boolean) {
            screenShareManager?.setScreenShareState(sharing)
        }

        override fun renderScreenShare(container: ViewGroup?, streamUuid: String) {
            screenShareManager?.renderScreenShare(container, streamUuid)
        }
    }

    private val userContext = object : UserContext() {
        override fun muteVideo(muted: Boolean) {
            userListManager?.muteLocalVideo(muted)
        }

        override fun muteAudio(muted: Boolean) {
            userListManager?.muteLocalAudio(muted)
        }

        override fun renderVideo(container: ViewGroup?, streamUuid: String) {
            userListManager?.renderContainer(container, streamUuid)
        }
    }

    private val videoContext = object : VideoContext() {
        override fun updateVideo(enabled: Boolean) {
            when (container) {
                is AgoraUI1v1Container -> {
                    oneToOneVideoManager?.muteLocalVideo(!enabled)
                }
                is AgoraUISmallClassContainer -> {
                    teacherVideoManager?.muteLocalVideo(!enabled)
                }
                is AgoraUILargeClassContainer -> {
                    teacherVideoManager?.muteLocalVideo(!enabled)
                }
            }
        }

        override fun updateAudio(enabled: Boolean) {
            when (container) {
                is AgoraUI1v1Container -> {
                    oneToOneVideoManager?.muteLocalAudio(!enabled)
                }
                is AgoraUISmallClassContainer -> {
                    teacherVideoManager?.muteLocalAudio(!enabled)
                }
                is AgoraUILargeClassContainer -> {
                    teacherVideoManager?.muteLocalAudio(!enabled)
                }
            }
        }

        override fun renderVideo(viewGroup: ViewGroup?, streamUuid: String) {
            when (container) {
                is AgoraUI1v1Container -> {
                    oneToOneVideoManager?.renderVideo(viewGroup, streamUuid)
                }
                is AgoraUISmallClassContainer -> {
                    teacherVideoManager?.renderVideo(viewGroup, streamUuid)
                }
                is AgoraUILargeClassContainer -> {
                    teacherVideoManager?.renderVideo(viewGroup, streamUuid)
                }
            }
        }
    }

    protected val whiteboardContext = object : WhiteboardContext() {
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
            userContext.getHandlers()?.forEach { handler ->
                handler.onRoster(this@BaseClassActivity, anchor,
                        when (container) {
                            is AgoraUISmallClassContainer -> AgoraUIRoster.RosterType.SmallClass.value()
                            is AgoraUILargeClassContainer -> AgoraUIRoster.RosterType.LargeClass.value()
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

    protected val privateChatContext = object : PrivateChatContext() {
        override fun getLocalUserInfo(): EduContextUserInfo {
            return EduContextUserInfo(
                    launchConfig!!.userUuid,
                    launchConfig!!.userName,
                    EduContextUserRole.Student)
        }

        override fun startPrivateChat(peerId: String,
                                      callback: EduContextCallback<EduContextPrivateChatInfo>?) {
            privateChatManager?.startPrivateChat(peerId,
                    Callback {
                        it?.let { _ ->
                            getCurFullUser(object : EduCallback<MutableList<EduUserInfo>> {
                                override fun onSuccess(res: MutableList<EduUserInfo>?) {
                                    res?.forEach { user ->
                                        if (user.userUuid == peerId) {
                                            callback?.onSuccess(
                                                    EduContextPrivateChatInfo(
                                                            getLocalUserInfo(),
                                                            EduContextUserInfo(
                                                                    user.userUuid,
                                                                    user.userName,
                                                                    toEduContextUserInfo(user.role)
                                                            )
                                                    )
                                            )
                                        }
                                    }
                                }

                                override fun onFailure(error: EduError) {

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
            privateChatManager?.stopSideVoiceChat(Callback {
                it?.let {
                    callback?.onSuccess(true)
                }
            })
        }
    }

    private val extAppContext = object : ExtAppContext {
        override fun launchExtApp(appIdentifier: String): Int {
            return extAppManager?.launchExtApp(appIdentifier)
                    ?: AgoraExtAppErrorCode.ExtAppEngineError
        }

        override fun getRegisteredExtApps(): List<AgoraExtAppInfo> {
            return extAppManager?.getRegisteredApps() ?: mutableListOf()
        }
    }

    protected val eduContext = object : EduContextPool {
        override fun chatContext(): ChatContext {
            return chatContext
        }

        override fun handsUpContext(): HandsUpContext {
            return handsUpContext
        }

        override fun roomContext(): RoomContext {
            return roomContext
        }

        override fun screenShareContext(): ScreenShareContext {
            return screenShareContext
        }

        override fun userContext(): UserContext {
            return userContext
        }

        override fun videoContext(): VideoContext {
            return videoContext
        }

        override fun whiteboardContext(): WhiteboardContext {
            return whiteboardContext
        }

        override fun privateChatContext(): PrivateChatContext {
            return privateChatContext
        }

        override fun extAppContext(): ExtAppContext {
            return extAppContext
        }
    }

    private fun initAndJoinRoom() {
        eduManager?.eduManagerEventListener = this
        launchConfig = intent.getParcelableExtra(Data.launchConfig)
        preCheckData = intent.getParcelableExtra(Data.precheckData)

        if (launchConfig == null) {
            Constants.AgoraLog.e("$tag -> init room fail, launch config is null")
            return
        }

        if (preCheckData == null) {
            Constants.AgoraLog.e("$tag -> init room fail, precheck data is null")
            return
        }

        launchConfig?.let { config ->
            eduRoom = getEduManager()?.createClassroom(
                    RoomCreateOptions(
                            config.roomUuid,
                            config.roomName,
                            config.roomType))

            eduRoom?.eventListener = this

            chatManager = ChatManager(this, eduRoom, chatContext, config)
            privateChatManager = PrivateChatManager(this, privateChatContext, eduRoom, config)

            joinConfig = onRoomJoinConfig()
            joinRoomAsStudent(
                    config.userName,
                    config.userUuid,
                    joinConfig.autoSubscribe,
                    joinConfig.autoPublish,
                    joinConfig.needUserListener,
                    object : EduCallback<EduStudent?> {
                        override fun onSuccess(res: EduStudent?) {
                            res?.let { student ->
                                setRoomJoinSuccess()
                                checkProcessSuccess()
                                preCheckData?.let { preCheck -> initEduCapabilityManagers(config, preCheck) }
                                onRoomJoined(true, student)
                            }
                        }

                        override fun onFailure(error: EduError) {
                            joinFailed(error.type, error.msg)
                            onRoomJoined(false, null, error)
                        }
                    })
        }
    }

    protected abstract fun onRoomJoinConfig(): JoinRoomConfiguration

    protected abstract fun onRoomJoined(success: Boolean, student: EduStudent?, error: EduError? = null)

    private fun joinRoomAsStudent(name: String?, uuid: String?,
                                  autoSubscribe: Boolean,
                                  autoPublish: Boolean,
                                  needUserListener: Boolean,
                                  callback: EduCallback<EduStudent?>) {
        if (isJoining) {
            Log.e(tag, "join fail because you are joining the classroom")
            return
        }

        if (launchConfig == null) {
            Log.e(tag, "join fail because no launch config info is found")
            return
        }

        isJoining = true
        val options = RoomJoinOptions(uuid!!, name, EduUserRole.STUDENT,
                RoomMediaOptions(autoSubscribe, autoPublish, launchConfig?.mediaOptions?.encryptionConfigs), launchConfig?.roomType)

        eduRoom?.joinClassroom(options, object : EduCallback<EduUser> {
            override fun onSuccess(res: EduUser?) {
                Log.e(tag, "joinRoomAsStudent-thread->${Thread.currentThread().id}")
                if (res != null) {
                    joinSuccess = true
                    isJoining = false

                    // report rtcAppScenario
                    eduManager?.reportAppScenario(launchConfig!!.roomType, BuildConfig.SERVICE_APAAS,
                            "1.1.0-offiicial")

                    if (needUserListener) {
                        res.eventListener = this@BaseClassActivity
                    }

                    val student = res as EduStudent
                    callback.onSuccess(student)
                    AgoraEduSDK.agoraEduLaunchCallback.onCallback(AgoraEduEvent.AgoraEduEventReady)
                } else {
                    val error = internalError("join failed: localUser is null")
                    callback.onFailure(error)
                    reportClassJoinSuccess("0", error.type.toString() + "", error.httpError.toString() + "")
                }
            }

            override fun onFailure(error: EduError) {
                isJoining = false
                callback.onFailure(error)
                reportClassJoinSuccess("0", error.type.toString() + "", error.httpError.toString() + "")
            }
        })
    }

    private fun initEduCapabilityManagers(config: AgoraEduLaunchConfig, preCheckData: RoomPreCheckRes) {
        whiteBoardManager!!.initBoardWithRoomToken(preCheckData.board.boardId,
                preCheckData.board.boardToken, config.userUuid)

        ChatManager(this, eduRoom, eduContext.chatContext(), config).let { manager ->
            chatManager = manager
            manager.initChat()
        }

        RoomStatusManager(this, eduContext.roomContext(), config, preCheckData, eduRoom).let { manager ->
            roomStatusManager = manager
            manager.setClassName(config.roomName)
            manager.initClassState()
        }

        contentLayout?.let { layout ->
            initExtAppManager(layout, config)
        }

        getLocalUser(object : EduCallback<EduUser?> {
            override fun onSuccess(res: EduUser?) {
                res?.let { localUser ->
                    TeacherVideoManager(applicationContext, config, eduRoom, localUser, videoContext).let { manager ->
                        manager.managerEventListener = teacherVideoManagerEventListener
                        teacherVideoManager = manager
                        manager.notifyUserDetailInfo(EduUserRole.STUDENT)
                        manager.notifyUserDetailInfo(EduUserRole.TEACHER)

                        manager.screenShareStarted = object : () -> Boolean {
                            override fun invoke(): Boolean {
                                return screenShareManager?.isScreenSharing() ?: false
                            }
                        }
                    }

                    ScreenShareManager(this@BaseClassActivity, eduContext,
                            config, eduRoom!!, localUser).let { manager ->
                        screenShareManager = manager
                        manager.screenShareStateChangedListener = object : (Boolean) -> Unit {
                            override fun invoke(p1: Boolean) {
                                teacherVideoManager?.container?.let { container ->
                                    teacherVideoManager?.videoGroupListener?.onRendererContainer(
                                            container, teacherVideoManager!!.teacherStreamUuid)
                                }
                            }
                        }
                    }

                    OneToOneVideoManager(applicationContext, config, eduRoom!!, localUser, videoContext).let { manager ->
                        manager.managerEventListener = oneToOneVideoManagerEventListener
                        oneToOneVideoManager = manager
                        manager.notifyUserDetailInfo(EduUserRole.STUDENT)
                        manager.notifyUserDetailInfo(EduUserRole.TEACHER)
                    }

                    eduContext.handsUpContext()?.let { handsUpContext ->
                        HandsUpManager(applicationContext, handsUpContext, config, eduRoom, localUser).let { manager ->
                            handsUpManager = manager
                            manager.initHandsUpData()
                        }
                    }

                    UserListManager(applicationContext, config, eduRoom!!, localUser, userContext).let { manager ->
                        manager.eventListener = userListManagerEventListener
                        userListManager = manager
                        manager.notifyUserList()
                    }
                }
            }

            override fun onFailure(error: EduError) {

            }
        })
    }

    private fun initExtAppManager(view: View, config: AgoraEduLaunchConfig) {
        extAppManager = object : AgoraExtAppManager(config.appId, this, view, config.roomUuid) {
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
    }

    private fun joinFailed(code: Int, reason: String) {
        val msg = "join classRoom failed->code:$code,reason:$reason"
        Constants.AgoraLog.e(tag, msg)
        AgoraEduSDK.agoraEduLaunchCallback.onCallback(AgoraEduEvent.AgoraEduEventFailed)
        val intent = intent.putExtra(AgoraEduSDK.CODE, code).putExtra(AgoraEduSDK.REASON, reason)
        setResult(Data.resultCode, intent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        val view = onContentViewLayout()
        view.fitsSystemWindows = true
        setContentView(view)
        initAndJoinRoom()
    }

    protected abstract fun onContentViewLayout(): ViewGroup

    override fun onStart() {
        super.onStart()
        launchConfig?.let { EyeProtection.setNeedShow(it.eyeCare == 1) }
    }

    override fun onDestroy() {
        super.onDestroy()
        privateChatManager?.dispose()
        roomStatusManager?.dispose()
        extAppManager?.dispose()
        eduRoom = null
        getEduManager()?.let {
            it.eduManagerEventListener = null
            it.release()
        }
        AgoraEduSDK.agoraEduLaunchCallback.onCallback(AgoraEduEvent.AgoraEduEventDestroyed)
    }

    override fun onBackPressed() {
        container?.showLeave()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        isNavigationBarChanged(this, object : OnNavigationStateListener {
            override fun onNavigationState(isShowing: Boolean, b: Int) {
                Log.e(tag, "isNavigationBarExist->$isShowing")
                contentLayout?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        contentLayout?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                        val w = contentLayout!!.width
                        val h = contentLayout!!.height
                        container?.resize(contentLayout!!, 0, 0, w, h)
                    }
                })
            }
        })
    }

    private fun isNavigationBarChanged(
            activity: Activity,
            onNavigationStateListener: OnNavigationStateListener?
    ) {
        val height: Int = getNavigationBarHeight(activity)
        ViewCompat.setOnApplyWindowInsetsListener(activity.window.decorView) { v, insets ->
            var isShowing = false
            var l = 0
            var r = 0
            var b = 0
            if (insets != null) {
                l = insets.systemWindowInsetLeft
                r = insets.systemWindowInsetRight
                b = insets.systemWindowInsetBottom
                isShowing = l == height || r == height || b == height
            }
            if (onNavigationStateListener != null && b <= height) {
                onNavigationStateListener.onNavigationState(isShowing, b)
            }
            ViewCompat.onApplyWindowInsets(v, insets)
        }
    }

    protected fun getLocalUser(callback: EduCallback<EduUser?>) {
        if (eduRoom == null) {
            callback.onFailure(internalError("current eduRoom is null"))
            return
        }

        eduRoom?.getLocalUser(object : EduCallback<EduUser> {
            override fun onSuccess(res: EduUser?) {
                if (res == null) {
                    callback.onFailure(internalError("current eduRoom`s localUsr is null"))
                } else {
                    callback.onSuccess(res)
                }
            }

            override fun onFailure(error: EduError) {
                callback.onFailure(error)
            }
        })
    }

    protected fun forceLeave(finish: Boolean) {
        runOnUiThread {
            // exit whiteBoard
            whiteBoardManager?.releaseBoard()
            // exit room
            eduRoom?.leave(object : EduCallback<Unit> {
                override fun onSuccess(res: Unit?) {
                    if (finish) {
                        finish()
                    }
                }

                override fun onFailure(error: EduError) {
                    Constants.AgoraLog.e("$tag:leave EduRoom error-> " +
                            "code: ${error.type}, reason: ${error.msg}")
                }
            })
        }
    }

    protected fun getLocalUserInfo(callback: EduCallback<EduUserInfo?>) {
        getLocalUser(object : EduCallback<EduUser?> {
            override fun onSuccess(res: EduUser?) {
                callback.onSuccess(res?.userInfo)
            }

            override fun onFailure(error: EduError) {
                callback.onFailure(error)
            }
        })
    }

    protected fun getCurFullUser(callback: EduCallback<MutableList<EduUserInfo>>) {
        eduRoom?.getFullUserList(callback)
                ?: callback.onFailure(internalError("current eduRoom is null"))
    }

    protected fun getCurFullStream(callback: EduCallback<MutableList<EduStreamInfo>>) {
        eduRoom?.getFullStreamList(callback)
                ?: callback.onFailure(internalError("current eduRoom is null"))
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

    protected fun getMainRoomUuid(callback: EduCallback<String?>) {
        eduRoom?.getRoomInfo(object : EduCallback<EduRoomInfo> {
            override fun onSuccess(res: EduRoomInfo?) {
                callback.onSuccess(res!!.roomUuid)
            }

            override fun onFailure(error: EduError) {
                callback.onFailure(error)
            }
        }) ?: callback.onFailure(internalError("current eduRoom is null"))
    }

    fun getReporter(): APaasReporter {
        return getAPaasReporter()
    }

    private fun reportClassJoinSuccess(result: String, errorCode: String, httpCode: String) {
        getReporter().reportRoomEntryEnd(result, errorCode, httpCode, null)
    }

    override fun onRemoteUsersInitialized(users: List<EduUserInfo>, classRoom: EduRoom) {

    }

    override fun onRemoteUsersJoined(users: List<EduUserInfo>, classRoom: EduRoom) {
        Constants.AgoraLog.e("$tag:Receive a callback when the remote user joined")
    }

    override fun onRemoteUserLeft(userEvent: EduUserEvent, classRoom: EduRoom) {
        Constants.AgoraLog.e("$tag:Receive a callback when the remote user left")
    }

    override fun onRemoteUserUpdated(userEvent: EduUserEvent, type: EduUserStateChangeType, classRoom: EduRoom) {
        Constants.AgoraLog.e("$tag:Receive a callback when the remote user modified")
    }

    override fun onRoomMessageReceived(message: EduMsg, classRoom: EduRoom) {

    }

    override fun onRoomChatMessageReceived(chatMsg: EduChatMsg, classRoom: EduRoom) {

    }

    override fun onRemoteStreamsInitialized(streams: List<EduStreamInfo>, classRoom: EduRoom) {
        Constants.AgoraLog.e("$tag:onRemoteStreamsInitialized")
    }

    override fun onRemoteStreamsAdded(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        Constants.AgoraLog.e("$tag:Receive callback to add remote stream")
    }

    override fun onRemoteStreamUpdated(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        Constants.AgoraLog.e("$tag:Receive callback to update remote stream")
    }

    override fun onRemoteStreamsRemoved(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        Constants.AgoraLog.e("$tag:Receive callback to remove remote stream")
    }

    override fun onRoomStatusChanged(type: EduRoomChangeType, operatorUser: EduUserInfo?, classRoom: EduRoom) {
        chatManager?.notifyMuteChatStatus(type)
    }

    override fun onRoomPropertiesChanged(classRoom: EduRoom, cause: MutableMap<String, Any>?) {
        Constants.AgoraLog.e("$tag:Received callback of roomProperty change")
        extAppManager?.handleRoomPropertiesChange(classRoom.roomProperties, cause)
    }

    override fun onNetworkQualityChanged(quality: NetworkQuality, user: EduBaseUserInfo, classRoom: EduRoom) {
        if (user.userUuid == launchConfig?.userUuid) {
            roomStatusManager?.updateNetworkState(quality)
        }
    }

    override fun onConnectionStateChanged(state: EduContextConnectionState, classRoom: EduRoom) {
        Constants.AgoraLog.e("$tag:onConnectionStateChanged-> " +
                "${state.name}, room: ${launchConfig?.roomUuid}")
        roomStatusManager?.updateConnectionState(state)

        if (state == EduContextConnectionState.Aborted) {
            forceLeave(true)
        }
    }

    override fun onRemoteVideoStateChanged(rtcChannel: RtcChannel?, uid: Int, state: Int, reason: Int, elapsed: Int) {
    }

    override fun onRemoteVideoStats(stats: RteRemoteVideoStats) {

    }

    override fun onLocalVideoStateChanged(localVideoState: Int, error: Int) {

    }

    override fun onLocalVideoStats(stats: RteLocalVideoStats) {

    }

    override fun onAudioVolumeIndicationOfLocalSpeaker(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?, totalVolume: Int) {

    }

    override fun onAudioVolumeIndicationOfRemoteSpeaker(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?, totalVolume: Int) {

    }

    override fun onLocalUserUpdated(userEvent: EduUserEvent, type: EduUserStateChangeType) {

    }

    override fun onLocalStreamAdded(streamEvent: EduStreamEvent) {
        Constants.AgoraLog.e("$tag:Receive callback to add local stream")
        when (streamEvent.modifiedStream.videoSourceType) {
            VideoSourceType.CAMERA -> {
                Constants.AgoraLog.e("$tag:Receive callback to add local camera stream")
            }
            VideoSourceType.SCREEN -> {
            }
        }
    }

    override fun onLocalStreamUpdated(streamEvent: EduStreamEvent) {
        Constants.AgoraLog.e("$tag:Receive callback to update local stream")
        when (streamEvent.modifiedStream.videoSourceType) {
            VideoSourceType.CAMERA -> {
            }
            VideoSourceType.SCREEN -> {
            }
        }
    }

    override fun onLocalStreamRemoved(streamEvent: EduStreamEvent) {
        Constants.AgoraLog.e("$tag:Receive callback to remove local stream")
        when (streamEvent.modifiedStream.videoSourceType) {
            VideoSourceType.CAMERA -> {
            }
            VideoSourceType.SCREEN -> {
            }
        }
    }

    override fun onLocalUserLeft(userEvent: EduUserEvent, leftType: EduUserLeftType) {
        if (leftType == EduUserLeftType.KickOff) {
            userListManager?.kickOut()
        }
    }

    override fun onUserMessageReceived(message: EduMsg) {

    }

    override fun onUserChatMessageReceived(chatMsg: EduChatMsg) {

    }

    override fun onUserActionMessageReceived(actionMessage: EduActionMessage) {

    }

    override fun onApply(actionMsgRes: AgoraActionMsgRes) {

    }

    override fun onInvite(actionMsgRes: AgoraActionMsgRes) {

    }

    override fun onAccept(actionMsgRes: AgoraActionMsgRes) {

    }

    override fun onReject(actionMsgRes: AgoraActionMsgRes) {

    }

    override fun onCancel(actionMsgRes: AgoraActionMsgRes) {

    }

    override fun onRemoteUserPropertiesChanged(classRoom: EduRoom, userInfo: EduUserInfo, cause: MutableMap<String, Any>?) {

    }

    override fun onLocalUserPropertiesChanged(userInfo: EduUserInfo, cause: MutableMap<String, Any>?) {

    }
}