package io.agora.agoraeduuikit.impl.whiteboard

import android.text.TextUtils
import android.text.TextUtils.isEmpty
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.widget.RelativeLayout
import com.agora.edu.component.helper.GsonUtils
import com.agora.edu.component.loading.AgoraLoadingView
import com.agora.edu.component.whiteboard.AgoraEduWhiteBoardControlComponent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.herewhite.sdk.*
import com.herewhite.sdk.domain.*
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole.Student
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole.Teacher
import io.agora.agoraeducore.core.context.EduBoardRoomPhase
import io.agora.agoraeducore.core.context.EduBoardRoomPhase.Companion.convert
import io.agora.agoraeducore.core.context.EduBoardRoomPhase.Disconnected
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.education.impl.Constants.AgoraLog
import io.agora.agoraeducore.core.internal.framework.impl.managers.AgoraWidgetManager.Companion.grantUser
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.launch.AgoraEduRoleType.AgoraEduRoleTypeTeacher
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCourseware
import io.agora.agoraeducore.core.internal.report.ReportManager
import io.agora.agoraeducore.core.internal.util.ColorUtil.colorToArray
import io.agora.agoraeducore.core.internal.util.ColorUtil.getColor
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.agoraeduuikit.impl.whiteboard.bean.*
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionSignal.*
import io.agora.agoraeduuikit.impl.whiteboard.netless.listener.BoardEventListener
import io.agora.agoraeduuikit.impl.whiteboard.netless.manager.BoardRoom
import io.agora.agoraeduuikit.impl.whiteboard.netless.manager.BoardRoomImpl
import org.json.JSONObject
import wendu.dsbridge.DWebView
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class AgoraWhiteBoardWidget : AgoraBaseWidget() {
    override val tag = "AgoraWhiteBoardWidget"
    private lateinit var whiteBoardView: WhiteboardView
    private lateinit var loadingView: AgoraLoadingView
    private lateinit var whiteBoardControlView: AgoraEduWhiteBoardControlComponent
    private val boardProxy = Proxy.newProxyInstance(BoardRoom::class.java.classLoader, arrayOf(BoardRoom::class.java),
        object : InvocationHandler {
            private val boardRoom: BoardRoom = BoardRoomImpl()

            override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
                val parameters = args?.filter { it !is BoardEventListener && it !is WhiteSdk }
                AgoraLog?.i("${BoardRoomImpl.TAG}->${method?.name}:${Gson().toJson(parameters)}")

                return if (args.isNullOrEmpty()) {
                    method?.invoke(boardRoom)
                } else {
                    method?.invoke(boardRoom, *args)
                }
            }
        }) as BoardRoom

    var uuid: String? = null
    private var whiteBoardAppId: String? = null
    private var region: String? = null
    private var whiteboardUuid: String? = null
    private var curLocalToken: String? = null
    private var aPaaSUserUuid: String? = null
    private var aPaaSUserName: String? = null
    private val miniScale = 0.1
    private val maxScale = 10.0
    private val sceneCameraConfigs: MutableMap<String, CameraState> = mutableMapOf()
    private var curBoardState: BoardState? = null
    private var curGranted: Boolean? = null
    private var curGrantedUsers = mutableListOf<String>()
    private val defaultCoursewareName = "init"
    private var loadPreviewPpt: Boolean = true
    private var curSceneState: SceneState? = null
    private var courseWareManager: CourseWareManager? = null
    private var inputTips = false
    private var transform = false
    private var boardFitMode = AgoraBoardFitMode.Retain
    private var disconnectErrorHappen = false
    private val boardAppIdKey = "boardAppId"
    private val boardTokenKey = "boardToken"
    private var firstGrantedTip = true
    private var videoDirection: String = "right" //视频窗UI的方向

    // Default appliance configuration
    private val curDrawingConfig = WhiteboardDrawingConfig()
    private val webChromeClient = object : WebChromeClient() {}

    private val boardEventListener = object : BoardEventListener {
        override fun onJoinSuccess(state: GlobalState?) {
            loadingView.visibility = View.GONE
            AgoraLog?.i(tag + ":onJoinSuccess->" + Gson().toJson(state))
            initDrawingConfig(object : Promise<Unit> {
                override fun then(t: Unit?) {
                    initWriteableFollowLocalRole(state, object : Promise<Boolean> {
                        override fun then(t: Boolean?) {
                            onGlobalStateChanged(state)
                        }

                        override fun catchEx(t: SDKError?) {
                        }
                    })
                }

                override fun catchEx(t: SDKError?) {
                    // if init failed, go ahead, do not block.
                    initWriteableFollowLocalRole(state, object : Promise<Boolean> {
                        override fun then(t: Boolean?) {
                            onGlobalStateChanged(state)
                        }

                        override fun catchEx(t: SDKError?) {
                        }
                    })
                }
            })
        }

        override fun onJoinFail(error: SDKError?) {
//            whiteBoardManagerEventListener?.onWhiteBoardJoinFail(error)
//            whiteboardContext.getHandlers()?.forEach {
//                it.onBoardPhaseChanged(EduBoardRoomPhase.disconnected)
//            }
            broadcastBoardPhaseState(Disconnected)
        }

        override fun onRoomPhaseChanged(phase: RoomPhase) {
            if (phase == RoomPhase.reconnecting || phase == RoomPhase.connecting) {
                // 连接中
                loadingView.visibility = View.VISIBLE
            } else {
                loadingView.visibility = View.GONE
            }

            AgoraLog?.i(tag + ":onRoomPhaseChanged->" + phase.name)
            if (phase == RoomPhase.connected) {
                restoreWhiteBoardAppliance()
                broadcastDrawingConfig()
            }
            broadcastBoardPhaseState(convert(phase.name))
        }

        override fun onSceneStateChanged(state: SceneState) {
            AgoraLog?.e("$tag:onSceneStateChanged->${Gson().toJson(state)}")

            // 设置页数
            whiteBoardControlView.setWhiteBoardPage(boardProxy)
//            downloadCourseWareIfNeeded()
//            adjustCameraWhenSwitchScene()

//            whiteboardContext.getHandlers()?.forEach { handler ->
//                state.let { state ->
//                    handler.onPageNo(state.index, state.scenes.size)
//                }
//            }
        }

        private fun adjustCameraWhenSwitchScene() {
            recoveryCameraStateRetain(curBoardState)
        }

        override fun onMemberStateChanged(state: MemberState) {
            AgoraLog?.e("$tag:onMemberStateChanged->${Gson().toJson(state)}")
        }

        override fun onCameraStateChanged(state: CameraState) {
            AgoraLog?.i("$tag:onCameraStateChanged:${Gson().toJson(state)}")
            if (curBoardState?.isGranted(aPaaSUserUuid) == true) {
                getCurSceneId()?.let {
                    sceneCameraConfigs[it] = state
                }
            }
        }

        override fun onDisconnectWithError(e: Exception?) {
            AgoraLog?.e("$tag:onDisconnectWithError->${e?.message}")
            disconnectErrorHappen = true
            if (!isEmpty(whiteboardUuid) && !isEmpty(curLocalToken) && !isEmpty(aPaaSUserUuid)) {
                joinWhiteBoard()
            }
        }

        override fun onRoomStateChanged(modifyState: RoomState?) {
            if (boardProxy.boardState != null) {
                val windowBoxState = boardProxy.boardState.windowBoxState ?: ""
                AgoraLog?.i("$tag:onRoomStateChanged-> 窗口大小${windowBoxState}")
                if (!TextUtils.isEmpty(windowBoxState)) {
                    // 移动位置
                    val layoutParams = whiteBoardControlView.layoutParams as RelativeLayout.LayoutParams
                    // maximized: 最大化, minimized: 最小化,normal   : 默认展开
                    if (windowBoxState == "minimized") { // 显示H5 icon
                        val context = whiteBoardControlView.context
                        layoutParams.setMargins(
                            context.resources.getDimensionPixelOffset(R.dimen.agora_wb_tools_margin_left_icon), 0, 0,
                            context.resources.getDimensionPixelOffset(R.dimen.agora_wb_tools_margin_bottom)
                        )

                        if (videoDirection == "left") {//视频窗UI在左边的情况
                            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0)
                            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                            layoutParams.setMargins(
                                0, 0, context.resources.getDimensionPixelOffset(R.dimen.agora_wb_tools_margin_left),
                                context.resources.getDimensionPixelOffset(R.dimen.agora_wb_tools_margin_bottom)
                            )
                        }
                    } else {
                        val context = whiteBoardControlView.context
                        layoutParams.setMargins(
                            context.resources.getDimensionPixelOffset(R.dimen.agora_wb_tools_margin_left), 0, 0,
                            context.resources.getDimensionPixelOffset(R.dimen.agora_wb_tools_margin_bottom)
                        )
                        if (videoDirection == "left") {//视频窗UI在左边的情况
                            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0)
                            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                            layoutParams.setMargins(
                                0, 0, context.resources.getDimensionPixelOffset(R.dimen.agora_wb_tools_margin_left),
                                context.resources.getDimensionPixelOffset(R.dimen.agora_wb_tools_margin_bottom)
                            )
                        }

                    }
                    whiteBoardControlView.layoutParams = layoutParams
                }
            }
        }

        override fun onCanUndoStepsUpdate(canUndoSteps: Long) {
            AgoraLog?.i("onCanUndoStepsUpdate uuid=${uuid}")
            uuid?.let {
                val list = AgoraWhiteBoardManager.getWhiteBoardList(it)
                if (list != null) {
                    for (listener in list) {
                        listener.onCanUndoStepsUpdate(canUndoSteps)
                    }
                }
            }
        }

        override fun onCanRedoStepsUpdate(canRedoSteps: Long) {
            uuid?.let {
                val list = AgoraWhiteBoardManager.getWhiteBoardList(it)
                if (list != null) {
                    for (listener in list) {
                        listener.onCanRedoStepsUpdate(canRedoSteps)
                    }
                }
            }
        }

        override fun onGlobalStateChanged(state: GlobalState?) {
            (state as? BoardState)?.let { newState ->
                AgoraLog?.d("$tag:onGlobalStateChanged->${state}")
//                handleFullScreenState(curBoardState, newState)
                handleUserDefinedWhiteboardState(curBoardState, newState)
                curBoardState = newState
                handleStudentStatus(newState)
                handleGrantChanged(newState)
            }
        }

        /**
         * FullScreen is business-defined state for whiteboard container
         * layout on UI layer. Besides the event callbacks, we should also
         * reset the camera (for whiteboard content scaling) to insure
         * the whole course ware file properly fits to the whole container.
         */
        private fun handleFullScreenState(oldState: BoardState?, newState: BoardState) {
            val isFullScreen = newState.isFullScreen
            when {
                oldState == null -> {

                }
                newState.isFullScreen == oldState.isFullScreen -> {
                    // is not init and no anything changed
                }
                else -> {
                    // when board`s size changed, scalePptToFit
                    scaleToFit()
//                    whiteboardContext.getHandlers()?.forEach {
//                        it.onFullScreenChanged(isFullScreen)
//                        it.onFullScreenEnabled(!isFullScreen)
//                    }
                }
            }

            if (grantChanged(oldState, newState)) {
                boardProxy.follow(!newState.isGranted(aPaaSUserUuid))
                // if granted，try recovery cameraConfig
                recoveryCameraStateRetain(newState)
            }
        }

        private fun handleUserDefinedWhiteboardState(oldState: BoardState?, newState: BoardState) {
            // Check if user defined properties have changed.
            // User defined properties are internal concepts, managed
            // as part of the entire whiteboard global state.
            // However, they are translated to "GlobalState"
            // of whiteboard in the current room.
            if (!newState.userDefinedPropertyEquals(oldState)) {
//                whiteboardContext.getHandlers()?.forEach {
//                    it.onWhiteboardGlobalStateChanged(newState.flexBoardState)
//                }
            }
        }

        /**
         * handle student status follow BoardState
         * if there is only student and no teacher,
         * 1: load student's default courseware.
         * 2: show whiteBoard's OptionItem、ControlView
         */
        private fun handleStudentStatus(newState: BoardState) {
            handleDefaultCoursewareIfNeeded(newState)
            showWhiteBoardOptionControlIfNeeded(newState)
        }

        private fun handleDefaultCoursewareIfNeeded(newState: BoardState) {
            val isStudent = widgetInfo?.localUserInfo?.userRole == Student.value
            // if there is only student and no teacher,load student's default courseware.
            if (isStudent && !newState.isTeacherFirstLogin && loadPreviewPpt) {
                loadPreviewPpt = false
                val setDefaultCoursewareFunc = {
                    AgoraLog?.i("$tag->set default courseware.")
                    courseWareManager?.loadPrivateCourseWare(object : Promise<Boolean> {
                        override fun then(t: Boolean) {
                            AgoraLog?.e("$tag:loadPrivateCourseWare -> $t")
                            if (t) boardProxy.scalePptToFit()
                        }

                        override fun catchEx(t: SDKError?) {
                            AgoraLog?.e("$tag:loadPrivateCourseWare-catchEx->${t?.message}")
                        }
                    })
                }
                boardProxy.setWritable(true, object : Promise<Boolean> {
                    override fun then(t: Boolean?) {
                        if (t == true) {
                            setDefaultCoursewareFunc.invoke()
                        }
                    }

                    override fun catchEx(t: SDKError?) {
                    }
                })
            }
        }

        private fun showWhiteBoardOptionControlIfNeeded(newState: BoardState) {
            widgetInfo?.localUserInfo?.let {
                val isStudent = it.userRole == Student.value
                // if current role is student and teacher not is here or current student is granted by teacher,
                // show optionItem、controlView
                // otherwise hide them.
                val show = isStudent && !newState.isTeacherFirstLogin || (newState.grantUsers.contains(it.userUuid))
                setWhiteBoardControlView(show)
                broadcastPermissionChanged(if (show) mutableListOf(it.userUuid) else mutableListOf())
            }
        }
    }
    private val sdkCommonCallback = object : CommonCallback {
        override fun throwError(args: Any?) {
            AgoraLog?.e("$tag:throwError->${Gson().toJson(args)}")
        }

        override fun urlInterrupter(sourceUrl: String?): String? {
            AgoraLog?.i("$tag:urlInterrupter->$sourceUrl")
            return null
        }

        override fun onPPTMediaPlay() {
            AgoraLog?.i("$tag:onPPTMediaPlay")
        }

        override fun onPPTMediaPause() {
            AgoraLog?.i("$tag:onPPTMediaPlay")
        }

        override fun onMessage(`object`: JSONObject?) {
            AgoraLog?.e("$tag:onMessage->${Gson().toJson(`object`)}")
        }

        override fun sdkSetupFail(error: SDKError?) {
            AgoraLog?.e("$tag:sdkSetupFail->${error?.jsStack}")
        }

        override fun onLogger(`object`: JSONObject?) {
            super.onLogger(`object`)
            AgoraLog?.i("$tag:onLogger->${`object`?.toString()}")
        }
    }
    private val whiteboardMixingBridgeListener = object : WhiteBoardAudioMixingBridgeListener {
        override fun onStartAudioMixing(filepath: String, loopback: Boolean, replace: Boolean, cycle: Int) {
            AgoraLog?.i("$tag WhiteBoardSDK: onStartAudioMixing filepath=$filepath loopback=$loopback replace=$replace cycle=$cycle")
            val code = eduCore?.eduContextPool()?.mediaContext()?.startAudioMixing(filepath, loopback, replace, cycle)
            AgoraLog?.i("$tag->invoke RTC  Mixing API state :714  || errorCode = ${code}")
            code?.let { // 0: 方法调用成功，记得监听 通过RTC的回调回调
                if (code != 0) { // 约定下来的状态值 除了startAudioMixing 失败传：714
                    AgoraLog?.i("$tag->invoke RTC  Mixing API state :714  || errorCode = ${code}")
                    invokeCallbackToWhiteBoard(714, code)
                }
            }
        }

            override fun onStopAudioMixing() {
            AgoraLog?.i("$tag WhiteBoardSDK: onStopAudioMixing")

            val code = eduCore?.eduContextPool()?.mediaContext()?.stopAudioMixing()
            invokeCallbackToWhiteBoard(0, code)
        }

        override fun onSetAudioMixingPosition(position: Int) {
            AgoraLog?.i("$tag WhiteBoardSDK: onSetAudioMixingPosition：$position")

            val code = eduCore?.eduContextPool()?.mediaContext()?.setAudioMixingPosition(position)
            invokeCallbackToWhiteBoard(0, code)
        }

        override fun pauseAudioMixing() {
            AgoraLog?.i("$tag WhiteBoardSDK: pauseAudioMixing")

            val code = eduCore?.eduContextPool()?.mediaContext()?.pauseAudioMixing()
            invokeCallbackToWhiteBoard(0, code)
        }

        override fun resumeAudioMixing() {
            AgoraLog?.i("$tag WhiteBoardSDK: resumeAudioMixing")

            val code = eduCore?.eduContextPool()?.mediaContext()?.resumeAudioMixing()
            invokeCallbackToWhiteBoard(0, code)
        }

        fun invokeCallbackToWhiteBoard(state: Long, errorCode: Int?) {
            if (state != 0L) { // 出错才调用
                errorCode?.let {
                    AgoraLog?.i("$tag invoke RTC  Mixing API state :0  || errorCode = ${errorCode}")
                    boardProxy.changeMixingState(state, errorCode.toLong())
                }
            }
        }
    }
    private var initJoinWhiteBoard = false

    // CloudDisk msgObserver
    val cloudDiskMsgObserver = object : AgoraWidgetMessageObserver {
        override fun onMessageReceived(msg: String, id: String) {
            val packet = GsonUtil.jsonToObject<AgoraBoardInteractionPacket>(msg)
            if (packet?.signal?.value == LoadCourseware.value) {
                val coursewareJson = GsonUtil.toJson(packet.body)
                coursewareJson?.let {
                    GsonUtil.jsonToObject<AgoraEduCourseware>(it)?.let { courseware ->
                        courseWareManager?.loadCourseware(courseware)
                    }
                }
            }
        }
    }

    init {
        BoardStyleInjector.setPosition(12, 12)
        WhiteDisplayerState.setCustomGlobalStateClass(BoardState::class.java)
        DWebView.setWebContentsDebuggingEnabled(true)
    }

    override fun init(container: ViewGroup) {
        super.init(container)
        val view = LayoutInflater.from(container.context).inflate(R.layout.agora_whiteborad_widget, null)
        whiteBoardView = view.findViewById(R.id.agora_whiteboard_view)
        loadingView = view.findViewById(R.id.agora_edu_loading)
        whiteBoardView.webChromeClient = webChromeClient
        boardProxy.setListener(boardEventListener)
        val p = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        container.addView(view, p)
        joinWhiteBoard()
        // init whiteBoardControlView and handle startClassBtn visibility
        whiteBoardControlView = view.findViewById(R.id.agora_whiteboard_control)
        // roomContext from AgoraEduWhiteBoardComponent.roomHandler.onJoinRoomSuccess
        // todo(cjw) acadsoc not need teacher in mobile
//        whiteBoardControlView.setRoomContext((widgetInfo?.extraInfo as? RoomContext), widgetInfo?.localUserInfo)
    }

    fun setDirection(videoDirection: String) {
        this.videoDirection = videoDirection
    }

    fun setWhiteBoardControlView(isShow: Boolean) {
        if (isShow) {
            whiteBoardControlView.visibility = View.VISIBLE
        } else {
            whiteBoardControlView.visibility = View.GONE
        }
    }

    private fun parseWhiteBoardConfigProperties(): Boolean {
        val extraProperties = this.widgetInfo?.roomProperties as? MutableMap<*, *>
        extraProperties?.let {
            this.whiteBoardAppId = it["boardAppId"] as? String ?: ""
            this.region = it["boardRegion"] as? String
            this.whiteboardUuid = it["boardId"] as? String ?: ""
            this.curLocalToken = it["boardToken"] as? String ?: ""
            this.aPaaSUserUuid = widgetInfo?.localUserInfo?.userUuid
            this.aPaaSUserName = widgetInfo?.localUserInfo?.userName
        }

        AgoraLog?.e("$tag:join board boardAppId-> $whiteBoardAppId || boardToken=$curLocalToken || boardId=$whiteboardUuid")

        return !isEmpty(whiteBoardAppId) && !isEmpty(whiteboardUuid) && !isEmpty(curLocalToken)
            && !isEmpty(aPaaSUserUuid)
    }

    private fun joinWhiteBoard() {
        if (!parseWhiteBoardConfigProperties()) {
            AgoraLog?.e("$tag->WhiteBoardConfigProperties isNullOrEmpty, please check roomProperties.widgets.netlessBoard")
            return
        }
        initJoinWhiteBoard = true
        if (courseWareManager == null) {
            courseWareManager = CourseWareManager(boardProxy)
        }
        // You can pass any data through extra; like this.
        widgetInfo?.extraInfo?.let { it ->
            if (it is MutableMap<*, *> && it.isNotEmpty() && it.keys.contains("fitMode")) {
                boardFitMode = it["fitMode"] as AgoraBoardFitMode
            }
            (it as? ArrayList<AgoraEduCourseware>)?.let { coursewares ->
                // set default courseware
                courseWareManager?.setPrivateCourseWare(coursewares.toMutableList())
            }
        }
        container?.post {
            boardProxy.getRoomPhase(object : Promise<RoomPhase> {
                override fun then(phase: RoomPhase) {
                    AgoraLog?.e(tag + ":then->" + phase.name)
                    if (phase != RoomPhase.connected) {
                        broadcastBoardPhaseState(Disconnected)
                        joinWhiteboard(whiteboardUuid!!, curLocalToken!!)
                        ReportManager.getAPaasReporter().reportWhiteBoardStart()
                        whiteBoardControlView.initView(boardProxy)
                    }
                }

                override fun catchEx(t: SDKError) {
                    AgoraLog?.e(tag + ":catchEx->" + t.message)
                    ToastManager.showShort(t.message!!)
                    loadingView.visibility = View.GONE
                }
            })
        }
    }

    private fun joinWhiteboard(uuid: String, boardToken: String) {
        loadingView.visibility = View.VISIBLE

        val configuration = WhiteSdkConfiguration(whiteBoardAppId, true)
        configuration.isEnableIFramePlugin = true
        configuration.isUserCursor = true
        configuration.region = TypeConverter.convertStringToRegion(region)
        configuration.useMultiViews = true

        val whiteSdk = WhiteSdk(
            whiteBoardView, whiteBoardView.context, configuration,
            sdkCommonCallback, AudioMixerBridgeImpl(whiteboardMixingBridgeListener)
        )

        val scaleWhiteBoardMap = HashMap<String, String>().apply {
            this["position"] = "fixed"
            this["left"] = "12px"
            this["bottom"] = "12px"
            this["width"] = "34px"
            this["height"] = "34px"
        }

        val windowParams = WindowParams()
        windowParams.setCollectorStyles(scaleWhiteBoardMap) // 设置窗口收缩按钮
        val params = RoomParams(uuid, boardToken, aPaaSUserUuid)
        params.cameraBound = CameraBound(miniScale, maxScale)
        params.isWritable = widgetInfo?.localUserInfo?.userRole == AgoraEduRoleTypeTeacher.value
        params.isDisableNewPencil = false
        params.windowParams = windowParams
        params.windowParams.setChessboard(false)
        params.userPayload = mutableMapOf(Pair("cursorName", aPaaSUserName))
        container?.let {
            // Fill the container to draw the courseware pages
            //val ratio = it.height.toFloat() / it.width.toFloat()
            val ratio = 9 / 16f
            AgoraLog?.e("白板比例：$ratio （h=${it.height},w=${it.width})")
            params.windowParams.setContainerSizeRatio(ratio)
        }
        // inject styleParams
        // params.windowParams.setCollectorStyles(BoardStyleInjector.getPositionStyle())

        boardProxy.init(whiteSdk, params)
        whiteBoardControlView.initView(boardProxy)
    }

    // receive msg from outside of widget.(other layout or container, and so on)
    override fun onMessageReceived(message: String) {
        super.onMessageReceived(message)
        val packet: AgoraBoardInteractionPacket? =
            GsonUtils.mGson.fromJson(message, AgoraBoardInteractionPacket::class.java)
        packet?.let {
            when (packet.signal) {
                MemberStateChanged -> {
                    (GsonUtils.mGson.fromJson(packet.body.toString(), AgoraBoardDrawingMemberState::class.java))?.let {
                        onMemberStateChanged(it)
                    } ?: Runnable {
                        AgoraLog?.e("$tag->${packet.signal}, (MemberStateChanged)packet.body convert failed")
                    }
                }
                BoardGrantDataChanged -> {
                    (GsonUtils.mGson.fromJson(packet.body.toString(), AgoraBoardGrantData::class.java))?.let { data ->
                        data.userUuids.forEach {
                            grantBoard(it, data.granted)
                        }
                    }
                        ?: Runnable { AgoraLog?.e("$tag->${packet.signal}, (BoardGrantDataChanged)packet.body convert failed") }
                }
                RTCAudioMixingStateChanged -> {
                    try {
                        val state = (packet.body as? Map<*, *>)?.get("first") as Double
                        val errorCode = (packet.body as? Map<*, *>)?.get("second") as Double
                        AgoraLog?.i("$tag RTCAudioMixingStateChanged state ${state} || errorCode = ${errorCode}")
                        boardProxy.changeMixingState(state.toLong(), errorCode.toLong())
                    } catch (e: Exception) {
                        e.printStackTrace()
                        AgoraLog?.e("$tag RTCAudioMixingStateChanged state error:${Log.getStackTraceString(e)}")
                    }
                }
                LoadCourseware -> {
                    (GsonUtils.mGson.fromJson(packet.body.toString(), AgoraEduCourseware::class.java))?.let {
                        courseWareManager?.loadCourseware(it)
                    } ?: Runnable {
                        AgoraLog?.e("$tag->${packet.signal}, (MemberStateChanged)packet.body convert failed")
                    }
                }
                else -> {
                }
            }
        }
    }


    override fun onWidgetRoomPropertiesUpdated(
        properties: MutableMap<String, Any>, cause: MutableMap<String, Any>?,
        keys: MutableList<String>
    ) {
        super.onWidgetRoomPropertiesUpdated(properties, cause, keys)
        if (properties.keys.contains(boardAppIdKey) && properties.keys.contains(boardTokenKey) && !initJoinWhiteBoard) {
            joinWhiteBoard()
        }
//        if (keys.contains(grantUser)) {
//            handleGrantChanged(getGrantUsers())
//        }
    }

    private fun handleGrantChanged(state: BoardState) {
        val isStudent = widgetInfo?.localUserInfo?.userRole == Student.value
        val granted = if (isStudent) {
            !state.isTeacherFirstLogin || state.grantUsers.contains(aPaaSUserUuid)
        } else {
            true
        }
        // student has permission before teacherFirstLogin.
        if (granted && !state.grantUsers.contains(aPaaSUserUuid)) {
            state.grantUsers.add(aPaaSUserUuid)
        }
        disableCameraTransform(!granted)
        disableDeviceInputs(!granted)

        if (granted != curGranted) {
            curGranted = granted
            // set writeable follow granted if curRole is student
            if (isStudent) {
                AgoraLog?.i("$tag->set writeable follow granted: $granted")
                boardProxy.setWritable(granted, object : Promise<Boolean> {
                    override fun then(t: Boolean?) {
                        if (t == granted) {
                            AgoraLog?.i("$tag->set followMode: ${!granted}")
                            boardProxy.follow(!granted)
                            if (!firstGrantedTip) {
                                AgoraUIToast.info(
                                    context = whiteBoardView.context, text = whiteBoardView.context
                                        .resources.getString(
                                            if (curGranted == true) R.string.fcr_netless_board_granted
                                            else R.string.fcr_netless_board_ungranted
                                        )
                                )
                            } else {
                                firstGrantedTip = false
                            }
                        } else {
                            AgoraLog?.i(
                                "$tag->setWritable follow granted failed: wanted: $granted, but " +
                                    "result is: $t"
                            )
                        }
                    }

                    override fun catchEx(t: SDKError?) {
                    }
                })
            } else {
                setWhiteBoardControlView(true)
            }
        }

        if (curGrantedUsers != state.grantUsers) {
            curGrantedUsers.clear()
            curGrantedUsers.addAll(state.grantUsers)
            broadcastPermissionChanged(state.grantUsers)
//                whiteBoardManagerEventListener?.onGrantedChanged()
        }
    }

    private fun getGrantUsers(): MutableList<String> {
//        val extra = widgetInfo?.properties?.get(grantUser) as? MutableMap<*, *>
        val tmp = widgetInfo?.roomProperties?.get(grantUser) as? ArrayList<*>
//        val tmp = extra?.get(grantUser) as? MutableList<*>
        return if (!tmp.isNullOrEmpty() && tmp[0] is String) {
            tmp.toList() as MutableList<String>
        } else {
            mutableListOf()
        }
    }

    override fun release() {
        super.release()
        releaseBoard()
    }

    /**
     * grant user to draw on whiteBoard
     * */
    private fun grantBoard(userId: String, granted: Boolean) {
        curBoardState?.let { state ->
            state.grantUser(userId, granted)
            boardProxy.setGlobalState(state)
        }
    }

    private fun getCurScenePath(): String? {
        return curSceneState?.scenePath
    }

    private fun getCurSceneId(): String? {
        val sceneNames = curSceneState?.scenes?.map { it.name }
        return if (sceneNames?.contains(defaultCoursewareName) == true) {
            defaultCoursewareName
        } else {
            val tmp = curSceneState?.scenePath?.split("/")
            tmp?.get(1)
        }
    }

    private fun initWriteableFollowLocalRole(state: GlobalState?, promise: Promise<Boolean>) {
        widgetInfo?.localUserInfo?.userRole?.let {
            (state as? BoardState)?.let { newState ->
                var a = false
                if (it == Student.value) {
                    // when teacher not come in, student can write on the board.
                    a = !newState.isTeacherFirstLogin
                } else if (it == Teacher.value) {
                    a = true
                }
                this@AgoraWhiteBoardWidget.boardProxy.setWritable(a, object : Promise<Boolean> {
                    override fun then(t: Boolean?) {
                        if (t == a) {
                            this@AgoraWhiteBoardWidget.disableDeviceInputs(!a)
                            promise.then(t)
                        }
                    }

                    override fun catchEx(t: SDKError?) {
                        promise.catchEx(t)
                    }
                })
            }
        }
    }

    private fun initDrawingConfig(promise: Promise<Unit>) {
        if (disconnectErrorHappen) {
            promise.then(Unit)
            return
        }
        boardProxy.setWritable(true, object : Promise<Boolean> {
            override fun then(t: Boolean?) {
                disableDeviceInputs(false)
                curDrawingConfig.activeAppliance = WhiteboardApplianceType.Clicker
                curDrawingConfig.color = getColor(whiteBoardView.context, R.color.agora_board_default_stroke)
                curDrawingConfig.fontSize = whiteBoardView.context.resources.getInteger(R.integer.agora_board_default_font_size)
                // init memberState and broadcastDrawingConfig(cover onRoomPhaseChanged called,
                // because restoreWhiteBoardAppliance before onJoinSuccess)
                restoreWhiteBoardAppliance()
                broadcastDrawingConfig()
//                boardProxy.setWritable(false, object : Promise<Boolean> {
                boardProxy.setWritable(true, object : Promise<Boolean> {
                    override fun then(t: Boolean?) {
                        disableDeviceInputs(true)
                        promise.then(Unit)
                    }

                    override fun catchEx(t: SDKError?) {
                        AgoraLog?.e("$tag->initDrawingConfig-restore Writable error:${t?.jsStack}")
                        promise.catchEx(t)
                    }
                })
            }

            override fun catchEx(t: SDKError?) {
                AgoraLog?.e("$tag->initDrawingConfig-setWritable:${t?.jsStack}")
                promise.catchEx(t)
            }
        })
    }

    /**
     * broadcastDrawingConfig to outside(such as toolbar or AgoraUIWhiteboardOption)
     * */
    private fun broadcastDrawingConfig() {
        val body = AgoraBoardInteractionPacket(MemberStateChanged, curDrawingConfig)
        sendMessage(GsonUtils.mGson.toJson(body))
    }

    /**
     * broadcast permissionChanged
     * */
    private fun broadcastPermissionChanged(grantedUsers: MutableList<String>) {
        val body = AgoraBoardInteractionPacket(BoardGrantDataChanged, grantedUsers)
        sendMessage(GsonUtils.mGson.toJson(body))
    }

    /**
     * broadcaster BoardPhaseState
     * */
    private fun broadcastBoardPhaseState(phase: EduBoardRoomPhase) {
        val body = AgoraBoardInteractionPacket(BoardPhaseChanged, phase)
        sendMessage(GsonUtils.mGson.toJson(body))
    }

    private fun disableDeviceInputs(disabled: Boolean) {
        val a = boardProxy.isDisableDeviceInputs
        if (disabled != a) {
            if (!inputTips) {
                inputTips = true
            }
        }
        boardProxy.disableDeviceInputs(disabled)

//        whiteboardContext.getHandlers()?.forEach {
//            it.onDrawingEnabled(!disabled)
//            it.onPagingEnabled(!disabled)
//        }
    }

    private fun disableCameraTransform(disabled: Boolean) {
        val a = boardProxy.isDisableCameraTransform
        if (disabled != a) {
            if (disabled) {
                if (!transform) {
                    transform = true
                }
                boardProxy.disableDeviceInputsTemporary(true)
            } else {
                boardProxy.disableDeviceInputsTemporary(boardProxy.isDisableDeviceInputs)
            }
        }
//        whiteboardContext.getHandlers()?.forEach {
//            it.onZoomEnabled(!disabled, !disabled)
//        }
        boardProxy.disableCameraTransform(disabled)
    }

    private fun scaleToFit() {
        if (boardFitMode == AgoraBoardFitMode.Auto) {
            boardProxy.scalePptToFit()
        } else {
            recoveryCameraStateRetain(curBoardState)
        }
    }

    private fun recoveryCameraStateRetain(state: BoardState?) {
        if (this.boardFitMode != AgoraBoardFitMode.Retain) {
            return
        }

        if (state?.isGranted(aPaaSUserUuid) == true) {
            recoveryCameraState()
        } else {
            boardProxy.follow(true)
        }
    }

    private fun recoveryCameraState() {
        val cameraState = sceneCameraConfigs[getCurSceneId()]
        if (cameraState != null) {
            val cameraConfig = CameraConfig()
            cameraConfig.centerX = cameraState.centerX
            cameraConfig.centerY = cameraState.centerY
            cameraConfig.scale = cameraState.scale
            cameraConfig.animationMode = AnimationMode.Immediately
            boardProxy.moveCamera(cameraConfig)
        } else {
            boardProxy.follow(true)
            boardProxy.follow(false)
        }
    }

    private fun releaseBoard() {
        AgoraLog?.e("$tag:releaseBoard")
        boardProxy.disconnect()
        whiteBoardView.removeAllViews()
        whiteBoardView.addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(p0: View?) {
                    // Nothing done
                }

                override fun onViewDetachedFromWindow(p0: View?) {
                    // WebView will prompt an warning when it is
                    // destroyed before being detached from view
                    // hierarchy.
                    AgoraLog?.i("$tag:whiteboard view destroy called")
                    whiteBoardView.destroy()
                }
            })

//        val now = System.currentTimeMillis()
//        whiteboardContext.getHandlers()?.forEach {
//            it.onWhiteboardLeft(launchConfig.boardId, now)
//        }
    }

    private fun restoreWhiteBoardAppliance() {
        val drawingMemberState = AgoraBoardDrawingMemberState()
        drawingMemberState.activeApplianceType = curDrawingConfig.activeAppliance
        drawingMemberState.strokeColor = curDrawingConfig.color
        drawingMemberState.textSize = curDrawingConfig.fontSize
        drawingMemberState.strokeWidth = curDrawingConfig.thick
        onMemberStateChanged(drawingMemberState)
    }

    private fun onMemberStateChanged(state: AgoraBoardDrawingMemberState) {
        AgoraLog?.i("$tag->onMemberStateChanged:${GsonUtils.mGson.toJson(state)}")

        if (state.activeApplianceType == WhiteboardApplianceType.WB_Clear) {
            boardProxy.cleanScene(false)
        } else if (state.activeApplianceType == WhiteboardApplianceType.WB_Pre) {
            boardProxy.undo()
        } else if (state.activeApplianceType == WhiteboardApplianceType.WB_Next) {
            boardProxy.redo()
        } else {
            val memberState = MemberState()
            state.activeApplianceType?.let {
                curDrawingConfig.activeAppliance = it
                val convertShape = TypeConverter.convertShape(it)
                if (convertShape != null) {
                    memberState.currentApplianceName = Appliance.SHAPE
                    memberState.shapeType = convertShape
                } else {
                    memberState.currentApplianceName = TypeConverter.convertApplianceToString(it)
                }
            }
            state.strokeColor?.let {
                curDrawingConfig.color = it
                memberState.strokeColor = colorToArray(it)
            }
            if (state.textSize != null) {
                curDrawingConfig.fontSize = state.textSize!!
                memberState.textSize = state.textSize!!.toDouble()
            } else {
                memberState.textSize = 0.0
            }
            if (state.strokeWidth != null) {
                curDrawingConfig.thick = state.strokeWidth!!
                memberState.strokeWidth = state.strokeWidth!!.toDouble()
            } else {
                memberState.strokeWidth = 0.0
            }
            boardProxy.setMemState(memberState)
        }
    }

    private fun grantChanged(oldState: BoardState?, newState: BoardState): Boolean {
        return oldState?.isGranted(aPaaSUserUuid) != newState.isGranted(aPaaSUserUuid)
    }

    /**
     * Load a doc and display the first page in single view mode.
     * In our design, each doc file is a separate scene directory. That
     * is, no pages from another doc file can be inserted into current
     * doc file directory.
     * @param dir the directory that the set of scenes belong to
     * @param scenes document pages as scenes
     */
    private fun openDocInSingleViewMode(dir: String, scenes: Array<Scene>, promise: Promise<Boolean>? = null) {
        if (scenes.isNotEmpty()) {
            // always put current scenes to the last position of
            // current scene directory. See structure of scene path from
            // https://developer.netless.link/android-zh/home/android-scenes
            boardProxy.putScenes(dir, scenes, Integer.MAX_VALUE)
            boardProxy.setScenePath(dir + File.pathSeparator + scenes[0].name, promise)
        }
    }

    private class CourseWareManager(val boardRoom: BoardRoom) {

        // The course ware that a user takes along with him.
        // User can only load and see his private course ware
        // in some designed situations.
        // Others cannot see his private course ware, of course
        private var privateCoursewares: MutableList<AgoraEduCourseware>? = null
        private var privateScenesList: MutableList<Array<Scene>> = mutableListOf()

        // A different scene directory means loading another
        // document file, at which time we may need to download
        // the document for better experience
        private var lastSceneDir: String? = null

        // Scenes loaded for all users in the room to see
        private var roomScenes = mutableListOf<Scene>()

        fun setPrivateCourseWare(coursewares: MutableList<AgoraEduCourseware>) {
            privateCoursewares = coursewares
            coursewares.forEach {
                val sceneArray = convert(it)
                privateScenesList.add(sceneArray)
            }
        }

        /**
         * Set current scene dir
         * @param dir nullable scene dir
         * @return true if current scene dir has changed to the new value,
         * false if the same dir has given
         */
        fun setSceneDir(dir: String?): Boolean {
            val same = dir == lastSceneDir
            lastSceneDir = dir
            return !same
        }

        private fun convert(courseware: AgoraEduCourseware): Array<Scene> {
            return mutableListOf<Scene>().apply {
                courseware.scenes?.let {
                    it.forEachIndexed { _, sceneInfo ->
                        val page = if (sceneInfo.ppt != null) {
                            val ppt = sceneInfo.ppt
                            PptPage(ppt?.src, ppt?.width, ppt?.height)
                        } else null

                        this.add(Scene(sceneInfo.name, page))
                    }
                }
            }.toTypedArray()
        }

        private fun load(param: WindowAppParam, promise: Promise<String>? = null) {
            boardRoom.setWindowApp(param, promise)
        }

        /**
         * Load and display a course ware file in the whiteboard view
         * container.
         * @param dir scene or resource path
         * @param scenes scene array
         * @param title title of the scenes in multi-view mode
         * load private courseware(student's or teacher's local courseware, preset in advance)
         */
        fun loadPrivateCourseWare(promise: Promise<Boolean>? = null) {
            if (privateCoursewares == null) {
                AgoraLog?.e("Load private courseware fails: no private courseware found.")
                return
            }

            if (privateScenesList.isNullOrEmpty()) {
                AgoraLog?.e("Load private courseware fails: empty private courseware content.")
                return
            }

            privateCoursewares?.forEach {
                loadCourseware(it)
            }
        }

        // load courseware during class，and called at any time by teacher
        fun loadCourseware(courseware: AgoraEduCourseware) {
            val dir = File.separator + courseware.resourceUuid
            var windowAppParam = when {
                courseware.conversion == null -> {
                    WindowAppParam.createMediaPlayerApp(courseware.resourceUrl, courseware.resourceName)
                }
                courseware.conversion?.canvasVersion == true -> {
                    val scenes = convert(courseware)
                    WindowAppParam.createSlideApp(dir, scenes, courseware.resourceName)
                }
                else -> {
                    val scenes = convert(courseware)
                    WindowAppParam.createDocsViewerApp(dir, scenes, courseware.resourceName)
                }
            }
            windowAppParam?.let {
                load(param = windowAppParam)
            }
        }
    }
}

internal object TypeConverter {
    fun convertStringToRegion(region: String?): Region {
        return when (region) {
            BoardRegionStr.cn -> {
                Region.cn
            }
            BoardRegionStr.na -> {
                Region.us
            }
            BoardRegionStr.eu -> {
                Region.gb_lon
            }
            BoardRegionStr.ap -> {
                Region.sg
            }
            else -> {
                Region.cn
            }
        }
    }

    fun convertShape(type: WhiteboardApplianceType): ShapeType? {
        return when (type) {
            WhiteboardApplianceType.Star -> {
                ShapeType.Pentagram
            }
            WhiteboardApplianceType.Rhombus -> {
                ShapeType.Rhombus
            }
            WhiteboardApplianceType.Triangle -> {
                ShapeType.Triangle
            }
            else -> null
        }
    }

    fun convertApplianceToString(type: WhiteboardApplianceType): String {
        return when (type) {
            WhiteboardApplianceType.Select -> {
                Appliance.SELECTOR
            }
            WhiteboardApplianceType.PenS -> {
                Appliance.PENCIL
            }
            WhiteboardApplianceType.Pen -> {
                Appliance.PENCIL
            }
            WhiteboardApplianceType.Rect -> {
                Appliance.RECTANGLE
            }
            WhiteboardApplianceType.Circle -> {
                Appliance.ELLIPSE
            }
            WhiteboardApplianceType.Line -> {
                Appliance.STRAIGHT
            }
            WhiteboardApplianceType.Eraser -> {
                Appliance.ERASER
            }
            WhiteboardApplianceType.Text -> {
                Appliance.TEXT
            }
            WhiteboardApplianceType.Clicker -> {
                Appliance.CLICKER
            }
            WhiteboardApplianceType.Laser -> {
                Appliance.LASER_POINTER
            }
            WhiteboardApplianceType.Arrow -> {
                Appliance.ARROW
            }
            else -> {
                // 其他教具
                ""
            }
        }
    }
}
