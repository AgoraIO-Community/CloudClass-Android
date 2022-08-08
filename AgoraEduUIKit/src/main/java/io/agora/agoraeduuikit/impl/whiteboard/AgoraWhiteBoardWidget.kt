package io.agora.agoraeduuikit.impl.whiteboard

import android.content.Context
import android.text.TextUtils
import android.text.TextUtils.isEmpty
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.agora.edu.component.loading.AgoraLoadingView
import com.agora.edu.component.whiteboard.AgoraEduWhiteBoardControlComponent
import com.google.gson.reflect.TypeToken
import com.herewhite.sdk.WhiteboardView
import com.herewhite.sdk.domain.*
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole.Student
import io.agora.agoraeducore.core.context.EduBoardRoomPhase
import io.agora.agoraeducore.core.context.EduBoardRoomPhase.Companion.convert
import io.agora.agoraeducore.core.context.EduBoardRoomPhase.Disconnected
import io.agora.agoraeducore.core.context.RoomContext
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.education.impl.Constants.AgoraLog
import io.agora.agoraeducore.core.internal.framework.impl.managers.AgoraWidgetManager
import io.agora.agoraeducore.core.internal.framework.impl.managers.AgoraWidgetManager.Companion.grantUser
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.launch.AgoraEduRoleType.AgoraEduRoleTypeStudent
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
import io.agora.agoraeduuikit.impl.whiteboard.netless.listener.SimpleBoardEventListener
import io.agora.agoraeduuikit.impl.whiteboard.netless.manager.BoardRoom
import io.agora.agoraeduuikit.util.FcrColorUtils
import io.agora.agoraeduuikit.whiteboard.FcrBoardRoom
import io.agora.agoraeduuikit.whiteboard.bean.FcrBoardRoomJoinConfig

class AgoraWhiteBoardWidget : AgoraBaseWidget() {
    override val tag = "AgoraWhiteBoard"
    private lateinit var whiteBoardView: WhiteboardView
    private lateinit var loadingView: AgoraLoadingView
    private lateinit var whiteBoardControlView: AgoraEduWhiteBoardControlComponent

    private var whiteBoardAppId: String? = null
    private var region: String? = null
    private var whiteboardUuid: String? = null
    private var curLocalToken: String? = null
    private var aPaaSUserUuid: String? = null
    private var aPaaSUserName: String? = null
    private var curBoardState: BoardState? = null
    private var curGranted: Boolean? = null
    private var curGrantedUsers = mutableListOf<String>()
    private val defaultCoursewareName = "init"
    private var courseware: AgoraEduCourseware? = null
    private var loadPreviewPpt: Boolean = false
    private var curSceneState: SceneState? = null
    private var curScenes: Array<Scene>? = null
    private var courseWareManager: FcrCourseWareManager? = null
    private var boardFitMode = AgoraBoardFitMode.Retain
    private var disconnectErrorHappen = false
    private val boardAppIdKey = "boardAppId"
    private val boardTokenKey = "boardToken"
    private val coursewareInitial = "initial"
    private var firstGrantedTip = true
    private var roomContext: RoomContext? = null
    private var initJoinWhiteBoard = false

    // Default appliance configuration
    private val curDrawingConfig = WhiteboardDrawingConfig()
    private val webChromeClient = object : WebChromeClient() {}
    private var mAppid = ""
    private var whiteBoardImg: FcrWhiteBoardImg? = null
    private var whiteBoardPath: FcrWhiteboardPath? = null
    private var whiteboardRoom: FcrBoardRoom? = null
    private var context: Context? = null

    var isAttributeGot: Boolean = false
    var isNeedShowLoading = true // 是否需要显示加载loading
    var uuid: String? = null

    lateinit var boardProxy: BoardRoom

    private val boardEventListener = object : SimpleBoardEventListener() {
        override fun onJoinSuccess(state: GlobalState) {
            super.onJoinSuccess(state)
            loadingView.visibility = View.GONE
            whiteBoardControlView.initView(boardProxy)
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
            roomContext = null
            broadcastBoardPhaseState(Disconnected)
        }

        override fun onRoomPhaseChanged(phase: RoomPhase) {
            super.onRoomPhaseChanged(phase)
            if (phase == RoomPhase.reconnecting || phase == RoomPhase.connecting) {
                // 连接中
                if (isNeedShowLoading) { // 分组重试中，不要加载框子
                    loadingView.visibility = View.VISIBLE
                }
            } else {
                loadingView.visibility = View.GONE
            }
            if (phase == RoomPhase.connected) {
                restoreWhiteBoardAppliance()
                broadcastDrawingConfig()
            }
            broadcastBoardPhaseState(convert(phase.name))
        }

        override fun onPageStateChanged(state: PageState) {
            super.onPageStateChanged(state)  // 设置页数
            whiteBoardControlView.setWhiteBoardPage(state.index, state.length)
        }

        override fun onDisconnectWithError(e: Exception?) {
            super.onDisconnectWithError(e)
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
                    val layoutParams = whiteBoardControlView.layoutParams as FrameLayout.LayoutParams
                    // maximized: 最大化, minimized: 最小化,normal   : 默认展开
                    if (windowBoxState == "minimized") { // 显示H5 icon
                        val context = whiteBoardControlView.context
                        layoutParams.setMargins(
                            context.resources.getDimensionPixelOffset(R.dimen.agora_wb_tools_margin_left_icon), 0, 0,
                            context.resources.getDimensionPixelOffset(R.dimen.agora_wb_tools_margin_bottom)
                        )
                    } else {
                        val context = whiteBoardControlView.context
                        layoutParams.setMargins(
                            context.resources.getDimensionPixelOffset(R.dimen.agora_wb_tools_margin_left), 0, 0,
                            context.resources.getDimensionPixelOffset(R.dimen.agora_wb_tools_margin_bottom)
                        )
                    }
                    whiteBoardControlView.layoutParams = layoutParams
                }
            }
        }

        override fun onCanUndoStepsUpdate(canUndoSteps: Long) {
            super.onCanUndoStepsUpdate(canUndoSteps)
            Constants.AgoraLog?.i("onCanUndoStepsUpdate uuid=${uuid}")
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
            Constants.AgoraLog?.i("onCanRedoStepsUpdate uuid=${uuid}")
            uuid?.let {
                val list = AgoraWhiteBoardManager.getWhiteBoardList(it)
                if (list != null) {
                    for (listener in list) {
                        listener.onCanRedoStepsUpdate(canRedoSteps)
                    }
                }
            }
        }

        override fun onGlobalStateChanged(state: GlobalState) {
            super.onGlobalStateChanged(state)
            (state as? BoardState)?.let { newState ->
                curBoardState = newState
                loadPrivateCourseWareIfNeeded(newState)
            }
        }

        private fun loadPrivateCourseWareIfNeeded(newState: BoardState) {
            if (!newState.isTeacherFirstLogin && courseware != null && curScenes != null && loadPreviewPpt) {
                loadPreviewPpt = false

                courseWareManager?.loadPrivateCourseWare(object : Promise<Boolean> {
                    override fun then(t: Boolean) {
                        AgoraLog?.e("$tag:loadPrivateCourseWare -> $t")
                        if (t) boardProxy.scalePptToFit()
                    }

                    override fun catchEx(t: SDKError?) {
                        AgoraLog?.e("$tag:catchEx->${t?.message}")
                    }
                })
            }
        }
    }

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

    override fun init(container: ViewGroup) {
        super.init(container)
        context = container.context
        val view = LayoutInflater.from(context).inflate(R.layout.agora_whiteborad_widget, null)
        whiteBoardView = view.findViewById(R.id.agora_whiteboard_view)
        loadingView = view.findViewById(R.id.agora_edu_loading)
        whiteBoardView.webChromeClient = webChromeClient

        val p = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        container.addView(view, p)
        // init whiteBoardControlView and handle startClassBtn visibility
        whiteBoardControlView = view.findViewById(R.id.agora_whiteboard_control)
        // roomContext from AgoraEduWhiteBoardComponent.roomHandler.onJoinRoomSuccess
        roomContext = widgetInfo?.extraInfo as? RoomContext
        roomContext?.let { whiteBoardControlView.setRoomContext(it, widgetInfo?.localUserInfo) }
        whiteBoardPath = FcrWhiteboardPath()
        whiteboardRoom = FcrBoardRoom(whiteBoardView)
        whiteboardRoom?.mixingBridgeListener = {
            sendMessage(GsonUtil.toJson(it))
        }
        boardProxy = whiteboardRoom!!.boardRoom
        boardProxy.setListener(boardEventListener)
        courseWareManager = FcrCourseWareManager(boardProxy)
        whiteBoardImg = FcrWhiteBoardImg(whiteBoardView.context)
        whiteBoardImg?.roomName = widgetInfo?.roomInfo?.roomName

        context?.let {
            whiteBoardView.setBackgroundColor(ContextCompat.getColor(it, R.color.fcr_system_foreground_color))
        }
        joinWhiteBoard()
    }

    fun setWhiteBoardControlView(isShow: Boolean) {
        ContextCompat.getMainExecutor(context).execute {
            if (isShow) {
                whiteBoardControlView.visibility = View.VISIBLE
            } else {
                whiteBoardControlView.visibility = View.GONE
            }
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

        widgetInfo?.extraInfo?.let {
            if (it is MutableMap<*, *> && it.isNotEmpty() && it.keys.contains("fitMode")) {
                boardFitMode = it["fitMode"] as AgoraBoardFitMode
            }
        }

        return !isEmpty(whiteBoardAppId) && !isEmpty(whiteboardUuid) && !isEmpty(curLocalToken) && !isEmpty(aPaaSUserUuid)
    }

    private fun joinWhiteBoard() {
        if (!parseWhiteBoardConfigProperties()) {
            AgoraLog?.e("$tag->WhiteBoardConfigProperties isNullOrEmpty, please check roomProperties.widgets.netlessBoard")
            return
        }
        initJoinWhiteBoard = true

        whiteboardRoom?.init(whiteBoardAppId!!, region)
        whiteBoardPath?.getCoursewareAttribute(mAppid)

        ContextCompat.getMainExecutor(whiteBoardView.context).execute {
            boardProxy.getRoomPhase(object : Promise<RoomPhase> {
                override fun then(phase: RoomPhase) {
                    AgoraLog?.e(tag + ":then->" + phase.name)
                    if (phase != RoomPhase.connected) {
                        broadcastBoardPhaseState(Disconnected)
                        joinWhiteboard(whiteboardUuid!!, curLocalToken!!)
                        ReportManager.getAPaasReporter()?.reportWhiteBoardStart()
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

    private fun getInitialWriteableState(): Boolean {
        try {
            // 避免数据格式不对
            var isWriteable = false
            val extraProperties = this.widgetInfo?.roomProperties as? MutableMap<*, *>
            extraProperties?.let {
                (extraProperties[AgoraWidgetManager.grantUser] as Map<String, Boolean>).forEach {
                    if (it.key == aPaaSUserUuid && it.value) {
                        isWriteable = true
                        return isWriteable
                    }
                }
            }
        } catch (e: Exception) {
        }
        if (widgetInfo?.localUserInfo?.userRole == AgoraEduRoleTypeTeacher.value) {
            return true
        } else if (widgetInfo?.localUserInfo?.userRole == AgoraEduRoleTypeStudent.value
            && roomContext?.getRoomInfo()?.roomType == RoomType.GROUPING_CLASS
        ) {
            return true
        }
        return false
    }

    private fun joinWhiteboard(uuid: String, boardToken: String) {
        AgoraLog?.e("$tag joinWhiteboard-> uuid=$uuid || boardToken=$boardToken")

        loadingView.visibility = View.VISIBLE

        val scaleWhiteBoardMap = HashMap<String, String>().apply {
            this["position"] = "fixed"
            this["left"] = "12px"
            this["bottom"] = "12px"
            this["width"] = "34px"
            this["height"] = "34px"
        }
        context?.let { ContextCompat.getColor(it, R.color.fcr_system_component_color) }
            ?.let { scaleWhiteBoardMap.put("backgroundColor", FcrColorUtils.toHexEncoding(it)) }
        val config = FcrBoardRoomJoinConfig()
        config.collectorStyles = scaleWhiteBoardMap
        config.roomId = uuid
        config.roomToken = boardToken
        config.userId = aPaaSUserUuid
        config.userName = aPaaSUserName
        config.hasOperationPrivilege = getInitialWriteableState()
        container?.let {
            config.boardRatio = it.height.toFloat() / it.width.toFloat()
            AgoraLog?.e("白板比例：${config.boardRatio} （h=${it.height},w=${it.width})")
        }
        whiteboardRoom?.join(config)

        if (config.hasOperationPrivilege) {
            aPaaSUserUuid?.let { // 白板授权，通知其他人
                broadcastPermissionChanged(arrayOf(it).toMutableList())
            }
        }
        whiteBoardControlView.initView(boardProxy)
    }

    // receive msg from outside of widget.(other layout or container, and so on)
    override fun onMessageReceived(message: String) {
        super.onMessageReceived(message)
        val packet: AgoraBoardInteractionPacket? =
            GsonUtil.gson.fromJson(message, AgoraBoardInteractionPacket::class.java)
        packet?.let {
            when (packet.signal) {
                MemberStateChanged -> {
                    (GsonUtil.gson.fromJson(packet.body.toString(), AgoraBoardDrawingMemberState::class.java))?.let {
                        onMemberStateChanged(it)
                    } ?: Runnable {
                        AgoraLog?.e("$tag->${packet.signal}, (MemberStateChanged)packet.body convert failed")
                    }
                }
                BoardGrantDataChanged -> {
                    (GsonUtil.gson.fromJson(packet.body.toString(), AgoraBoardGrantData::class.java))?.let { data ->
                        data.userUuids.forEach {
                            grantBoard(it, data.granted)
                        }
                    }
                        ?: Runnable { AgoraLog?.e("$tag->${packet.signal}, (BoardGrantDataChanged)packet.body convert failed") }
                }
                RTCAudioMixingStateChanged -> {
                    (GsonUtil.gson.fromJson<Pair<Int, Int>>(
                        packet.body.toString(),
                        object : TypeToken<Pair<Int, Int>>() {}.type
                    ))?.let { pair ->
                        boardProxy.changeMixingState(pair.first, pair.second)
                    }
                        ?: Runnable { AgoraLog?.e("$tag->${packet.signal}, (RTCAudioMixingStateChanged)packet.body convert failed") }
                }
                LoadCourseware -> {
                    (GsonUtil.gson.fromJson(packet.body.toString(), AgoraEduCourseware::class.java))?.let {
                        courseWareManager?.loadCourseware(it)
                    } ?: Runnable {
                        AgoraLog?.e("$tag->${packet.signal}, (MemberStateChanged)packet.body convert failed")
                    }
                }
                // 保存白板图片
                BoardImage -> {
                    AgoraLog?.e("$tag:保存白板图片")
                    context?.let {
                        boardProxy.room.backgroundColor = getColor(context, R.color.fcr_system_background_color)
                    }
                    whiteBoardImg?.saveImgToGallery(boardProxy.room)
                }

                BoardRatioChange -> {
                    // 避免白板收缩的时候，还无法准确获取宽高
                    container?.postDelayed({
                        container?.let {
                            val ratio = it.height.toFloat() / it.width.toFloat()
                            boardProxy.setContainerSizeRatio(ratio)
                            AgoraLog?.e("白板比例：${ratio} （h=${it.height},w=${it.width})")
                        }
                    }, 150)
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
            ContextCompat.getMainExecutor(context).execute {
                joinWhiteBoard()
            }
        }
        // 白板授权
        keys.forEach {
            if (it.startsWith(grantUser)) {
                handleGrantChanged(getGrantUsers())
            }
        }
    }

    override fun onWidgetRoomPropertiesDeleted(
        properties: MutableMap<String, Any>,
        cause: MutableMap<String, Any>?,
        keys: MutableList<String>
    ) {
        super.onWidgetRoomPropertiesDeleted(properties, cause, keys)
        keys.forEach {
            if (it.startsWith(grantUser)) {
                handleGrantChanged(getGrantUsers())
            }
        }

        // 取消白板授权
//        if (properties.containsKey(grantUser)) {
//            val grantedUsers = getGrantUsers(properties)
//            if (grantedUsers.contains(aPaaSUserUuid)) {
//                handleGrantChanged(mutableListOf())
//            }
//        }
    }

    private fun handleGrantChanged(grantedUsers: MutableList<String>) {
        val isStudent = widgetInfo?.localUserInfo?.userRole == Student.value
        val granted = if (isStudent) grantedUsers.contains(aPaaSUserUuid) else true
        disableCameraTransform(!granted)
        disableDeviceInputs(!granted)
        setWhiteBoardControlView(granted)

        if (granted != curGranted) {
            curGranted = granted
            // set writeable follow granted if curRole is student
            if (isStudent) {
                AgoraLog?.i("$tag->set writeable follow granted: $granted")
                boardProxy.setWritable(granted, object : Promise<Boolean> {
                    override fun then(t: Boolean?) {
                        AgoraLog?.i("$tag->writeable value: $t")
                        if (t == true && roomContext?.getRoomInfo()?.roomType == RoomType.GROUPING_CLASS) {
                            // 如果是分组内，判断是否带入课件
                            val initial = widgetInfo?.localUserProperties?.get(coursewareInitial) as? Double
                            if (!isAttributeGot && initial == 1.0) {
                                whiteBoardPath?.setCoursewareAttribute(mAppid, boardProxy)
                                isAttributeGot = true
                            }
                        }
                    }

                    override fun catchEx(t: SDKError?) {
                        AgoraLog?.e("$tag->initDrawingConfig-setWritable:${t?.jsStack}")
                    }
                })
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
            }
        }

        if (curGrantedUsers != grantedUsers) {
            curGrantedUsers.clear()
            curGrantedUsers.addAll(grantedUsers)
            broadcastPermissionChanged(grantedUsers)
        }
    }


    private fun getGrantUsers(): MutableList<String> {
        val list = mutableListOf<String>()
        try {

            val usersMap = widgetInfo?.roomProperties?.get(grantUser) as? MutableMap<String, Boolean>
            usersMap?.forEach {
                if (it.value) {
                    list.add(it.key)
                }
            }
        } catch (e: Exception) {
        }

        return list
    }

    override fun release() {
        super.release()
        releaseBoard()
    }

    /**
     * grant user to draw on whiteBoard
     * */
    fun grantBoard(userId: String, granted: Boolean) {
        // 白板的
        /*curBoardState?.let { state ->
            state.grantUser(userId, granted)
            boardProxy.setGlobalState(state)
        }*/

        if (granted) {
            // widgets.netlessBoard.extra.grantedUsers:{userUuid: true | false}
            val map = mutableMapOf<String, Any>()
            //keys：grantedUsers.e10adc3949ba59abbe56e057f20f883e2
            map["$grantUser.${userId}"] = true
            // {"extra":{"grantedUsers.50ed4a9ff3566029759e2c0e4dd5a9922":true}}
            updateRoomProperties(map, mutableMapOf(), null)
        } else {
            val keys = mutableListOf<String>()
            keys.add("$grantUser.${userId}")
            deleteRoomProperties(keys, mutableMapOf(), null)
        }
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
            val writeable = getInitialWriteableState()
            this@AgoraWhiteBoardWidget.boardProxy.setWritable(writeable, object : Promise<Boolean> {
                override fun then(t: Boolean?) {
                    AgoraLog?.i("$tag->writeable value: $t")
                    if (t == true && roomContext?.getRoomInfo()?.roomType == RoomType.GROUPING_CLASS) {
                        // 如果是分组内，判断是否带入课件
                        val initial = widgetInfo?.localUserProperties?.get(coursewareInitial) as? Double
                        if (!isAttributeGot && initial == 1.0) {
                            whiteBoardPath?.setCoursewareAttribute(mAppid, boardProxy)
                            isAttributeGot = true
                        }
                    }
                }

                override fun catchEx(t: SDKError?) {
                    AgoraLog?.e("$tag->initDrawingConfig-setWritable:${t?.jsStack}")
                }
            })
            this@AgoraWhiteBoardWidget.disableDeviceInputs(!writeable)
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
                curDrawingConfig.fontSize = context!!.resources.getInteger(R.integer.agora_board_default_font_size)
                restoreWhiteBoardAppliance()
                broadcastDrawingConfig()
                boardProxy.setWritable(false)
                disableDeviceInputs(true)
                promise.then(Unit)
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
        sendMessage(GsonUtil.gson.toJson(body))
    }

    /**
     * broadcast permissionChanged
     * */
    private fun broadcastPermissionChanged(grantedUsers: MutableList<String>) {
        val data = AgoraBoardGrantData(true, grantedUsers)
        val packet = AgoraBoardInteractionPacket(BoardGrantDataChanged, data)
        sendMessage(GsonUtil.gson.toJson(packet))
    }

    /**
     * broadcaster BoardPhaseState
     * */
    private fun broadcastBoardPhaseState(phase: EduBoardRoomPhase) {
        val body = AgoraBoardInteractionPacket(BoardPhaseChanged, phase)
        sendMessage(GsonUtil.gson.toJson(body))
    }

    fun hideWhiteboardTools() {
        curGrantedUsers.clear()
        val body = AgoraBoardInteractionPacket(BoardGrantDataChanged, curGrantedUsers)
        sendMessage(GsonUtil.gson.toJson(body))
    }

    fun setAppid(appid: String) {
        mAppid = appid
        AgoraLog?.i(tag + ":setAppID>>>>>>>>>>>>>>->" + appid)
    }

    private fun disableDeviceInputs(disabled: Boolean) {
        boardProxy.disableDeviceInputs(disabled)
    }

    private fun disableCameraTransform(disabled: Boolean) {
        val a = boardProxy.isDisableCameraTransform
        if (disabled != a) {
            if (disabled) {
                boardProxy.disableDeviceInputsTemporary(true)
            } else {
                boardProxy.disableDeviceInputsTemporary(boardProxy.isDisableDeviceInputs)
            }
        }
        boardProxy.disableCameraTransform(disabled)
    }

    private fun releaseBoard() {
        AgoraLog?.e("$tag:releaseBoard")
        boardProxy.setListener(null)
        whiteboardRoom?.leave()
        if (this::whiteBoardView.isInitialized) {
            whiteBoardView.removeAllViews()
            whiteBoardView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(p0: View?) {
                    // Nothing done
                }

                override fun onViewDetachedFromWindow(p0: View?) {
                    AgoraLog?.i("$tag:whiteboard view destroy called")
                    whiteBoardView.destroy()
                }
            })
        }
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
        AgoraLog?.i("$tag->onMemberStateChanged:${GsonUtil.gson.toJson(state)}")

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
                val convertShape = FcrWhiteboardConverter.convertShape(it)
                if (convertShape != null) {
                    memberState.currentApplianceName = Appliance.SHAPE
                    memberState.shapeType = convertShape
                } else {
                    memberState.currentApplianceName = FcrWhiteboardConverter.convertApplianceToString(it)
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

    fun setHiddenLoading() {
        loadingView.visibility = View.GONE
    }
}