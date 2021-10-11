package io.agora.agoraeducore.sdk.app.activities

import io.agora.agoraeducore.core.internal.launch.AgoraEduLatencyLevel.AgoraEduLatencyLevelUltraLow
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.AgoraEduCoreConfig
import io.agora.agoraeducore.core.AgoraEduCoreStateListener
import io.agora.agoraeducontext.EduContextCallback
import io.agora.agoraeducontext.EduContextDeviceLifecycle
import io.agora.agoraeducontext.EduContextError
import io.agora.agoraeducontext.EduContextErrors.parameterErrCode
import io.agora.agoraeducore.core.context.EduContextPool
import io.agora.agoraeducore.core.internal.edu.classroom.OnNavigationStateListener
import io.agora.agoraeducore.core.internal.launch.AgoraEduLaunchConfig
import io.agora.agoraeducore.core.internal.util.AppUtil.getNavigationBarHeight
import io.agora.agoraeducore.core.internal.framework.data.EduError
import io.agora.agoraeducore.core.internal.education.api.room.data.EduRoomState
import io.agora.agoraeducore.core.internal.education.impl.Constants.Companion.AgoraLog
import io.agora.agoraeducore.core.internal.server.struct.response.RoomPreCheckRes
import io.agora.agoraeduuikit.interfaces.protocols.IAgoraUIContainer
import java.lang.ref.WeakReference

abstract class BaseClassActivity : AppCompatActivity() {
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
    private var screenOffReceiver: ScreenOffBroadcastReceiver? = null

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
                        startTime = preCheckData!!.startTime,
                        duration = preCheckData!!.duration,
                        rtcRegion = preCheckData!!.rtcRegion ?: "",
                        rtmRegion = preCheckData!!.rtmRegion ?: "",
                        mediaOptions = launchConfig!!.mediaOptions,
                        userProperties = launchConfig!!.userProperties,
                        widgetConfigs = launchConfig!!.widgetConfigs,
                        state = preCheckData!!.state,
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
                        vendorId = launchConfig!!.vendorId,
                        logDir = launchConfig!!.logDir,
                        autoSubscribe = autoSubscribe,
                        autoPublish = autoPublish,
                        needUserListener = true),
                object : AgoraEduCoreStateListener {
                    override fun onCreated() {
                        // receiver need eduContextPool, so, register here.
                        registerScreenOffReceiver()
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

    private fun registerScreenOffReceiver() {
        screenOffReceiver = ScreenOffBroadcastReceiver(this, eduCore()?.eduContextPool())
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, filter)
    }

    private fun unregisterScreenOffReceiver() {
        screenOffReceiver?.let {
            unregisterReceiver(it)
        }
    }

    override fun onStop() {
        super.onStop()
        AgoraLog.d("$tag->onReceive:onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        eduCore()?.release()
        container?.release()
        unregisterScreenOffReceiver()
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

/**
 * if screenOff long time, app will be recycled, when screenOn by user, app must crash because of rtmClient
 * is released(or some property has not been initialized), so quit activity when screen off to solve this bug.
 * */
class ScreenOffBroadcastReceiver(context: Context, eduContextPool: EduContextPool?) : BroadcastReceiver() {
    private val tag = "ScreenOffBroadcastReceiver"
    private val weakContext = WeakReference(context)
    private val weakEduContextPool = WeakReference(eduContextPool)

    companion object {
        var autoHandleScreenOffEvent = true
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_SCREEN_OFF && autoHandleScreenOffEvent) {
            AgoraLog.d("$tag->onReceive:auto handle ACTION_SCREEN_OFF event")
            weakContext.get()?.let {
                // restore deviceState before finish(APAAS-1242)
                weakEduContextPool.get()?.deviceContext()?.setDeviceLifecycle(EduContextDeviceLifecycle.Resume)
                val activity = context as? Activity
                if (activity?.isFinishing == false && !activity.isDestroyed) {
                    weakContext.clear()
                    activity.finish()
                }
            }
        }
    }
}