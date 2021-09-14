package io.agora.edu.sdk.app.activities

import io.agora.edu.core.internal.launch.AgoraEduLatencyLevel.AgoraEduLatencyLevelUltraLow
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.RelativeLayout
import androidx.core.view.ViewCompat
import io.agora.edu.core.AgoraEduCore
import io.agora.edu.core.AgoraEduCoreConfig
import io.agora.edu.core.AgoraEduCoreStateListener
import io.agora.edu.core.context.EduContextCallback
import io.agora.edu.core.context.EduContextError
import io.agora.edu.core.context.EduContextErrors.parameterErrCode
import io.agora.edu.core.internal.edu.classroom.OnNavigationStateListener
import io.agora.edu.core.internal.launch.AgoraEduLaunchConfig
import io.agora.edu.core.internal.util.AppUtil.getNavigationBarHeight
import io.agora.edu.core.internal.widget.EyeProtection
import io.agora.edu.core.internal.framework.data.EduError
import io.agora.edu.core.internal.education.api.room.data.EduRoomState
import io.agora.edu.core.internal.education.impl.Constants.Companion.AgoraLog
import io.agora.edu.core.internal.server.struct.response.RoomPreCheckRes
import io.agora.edu.uikit.interfaces.protocols.IAgoraUIContainer

abstract class BaseClassActivity : BaseActivity() {
    private companion object {
        const val tag = "BaseClassActivity"
        const val launchConfig = "LAUNCHCONFIG"
        const val preCheckData = "PRECHECKDATA"
        const val resultCode = 808
    }

    protected var launchConfig: AgoraEduLaunchConfig? = null
    private var preCheckData: RoomPreCheckRes? = null
    protected var contentLayout: RelativeLayout? = null
    protected var container: IAgoraUIContainer? = null

    private var eduCore: AgoraEduCore? = null

    protected fun eduCore(): AgoraEduCore? {
        return eduCore
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        val view = onContentViewLayout()
        view.fitsSystemWindows = true
        setContentView(view)
        initData()
    }

    protected abstract fun onContentViewLayout(): RelativeLayout

    private fun initData() {
        launchConfig = intent.getParcelableExtra(Companion.launchConfig)
        preCheckData = intent.getParcelableExtra(Companion.preCheckData)
    }

    override fun onStart() {
        super.onStart()
        launchConfig?.let { EyeProtection.setNeedShow(it.eyeCare == 1) }
    }

    override fun onBackPressed() {
        container?.showLeave()
    }

    protected fun createEduCore(autoSubscribe: Boolean, autoPublish: Boolean, callback: EduContextCallback<Unit>) {
        if (launchConfig == null) {
            val msg = "init room fail, launch config is null"
            AgoraLog.e("$tag -> $msg")
            callback.onFailure(EduContextError(parameterErrCode, msg))
            return
        }

        if (preCheckData == null) {
            val msg = "init room fail, precheck data is null"
            AgoraLog.e("$tag -> $msg")
            callback.onFailure(EduContextError(parameterErrCode, msg))
            return
        }

        eduCore = AgoraEduCore(this@BaseClassActivity.applicationContext,
                AgoraEduCoreConfig(
                        appId = launchConfig!!.appId,
                        userName = launchConfig!!.userName,
                        userUuid = launchConfig!!.userUuid,
                        roomName = launchConfig!!.roomName,
                        roomUuid = launchConfig!!.roomUuid,
                        roleType = launchConfig!!.roleType,
                        roomType = launchConfig!!.roomType,
                        rtmToken = launchConfig!!.rtmToken,
                        startTime = launchConfig!!.startTime ?: 0,
                        duration = launchConfig!!.duration ?: 0,
                        rtcRegion = preCheckData!!.rtcRegion ?: "",
                        rtmRegion = preCheckData!!.rtmRegion ?: "",
                        mediaOptions = launchConfig!!.mediaOptions,
                        userProperties = launchConfig!!.userProperties,
                        widgetConfigs = launchConfig!!.widgetConfigs,
                        state = EduRoomState.INIT.value,
                        closeDelay = preCheckData!!.closeDelay,
                        lastMessageId = preCheckData!!.lastMessageId,
                        muteChat = preCheckData!!.muteChat,
                        boardAppId = preCheckData!!.board?.boardAppId ?: "",
                        boardId = preCheckData!!.board?.boardId ?: "",
                        boardToken = preCheckData!!.board?.boardToken ?: "",
                        boardRegion = preCheckData!!.board?.boardRegion ?: "",
                        videoEncoderConfig = launchConfig!!.videoEncoderConfig,
                        streamState = launchConfig!!.streamState,
                        boardFitMode = launchConfig!!.boardFitMode,
                        latencyLevel = launchConfig!!.latencyLevel ?: AgoraEduLatencyLevelUltraLow,
                        eyeCare = launchConfig!!.eyeCare,
                        vendorId = launchConfig!!.vendorId,
                        logDir = launchConfig!!.logDir,
                        autoSubscribe = autoSubscribe,
                        autoPublish = autoPublish,
                        needUserListener = true),
                object : AgoraEduCoreStateListener {
                    override fun onCreated() {
                        callback.onSuccess(Unit)
                    }

                    override fun onError(error: EduError) {
                        AgoraLog.e("$tag -> ${error.msg}")
                        callback.onFailure(EduContextError(error.type, error.msg))
                    }
                }
        )
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

    private fun isNavigationBarChanged(activity: Activity,
                                       onNavigationStateListener: OnNavigationStateListener) {
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
            if (b <= height) {
                onNavigationStateListener.onNavigationState(isShowing, b)
            }
            ViewCompat.onApplyWindowInsets(v, insets)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        eduCore()?.release()
        container?.release()
    }
}

class ClassJoinStateCountDown {
    private val max = 2
    private var current = max

    @Synchronized
    fun countdownRoomInit() {
        current--
    }

    @Synchronized
    fun countdownUiReady() {
        current--
    }

    @Synchronized
    fun isDone(): Boolean {
        return current == 0
    }
}