package io.agora.edu.classroom.widget.whiteboard

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.herewhite.sdk.*
import com.herewhite.sdk.domain.*
import io.agora.base.ToastManager
import io.agora.edu.R
import io.agora.edu.classroom.widget.video.VideoViewTextureOutlineProvider
import io.agora.edu.classroom.widget.whiteboard.WhiteBoardToolAttrs.ellipseSizes
import io.agora.edu.classroom.widget.whiteboard.WhiteBoardToolAttrs.eraserSizes
import io.agora.edu.classroom.widget.whiteboard.WhiteBoardToolAttrs.modes
import io.agora.edu.classroom.widget.whiteboard.WhiteBoardToolAttrs.penSizes
import io.agora.edu.classroom.widget.whiteboard.WhiteBoardToolAttrs.penStyles
import io.agora.edu.classroom.widget.whiteboard.WhiteBoardToolAttrs.rectangleSizes
import io.agora.edu.classroom.widget.whiteboard.WhiteBoardToolAttrs.textSizes
import io.agora.edu.classroom.widget.window.AbstractWindow
import io.agora.edu.common.bean.board.BoardState
import io.agora.edu.util.ColorUtil
import io.agora.education.impl.Constants
import io.agora.whiteboard.netless.listener.BoardEventListener
import io.agora.whiteboard.netless.listener.GlobalStateChangeListener
import io.agora.whiteboard.netless.manager.BoardManager

class WhiteBoardWindow : AbstractWindow, View.OnTouchListener, BoardEventListener, CommonCallbacks,
        ToolWindow.ToolWindowListener, PageControlWindow.PageControlListener {
    private val TAG = "WhiteBoardWindow"
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var whiteBoardView: WhiteboardView
    private lateinit var loadingPb: ProgressBar

    private lateinit var whiteBoardAppId: String
    private var whiteSdk: WhiteSdk? = null
    private val boardManager = BoardManager()
    private var curLocalUuid: String? = null
    private var curLocalToken: String? = null
    private var localUserUuid: String? = null
    private val miniScale = 0.1
    private val maxScale = 10.0
    private val scaleStepper = 0.2

    /*初始化时不进行相关提示*/
    private var inputTips = false
    private var transform = false

    /*是否允许在开启白板跟随的情况下，进行书写(仅仅书写，不包括移动缩放)*/
    var inputWhileFollow = false

    /**尽可能早的设置这两个监听器*/
    var globalStateChangeListener: GlobalStateChangeListener? = null
    var whiteBoardEventListener: WhiteBoardEventListener? = null

    private lateinit var originLayoutParams: LayoutParams

    private val toolModeAttr: ToolModeAttr = ToolModeAttr()

    constructor(context: Context) : super(context) {
        view()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        view()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        view()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun view() {
        inflate(context, R.layout.white_board_window_layout, this)
        (context as Activity).window.setBackgroundDrawableResource(android.R.color.transparent)
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                originLayoutParams = this@WhiteBoardWindow.layoutParams as LayoutParams
            }
        })
        rootLayout = findViewById(R.id.root_Layout)
        whiteBoardView = findViewById(R.id.white_board_view)
//        setWhiteBoardCorner(true)
        loadingPb = findViewById(R.id.pb_loading)
        whiteBoardView.setOnTouchListener(this)
        whiteBoardView.addOnLayoutChangeListener { v: View?, left: Int, top: Int, right: Int,
                                                   bottom: Int, oldLeft: Int, oldTop: Int,
                                                   oldRight: Int, oldBottom: Int ->
            boardManager.refreshViewSize()
        }
        whiteBoardEventListener?.let {
            it.onDisableDeviceInput(boardManager.isDisableDeviceInputs)
        }
    }

    fun initWithAppId(whiteBoardAppId: String) {
        this.whiteBoardAppId = whiteBoardAppId
        WhiteDisplayerState.setCustomGlobalStateClass(BoardState::class.java)
        val configuration = WhiteSdkConfiguration(whiteBoardAppId, true)
        whiteSdk = WhiteSdk(whiteBoardView, context, configuration, this)
        boardManager.setListener(this)
    }

    private fun setWhiteBoardCorner(corner: Boolean) {
        var radius1: Float = if (corner) context.resources.getDimensionPixelSize(R.dimen.whiteboard_window_content_corner).toFloat() else 0f
        val outlineProvider1 = VideoViewTextureOutlineProvider(radius1)
        whiteBoardView.outlineProvider = outlineProvider1
        whiteBoardView.clipToOutline = corner
    }

    fun initBoardWithRoomToken(uuid: String?, boardToken: String?, localUserUuid: String?) {
        if (TextUtils.isEmpty(uuid) || TextUtils.isEmpty(boardToken)) {
            return
        }
        curLocalUuid = uuid
        curLocalToken = boardToken
        this.localUserUuid = localUserUuid
        boardManager.getRoomPhase(object : Promise<RoomPhase> {
            override fun then(phase: RoomPhase) {
                Constants.AgoraLog.e(TAG + ":then->" + phase.name)
                if (phase != RoomPhase.connected) {
                    post { loadingPb.visibility = VISIBLE }
                    val params = RoomParams(uuid, boardToken)
                    params.cameraBound = CameraBound(miniScale, maxScale)
                    boardManager.init(whiteSdk, params)
                }
            }

            override fun catchEx(t: SDKError) {
                Constants.AgoraLog.e(TAG + ":catchEx->" + t.message)
                ToastManager.showShort(t.message!!)
            }
        })
    }

    fun disableDeviceInputs(disabled: Boolean) {
        val a = boardManager.isDisableDeviceInputs
        if (disabled != a) {
            if (!inputTips) {
                inputTips = true
            } else {
                ToastManager.showShort(if (disabled) R.string.revoke_board else R.string.authorize_board)
            }
        }
        whiteBoardEventListener?.let {
            it.onDisableDeviceInput(disabled)
        }
        boardManager.disableDeviceInputs(disabled)
    }

    fun disableCameraTransform(disabled: Boolean) {
        val a = boardManager.isDisableCameraTransform
        if (disabled != a) {
            if (disabled) {
                if (!transform) {
                    transform = true
                } else {
//                    ToastManager.showShort(R.string.follow_tips);
                }
                boardManager.disableDeviceInputsTemporary(true)
            } else {
                boardManager.disableDeviceInputsTemporary(boardManager.isDisableDeviceInputs)
            }
        }
        whiteBoardEventListener?.let {
            it.onDisableCameraTransform(disabled)
        }
        boardManager.disableCameraTransform(disabled)
    }

    fun setWritable(writable: Boolean) {
        boardManager.setWritable(writable)
    }

    fun releaseBoard() {
        boardManager.disconnect()
        whiteBoardView.removeAllViews()
        whiteBoardView.destroy()
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            whiteBoardView.requestFocus()
            if (boardManager.isDisableCameraTransform && !boardManager.isDisableDeviceInputs) {
                ToastManager.showShort(R.string.follow_tips)
                return true
            }
        }
        return false
    }

    /**BoardEventListener*/
    override fun onJoinSuccess(state: GlobalState?) {
        Constants.AgoraLog.e(TAG + ":onJoinSuccess->" + Gson().toJson(state))
        if (globalStateChangeListener != null) {
            globalStateChangeListener!!.onGlobalStateChanged(state)
        }
    }

    override fun onRoomPhaseChanged(phase: RoomPhase?) {
        Constants.AgoraLog.e(TAG + ":onRoomPhaseChanged->" + phase!!.name)
        loadingPb.visibility = if (phase == RoomPhase.connected) GONE else VISIBLE
    }

    override fun onGlobalStateChanged(state: GlobalState?) {
        if (globalStateChangeListener != null) {
            globalStateChangeListener!!.onGlobalStateChanged(state)
        }
    }

    override fun onSceneStateChanged(state: SceneState?) {
        Constants.AgoraLog.e("$TAG:onSceneStateChanged")
        whiteBoardEventListener?.let {
            it.onSceneStateChanged(state)
        }
    }

    override fun onMemberStateChanged(state: MemberState?) {
        whiteBoardEventListener?.let {
            it.onMemberStateChanged(state)
        }
    }

    override fun onDisconnectWithError(e: Exception?) {
        initBoardWithRoomToken(curLocalUuid, curLocalToken, localUserUuid)
    }

    /**CommonCallbacks*/
    override fun throwError(args: Any?) {
        Log.e(TAG, "error->${Gson().toJson(args)}")
    }

    override fun urlInterrupter(sourceUrl: String?): String? {
        return null
    }

    override fun onPPTMediaPlay() {
    }

    override fun onPPTMediaPause() {
    }

    override fun sdkSetupFail(error: SDKError?) {
//        initData()
//        initBoardWithRoomToken(curLocalUuid, curLocalToken, localUserUuid)
        /**当回调这里的时候，需要重新初始化SDK(包括重新初始化 WhiteboardView)，然后再进行调用才可以*/
        Log.e(TAG, "error->${error?.jsStack}")
    }

    /**WhiteBoardToolBarListener*/
    override fun onModeChanged(modeIndex: Int) {
        toolModeAttr.modeIndex = modeIndex
        boardManager.appliance = modes[modeIndex]
    }

    override fun onColorSelected(rgb: Int) {
        toolModeAttr.rgb = rgb
        boardManager.strokeColor = ColorUtil.colorToArray(rgb)
    }

    override fun onThicknessSelected(thicknessIndex: Int) {
        toolModeAttr.thicknessIndex = thicknessIndex
        boardManager.strokeWidth = penSizes[thicknessIndex]
    }

    override fun onPencilStyleSelected(styleIndex: Int) {
        toolModeAttr.pencilStyleIndex = styleIndex
        when (penStyles[styleIndex]) {
            PenTheme.Arrow -> {
                boardManager.appliance = Appliance.ARROW
            }
            PenTheme.StraightLine -> {
                boardManager.appliance = Appliance.STRAIGHT
            }
            PenTheme.Fluorescent -> {
                boardManager.appliance = Appliance.PENCIL
            }
            PenTheme.Pencil -> {
                boardManager.appliance = Appliance.PENCIL
            }
        }
    }

    override fun onFontSizeSelected(fontIndex: Int) {
        toolModeAttr.fontSizeIndex = fontIndex
        boardManager.textSize = textSizes[fontIndex]
    }

    /**PageControlListener*/
    override fun onPrevious() {
        boardManager.pptPreviousStep()
    }

    override fun onNext() {
        boardManager.pptNextStep()
    }

    override fun onEnlarge() {
        var curScale = boardManager.zoomScale
        curScale += scaleStepper
        if (curScale in miniScale..maxScale) {
            boardManager.zoom(curScale)
        }
    }

    override fun onNarrow() {
        var curScale = boardManager.zoomScale
        curScale -= scaleStepper
        if (curScale in miniScale..maxScale) {
            boardManager.zoom(curScale)
        }
    }

    override fun onFullScreen() {
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        layoutParams = params
        val drawable = ContextCompat.getDrawable(context, R.drawable.whiteboard_window_bg_no_corner)
        rootLayout.background = drawable
    }

    override fun onFitScreen() {
        layoutParams = originLayoutParams
        val drawable = ContextCompat.getDrawable(context, R.drawable.whiteboard_window_bg)
        rootLayout.background = drawable
    }
}