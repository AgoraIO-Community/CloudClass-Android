package io.agora.agoraeducore.core.internal.edu.classroom

import io.agora.agoraeducore.core.internal.launch.AgoraEduRoleType.AgoraEduRoleTypeTeacher
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import com.google.gson.Gson
import com.herewhite.sdk.*
import com.herewhite.sdk.domain.*
import com.herewhite.sdk.domain.Appliance.SELECTOR
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.AgoraEduCoreConfig
import io.agora.agoraeducore.core.internal.edu.classroom.widget.whiteboard.BoardPreloadEventListener
import io.agora.agoraeducore.core.internal.edu.classroom.widget.whiteboard.BoardPreloadManager
import io.agora.agoraeducore.core.internal.edu.common.bean.board.BoardExt
import io.agora.agoraeducore.core.internal.edu.common.bean.board.BoardState
import io.agora.agoraeducore.core.internal.launch.AgoraEduCourseware
import io.agora.agoraeducore.core.internal.edu.classroom.widget.whiteboard.BoardRegionStr.ap
import io.agora.agoraeducore.core.internal.edu.classroom.widget.whiteboard.BoardRegionStr.cn
import io.agora.agoraeducore.core.internal.edu.classroom.widget.whiteboard.BoardRegionStr.eu
import io.agora.agoraeducore.core.internal.edu.classroom.widget.whiteboard.BoardRegionStr.na
import io.agora.agoraeducore.core.internal.util.ColorUtil
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducontext.EduBoardRoomPhase
import io.agora.agoraeducontext.WhiteboardApplianceType
import io.agora.agoraeducore.core.context.WhiteboardContext
import io.agora.agoraeducontext.WhiteboardDrawingConfig
import io.agora.agoraeducore.core.internal.edu.classroom.widget.whiteboard.AudioMixerBridgeImpl
import io.agora.agoraeducore.core.internal.edu.classroom.widget.whiteboard.WhiteBoardAudioMixingBridgeListener
import io.agora.agoraeducore.core.internal.education.impl.Constants.Companion.AgoraLog
import io.agora.agoraeducore.core.internal.launch.AgoraEduSDK.DYNAMIC_URL
import io.agora.agoraeducore.core.internal.launch.AgoraEduSDK.DYNAMIC_URL1
import io.agora.agoraeducore.core.internal.report.ReportManager
import io.agora.agoraeducore.core.internal.whiteboard.netless.bean.AgoraBoardFitMode
import io.agora.agoraeducore.core.internal.whiteboard.netless.listener.BoardEventListener
import io.agora.agoraeducore.core.internal.whiteboard.netless.listener.GlobalStateChangeListener
import io.agora.agoraeducore.core.internal.whiteboard.netless.manager.BoardProxy
import org.json.JSONObject
import wendu.dsbridge.DWebView
import java.io.File
import io.agora.agoraeducore.R
import io.agora.agoraeducore.core.internal.launch.AgoraEduRoleType

@SuppressLint("ClickableViewAccessibility")
internal class WhiteBoardManager(
        val context: Context,
        private val launchConfig: AgoraEduCoreConfig,
        private val whiteBoardViewContainer: ViewGroup,
        private val whiteboardContext: WhiteboardContext) : CommonCallbacks, BoardEventListener,
        GlobalStateChangeListener, BoardPreloadEventListener, WhiteBoardAudioMixingBridgeListener {
    private val tag = "WhiteBoardManager"

    private lateinit var whiteBoardAppId: String
    private lateinit var whiteSdk: WhiteSdk
    private var whiteBoardView: WhiteboardView = WhiteboardView(context)
    private val boardProxy = BoardProxy()
    private var curLocalUuid: String? = null
    private var curLocalToken: String? = null
    private var localUserUuid: String? = null
    private val miniScale = 0.1
    private val maxScale = 10.0
    private val scaleStepper = 0.1
    private var curBoardState: BoardState? = null
    private var curGranted: Boolean = false
    private var curGrantedUsers = mutableListOf<String>()
    private var followTips = false
    private var curFollowState = false
    var whiteBoardManagerEventListener: WhiteBoardManagerEventListener? = null

    // this's not a real-time value
    private var curSceneState: SceneState? = null
    private var boardPreloadManager: BoardPreloadManager? = null
    private var courseware: AgoraEduCourseware? = null
    private val defaultCoursewareName = "init"
    private var scenePpts: Array<Scene?>? = null

    //        private var loadPreviewPpt: Boolean = true
    private var loadPreviewPpt: Boolean = false
    private var lastSceneDir: String? = null
    private var inputTips = false
    private var transform = false
    private val webViewClient = object : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            val host = request?.url?.host
            host?.let {
                boardPreloadManager?.let {
                    val response = boardPreloadManager!!.checkCache(request)
                    response?.let {
                        AgoraLog.i("$tag:blocked link:${request?.url.toString()}")
                        AgoraLog.i("$tag:response is not null")
                        return response
                    }
                }
            }
            return super.shouldInterceptRequest(view, request)
        }
    }
    private val webChromeClient = object : WebChromeClient() {
        override fun onConsoleMessage(message: String?, lineNumber: Int, sourceId: String?) {
            super.onConsoleMessage(message, lineNumber, sourceId)
            AgoraLog.d("$tag:console[$message, sourceID->($sourceId:$lineNumber)]")
        }

        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            AgoraLog.d("$tag:console[${consoleMessage?.message()}, " +
                    "sourceID->(${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})]")
            return super.onConsoleMessage(consoleMessage)
        }
    }
    private val curDrawingConfig = WhiteboardDrawingConfig()
    private val sceneCameraConfigs: MutableMap<String, CameraState> = mutableMapOf()

    @SuppressLint("ClickableViewAccessibility")
    private val onTouchListener = View.OnTouchListener { v, event ->
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            whiteBoardView.requestFocus()
            if (boardProxy.isDisableCameraTransform && !boardProxy.isDisableDeviceInputs) {
                ToastManager.showShort(R.string.follow_tips)
                return@OnTouchListener true
            }
        }
        return@OnTouchListener false
    }

    init {
        DWebView.setWebContentsDebuggingEnabled(true)
        whiteBoardView.setBackgroundColor(0)
        whiteBoardView.settings.allowFileAccessFromFileURLs = true
        whiteBoardView.webViewClient = webViewClient
        whiteBoardView.webChromeClient = webChromeClient
        whiteBoardView.setOnTouchListener(onTouchListener)
        whiteboardContext.getHandlers()?.forEach {
            it.onDrawingEnabled(!boardProxy.isDisableDeviceInputs)
            it.onPagingEnabled(!boardProxy.isDisableDeviceInputs)
        }
        val layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        whiteBoardView.layoutParams = layoutParams
        whiteBoardViewContainer.addView(whiteBoardView)
        boardProxy.setListener(this)
        // if userRole is not teacher, default not permission to write.
        if (launchConfig.roleType != AgoraEduRoleType.AgoraEduRoleTypeTeacher.value) {
            boardProxy.setWritable(false)
        }
    }

    fun initData(roomUuid: String, whiteBoardAppId: String, region: String?, courseware: AgoraEduCourseware?) {
        AgoraLog.i("$tag:initWithAppId--roomUuid:$roomUuid, whiteBoardAppId:$whiteBoardAppId, " +
                "region:$region, courseware:${Gson().toJson(courseware)}")
        this.whiteBoardAppId = whiteBoardAppId
        WhiteDisplayerState.setCustomGlobalStateClass(BoardState::class.java)
        val configuration = WhiteSdkConfiguration(whiteBoardAppId, true)
        configuration.isEnableIFramePlugin = true
        configuration.isUserCursor = true
        configuration.region = region(region)
        AgoraLog.i("$tag:newWhiteSdk---0")
        whiteSdk = WhiteSdk(whiteBoardView, context, configuration, this,
                AudioMixerBridgeImpl(this))
        AgoraLog.i("$tag:newWhiteSdk---1")
        boardProxy.setListener(this)
        boardPreloadManager = BoardPreloadManager(context, roomUuid)
        boardPreloadManager?.listener = this
        /**Data type conversion */
        courseware?.let { ware ->
            AgoraLog.i("$tag:default courseware is not null")
            this.courseware = courseware
            ware.scenes?.let {
                scenePpts = arrayOfNulls(it.size)
                for (i in it.indices) {
                    var pptPage: PptPage? = null
                    val element = it[i]
                    val ppt = element.ppt
                    if (ppt != null) {
                        pptPage = PptPage(ppt.src, ppt.width, ppt.height)
                    }
                    val scene = Scene(element.name, pptPage)
                    scenePpts!![i] = scene
                }
            }
        }
    }

    fun initBoardWithRoomToken(uuid: String?, boardToken: String?, localUserUuid: String?) {
        if (TextUtils.isEmpty(uuid) || TextUtils.isEmpty(boardToken)) {
            return
        }
        curLocalUuid = uuid
        curLocalToken = boardToken
        this.localUserUuid = localUserUuid
        whiteBoardViewContainer.post {
            boardProxy.getRoomPhase(object : Promise<RoomPhase> {
                override fun then(phase: RoomPhase) {
                    Constants.AgoraLog.e(tag + ":then->" + phase.name)
                    if (phase != RoomPhase.connected) {
                        whiteboardContext.getHandlers()?.forEach {
                            it.onBoardPhaseChanged(EduBoardRoomPhase.disconnected)
                        }
                        val params = RoomParams(uuid, boardToken)
                        params.cameraBound = CameraBound(miniScale, maxScale)
                        params.isDisableNewPencil = false
                        // if userRole is not teacher, default not permission to write.
                        params.isWritable = launchConfig.roleType == AgoraEduRoleTypeTeacher.value
                        boardProxy.init(whiteSdk, params)
                        ReportManager.getAPaasReporter().reportWhiteBoardStart()
                    }
                }

                override fun catchEx(t: SDKError) {
                    Constants.AgoraLog.e(tag + ":catchEx->" + t.message)
                    ToastManager.showShort(t.message!!)
                }
            })
        }
    }

    fun changeMixingState(state: Int, errorCode: Int) {
        whiteSdk.audioMixerImplement?.setMediaState(state.toLong(), errorCode.toLong())
    }

    private fun disableDeviceInputs(disabled: Boolean) {
        val a = boardProxy.isDisableDeviceInputs
        if (disabled != a) {
            if (!inputTips) {
                inputTips = true
            } else {
//                ToastManager.showShort(if (disabled) R.string.revoke_board else R.string.authorize_board)
            }
        }
        whiteboardContext.getHandlers()?.forEach {
            it.onDrawingEnabled(!disabled)
            it.onPagingEnabled(!disabled)
        }
        boardProxy.disableDeviceInputs(disabled)
        // 未授权时禁止点击h5的翻页按钮/授权时可以点击
        whiteBoardView.evaluateJavascript("javascript: (function() {\n" +
                "    room.getInvisiblePlugin(\"IframeBridge\") && room.getInvisiblePlugin(\"IframeBridge\").computedZindex();\n" +
                "    room.getInvisiblePlugin(\"IframeBridge\") && room.getInvisiblePlugin(\"IframeBridge\").updateStyle();\n" +
                "    console.log(\"getInvisiblePlugin-computedZindex-updateStyle\");\n" +
                "})();")
    }

    private fun disableCameraTransform(disabled: Boolean) {
        val a = boardProxy.isDisableCameraTransform
        if (disabled != a) {
            if (disabled) {
                if (!transform) {
                    transform = true
                } else {
//                    ToastManager.showShort(R.string.follow_tips);
                }
                boardProxy.disableDeviceInputsTemporary(true)
            } else {
                boardProxy.disableDeviceInputsTemporary(boardProxy.isDisableDeviceInputs)
            }
        }
        whiteboardContext.getHandlers()?.forEach {
            it.onZoomEnabled(!disabled, !disabled)
        }
        boardProxy.disableCameraTransform(disabled)
    }

    fun isGranted(userUuid: String): Boolean {
        return curBoardState?.isGranted(userUuid) ?: false
    }

    fun getCurScenePath(): String? {
        return curSceneState?.scenePath
    }

    fun getCurSceneId(): String? {
        val sceneNames = curSceneState?.scenes?.map { it.name }
        return if (sceneNames?.contains(defaultCoursewareName) == true) {
            defaultCoursewareName
        } else {
            val tmp = curSceneState?.scenePath?.split("/")
            tmp?.get(1)
        }
    }

    fun releaseBoard() {
        Constants.AgoraLog.e("$tag:releaseBoard")
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

        cancelCurPreload()

        val now = System.currentTimeMillis()
        whiteboardContext.getHandlers()?.forEach {
            it.onWhiteboardLeft(launchConfig.boardId, now)
        }
    }

    private fun cancelCurPreload() {
        boardPreloadManager?.cancelPreload()
    }

    private fun cancelCurPreloadBySwitchScene() {
        cancelCurPreload()
        whiteboardContext.getHandlers()?.forEach {
            it.onDownloadCanceled("")
        }
    }

    private fun region(region: String?): Region? {
        return when (region) {
            cn -> {
                Region.cn
            }
            na -> {
                Region.us
            }
            eu -> {
                Region.gb_lon
            }
            ap -> {
                Region.sg
            }
            else -> {
                Region.cn
            }
        }
    }

    private fun applianceConvert(type: WhiteboardApplianceType): String {
        return when (type) {
            WhiteboardApplianceType.Select -> {
                SELECTOR
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
        }
    }

    private fun applianceConvert(appliance: String): WhiteboardApplianceType {
        return when (appliance) {
            Appliance.CLICKER -> {
                WhiteboardApplianceType.Clicker
            }
            SELECTOR -> {
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
            else -> {
                WhiteboardApplianceType.Clicker
            }
        }
    }

    private fun initWhiteBoardAppliance() {
        if (!TextUtils.isEmpty(boardProxy.appliance)) {
            onApplianceSelected(curDrawingConfig.activeAppliance)
        } else {
            onApplianceSelected(WhiteboardApplianceType.Clicker)
        }
        if (boardProxy.strokeColor != null) {
            onColorSelected(curDrawingConfig.color)
        } else {
            onColorSelected(context.resources.getColor(R.color.agora_board_default_stroke))
        }
        if (boardProxy.textSize != null) {
            onFontSizeSelected(curDrawingConfig.fontSize)
        } else {
            onFontSizeSelected(context.resources.getInteger(R.integer.agora_board_default_font_size))
        }
        if (boardProxy.strokeWidth != null) {
            onThicknessSelected(curDrawingConfig.thick)
        } else {
            onThicknessSelected(context.resources.getInteger(R.integer.agora_board_default_thickness))
        }
    }

    private fun downloadCourseware(state: SceneState?) {
        var curDownloadUrl: String
        val resourceUuid = state!!.scenePath.split(File.separator.toRegex()).toTypedArray()[1]
        if (resourceUuid == defaultCoursewareName) {
            cancelCurPreloadBySwitchScene()
        } else if (resourceUuid == courseware?.resourceUuid) {
            cancelCurPreloadBySwitchScene()
            /**Open the download(the download module will check whether it exists locally)*/
            courseware?.resourceUrl?.let {
                curDownloadUrl = it
                boardPreloadManager?.preload(curDownloadUrl)
            }
        } else if (curBoardState != null && curBoardState!!.materialList != null) {
            for (taskInfo in curBoardState!!.materialList) {
                if (taskInfo.resourceUuid == resourceUuid && !TextUtils.isEmpty(resourceUuid)
                        && !TextUtils.isEmpty(taskInfo.taskUuid) && taskInfo.ext == BoardExt.pptx) {
                    Constants.AgoraLog.e("$tag:Start to download the courseware set by the teacher0")
                    cancelCurPreloadBySwitchScene()
                    /**Open the download(the download module will check whether it exists locally)*/
                    curDownloadUrl = buildDownloadUrl(state, taskInfo.taskUuid)
                    Constants.AgoraLog.e("$tag:Start to download the courseware set by the teacher1")
                    boardPreloadManager?.preload(curDownloadUrl)
                }
            }
        }
    }

    private fun buildDownloadUrl(state: SceneState?, taskUuid: String?): String {
        state?.let {
            val srcUri = Uri.parse(it.scenes[0].ppt.src)
            return if (srcUri.host.isNullOrEmpty()) DYNAMIC_URL else String.format(DYNAMIC_URL1,
                    srcUri.host, taskUuid)
        }
        return DYNAMIC_URL
    }

    private fun scaleToFit() {
        if (launchConfig.boardFitMode == AgoraBoardFitMode.Auto) {
            boardProxy.scalePptToFit()
        } else {
            recoveryCameraState2(curBoardState)
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

    private fun recoveryCameraState2(state: BoardState?) {
        if (launchConfig.boardFitMode != AgoraBoardFitMode.Retain) {
            return
        }
        if (state?.isGranted(localUserUuid) == true) {
            recoveryCameraState()
        } else {
            boardProxy.follow(true)
        }
    }

    private fun grantChanted(latestBoardState: BoardState): Boolean {
        return curBoardState?.isGranted(localUserUuid) != latestBoardState.isGranted(localUserUuid)
    }

    /** CommonCallbacks */
    override fun throwError(args: Any?) {
        AgoraLog.e("$tag:throwError->${Gson().toJson(args)}")
    }

    override fun urlInterrupter(sourceUrl: String?): String? {
        return null
    }

    override fun onPPTMediaPlay() {
    }

    override fun onPPTMediaPause() {
    }

    override fun onMessage(`object`: JSONObject?) {
    }

    override fun sdkSetupFail(error: SDKError?) {
        AgoraLog.e("$tag:sdkSetupFail->${error?.jsStack}")
    }

    override fun onLogger(`object`: JSONObject?) {
        super.onLogger(`object`)
        AgoraLog.i("$tag:onLogger->${`object`?.toString()}")
    }

    /** BoardEventListener */
    override fun onJoinSuccess(state: GlobalState) {
        Constants.AgoraLog.e(tag + ":onJoinSuccess->" + Gson().toJson(state))
        onGlobalStateChanged(state)

        val config = WhiteboardDrawingConfig()
        config.activeAppliance = applianceConvert(SELECTOR)
        config.color = ColorUtil.converRgbToArgb(boardProxy.strokeColor)
        config.fontSize = boardProxy.textSize.toInt()
        config.thick = boardProxy.strokeWidth.toInt()
        whiteBoardManagerEventListener?.onWhiteBoardJoinSuccess(config)
        whiteboardContext.getHandlers()?.forEach {
            it.onDrawingConfig(config)
        }
    }

    override fun onJoinFail(error: SDKError?) {
        whiteBoardManagerEventListener?.onWhiteBoardJoinFail(error)
        whiteboardContext.getHandlers()?.forEach {
            it.onBoardPhaseChanged(EduBoardRoomPhase.disconnected)
        }
    }

    override fun onRoomPhaseChanged(phase: RoomPhase) {
        Constants.AgoraLog.i(tag + ":onRoomPhaseChanged->" + phase!!.name)
        AgoraLog.i("$tag:whiteboard initialization completed")
        if (phase == RoomPhase.connected) {
            initWhiteBoardAppliance()
        }
        whiteboardContext.getHandlers()?.forEach {
            it.onBoardPhaseChanged(EduBoardRoomPhase.convert(phase.name))
        }
    }

    override fun onSceneStateChanged(state: SceneState) {
        AgoraLog.e("$tag:onSceneStateChanged->${Gson().toJson(state)}")
        var download = false
        state.let {
            curSceneState = state
            whiteBoardManagerEventListener?.onSceneChanged(it)
            val index: Int = curSceneState!!.scenePath.lastIndexOf(File.separator)
            val dir: String = curSceneState!!.scenePath.substring(0, index)
            if (TextUtils.isEmpty(lastSceneDir)) {
                lastSceneDir = dir
                download = true
            } else if (!lastSceneDir.equals(dir)) {
                lastSceneDir = dir
                download = true
            }
        }
        if (download) {
            downloadCourseware(state)
        }

        // if granted, try recovery cameraConfig
        recoveryCameraState2(curBoardState)
        whiteboardContext.getHandlers()?.forEach { handler ->
            state.let { state ->
                handler.onPageNo(state.index, state.scenes.size)
            }
        }
    }

    override fun onMemberStateChanged(state: MemberState) {
        Constants.AgoraLog.e("$tag:onMemberStateChanged->${Gson().toJson(state)}")
        whiteboardContext.getHandlers()?.forEach { handler ->
            state?.let { state ->
            }
        }
    }

    override fun onCameraStateChanged(state: CameraState) {
        AgoraLog.i("$tag:onCameraStateChanged:${Gson().toJson(state)}")
        if (curBoardState?.isGranted(localUserUuid) == true) {
            getCurSceneId()?.let {
                sceneCameraConfigs[it] = state
            }
        }
    }

    override fun onDisconnectWithError(e: Exception?) {
        AgoraLog.e("$tag:onDisconnectWithError->${e?.printStackTrace()}")
        initBoardWithRoomToken(curLocalUuid, curLocalToken, localUserUuid)
    }

    /** GlobalStateChangeListener */
    override fun onGlobalStateChanged(state: GlobalState) {
        state.let {
            val latestBoardState = state as BoardState
            when {
                curBoardState == null -> {
                    // when init, only sync data
                    val isFullScreen = latestBoardState.isFullScreen
                    whiteboardContext.getHandlers()?.forEach {
                        it.onFullScreenChanged(isFullScreen)
                        it.onFullScreenEnabled(!isFullScreen)
                    }
                }
                latestBoardState.isFullScreen == curBoardState?.isFullScreen -> {
                    // is not init and no anything changed
                }
                else -> {
                    // when board`s size changed, scalePptToFit
                    scaleToFit()
                    val isFullScreen = latestBoardState.isFullScreen
                    whiteboardContext.getHandlers()?.forEach {
                        it.onFullScreenChanged(isFullScreen)
                        it.onFullScreenEnabled(!isFullScreen)
                    }
                }
            }
            if (grantChanted(latestBoardState)) {
                boardProxy.follow(!latestBoardState.isGranted(localUserUuid))
                // if granted，try recovery cameraConfig
                recoveryCameraState2(state)
            }
            curBoardState = state
            if (!curBoardState!!.isTeacherFirstLogin && courseware != null && scenePpts != null
                    && loadPreviewPpt) {
                loadPreviewPpt = false
                boardProxy.putScenes(File.separator + courseware?.resourceUuid, scenePpts!!, 0)
                boardProxy.setScenePath(File.separator + courseware?.resourceUuid + File.separator
                        + scenePpts!![0]!!.name, object : Promise<Boolean> {
                    override fun then(t: Boolean) {
                        AgoraLog.e("$tag:setScenePath->$t")
                        if (t) {
                            // boardProxy.scalePptToFit()
                            boardProxy.scalePptToFit()
                        }
                    }

                    override fun catchEx(t: SDKError?) {
                        Constants.AgoraLog.e("$tag:catchEx->${t?.message}")
                    }
                })
            }
            if (curBoardState != null) {
                val granted = curBoardState!!.isGranted(localUserUuid)
                // set local device input switch
                disableDeviceInputs(!granted)
                if (granted != curGranted) {
                    // granted changed
                    curGranted = granted
                    whiteboardContext.getHandlers()?.forEach {
                        it.onPermissionGranted(granted)
                    }
                    // set writeable follow granted
                    boardProxy.setWritable(granted)
                }
                val follow = curBoardState!!.isFollow
                if (followTips) {
                    if (curFollowState != follow) {
                        curFollowState = follow
                        // ToastManager.showShort(follow ? R.string.open_follow_board : R.string.relieve_follow_board);
                    }
                } else {
                    followTips = true
                    curFollowState = follow
                }
                disableCameraTransform(!granted)
                val grantedUsers = curBoardState!!.grantUsers
                if (curGrantedUsers != grantedUsers) {
                    curGrantedUsers.clear()
                    curGrantedUsers.addAll(grantedUsers)
                    whiteBoardManagerEventListener?.onGrantedChanged()
                }
            }
        }
    }

    /** BoardPreloadManager */
    override fun onBoardResourceStartDownload(url: String) {
        AgoraLog.i("$tag:onBoardResourceStartDownload->url:$url")
    }

    override fun onBoardResourceProgress(url: String, progress: Double) {
        AgoraLog.i("$tag:onBoardResourceProgress->url:$url, progress:$progress")
        whiteboardContext.getHandlers()?.forEach {
            it.onDownloadProgress(url, progress.toFloat())
        }
    }

    override fun onBoardResourceLoadTimeout(url: String) {
        AgoraLog.e("$tag:onBoardResourceLoadTimeout->url:$url")
        whiteboardContext.getHandlers()?.forEach {
            it.onDownloadTimeout(url)
        }
    }

    override fun onBoardResourceReady(url: String) {
        AgoraLog.i("$tag:onBoardResourceReady->url:$url")
        whiteboardContext.getHandlers()?.forEach {
            it.onDownloadCompleted(url)
        }
    }

    override fun onBoardResourceLoadFailed(url: String) {
        AgoraLog.e("$tag:onBoardResourceLoadFailed->url:$url")
        whiteboardContext.getHandlers()?.forEach {
            it.onDownloadError(url)
        }
    }

    fun onApplianceSelected(type: WhiteboardApplianceType) {
        curDrawingConfig.activeAppliance = type
        boardProxy.appliance = applianceConvert(type)
    }

    fun onColorSelected(color: Int) {
        curDrawingConfig.color = color
        val rgb = ColorUtil.colorToArray(color)
        boardProxy.strokeColor = rgb
    }

    fun onFontSizeSelected(size: Int) {
        curDrawingConfig.fontSize = size
        boardProxy.textSize = size.toDouble()
    }

    fun onThicknessSelected(thick: Int) {
        curDrawingConfig.thick = thick
        boardProxy.strokeWidth = thick.toDouble()
    }

    fun onBoardInputEnabled(enabled: Boolean) {
        val granted = isGranted(launchConfig.userUuid)
        whiteboardContext.getHandlers()?.forEach {
            it.onDrawingEnabled(granted && enabled)
            it.onInteractionEnabled(enabled)
        }
    }

    fun onDownloadSkipped(url: String?) {
        boardPreloadManager?.cancelPreload()
    }

    fun onDownloadCanceled(url: String?) {
        boardPreloadManager?.cancelPreload()
    }

    fun onDownloadRetry(url: String?) {
        boardPreloadManager?.let {
            url?.let { v -> it.preload(v) }
        }
    }

    fun onBoardFullScreen(full: Boolean) {
        AgoraLog.i("$tag:onFullScreen->$full")
        // when board`s size changed, scalePptToFit
//                boardProxy.scalePptToFit()
        scaleToFit()
        whiteboardContext.getHandlers()?.forEach {
            it.onFullScreenChanged(full)
        }
    }

    fun onBoardZoomOut() {
        AgoraLog.i("$tag:onZoomOut")
        var curScale = boardProxy.zoomScale
        curScale -= scaleStepper
        if (curScale in miniScale..maxScale) {
            boardProxy.zoom(curScale)
        }
    }

    fun onBoardZoomIn() {
        AgoraLog.i("$tag:onZoomIn")
        var curScale = boardProxy.zoomScale
        curScale += scaleStepper
        if (curScale in miniScale..maxScale) {
            boardProxy.zoom(curScale)
        }
    }

    fun onBoardPrevPage() {
        AgoraLog.i("$tag:onPrevPage")
        boardProxy.pptPreviousStep()
    }

    fun onBoardNextPage() {
        AgoraLog.i("$tag:onNextPage")
        boardProxy.pptNextStep()
    }

    // WhiteBoardAudioMixingBridgeListener
    override fun onStartAudioMixing(filepath: String, loopback: Boolean, replace: Boolean, cycle: Int) {
        whiteBoardManagerEventListener?.onStartAudioMixing(filepath, loopback, replace, cycle)
    }

    override fun onStopAudioMixing() {
        whiteBoardManagerEventListener?.onStopAudioMixing()
    }

    override fun onSetAudioMixingPosition(position: Int) {
        whiteBoardManagerEventListener?.onSetAudioMixingPosition(position)
    }
}

interface WhiteBoardManagerEventListener {
    fun onWhiteBoardJoinSuccess(config: WhiteboardDrawingConfig)

    fun onWhiteBoardJoinFail(error: SDKError?)

    fun onSceneChanged(state: SceneState)

    fun onGrantedChanged()

    fun onStartAudioMixing(filepath: String, loopback: Boolean, replace: Boolean, cycle: Int)

    fun onStopAudioMixing()

    fun onSetAudioMixingPosition(position: Int)
}