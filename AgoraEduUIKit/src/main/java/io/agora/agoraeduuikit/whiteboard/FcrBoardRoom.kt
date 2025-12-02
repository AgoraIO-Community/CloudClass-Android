package io.agora.agoraeduuikit.whiteboard

import android.content.Context
import com.herewhite.sdk.*
import com.herewhite.sdk.domain.*
import io.agora.agoraeducore.core.internal.base.PreferenceManager
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeduuikit.impl.whiteboard.AudioMixerBridgeImpl
import io.agora.agoraeduuikit.impl.whiteboard.FcrWhiteboardConverter
import io.agora.agoraeduuikit.impl.whiteboard.WhiteBoardAudioMixingBridgeListener
import io.agora.agoraeduuikit.impl.whiteboard.bean.*
import io.agora.agoraeduuikit.impl.whiteboard.netless.listener.BoardEventListener
import io.agora.agoraeduuikit.impl.whiteboard.netless.manager.BoardRoom
import io.agora.agoraeduuikit.impl.whiteboard.netless.manager.BoardUtils
import io.agora.agoraeduuikit.whiteboard.bean.FcrBoardRoomJoinConfig
import wendu.dsbridge.DWebView
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * author : felix
 * date : 2022/6/7
 * description :
 */
class FcrBoardRoom(var whiteBoardView: WhiteboardView) {
    var whiteBoardSDKLog: FcrBoardSDKLog = FcrBoardSDKLog()
    lateinit var whiteSdk: WhiteSdk
    var context: Context = whiteBoardView.context
    var roomParams: RoomParams? = null
    val TAG = "WhiteBoardSDK"
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
                    LogX.i(TAG,"BoardRoom invoke ${method?.name}:${GsonUtil.gson.toJson(parameters)}")
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

        whiteSdk = WhiteSdk(
            whiteBoardView,
            context,
            configuration,
            whiteBoardSDKLog,
            AudioMixerBridgeImpl(whiteboardMixingBridgeListener)
        )
    }

    fun join(config: FcrBoardRoomJoinConfig) {
        val windowParams = WindowParams()
        windowParams.collectorStyles = config.collectorStyles // 设置窗口收缩按钮

        LogX.e(TAG, "WhiteBoardSDK: join room uuid = ${config.roomId} ｜｜ room token = ${config.roomToken}")

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

    fun leave(promise: Promise<Any>) {
        LogX.i(TAG, "WhiteBoard disconnect")
        boardRoom.disconnect(promise)
    }

    private val whiteboardMixingBridgeListener = object : WhiteBoardAudioMixingBridgeListener {
        override fun onStartAudioMixing(filepath: String, loopback: Boolean, replace: Boolean, cycle: Int) {
            LogX.i(TAG, "onStartAudioMixing")

            val data = AgoraBoardAudioMixingRequestData(
                type = AgoraBoardAudioMixingRequestType.Start,
                filepath = filepath, loopback = loopback, replace = replace, cycle = cycle
            )
            broadcastAudioMixingRequest(data)
        }

        override fun onStopAudioMixing() {
            LogX.i(TAG, "onStopAudioMixing")

            val data = AgoraBoardAudioMixingRequestData(AgoraBoardAudioMixingRequestType.Stop)
            broadcastAudioMixingRequest(data)
        }

        override fun onSetAudioMixingPosition(position: Int) {
            LogX.i(TAG, "onSetAudioMixingPosition : $position")

            val data =
                AgoraBoardAudioMixingRequestData(AgoraBoardAudioMixingRequestType.SetPosition, position = position)
            broadcastAudioMixingRequest(data)
        }

        override fun pauseAudioMixing() {
            LogX.i(TAG, "pauseAudioMixing")

            val data = AgoraBoardAudioMixingRequestData(AgoraBoardAudioMixingRequestType.PAUSE)
            broadcastAudioMixingRequest(data)
        }

        override fun resumeAudioMixing() {
            LogX.i(TAG, "resumeAudioMixing")

            val data = AgoraBoardAudioMixingRequestData(AgoraBoardAudioMixingRequestType.RESUME)
            broadcastAudioMixingRequest(data)
        }

        private fun broadcastAudioMixingRequest(data: AgoraBoardAudioMixingRequestData) {
            val packet = AgoraBoardInteractionPacket(AgoraBoardInteractionSignal.BoardAudioMixingRequest, data)
            mixingBridgeListener?.invoke(packet)
        }
    }
}