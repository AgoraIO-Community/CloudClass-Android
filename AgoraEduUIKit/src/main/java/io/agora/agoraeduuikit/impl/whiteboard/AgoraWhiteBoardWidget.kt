package io.agora.agoraeduuikit.impl.whiteboard

import android.content.Context
import android.text.TextUtils
import android.text.TextUtils.isEmpty
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.agora.edu.component.loading.AgoraLoadingView
import com.agora.edu.component.teachaids.networkdisk.Statics
import com.agora.edu.component.whiteboard.AgoraEduWhiteBoardControlComponent
import com.agora.edu.component.whiteboard.data.AgoraEduApplianceData
import com.herewhite.sdk.WhiteboardView
import com.herewhite.sdk.domain.*
import io.agora.agoraeducore.core.context.AgoraEduContextClassState
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole.Student
import io.agora.agoraeducore.core.context.EduBoardRoomPhase
import io.agora.agoraeducore.core.context.EduBoardRoomPhase.Companion.convert
import io.agora.agoraeducore.core.context.EduBoardRoomPhase.Disconnected
import io.agora.agoraeducore.core.context.RoomContext
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.framework.data.EduBaseUserInfo
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.impl.handler.UserHandler
import io.agora.agoraeducore.core.internal.framework.impl.managers.AgoraWidgetManager.Companion.grantUser
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.launch.AgoraEduRoleType.AgoraEduRoleTypeTeacher
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCourseware
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.core.internal.report.ReportManager
import io.agora.agoraeducore.core.internal.transport.AgoraTransportEvent
import io.agora.agoraeducore.core.internal.transport.AgoraTransportEventId
import io.agora.agoraeducore.core.internal.transport.AgoraTransportManager
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
    override val TAG = "AgoraWhiteBoard"
    private lateinit var whiteBoardView: WhiteboardView
    private lateinit var loadingView: AgoraLoadingView
    private lateinit var whiteBoardControlView: AgoraEduWhiteBoardControlComponent

    private var whiteBoardAppId: String? = null
    private var region: String? = null
    private var whiteboardUuid: String? = null
    private var curLocalToken: String? = null
    private var aPaaSUserUuid: String? = null
    private var aPaaSUserName: String? = null
    private var curGranted: Boolean? = null
    private var curGrantedUsers = mutableListOf<String>()
    private val defaultCoursewareName = "init"
    private var curSceneState: SceneState? = null
    private var courseWareManager: FcrCourseWareManager? = null
    private var disconnectErrorHappen = false
    private val boardAppIdKey = "boardAppId"
    private val boardTokenKey = "boardToken"
    private val coursewareInitial = "initial"
    private var firstGrantedTip = true
    private var roomContext: RoomContext? = null
    private var initJoinWhiteBoard = false

    private val curDrawingConfig = WhiteboardDrawingConfig()
    private val webChromeClient = object : WebChromeClient() {}
    private var mAppid = ""
    private var whiteBoardImg: FcrWhiteBoardImg? = null
    private var whiteBoardPath: FcrWhiteboardPath? = null
    private var whiteboardRoom: FcrBoardRoom? = null
    private var context: Context? = null
    private lateinit var boardProxy: BoardRoom

    val coursewaresList = ArrayList<AgoraEduCourseware>()
    var isAttributeGot: Boolean = false
    var isNeedShowLoading = true // 是否需要显示加载loading
    var uuid: String? = null

    private val boardEventListener = object : SimpleBoardEventListener() {
        override fun onJoinSuccess(state: GlobalState) {
            super.onJoinSuccess(state)
            loadingView.visibility = View.GONE
            whiteBoardControlView.initView(boardProxy)
            setCurGrantedUsers()
            initWriteableFollowLocalRole()
            onGlobalStateChanged(state)
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
                    val event = AgoraTransportEvent(AgoraTransportEventId.EVENT_ID_WHITEBOARD_LOADING)
                    event.arg2 = true
                    AgoraTransportManager.notify(event)
                    //loadingView.visibility = View.VISIBLE
                }
            } else {
                val event = AgoraTransportEvent(AgoraTransportEventId.EVENT_ID_WHITEBOARD_LOADING)
                event.arg2 = false
                AgoraTransportManager.notify(event)
                //loadingView.visibility = View.GONE
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
                //LogX.i(this@AgoraWhiteBoardWidget.TAG, "onRoomStateChanged-> 窗口大小${windowBoxState}")
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
            LogX.i(this@AgoraWhiteBoardWidget.TAG, "onCanUndoStepsUpdate uuid=${uuid}")
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
            LogX.i(this@AgoraWhiteBoardWidget.TAG, "onCanRedoStepsUpdate uuid=${uuid}")
            uuid?.let {
                val list = AgoraWhiteBoardManager.getWhiteBoardList(it)
                if (list != null) {
                    for (listener in list) {
                        listener.onCanRedoStepsUpdate(canRedoSteps)
                    }
                }
            }
        }
    }

    val cloudDiskMsgObserver = object : AgoraWidgetMessageObserver {
        override fun onMessageReceived(msg: String, id: String) {
            val packet = GsonUtil.jsonToObject<AgoraBoardInteractionPacket>(msg)
            if (packet?.signal?.value == LoadCourseware.value) {
                GsonUtil.toJson(packet.body).let {
                    GsonUtil.jsonToObject<AgoraEduCourseware>(it)?.let { courseware ->
                        if (courseWareManager?.isImage(courseware.ext) == true) {
                            context?.let {
                                courseWareManager?.loadImage(it, courseware.resourceUrl)
                            }
                        } else {
                            courseWareManager?.loadCourseware(courseware)
                        }

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
        whiteBoardView.webChromeClient = webChromeClient
        whiteBoardView.setBackgroundColor(ContextCompat.getColor(context!!, R.color.fcr_system_foreground_color))

        loadingView = view.findViewById(R.id.agora_edu_loading)

        val p = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        container.addView(view, p)

        whiteBoardControlView = view.findViewById(R.id.agora_whiteboard_control)
        roomContext = eduCore?.eduContextPool()?.roomContext()
        roomContext?.let { whiteBoardControlView.setRoomContext(it, widgetInfo?.localUserInfo) }
        whiteBoardPath = FcrWhiteboardPath()
        whiteboardRoom = FcrBoardRoom(whiteBoardView)
        boardProxy = whiteboardRoom!!.boardRoom
        boardProxy.setListener(boardEventListener)
        courseWareManager = FcrCourseWareManager(boardProxy)
        whiteBoardImg = FcrWhiteBoardImg(whiteBoardView.context)
        whiteBoardImg?.roomName = widgetInfo?.roomInfo?.roomName
        eduCore?.eduContextPool()?.userContext()?.addHandler(userHandler)

        setBoardMixing()
        setStartRoomStatus()
        initWhiteboardDrawingConfig()
        joinWhiteBoard()
    }

    fun initWhiteboardDrawingConfig(){
        curDrawingConfig.activeAppliance = WhiteboardApplianceType.Clicker
        curDrawingConfig.color = getColor(whiteBoardView.context, R.color.agora_board_default_stroke)
        curDrawingConfig.fontSize = context!!.resources.getInteger(R.integer.agora_board_default_font_size)
        curDrawingConfig.thick = 2
    }

    fun setStartRoomStatus() {
        if (eduCore?.eduContextPool()?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Teacher) {
            eduCore?.eduContextPool()?.roomContext()?.addHandler(object : RoomHandler() {
                override fun onClassStateUpdated(state: AgoraEduContextClassState) {
                    super.onClassStateUpdated(state)
                    if (state == AgoraEduContextClassState.Before) {
                        whiteBoardControlView.setStartView(true)
                    } else {
                        whiteBoardControlView.setStartView(false)
                    }
                }
            })
        }
    }

    fun setBoardMixing(){
        whiteboardRoom?.mixingBridgeListener = {
            var code: Int? = null
            val data = it.body as AgoraBoardAudioMixingRequestData
            when (data.type) {
                AgoraBoardAudioMixingRequestType.Start -> {
                    val temp = eduCore?.eduContextPool()?.mediaContext()?.startAudioMixing(data.filepath, data.loopback, data.replace, data.cycle)
                    temp?.let {
                        if (temp != 0) { // 约定下来的状态值 除了startAudioMixing 传 714 外，其他传state 0
                            LogX.i(TAG,"invoke RTC  Mixing API state :714  || errorCode = ${code}")
                            boardProxy.changeMixingState(714, temp.toLong())
                        }
                    }
                }

                AgoraBoardAudioMixingRequestType.Stop -> {
                    code = eduCore?.eduContextPool()?.mediaContext()?.stopAudioMixing()
                }

                AgoraBoardAudioMixingRequestType.PAUSE -> {
                    code = eduCore?.eduContextPool()?.mediaContext()?.pauseAudioMixing()
                }

                AgoraBoardAudioMixingRequestType.RESUME -> {
                    code = eduCore?.eduContextPool()?.mediaContext()?.resumeAudioMixing()
                }

                AgoraBoardAudioMixingRequestType.SetPosition -> {
                    code = eduCore?.eduContextPool()?.mediaContext()?.setAudioMixingPosition(data.position)
                }
            }

            code?.let {
                if (code != 0) { // 约定下来的状态值 除了startAudioMixing 传 714 外，其他传state 0
                    LogX.i(TAG,"invoke RTC  Mixing API state :0  || errorCode = ${code}")
                    boardProxy.changeMixingState(0, code.toLong())
                }
            }
        }
    }

    fun setWhiteBoardControlView(isShow: Boolean) {
        LogX.i(TAG, "whiteBoardControlView : $isShow")
        ContextCompat.getMainExecutor(context).execute {
            if (isShow) {
                whiteBoardControlView.visibility = View.VISIBLE
            } else {
                whiteBoardControlView.visibility = View.GONE
            }
        }
    }

    fun parseWhiteBoardConfigProperties(): Boolean {
        val extraProperties = this.widgetInfo?.roomProperties as? MutableMap<*, *>
        extraProperties?.let {
            this.whiteBoardAppId = it["boardAppId"] as? String ?: ""
            this.region = it["boardRegion"] as? String
            this.whiteboardUuid = it["boardId"] as? String ?: ""
            this.curLocalToken = it["boardToken"] as? String ?: ""
            this.aPaaSUserUuid = widgetInfo?.localUserInfo?.userUuid
            this.aPaaSUserName = widgetInfo?.localUserInfo?.userName
        }

        LogX.i(TAG, "whiteboard region：${region}")

        widgetInfo?.extraInfo?.let {
            if (it is MutableMap<*, *> && it.isNotEmpty()) {
                // 课件
                if(it.keys.contains(Statics.publicResourceKey)){
                    coursewaresList.clear()
                    val list = it[Statics.publicResourceKey] as? List<AgoraEduCourseware>
                    if (!list.isNullOrEmpty()) {
                        coursewaresList.addAll(list)
                    }
                }
            }
        }

        return !isEmpty(whiteBoardAppId) && !isEmpty(whiteboardUuid) && !isEmpty(curLocalToken) && !isEmpty(aPaaSUserUuid)
    }

    fun joinWhiteBoard() {
        // 老师进入的时候，显示必须显示
        if (eduCore?.eduContextPool()?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Teacher) {
            setWhiteBoardControlView(true)
        }

        if (!parseWhiteBoardConfigProperties()) {
            LogX.e(TAG, "->WhiteBoardConfigProperties isNullOrEmpty, please check roomProperties.widgets.netlessBoard")
            return
        }

        initJoinWhiteBoard = true

        whiteboardRoom?.init(whiteBoardAppId!!, region)
        whiteBoardPath?.getCoursewareAttribute(mAppid)

        ContextCompat.getMainExecutor(whiteBoardView.context).execute {
            boardProxy.getRoomPhase(object : Promise<RoomPhase> {
                override fun then(phase: RoomPhase) {
                    LogX.e(TAG, "getRoomPhase ->" + phase.name)
                    if (phase != RoomPhase.connected) {
                        broadcastBoardPhaseState(Disconnected)
                        joinWhiteboard(whiteboardUuid!!, curLocalToken!!)
                        ReportManager.getAPaasReporter()?.reportWhiteBoardStart()
                    }
                }

                override fun catchEx(t: SDKError) {
                    LogX.e(TAG, "getRoomPhase : catchEx->" + t.message)
                    ToastManager.showShort(t.message!!)
                    loadingView.visibility = View.GONE
                }
            })
        }
    }

    /**
     * 是否打开课件：
     * 1、 1v1 教室
     * 2、 老师不在教室，学生打开课件
     * 3、 需要白板 writeable 权限
     */
    fun isLoadCourseWare(): Boolean {
//        var isLoadCourseWare = false
//        if (eduCore?.config?.roomType == RoomType.ONE_ON_ONE.value && coursewaresList.isNotEmpty()) {
//            isLoadCourseWare = true
//
//            val userList = eduCore?.eduContextPool()?.userContext()?.getAllUserList()
//            run outside@{
//                userList?.forEach { // 老师在教室里，不需要打开课件
//                    if (it.role == AgoraEduContextUserRole.Teacher) {
//                        isLoadCourseWare = false
//                        return@outside
//                    }
//                }
//            }
//        }
//        return isLoadCourseWare
        return false
    }

    /**
     * 加载课件资源，进入就打开课件，writeable为true，才可以加载
     */
    fun loadCourseWare() { // 1v1 默认进入打开课件
        if (isLoadCourseWare()) {
            LogX.i(TAG, "loadCourseWare : $coursewaresList")
            courseWareManager?.loadCourseware(coursewaresList)
            boardProxy.scalePptToFit()
        }
    }

    /**
     * 获取进入教室，设置已经白板授权的人
     */
    fun setCurGrantedUsers() {
        val list = AgoraEduApplianceData.getBoardAllGrantUsers(widgetInfo)
        if (list.isNotEmpty()) {
            curGrantedUsers.clear()
            curGrantedUsers.addAll(list)
            //broadcastPermissionChanged(list)
        }
    }

    private fun getInitialWriteableState(): Boolean {
        var isWriteable = AgoraEduApplianceData.isBoardGrant(widgetInfo, aPaaSUserUuid)

        if (widgetInfo?.localUserInfo?.userRole == AgoraEduRoleTypeTeacher.value) {
            isWriteable = true
        } else if (roomContext?.getRoomInfo()?.roomType == RoomType.GROUPING_CLASS) {
            isWriteable = true
        } else if (isLoadCourseWare()) {
            isWriteable = true
        }
        return isWriteable
    }

    private fun joinWhiteboard(uuid: String, boardToken: String) {
        LogX.e(TAG,"joinWhiteboard-> uuid=$uuid || boardToken=$boardToken")

        //loadingView.visibility = View.VISIBLE
        val event = AgoraTransportEvent(AgoraTransportEventId.EVENT_ID_WHITEBOARD_LOADING)
        event.arg2 = true
        AgoraTransportManager.notify(event)

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
            LogX.e(TAG, "白板比例：${config.boardRatio} （h=${it.height},w=${it.width})")
        }
        whiteboardRoom?.join(config)

        if (config.hasOperationPrivilege) {
            aPaaSUserUuid?.let { // 白板授权，通知其他人
                val list = AgoraEduApplianceData.getBoardAllGrantUsers(widgetInfo)
//                if (roomContext?.getRoomInfo()?.roomType == RoomType.LARGE_CLASS) {
                    // 刚刚进入大班课，是不上台的，取消上一次上台的权限
//                    if (list.contains(aPaaSUserUuid)) {
//                        list.remove(aPaaSUserUuid)
//
//                        val keys = mutableListOf<String>()
//                        keys.add("$grantUser.${aPaaSUserUuid}")
//                        deleteRoomProperties(keys, mutableMapOf(), null)
//                    }
//                }
                // 打开课件需要白板权限
                if (isLoadCourseWare()) {
                    if (aPaaSUserUuid != null && !list.contains(aPaaSUserUuid)) {
                        list.add(aPaaSUserUuid!!)
                    }
                }
                broadcastPermissionChanged(list)
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
                        setMemberState(it)
                    } ?: Runnable {
                        LogX.e(TAG, "${packet.signal}, (MemberStateChanged)packet.body convert failed")
                    }
                }
                BoardGrantDataChanged -> {
                    (GsonUtil.gson.fromJson(packet.body.toString(), AgoraBoardGrantData::class.java))?.let { data ->
                        data.userUuids.forEach {
                            grantBoard(it, data.granted)
                        }
                    }
                        ?: Runnable {
                            LogX.e(
                                TAG,
                                "${packet.signal}, (BoardGrantDataChanged)packet.body convert failed"
                            )
                        }
                }
                RTCAudioMixingStateChanged -> {
                    try {
                        val state = (packet.body as? Map<*, *>)?.get("first") as Double
                        val errorCode = (packet.body as? Map<*, *>)?.get("second") as Double
                        LogX.i(TAG,"RTCAudioMixingStateChanged state ${state} || errorCode = ${errorCode}")
                        boardProxy.changeMixingState(state.toLong(), errorCode.toLong())
                    } catch (e: Exception) {
                        e.printStackTrace()
                        LogX.e(TAG,"RTCAudioMixingStateChanged state error:${Log.getStackTraceString(e)}")
                    }
                }
                LoadCourseware -> {
                    (GsonUtil.gson.fromJson(packet.body.toString(), AgoraEduCourseware::class.java))?.let {
                        courseWareManager?.loadCourseware(it)
                    } ?: Runnable {
                        LogX.e(TAG, "${packet.signal}, (MemberStateChanged)packet.body convert failed")
                    }
                }
                // 保存白板图片
                BoardImage -> {
                    LogX.e(TAG, "保存白板图片")
                    context?.let {
                        if (boardProxy.room != null) {
                            boardProxy.room.backgroundColor = getColor(it, R.color.fcr_system_foreground_color)
                            whiteBoardImg?.saveImgToGallery(boardProxy.room)
                        }
                    }
                }

                BoardRatioChange -> {
                    // 避免白板收缩的时候，还无法准确获取宽高
                    container?.postDelayed({
                        container?.let {
                            val ratio = it.height.toFloat() / it.width.toFloat()
                            boardProxy.setContainerSizeRatio(ratio)
                            LogX.e(TAG, "白板比例：${ratio} （h=${it.height},w=${it.width})")
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
        keys: MutableList<String>, operator: EduBaseUserInfo?
    ) {
        super.onWidgetRoomPropertiesUpdated(properties, cause, keys, operator)
        if (properties.keys.contains(boardAppIdKey) && properties.keys.contains(boardTokenKey) && !initJoinWhiteBoard) {
            ContextCompat.getMainExecutor(context).execute {
                joinWhiteBoard()
            }
        }
        // 白板授权
        keys.forEach {
            if (it.startsWith(grantUser)) {
                handleGrantChanged(AgoraEduApplianceData.getBoardAllGrantUsers(widgetInfo))
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
                handleGrantChanged(AgoraEduApplianceData.getBoardAllGrantUsers(widgetInfo))
            }
        }
    }

    private fun handleGrantChanged(grantedUsers: MutableList<String>) {
        val isStudent = widgetInfo?.localUserInfo?.userRole == Student.value
        val granted = if (isStudent) grantedUsers.contains(aPaaSUserUuid) else true
        //disableCameraTransform(!granted) // 缩放逻辑
        disableDeviceInputs(!granted)
        setWhiteBoardControlView(granted)

        if (granted != curGranted) {
            curGranted = granted
            // set writeable follow granted if curRole is student
            if (isStudent) {
                LogX.i(TAG, "->set writeable follow granted: $granted")
                boardProxy.setWritable(granted, object : Promise<Boolean> {
                    override fun then(t: Boolean?) {
                        LogX.i(TAG, "writeable value: $t")

                        if (granted) {
                            restoreWhiteBoardAppliance()
                        }

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
                        LogX.e(TAG, "initDrawingConfig-setWritable:${t?.jsStack}")
                    }
                })
                LogX.i(TAG, "set followMode: ${!granted}")
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

    override fun release() {
        super.release()
        releaseBoard()
    }

    /**
     * grant user to draw on whiteBoard
     * */
    fun grantBoard(userId: String, granted: Boolean) {
        LogX.e(TAG,"aPaaSUserUuid-->${aPaaSUserUuid} || userId=$userId")

        LogX.e(TAG,"isBoardGrant-->${AgoraEduApplianceData.isBoardGrant(widgetInfo, userId)} || granted=$granted")
        if (aPaaSUserUuid == userId && AgoraEduApplianceData.isBoardGrant(widgetInfo, userId) == granted) {
            return
        }
        if (granted) {
            // widgets.netlessBoard.extra.grantedUsers:{userUuid: true | false}
            val map = mutableMapOf<String, Any>()
            //keys：grantedUsers.e10adc3949ba59abbe56e057f20f883e2
            map["$grantUser.${userId}"] = true
            // {"extra":{"grantedUsers.50ed4a9ff3566029759e2c0e4dd5a9922":true}}
            updateRoomProperties(map, mutableMapOf(), null)

            LogX.e(TAG,"给我授权")

        } else {
            val keys = mutableListOf<String>()
            keys.add("$grantUser.${userId}")
            deleteRoomProperties(keys, mutableMapOf(), null)

            LogX.e(TAG,"删除我的授权")

        }
    }

    fun isGrant(): Boolean {
        return whiteboardRoom?.getWritable() ?: if (::whiteBoardControlView.isInitialized) {
            whiteBoardControlView.isShown
        } else false
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
                    LogX.i(TAG, "writeable value: $t")

                    if (writeable) {
                        restoreWhiteBoardAppliance()
                    }

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
                    LogX.e(TAG, "initDrawingConfig-setWritable:${t?.jsStack}")
                }
            })
            this@AgoraWhiteBoardWidget.disableDeviceInputs(!writeable)
        }
    }

//    private fun initDrawingConfig(promise: Promise<Unit>) {
//        // 老师进入的时候，显示必须显示
//        if (eduCore?.eduContextPool()?.userContext()?.getLocalUserInfo()?.role == AgoraEduContextUserRole.Teacher) {
//            setWhiteBoardControlView(true)
//        }
//
//        if (disconnectErrorHappen) {
//            promise.then(Unit)
//            return
//        }
//        boardProxy.setWritable(true, object : Promise<Boolean> {
//            override fun then(t: Boolean?) {
//                disableDeviceInputs(false)
//                curDrawingConfig.activeAppliance = WhiteboardApplianceType.Clicker
//                curDrawingConfig.color = getColor(whiteBoardView.context, R.color.agora_board_default_stroke)
//                curDrawingConfig.fontSize = context!!.resources.getInteger(R.integer.agora_board_default_font_size)
//                restoreWhiteBoardAppliance()
//                broadcastDrawingConfig()
//                loadCourseWare()
//                if(!getInitialWriteableState()) {
//                    boardProxy.setWritable(false)
//                }
//                disableDeviceInputs(true)
//                promise.then(Unit)
//            }
//
//            override fun catchEx(t: SDKError?) {
//                LogX.e(TAG, "initDrawingConfig-setWritable:${t?.jsStack}")
//                promise.catchEx(t)
//            }
//        })
//    }

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
        //if (grantedUsers.isNotEmpty()) {
            val data = AgoraBoardGrantData(true, grantedUsers)
            val packet = AgoraBoardInteractionPacket(BoardGrantDataChanged, data)
            sendMessage(GsonUtil.gson.toJson(packet))
        //}
    }

    /**
     * broadcaster BoardPhaseState
     * */
    private fun broadcastBoardPhaseState(phase: EduBoardRoomPhase) {
        val body = AgoraBoardInteractionPacket(BoardPhaseChanged, phase)
        sendMessage(GsonUtil.gson.toJson(body))
    }

    fun hideWhiteboardTools() {
        //curGrantedUsers.clear()
        val body = AgoraBoardInteractionPacket(BoardGrantDataChanged, curGrantedUsers)
        sendMessage(GsonUtil.gson.toJson(body))
    }

    fun setAppid(appid: String) {
        mAppid = appid
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
        LogX.e(TAG, "releaseBoard")
        eduCore?.eduContextPool()?.userContext()?.removeHandler(userHandler)
        boardProxy.setListener(null)
        whiteboardRoom?.leave(object : Promise<Any> {
            override fun then(t: Any?) {
                whiteBoardView.removeAllViews()
                if (this@AgoraWhiteBoardWidget::whiteBoardView.isInitialized) {
                    LogX.i(TAG, "whiteboard view destroy called")
                    // 只能在 board disconnect 之后调用
                    whiteBoardView.destroy()
                }
            }

            override fun catchEx(t: SDKError?) {
                whiteBoardView.removeAllViews()
                if (this@AgoraWhiteBoardWidget::whiteBoardView.isInitialized) {
                    LogX.i(TAG, "whiteboard view destroy called")
                    whiteBoardView.destroy()
                }
            }
        })
//        if (this::whiteBoardView.isInitialized) {
//            whiteBoardView.removeAllViews()
//            whiteBoardView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
//                override fun onViewAttachedToWindow(p0: View) {
//                    // Nothing done
//                }
//
//                override fun onViewDetachedFromWindow(p0: View) {
//                    LogX.i(TAG, "whiteboard view destroy called")
//                    // 需要在 board disconnect 之后调用
//                    whiteBoardView.destroy()
//                }
//            })
//        }
    }

    private fun restoreWhiteBoardAppliance() {
        initWhiteboardDrawingConfig()

        val drawingMemberState = AgoraBoardDrawingMemberState()
        drawingMemberState.activeApplianceType = curDrawingConfig.activeAppliance
        drawingMemberState.strokeColor = curDrawingConfig.color
        drawingMemberState.textSize = curDrawingConfig.fontSize
        drawingMemberState.strokeWidth = curDrawingConfig.thick
        setMemberState(drawingMemberState)
    }

    private fun setMemberState(state: AgoraBoardDrawingMemberState) {
        LogX.i(TAG, "setMemberState:${GsonUtil.gson.toJson(state)} , isWritable = ${whiteboardRoom?.getWritable()}")

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

    val userHandler = object : UserHandler() {
        override fun onLocalUserKickedOut() {
            super.onLocalUserKickedOut()
            releaseBoard()
        }
    }
}