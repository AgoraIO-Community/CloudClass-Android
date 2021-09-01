package io.agora.edu.classroom

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import com.google.gson.Gson
import com.herewhite.sdk.*
import com.herewhite.sdk.domain.*
import com.herewhite.sdk.domain.Appliance.SELECTOR
import io.agora.base.ToastManager
import io.agora.edu.R
import io.agora.edu.classroom.widget.whiteboard.BoardPreloadEventListener
import io.agora.edu.classroom.widget.whiteboard.BoardPreloadManager
import io.agora.edu.common.bean.board.BoardExt
import io.agora.edu.common.bean.board.BoardState
import io.agora.edu.launch.AgoraEduCourseware
import io.agora.edu.launch.AgoraEduLaunchConfig
import io.agora.edu.launch.AgoraEduRegionStr.ap
import io.agora.edu.launch.AgoraEduRegionStr.cn
import io.agora.edu.launch.AgoraEduRegionStr.eu
import io.agora.edu.launch.AgoraEduRegionStr.na
import io.agora.edu.launch.AgoraEduSDK
import io.agora.edu.util.ColorUtil
import io.agora.education.impl.Constants
import io.agora.education.impl.Constants.Companion.AgoraLog
import io.agora.educontext.EduBoardRoomPhase
import io.agora.educontext.WhiteboardApplianceType
import io.agora.educontext.WhiteboardDrawingConfig
import io.agora.educontext.context.WhiteboardContext
import io.agora.extapp.ExtAppTrackListener
import io.agora.extension.ExtAppPosition
import io.agora.report.ReportManager
import io.agora.whiteboard.netless.bean.AgoraBoardFitMode
import io.agora.whiteboard.netless.listener.BoardEventListener
import io.agora.whiteboard.netless.listener.GlobalStateChangeListener
import io.agora.whiteboard.netless.manager.BoardProxy
import org.json.JSONObject
import wendu.dsbridge.DWebView
import java.io.File
import java.util.*

@SuppressLint("ClickableViewAccessibility")
class WhiteBoardManager(
        val context: Context,
        val launchConfig: AgoraEduLaunchConfig,
        private val whiteBoardViewContainer: ViewGroup,
        private val whiteboardContext: WhiteboardContext) : CommonCallbacks, BoardEventListener,
        GlobalStateChangeListener, BoardPreloadEventListener {
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
    var extAppTrackListener: ExtAppTrackListener? = null

    // this is not a real-time value
    private var curSceneState: SceneState? = null
    private var boardPreloadManager: BoardPreloadManager? = null
    private var courseware: AgoraEduCourseware? = null
    private val defaultCoursewareName = "init"
    private var scenePpts: Array<Scene?>? = null

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
                        Log.e(tag, "blocked link:${request?.url.toString()}")
                        Log.e(tag, "response is not null")
                        return response
                    }
                }
            }
            return super.shouldInterceptRequest(view, request)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            view?.let {
                BoardStyleInjector.injectBoardStyles(it)
            }
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
    }

    fun initData(roomUuid: String, whiteBoardAppId: String, region: String?, courseware: AgoraEduCourseware?) {
        Log.e(tag, "initWithAppId")
        this.whiteBoardAppId = whiteBoardAppId
        WhiteDisplayerState.setCustomGlobalStateClass(BoardState::class.java)
        val configuration = WhiteSdkConfiguration(whiteBoardAppId, true)
        configuration.isEnableIFramePlugin = true
        configuration.isUserCursor = true
        configuration.region = region(region)
        Log.e(tag, "newWhiteSdk---0")
        whiteSdk = WhiteSdk(whiteBoardView, context, configuration, this)
        Log.e(tag, "newWhiteSdk---1")
        boardProxy.setListener(this)
        boardPreloadManager = BoardPreloadManager(context, roomUuid)
        boardPreloadManager?.listener = this
        /**Data type conversion */
        courseware?.let { ware ->
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
                    AgoraLog.e(tag + ":then->" + phase.name)
                    if (phase != RoomPhase.connected) {
                        whiteboardContext.getHandlers()?.forEach {
                            it.onBoardPhaseChanged(EduBoardRoomPhase.disconnected)
                        }
                        val params = RoomParams(uuid, boardToken)
                        params.cameraBound = CameraBound(miniScale, maxScale)
                        params.isDisableNewPencil = false
                        params.useMultiViews = true

                        val collectorStyleMap = HashMap<String, String>()
                        collectorStyleMap["position"] = "fixed"
                        collectorStyleMap["left"] = "60px"
                        collectorStyleMap["bottom"] = "12px"

                        val ratio = whiteBoardViewContainer.height /
                                whiteBoardViewContainer.width.toFloat()
                        params.windowParams = WindowParams()
                        params.windowParams
                            .setCollectorStyles(collectorStyleMap)
                            .setChessboard(false)
                            .setContainerSizeRatio(ratio)

                        boardProxy.init(whiteSdk, params)
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
        AgoraLog.e("$tag:releaseBoard")
        boardProxy.disconnect()
        whiteBoardView.removeAllViews()
        whiteBoardView.destroy()
        cancelCurPreload()
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
                    AgoraLog.e("$tag:Start to download the courseware set by the teacher0")
                    cancelCurPreloadBySwitchScene()
                    /**Open the download(the download module will check whether it exists locally)*/
                    curDownloadUrl = String.format(AgoraEduSDK.DYNAMIC_URL, taskInfo.taskUuid)
                    AgoraLog.e("$tag:Start to download the courseware set by the teacher1")
                    boardPreloadManager?.preload(curDownloadUrl)
                }
            }
        }
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
        Log.e(tag, "throwError->${Gson().toJson(args)}")
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
        Log.e(tag, "sdkSetupFail->${error?.jsStack}")
    }

    /** BoardEventListener */
    override fun onJoinSuccess(state: GlobalState) {
        AgoraLog.e(tag + ":onJoinSuccess->" + Gson().toJson(state))
        onGlobalStateChanged(state)

        // set default config
//        initWhiteBoardAppliance()
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
        AgoraLog.e(tag + ":onRoomPhaseChanged->" + phase!!.name)
        Log.e(tag, "whiteboard initialization completed")
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
        AgoraLog.e("$tag:onMemberStateChanged->${Gson().toJson(state)}")
        whiteboardContext.getHandlers()?.forEach { handler ->
            state?.let { state ->
            }
        }
    }

    override fun onCameraStateChanged(state: CameraState) {
        AgoraLog.i("$tag->onCameraStateChanged:${Gson().toJson(state)}")
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

            // Check if user defined properties have changed.
            // User defined properties are internal concepts, managed
            // as part of the entire whiteboard global state.
            // However, they are translated to "GlobalState"
            // of whiteboard in the current room.
            if (!state.userDefinedPropertyEquals(curBoardState)) {
                whiteboardContext.getHandlers()?.forEach {
                    it.onWhiteboardGlobalStateChanged(state.flexBoardState)
                }
            }

            // whether ext app track states update
            if (!state.extAppTracksEquals(curBoardState)) {
                extAppTrackListener?.onExtAppTrackUpdated(state.extAppMovements)
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
                disableDeviceInputs(!granted)
                if (granted != curGranted) {
                    curGranted = granted
                    whiteboardContext.getHandlers()?.forEach {
                        it.onPermissionGranted(granted)
                    }
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
                disableDeviceInputs(!granted)
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
        Log.e(tag, "onBoardResourceStartDownload")
    }

    override fun onBoardResourceProgress(url: String, progress: Double) {
        Log.e(tag, "onBoardResourceProgress->$progress")
        whiteboardContext.getHandlers()?.forEach {
            it.onDownloadProgress(url, progress.toFloat())
        }
    }

    override fun onBoardResourceLoadTimeout(url: String) {
        Log.e(tag, "onBoardResourceLoadTimeout")
        whiteboardContext.getHandlers()?.forEach {
            it.onDownloadTimeout(url)
        }
    }

    override fun onBoardResourceReady(url: String) {
        Log.e(tag, "onBoardResourceReady")
        whiteboardContext.getHandlers()?.forEach {
            it.onDownloadCompleted(url)
        }
    }

    override fun onBoardResourceLoadFailed(url: String) {
        Log.e(tag, "onBoardResourceLoadFailed")
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
        Log.e(tag, "onFullScreen->$full")
        // when board`s size changed, scalePptToFit
        scaleToFit()
        whiteboardContext.getHandlers()?.forEach {
            it.onFullScreenChanged(full)
        }
    }

    fun onBoardZoomOut() {
        Log.e(tag, "onZoomOut")
        var curScale = boardProxy.zoomScale
        curScale -= scaleStepper
        if (curScale in miniScale..maxScale) {
            boardProxy.zoom(curScale)
        }
    }

    fun onBoardZoomIn() {
        Log.e(tag, "onZoomIn")
        var curScale = boardProxy.zoomScale
        curScale += scaleStepper
        if (curScale in miniScale..maxScale) {
            boardProxy.zoom(curScale)
        }
    }

    fun onBoardPrevPage() {
        Log.e(tag, "onPrevPage")
        boardProxy.pptPreviousStep()
    }

    fun onBoardNextPage() {
        Log.e(tag, "onNextPage")
        boardProxy.pptNextStep()
    }

    fun getFlexWhiteboardState(): Map<String, Any> {
        return curBoardState?.flexBoardState ?: mapOf()
    }

    fun setFlexWhiteboardState(properties: Map<String, Any>) {
        val state = curBoardState?.copy() ?: BoardState()
        state.flexBoardState = properties
        boardProxy.setGlobalState(state)
    }

    internal fun setExtAppTrackInfo(identifier: String, userId: String, x: Float, y: Float) {
        val state = curBoardState?.copy() ?: BoardState()
        state.setExtAppMovement(identifier, userId, x, y)
        boardProxy.setGlobalState(state)
    }

    fun getExtAppTrack(identifier: String): ExtAppPosition {
        val track = ExtAppPosition()
        curBoardState?.extAppMovements?.get(identifier)?.let {
            track.userId = it.userId
            track.x = it.x
            track.y = it.y
        }

        return track
    }
}

interface WhiteBoardManagerEventListener {
    fun onWhiteBoardJoinSuccess(config: WhiteboardDrawingConfig)

    fun onWhiteBoardJoinFail(error: SDKError?)

    fun onSceneChanged(state: SceneState)

    fun onGrantedChanged()
}

data class BoardStyleParams(
    val left: Int = 0,
    val bottom: Int = 0,
    val styles: List<String>
)

internal object BoardStyleInjector {
    private var left: Int = 0
    private var bottom: Int = 0
    private var styles: MutableList<String> = mutableListOf()

    fun setPosition(left: Int = 0, bottom: Int = 0) {
        this.left = left
        this.bottom = bottom
    }

    fun addStyle(style: String) {
        styles.add(style)
    }

    private const val javascriptFormat =
        "var style = document.createElement('style');\n" +
                "style.innerHTML = %s\n" +
                "document.head.appendChild(style);"

    fun injectBoardStyles(view: WebView) {
        val locale = Locale.getDefault()
        styles.forEach { js ->
            injectBoardStyle(locale, view, js)
        }
    }

    private fun injectBoardStyle(locale: Locale, view: WebView, attr: String) {
        val js = String.format(locale, javascriptFormat, attr)
        view.loadUrl("javascript: $js")
    }
}