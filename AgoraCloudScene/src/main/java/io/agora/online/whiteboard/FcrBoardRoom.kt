package io.agora.online.whiteboard

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.herewhite.sdk.*
import com.herewhite.sdk.domain.*
import com.herewhite.sdk.window.SlideListener
import io.agora.agoraeducore.core.internal.base.PreferenceManager
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.online.R
import io.agora.online.impl.whiteboard.AudioMixerBridgeImpl
import io.agora.online.impl.whiteboard.FcrWhiteboardConverter
import io.agora.online.impl.whiteboard.WhiteBoardAudioMixingBridgeListener
import io.agora.online.impl.whiteboard.bean.*
import io.agora.online.impl.whiteboard.netless.listener.BoardEventListener
import io.agora.online.impl.whiteboard.netless.manager.BoardRoom
import io.agora.online.impl.whiteboard.netless.manager.BoardUtils
import io.agora.online.whiteboard.bean.FcrBoardRoomJoinConfig
import wendu.dsbridge.DWebView
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * author : felix
 * date : 2022/6/7
 * description :
 */
class FcrBoardRoom(var whiteBoardView: WhiteboardView) : SlideListener {
    var whiteBoardSDKLog: FcrBoardSDKLog = FcrBoardSDKLog()
    lateinit var whiteSdk: WhiteSdk
    var context: Context = whiteBoardView.context
    var roomParams: RoomParams? = null
    var retryTime = 0
    val TAG = "WhiteBoardSDK"
    val handler = Handler(Looper.getMainLooper())
    var roomListener: FcrBoardRoomListener? = null
        set(value) {
            whiteBoardSDKLog.roomListener = value
            field = value
        }

    var mixingBridgeListener: ((AgoraBoardInteractionPacket) -> Unit)? = null

    val boardRoom = Proxy.newProxyInstance(BoardRoom::class.java.classLoader, arrayOf(BoardRoom::class.java),
        object : InvocationHandler {
            private val boardRoom: BoardRoom = FcrBoardMainWindow()

            override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
                val parameters = args?.filter { it !is BoardEventListener && it !is WhiteSdk }

                try {
                    LogX.i("${FcrBoardMainWindow.TAG}->${method?.name}:${GsonUtil.gson.toJson(parameters)}")
                } catch (e: Exception) {
                }

                return if (args.isNullOrEmpty()) {
                    method?.invoke(boardRoom)
                } else {
                    method?.invoke(boardRoom, *args)
                }
            }
        }) as BoardRoom

    fun init(whiteBoardAppId: String, region: String?) {
        WhiteDisplayerState.setCustomGlobalStateClass(BoardState::class.java)
        val isDebugMode = PreferenceManager.get(Constants.KEY_SP_USE_OPEN_TEST_MODE, false)
        if (isDebugMode) {
            DWebView.setWebContentsDebuggingEnabled(true)
        }

        val configuration = WhiteSdkConfiguration(whiteBoardAppId, true)
        configuration.isEnableIFramePlugin = true
        configuration.isUserCursor = true
        configuration.region = FcrWhiteboardConverter.convertStringToRegion(region)
        configuration.useMultiViews = true
        configuration.isEnableAppliancePlugin = true;

        whiteSdk = WhiteSdk(
            whiteBoardView,
            context,
            configuration,
            whiteBoardSDKLog,
            AudioMixerBridgeImpl(whiteboardMixingBridgeListener)
        )
        whiteSdk.setSlideListener(this);
    }

    override fun onSlideError(
        errorType: SlideErrorType?,
        errorMsg: String?,
        slideId: String?,
        slideIndex: Int
    ) {
        if (errorType == SlideErrorType.RESOURCE_ERROR || errorType == SlideErrorType.CANVAS_CRASH) {
            if (this.retryTime == 0) {
                this.retry(slideId!!, slideIndex)
            } else if (this.retryTime < 5) {
                handler.postDelayed({
                    this.retry(slideId!!, slideIndex)
                }, 5000)
            } else {
                this.retryTime = 0
                ContextCompat.getMainExecutor(context).execute {
                    ToastManager.showShort(context, R.string.fcr_board_slide_retry_failure)
                }
            }
        } else if (errorType == SlideErrorType.RUNTIME_ERROR) {
            this.whiteSdk.recoverSlide(slideId, slideIndex)
        } else if(errorType == SlideErrorType.RUNTIME_WARN){
            LogX.w(TAG, "slide page: ${slideIndex}, error: ${errorMsg.toString()}")
        }
    }

    private fun retry(slideId: String, slideIndex: Int){9
        this.retryTime += 1
        this.whiteSdk.recoverSlide(slideId, slideIndex)
        ContextCompat.getMainExecutor(whiteBoardView.context).execute{
            ToastManager.showShort(context.getString(R.string.fcr_board_slide_retry) + this.retryTime + "/5")
        }
    }

    fun join(config: FcrBoardRoomJoinConfig) {
        val windowParams = WindowParams()
        windowParams.collectorStyles = config.collectorStyles // 设置窗口收缩按钮

        LogX.e("WhiteBoardSDK: room uuid = ${config.roomId} ｜｜ room token = ${config.roomToken}")

        roomParams = RoomParams(config.roomId, config.roomToken, config.userId)
        roomParams?.cameraBound = CameraBound(0.1, 10.0)
        roomParams?.isWritable = config.hasOperationPrivilege
        roomParams?.isDisableNewPencil = false
        roomParams?.windowParams = windowParams
        roomParams?.windowParams?.chessboard = false
        roomParams?.userPayload = mutableMapOf(Pair("cursorName", config.userName))
        roomParams?.windowParams?.containerSizeRatio = config.boardRatio
        roomParams?.isUsingFloatBar = true // 开启或关闭图片、画笔、文本等的浮窗操作工具
        BoardUtils.registerTalkative(context, whiteSdk) // joinRoom 之前

        boardRoom.init(whiteSdk, roomParams)  // join room
    }

    fun getWritable(): Boolean {
        return boardRoom.writable
    }

    fun leave() {
        boardRoom.disconnect()
    }

    private val whiteboardMixingBridgeListener = object : WhiteBoardAudioMixingBridgeListener {
        override fun onStartAudioMixing(filepath: String, loopback: Boolean, replace: Boolean, cycle: Int) {
            LogX.i("WhiteBoardSDK: onStartAudioMixing")

            val data = AgoraBoardAudioMixingRequestData(
                type = AgoraBoardAudioMixingRequestType.Start,
                filepath = filepath, loopback = loopback, replace = replace, cycle = cycle
            )
            broadcastAudioMixingRequest(data)
        }

        override fun onStopAudioMixing() {
            LogX.i("WhiteBoardSDK: onStopAudioMixing")

            val data = AgoraBoardAudioMixingRequestData(AgoraBoardAudioMixingRequestType.Stop)
            broadcastAudioMixingRequest(data)
        }

        override fun onSetAudioMixingPosition(position: Int) {
            LogX.i("WhiteBoardSDK: onSetAudioMixingPosition : $position")

            val data =
                AgoraBoardAudioMixingRequestData(AgoraBoardAudioMixingRequestType.SetPosition, position = position)
            broadcastAudioMixingRequest(data)
        }

        override fun pauseAudioMixing() {
            LogX.i("WhiteBoardSDK: pauseAudioMixing")

            val data = AgoraBoardAudioMixingRequestData(AgoraBoardAudioMixingRequestType.PAUSE)
            broadcastAudioMixingRequest(data)
        }

        override fun resumeAudioMixing() {
            LogX.i("WhiteBoardSDK: resumeAudioMixing")

            val data = AgoraBoardAudioMixingRequestData(AgoraBoardAudioMixingRequestType.RESUME)
            broadcastAudioMixingRequest(data)
        }

        private fun broadcastAudioMixingRequest(data: AgoraBoardAudioMixingRequestData) {
            val packet = AgoraBoardInteractionPacket(AgoraBoardInteractionSignal.BoardAudioMixingRequest, data)
            mixingBridgeListener?.invoke(packet)
        }
    }
}