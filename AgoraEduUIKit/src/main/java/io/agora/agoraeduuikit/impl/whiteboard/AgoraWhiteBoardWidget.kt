package io.agora.agoraeduuikit.impl.whiteboard

import android.text.TextUtils.isEmpty
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.RelativeLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.herewhite.sdk.*
import com.herewhite.sdk.domain.*
import io.agora.agoraeduuikit.impl.whiteboard.bean.*
import io.agora.agoraeduuikit.R
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.EduBoardRoomPhase
import io.agora.agoraeducore.core.context.EduBoardRoomPhase.Companion.convert
import io.agora.agoraeducore.core.context.EduBoardRoomPhase.Disconnected
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeduuikit.impl.whiteboard.bean.BoardState
import io.agora.agoraeducore.core.internal.education.impl.Constants.Companion.AgoraLog
import io.agora.agoraeducore.core.internal.framework.impl.managers.AgoraWidgetManager.Companion.grantUser
import io.agora.agoraeducore.core.internal.launch.AgoraEduCourseware
import io.agora.agoraeducore.core.internal.launch.AgoraEduRoleType.AgoraEduRoleTypeTeacher
import io.agora.agoraeducore.core.internal.report.ReportManager
import io.agora.agoraeducore.core.internal.util.ColorUtil.colorToArray
import io.agora.agoraeducore.core.internal.util.ColorUtil.getColor
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.agoraeduuikit.impl.whiteboard.TypeConverter.convertApplianceToString
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionSignal.*
import io.agora.agoraeduuikit.impl.whiteboard.bean.BoardStyleInjector
import io.agora.agoraeduuikit.impl.whiteboard.netless.listener.BoardEventListener
import io.agora.agoraeduuikit.impl.whiteboard.netless.manager.BoardRoom
import io.agora.agoraeduuikit.impl.whiteboard.netless.manager.BoardRoomImpl
import org.json.JSONObject
import wendu.dsbridge.DWebView
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.*
import kotlin.collections.HashMap

class AgoraWhiteBoardWidget() : AgoraBaseWidget() {
    override val tag = "AgoraWhiteBoardWidget"
    private lateinit var whiteBoardView: WhiteboardView
    private val boardProxy = Proxy.newProxyInstance(BoardRoom::class.java.classLoader,
        arrayOf(BoardRoom::class.java), object : InvocationHandler {
        private val boardRoom: BoardRoom = BoardRoomImpl()
        override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
            val parameters = args?.filter { it !is BoardEventListener && it !is WhiteSdk }
            AgoraLog.i("${BoardRoomImpl.TAG}->${method?.name}:${Gson().toJson(parameters)}")
            return if (args.isNullOrEmpty()) {
                method?.invoke(boardRoom)
            } else {
                method?.invoke(boardRoom, *args)
            }
        }
    }) as BoardRoom
    private var whiteBoardAppId: String? = null
    private var region: String? = null
    private var whiteboardUuid: String? = null
    private var curLocalToken: String? = null
    private var aPaaSUserUuid: String? = null
    private val miniScale = 0.1
    private val maxScale = 10.0
    private val sceneCameraConfigs: MutableMap<String, CameraState> = mutableMapOf()
    private var curBoardState: BoardState? = null
    private var curGranted: Boolean? = null
    private var curGrantedUsers = mutableListOf<String>()
    private val defaultCoursewareName = "init"
    private var courseware: AgoraEduCourseware? = null
    private var loadPreviewPpt: Boolean = false
    private var curSceneState: SceneState? = null
    private var curScenes: Array<Scene>? = null
    private var lastSceneDir: String? = null
    private var courseWareManager: CourseWareManager? = null
    private var inputTips = false
    private var transform = false
    private var boardFitMode = AgoraBoardFitMode.Retain
    private var disconnectErrorHappen = false
    private val boardAppIdKey = "boardAppId"
    private val boardTokenKey = "boardToken"
    private var firstGrantedTip = true

    // Default appliance configuration
    private val curDrawingConfig = WhiteboardDrawingConfig()
    private val webChromeClient = object : WebChromeClient() {
//        override fun onConsoleMessage(message: String?, lineNumber: Int, sourceId: String?) {
//            @Suppress("DEPRECATION")
//            super.onConsoleMessage(message, lineNumber, sourceId)
//            AgoraLog.d("$tag:console[$message, sourceID->($sourceId:$lineNumber)]")
//        }
//
//        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
//            AgoraLog.d("$tag:console[${consoleMessage?.message()}, " +
//                "sourceID->(${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})]")
//            return super.onConsoleMessage(consoleMessage)
//        }
    }
    private val boardEventListener = object : BoardEventListener {
        override fun onJoinSuccess(state: GlobalState) {
            AgoraLog.i(tag + ":onJoinSuccess->" + Gson().toJson(state))
            initDrawingConfig(object : Promise<Unit> {
                override fun then(t: Unit?) {
                    initWriteableFollowLocalRole()
                    onGlobalStateChanged(state)
                }

                override fun catchEx(t: SDKError?) {
                    initWriteableFollowLocalRole()
                    onGlobalStateChanged(state)
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
            AgoraLog.i(tag + ":onRoomPhaseChanged->" + phase.name)
            if (phase == RoomPhase.connected) {
                restoreWhiteBoardAppliance()
                broadcastDrawingConfig()
            }
            broadcastBoardPhaseState(convert(phase.name))
        }

        override fun onSceneStateChanged(state: SceneState) {
            AgoraLog.e("$tag:onSceneStateChanged->${Gson().toJson(state)}")
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
            AgoraLog.e("$tag:onMemberStateChanged->${Gson().toJson(state)}")
        }

        override fun onCameraStateChanged(state: CameraState) {
            AgoraLog.i("$tag:onCameraStateChanged:${Gson().toJson(state)}")
            if (curBoardState?.isGranted(aPaaSUserUuid) == true) {
                getCurSceneId()?.let {
                    sceneCameraConfigs[it] = state
                }
            }
        }

        override fun onDisconnectWithError(e: Exception?) {
            AgoraLog.e("$tag:onDisconnectWithError->${e?.message}")
            disconnectErrorHappen = true
            if (!isEmpty(whiteboardUuid) && !isEmpty(curLocalToken) && !isEmpty(aPaaSUserUuid)) {
                joinWhiteBoard()
            }
        }

        override fun onGlobalStateChanged(state: GlobalState) {
            (state as? BoardState)?.let { newState ->
                AgoraLog.d("$tag:onGlobalStateChanged->${state}")
//                handleFullScreenState(curBoardState, newState)
                handleUserDefinedWhiteboardState(curBoardState, newState)
                curBoardState = newState
                loadPrivateCourseWareIfNeeded(newState)
                handleGrantChanged(state.grantUsers)
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

        private fun loadPrivateCourseWareIfNeeded(newState: BoardState) {
            if (!newState.isTeacherFirstLogin && courseware != null && curScenes != null && loadPreviewPpt) {
                loadPreviewPpt = false

                courseWareManager?.loadPrivateCourseWare(object : Promise<Boolean> {
                    override fun then(t: Boolean) {
                        AgoraLog.e("$tag:loadPrivateCourseWare -> $t")
                        if (t) boardProxy.scalePptToFit()
                    }

                    override fun catchEx(t: SDKError?) {
                        AgoraLog.e("$tag:catchEx->${t?.message}")
                    }
                })
            }
        }
    }
    private val sdkCommonCallback = object : CommonCallback {
        override fun throwError(args: Any?) {
            AgoraLog.e("$tag:throwError->${Gson().toJson(args)}")
        }

        override fun urlInterrupter(sourceUrl: String?): String? {
            AgoraLog.i("$tag:urlInterrupter->$sourceUrl")
            return null
        }

        override fun onPPTMediaPlay() {
            AgoraLog.i("$tag:onPPTMediaPlay")
        }

        override fun onPPTMediaPause() {
            AgoraLog.i("$tag:onPPTMediaPlay")
        }

        override fun onMessage(`object`: JSONObject?) {
            AgoraLog.e("$tag:onMessage->${Gson().toJson(`object`)}")
        }

        override fun sdkSetupFail(error: SDKError?) {
            AgoraLog.e("$tag:sdkSetupFail->${error?.jsStack}")
        }

        override fun onLogger(`object`: JSONObject?) {
            super.onLogger(`object`)
            AgoraLog.i("$tag:onLogger->${`object`?.toString()}")
        }
    }
    private val whiteboardMixingBridgeListener = object : WhiteBoardAudioMixingBridgeListener {
        override fun onStartAudioMixing(filepath: String, loopback: Boolean, replace: Boolean, cycle: Int) {
            val data = AgoraBoardAudioMixingRequestData(type = AgoraBoardAudioMixingRequestType.Start,
                filepath = filepath, loopback = loopback, replace = replace, cycle = cycle)
            broadcastAudioMixingRequest(data)
        }

        override fun onStopAudioMixing() {
            val data = AgoraBoardAudioMixingRequestData(type = AgoraBoardAudioMixingRequestType.Stop)
            broadcastAudioMixingRequest(data)
        }

        override fun onSetAudioMixingPosition(position: Int) {
            val data = AgoraBoardAudioMixingRequestData(type = AgoraBoardAudioMixingRequestType.Stop,
                position = position)
            broadcastAudioMixingRequest(data)
        }

        private fun broadcastAudioMixingRequest(data: AgoraBoardAudioMixingRequestData) {
            val packet = AgoraBoardInteractionPacket(BoardAudioMixingRequest, data)
            sendMessage(Gson().toJson(packet))
        }
    }
    private var initJoinWhiteBoard = false

    init {
        BoardStyleInjector.setPosition(12, 12)
        WhiteDisplayerState.setCustomGlobalStateClass(BoardState::class.java)
        DWebView.setWebContentsDebuggingEnabled(true)
    }

    override fun init(parent: ViewGroup, width: Int, height: Int, top: Int, left: Int) {
        super.init(parent, width, height, top, left)
        whiteBoardView = WhiteboardView(parent.context)
//        whiteBoardView.setBackgroundColor(0)
        whiteBoardView.webChromeClient = webChromeClient
        boardProxy.setListener(boardEventListener)
        val layoutParams = RelativeLayout.LayoutParams(width, height)
        layoutParams.leftMargin = left
        layoutParams.topMargin = top
        parent.addView(whiteBoardView, layoutParams)
        joinWhiteBoard()
    }

    private fun parseWhiteBoardConfigProperties(): Boolean {
        val extraProperties = this.widgetInfo?.roomProperties as? MutableMap<*, *>
        extraProperties?.let {
            this.whiteBoardAppId = it["boardAppId"] as? String ?: ""
            this.region = it["boardRegion"] as? String
            this.whiteboardUuid = it["boardId"] as? String ?: ""
            this.curLocalToken = it["boardToken"] as? String ?: ""
            this.aPaaSUserUuid = widgetInfo?.localUserInfo?.userUuid
        }
        return !isEmpty(whiteBoardAppId) && !isEmpty(whiteboardUuid) && !isEmpty(curLocalToken)
            && !isEmpty(aPaaSUserUuid)
    }

    private fun joinWhiteBoard() {
        if (!parseWhiteBoardConfigProperties()) {
            AgoraLog.e("$tag->WhiteBoardConfigProperties isNullOrEmpty, please check " +
                "roomProperties.widgets.netlessBoard")
            return
        }
        initJoinWhiteBoard = true
        if (courseWareManager == null) {
            courseWareManager = CourseWareManager(true)
        }
        // You can pass any data through extra; like this.
        widgetInfo?.extraInfo?.let {
            if (it is MutableMap<*, *> && it.isNotEmpty() && it.keys.contains("fitMode")) {
                boardFitMode = it["fitMode"] as AgoraBoardFitMode
            }
        }
        container?.post {
            boardProxy.getRoomPhase(object : Promise<RoomPhase> {
                override fun then(phase: RoomPhase) {
                    AgoraLog.e(tag + ":then->" + phase.name)
                    if (phase != RoomPhase.connected) {
                        broadcastBoardPhaseState(Disconnected)
                        joinWhiteboard(whiteboardUuid!!, curLocalToken!!)
                        ReportManager.getAPaasReporter().reportWhiteBoardStart()
                    }
                }

                override fun catchEx(t: SDKError) {
                    AgoraLog.e(tag + ":catchEx->" + t.message)
                    ToastManager.showShort(t.message!!)
                }
            })
        }
    }

    private fun joinWhiteboard(uuid: String, boardToken: String) {
        val configuration = WhiteSdkConfiguration(whiteBoardAppId, true)
        configuration.isEnableIFramePlugin = true
        configuration.isUserCursor = true
        configuration.region = TypeConverter.convertStringToRegion(region)
        configuration.useMultiViews = true

        val whiteSdk = WhiteSdk(whiteBoardView, whiteBoardView.context, configuration,
            sdkCommonCallback, AudioMixerBridgeImpl(whiteboardMixingBridgeListener))

        val params = RoomParams(uuid, boardToken, aPaaSUserUuid)
        params.cameraBound = CameraBound(miniScale, maxScale)
        params.isWritable = widgetInfo?.localUserInfo?.userRole == AgoraEduRoleTypeTeacher.value
        params.isDisableNewPencil = false
        params.windowParams = WindowParams()
        params.windowParams.setChessboard(false)
        container?.let {
            // Fill the container to draw the courseware pages
            val ratio = it.height.toFloat() / it.width.toFloat()
            params.windowParams.setContainerSizeRatio(ratio)
        }
        // inject styleParams
        params.windowParams.setCollectorStyles(BoardStyleInjector.getPositionStyle())

        boardProxy.init(whiteSdk, params)
    }

    // receive msg from outside of widget.(other layout or container, and so on)
    override fun onMessageReceived(message: String) {
        super.onMessageReceived(message)
        val packet: AgoraBoardInteractionPacket? = Gson().fromJson(message, AgoraBoardInteractionPacket::class.java)
        packet?.let {
            if (packet.signal == MemberStateChanged) {
                (Gson().fromJson(packet.body.toString(), AgoraBoardDrawingMemberState::class.java))?.let {
                    onMemberStateChanged(it)
                } ?: Runnable {
                    AgoraLog.e("$tag->${packet.signal}, packet.body convert failed")
                }
            } else if (packet.signal == BoardGrantDataChanged) {
                (Gson().fromJson(packet.body.toString(), AgoraBoardGrantData::class.java))?.let { data ->
                    data.userUuids.forEach {
                        grantBoard(it, data.granted)
                    }
                } ?: Runnable { AgoraLog.e("$tag->${packet.signal}, packet.body convert failed") }
            } else if (packet.signal == RTCAudioMixingStateChanged) {
                (Gson().fromJson<Pair<Int, Int>>(packet.body.toString(),
                    object : TypeToken<Pair<Int, Int>>() {}.type))?.let { pair ->
                    boardProxy.changeMixingState(pair.first, pair.second)
                } ?: Runnable { AgoraLog.e("$tag->${packet.signal}, packet.body convert failed") }
            } else {
            }
        }
    }


    override fun onWidgetRoomPropertiesUpdated(properties: MutableMap<String, Any>, cause: MutableMap<String, Any>?,
                                               keys: MutableList<String>) {
        super.onWidgetRoomPropertiesUpdated(properties, cause, keys)
        if (properties.keys.contains(boardAppIdKey) && properties.keys.contains(boardTokenKey) && !initJoinWhiteBoard) {
            joinWhiteBoard()
        }
//        if (keys.contains(grantUser)) {
//            handleGrantChanged(getGrantUsers())
//        }
    }

    private fun handleGrantChanged(grantedUsers: MutableList<String>) {
        val granted = grantedUsers.contains(aPaaSUserUuid)
        disableCameraTransform(!granted)
        disableDeviceInputs(!granted)

        if (granted != curGranted) {
            curGranted = granted
            // set writeable follow granted if curRole is student
            if (widgetInfo?.localUserInfo?.userRole == AgoraEduContextUserRole.Student.value) {
                AgoraLog.i("$tag->set writeable follow granted: $granted")
                boardProxy.setWritable(granted)
                AgoraLog.i("$tag->set followMode: ${!granted}")
                boardProxy.follow(!granted)
                if (!firstGrantedTip) {
                    AgoraUIToast.info(context = whiteBoardView.context, text = whiteBoardView.context
                        .resources.getString(if (curGranted == true) R.string.authorize_board
                        else R.string.revoke_board))
                } else {
                    firstGrantedTip = false
                }
            }
        }

        if (curGrantedUsers != grantedUsers) {
            curGrantedUsers.clear()
            curGrantedUsers.addAll(grantedUsers)
            broadcastPermissionChanged(grantedUsers)
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

    private fun initWriteableFollowLocalRole() {
        widgetInfo?.localUserInfo?.userRole?.let {
            this@AgoraWhiteBoardWidget.boardProxy.setWritable(
                it == AgoraEduContextUserRole.Teacher.value)
            this@AgoraWhiteBoardWidget.disableDeviceInputs(
                it != AgoraEduContextUserRole.Teacher.value
            )
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
                curDrawingConfig.thick = whiteBoardView.context.resources.getInteger(R.integer.agora_board_default_thickness)
                // init memberState and broadcastDrawingConfig(cover onRoomPhaseChanged called,
                // because restoreWhiteBoardAppliance before onJoinSuccess)
                restoreWhiteBoardAppliance()
                broadcastDrawingConfig()
                boardProxy.setWritable(false)
                disableDeviceInputs(true)
                promise.then(Unit)
            }

            override fun catchEx(t: SDKError?) {
                AgoraLog.e("$tag->initDrawingConfig-setWritable:${t?.jsStack}")
                promise.catchEx(t)
            }
        })
    }

    /**
     * broadcastDrawingConfig to outside(such as toolbar or AgoraUIWhiteboardOption)
     * */
    private fun broadcastDrawingConfig() {
        val body = AgoraBoardInteractionPacket(MemberStateChanged, curDrawingConfig)
        sendMessage(Gson().toJson(body))
    }

    /**
     * broadcast permissionChanged
     * */
    private fun broadcastPermissionChanged(grantedUsers: MutableList<String>) {
        val body = AgoraBoardInteractionPacket(BoardGrantDataChanged, grantedUsers)
        sendMessage(Gson().toJson(body))
    }

    /**
     * broadcaster BoardPhaseState
     * */
    private fun broadcastBoardPhaseState(phase: EduBoardRoomPhase) {
        val body = AgoraBoardInteractionPacket(BoardPhaseChanged, phase)
        sendMessage(Gson().toJson(body))
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
        AgoraLog.e("$tag:releaseBoard")
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
                    AgoraLog.i("$tag:whiteboard view destroy called")
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
        AgoraLog.i("$tag->onMemberStateChanged:${Gson().toJson(state)}")
        val memberState = MemberState()
        state.activeApplianceType?.let {
            curDrawingConfig.activeAppliance = it
            memberState.currentApplianceName = convertApplianceToString(it)
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

    private fun grantChanged(oldState: BoardState?, newState: BoardState): Boolean {
        return oldState?.isGranted(aPaaSUserUuid) != newState.isGranted(aPaaSUserUuid)
    }

    private fun openDocsViewerInMultiViewMode(path: String, scenes: Array<Scene>,
                                              title: String, promise: Promise<String>? = null) {
        boardProxy.setWindowApp(WindowAppParam.createDocsViewerApp(path, scenes, title), promise)
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

    private inner class CourseWareManager(private val multiWindowMode: Boolean) {

        // The course ware that a user takes along with him.
        // User can only load and see his private course ware
        // in some designed situations.
        // Others cannot see his private course ware, of course
        private var privateCourseWare: AgoraEduCourseware? = null
        private var privateScenes: Array<Scene>? = null

        // A different scene directory means loading another
        // document file, at which time we may need to download
        // the document for better experience
        private var lastSceneDir: String? = null

        // Scenes loaded for all users in the room to see
        private var roomScenes = mutableListOf<Scene>()

        fun setPrivateCourseWare(courseware: AgoraEduCourseware) {
            privateCourseWare = courseware
            privateScenes = convert(courseware)
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
                            PptPage(ppt.src, ppt.width, ppt.height)
                        } else null

                        this.add(Scene(sceneInfo.name, page))
                    }
                }
            }.toTypedArray()
        }

        /**
         * Load and display a course ware file in the whiteboard view
         * container.
         * @param dir scene or resource path
         * @param scenes scene array
         * @param extra title of the scenes in multi-view mode
         */
        fun load(dir: String, scenes: Array<Scene>, extra: String, promise: Promise<String>? = null) {
            if (multiWindowMode) {
                this@AgoraWhiteBoardWidget.openDocsViewerInMultiViewMode(dir, scenes, extra, promise)
            } else {
                this@AgoraWhiteBoardWidget.openDocInSingleViewMode(dir, scenes)
            }
        }

        fun loadPrivateCourseWare(promise: Promise<Boolean>? = null) {
            if (privateCourseWare == null) {
                AgoraLog.e("Load private courseware fails: no private courseware found.")
                return
            }

            if (privateScenes.isNullOrEmpty()) {
                AgoraLog.e("Load private courseware fails: empty private courseware content.")
                return
            }

            val dir = File.separator + privateCourseWare!!.resourceUuid
            val page = privateScenes!![0].name
            load(dir, privateScenes!!, page, object : Promise<String> {
                override fun then(t: String?) {
                    promise?.then(true)
                }

                override fun catchEx(t: SDKError?) {
                    promise?.catchEx(t)
                }
            })
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

    fun convertApplianceToString(type: WhiteboardApplianceType): String {
        return when (type) {
            WhiteboardApplianceType.Select -> {
                Appliance.SELECTOR
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
        }
    }

    fun convertStringToAppliance(appliance: String): WhiteboardApplianceType {
        return when (appliance) {
            Appliance.CLICKER -> {
                WhiteboardApplianceType.Clicker
            }
            Appliance.SELECTOR -> {
                WhiteboardApplianceType.Select
            }
            Appliance.PENCIL -> {
                WhiteboardApplianceType.Pen
            }
            Appliance.RECTANGLE -> {
                WhiteboardApplianceType.Rect
            }
            Appliance.ELLIPSE -> {
                WhiteboardApplianceType.Circle
            }
            Appliance.STRAIGHT -> {
                WhiteboardApplianceType.Line
            }
            Appliance.ERASER -> {
                WhiteboardApplianceType.Eraser
            }
            Appliance.TEXT -> {
                WhiteboardApplianceType.Text
            }
            Appliance.LASER_POINTER -> {
                WhiteboardApplianceType.Laser
            }
            else -> {
                WhiteboardApplianceType.Clicker
            }
        }
    }
}
