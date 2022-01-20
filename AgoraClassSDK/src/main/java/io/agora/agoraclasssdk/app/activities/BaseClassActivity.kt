package io.agora.agoraclasssdk.app.activities

import io.agora.agoraeducore.core.internal.launch.AgoraEduLatencyLevel.AgoraEduLatencyLevelUltraLow
import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import io.agora.agoraclasssdk.R
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.AgoraEduCoreConfig
import io.agora.agoraeducore.core.AgoraEduCoreStateListener
import io.agora.agoraeducore.core.AgoraEduCoreStatics.selectImageResultCode
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.context.EduContextErrors.parameterErrCode
import io.agora.agoraeducore.core.internal.edu.classroom.OnNavigationStateListener
import io.agora.agoraeducore.core.internal.launch.AgoraEduLaunchConfig
import io.agora.agoraeducore.core.internal.util.AppUtil.getNavigationBarHeight
import io.agora.agoraeducore.core.internal.framework.data.EduError
import io.agora.agoraeducore.core.internal.education.impl.Constants.Companion.AgoraLog
import io.agora.agoraeducore.core.internal.framework.data.EduUserRole
import io.agora.agoraeducore.core.internal.framework.impl.handler.MediaHandler
import io.agora.agoraeducore.core.internal.server.struct.response.RoomPreCheckRes
import io.agora.agoraeduuikit.impl.container.AgoraUIDeviceSetting
import io.agora.agoraeduuikit.interfaces.protocols.IAgoraUIContainer
import java.lang.ref.WeakReference
import io.agora.agoraeducore.core.context.AgoraEduContextDeviceState2.Open

abstract class BaseClassActivity : AppCompatActivity() {
    private companion object {
        const val tag = "BaseClassActivity"
        const val launchConfig = "LAUNCHCONFIG"
        const val preCheckData = "PRECHECKDATA"
    }

    protected var launchConfig: AgoraEduLaunchConfig? = null
    private var preCheckData: RoomPreCheckRes? = null

    protected var activityLayout: RelativeLayout? = null
    protected var contentLayout: RelativeLayout? = null
    protected var container: IAgoraUIContainer? = null
    private var screenOffReceiver: ScreenOffBroadcastReceiver? = null
    private var deviceLifeCycleManager: MediaDeviceLifeCycleManager? = null

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

    override fun onResume() {
        super.onResume()
        AgoraLog.e("$tag -> onResume")
    }

    override fun onStop() {
        super.onStop()
        AgoraLog.e("$tag -> onStop")
    }

    protected abstract fun onContentViewLayout(): RelativeLayout

    protected open fun onCreateClassJoinMediaOption(): ClassJoinMediaOption {
        // By default, students do not publish media streams.
        // All roles will automatically subscribe any streams that
        // exist in the room
        return ClassJoinMediaOption(true,
            launchConfig?.roleType != EduUserRole.STUDENT.value)
    }

    private fun initData() {
        launchConfig = intent.getParcelableExtra(Companion.launchConfig)
        preCheckData = intent.getParcelableExtra(Companion.preCheckData)
        registerScreenOffReceiver()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == selectImageResultCode && resultCode == RESULT_OK) {
            // result of select image
            val intent = Intent()
            intent.action = packageName.plus(resources.getString(R.string.chat_window_select_image_action))
            intent.putExtra(resources.getString(R.string.chat_window_select_image_key), data?.data)
            sendBroadcast(intent)
        }
    }

    override fun onBackPressed() {
        container?.showLeave()
    }

    protected fun createEduCore(callback: EduContextCallback<Unit>) {
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

        val mediaOption = onCreateClassJoinMediaOption()

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
                videoEncoderConfig = launchConfig!!.videoEncoderConfig,
                agoraEduStreamState = launchConfig!!.agoraEduStreamState,
                latencyLevel = launchConfig!!.latencyLevel ?: AgoraEduLatencyLevelUltraLow,
                eyeCare = launchConfig!!.eyeCare,
                vendorId = launchConfig!!.vendorId,
                logDir = launchConfig!!.logDir,
                autoSubscribe = mediaOption.autoSubscribe,
                autoPublish = mediaOption.autoPublish,
                needUserListener = true),
            object : AgoraEduCoreStateListener {
                override fun onCreated() {
                    // init mediaDeviceLifeCycleManager
                    deviceLifeCycleManager = MediaDeviceLifeCycleManager(this@BaseClassActivity,
                        eduCore()?.eduContextPool())
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
            var b = 0
            if (insets != null) {
                val l = insets.systemWindowInsetLeft
                val r = insets.systemWindowInsetRight
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
        screenOffReceiver = ScreenOffBroadcastReceiver(this)
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, filter)
    }

    private fun unregisterScreenOffReceiver() {
        screenOffReceiver?.let {
            unregisterReceiver(it)
        }
    }

    /**
     * Default setting of system devices as below:
     * - system speaker is turned on
     * - system camera is turned on for teachers, and turned off for students
     * - system microphone is turned on for teachers, and turned off for students
     * Note, this method is valid to use only after joining the classroom
     * successfully.
     */
    protected fun initSystemDevices() {
        eduCore()?.eduContextPool()?.mediaContext()
            ?.openSystemDevice(AgoraEduContextSystemDevice.Speaker)

        eduCore()?.eduContextPool()?.userContext()?.getLocalUserInfo()?.let {
            if (it.role == AgoraEduContextUserRole.Teacher) {
                eduCore()?.eduContextPool()?.mediaContext()?.openSystemDevice(
                    if (AgoraUIDeviceSetting.isFrontCamera()) {
                        AgoraEduContextSystemDevice.CameraFront
                    } else {
                        AgoraEduContextSystemDevice.CameraBack
                    })
                eduCore()?.eduContextPool()?.mediaContext()
                    ?.openSystemDevice(AgoraEduContextSystemDevice.Microphone)
            } else {
                eduCore()?.eduContextPool()?.mediaContext()
                    ?.closeSystemDevice(AgoraEduContextSystemDevice.CameraFront)
                eduCore()?.eduContextPool()?.mediaContext()
                    ?.closeSystemDevice(AgoraEduContextSystemDevice.CameraBack)
                eduCore()?.eduContextPool()?.mediaContext()
                    ?.closeSystemDevice(AgoraEduContextSystemDevice.Microphone)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        deviceLifeCycleManager?.dispose()
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

data class ClassJoinMediaOption(
    val autoSubscribe: Boolean,
    val autoPublish: Boolean
)

/**
 * if screenOff long time, app will be recycled, when screenOn by user, app must crash because of rtmClient
 * is released(or some property has not been initialized), so quit activity when screen off to solve this bug.
 * */
class ScreenOffBroadcastReceiver(activity: Activity) : BroadcastReceiver() {
    private val tag = "ScreenOffBroadcastReceiver"
    private val weakContext = WeakReference(activity)

    companion object {
        var autoHandleScreenOffEvent = true
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_SCREEN_OFF && autoHandleScreenOffEvent) {
            AgoraLog.d("$tag->onReceive:auto handle ACTION_SCREEN_OFF event")
            weakContext.get()?.let {
                if (!it.isFinishing && !it.isDestroyed) {
                    weakContext.clear()
                    it.finish()
                }
            }
        }
    }
}

/**
 *
 * */
class MediaDeviceLifeCycleManager(activity: Activity, eduContextPool: EduContextPool?) {
    private val tag = "DeviceLifeCycleManager"
    private var application: Application? = activity.application
    private val timeout = 10L * 1000L
    private var handler: Handler? = Handler(Looper.getMainLooper())
    private var cameraStateRestoreValve = false
    private var micStateRestoreValve = false
    private var lastCameraDeviceState: AgoraEduContextDeviceState2? = null
    private var lastCameraDeviceInfo: AgoraEduContextDeviceInfo? = null
    private var lastMicDeviceState: AgoraEduContextDeviceState2? = null
    private var lastMicDeviceInfo: AgoraEduContextDeviceInfo? = null
    private val mediaHandler = object : MediaHandler() {
        override fun onLocalDeviceStateUpdated(deviceInfo: AgoraEduContextDeviceInfo, state: AgoraEduContextDeviceState2) {
            super.onLocalDeviceStateUpdated(deviceInfo, state)
            if (deviceInfo.isCamera()) {
                lastCameraDeviceInfo = deviceInfo
            } else if (deviceInfo.isMic()) {
                lastMicDeviceInfo = deviceInfo
            }
        }
    }
    private val disableCameraRunnable = Runnable {
        lastCameraDeviceInfo?.let {
            cameraStateRestoreValve = true
            eduContextPool?.mediaContext()?.closeLocalDevice(it)
        }
    }
    private val disableMicRunnable = Runnable {
        lastMicDeviceInfo?.let {
            micStateRestoreValve = true
            eduContextPool?.mediaContext()?.closeLocalDevice(it)
        }
    }
    private val callback = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        }

        override fun onActivityStarted(activity: Activity) {
        }

        override fun onActivityResumed(activity: Activity) {
        }

        override fun onActivityPostResumed(activity: Activity) {
            super.onActivityPostResumed(activity)
            if (activity is BaseClassActivity) {
                AgoraLog.i("$tag->onActivityPostResumed-activity:${activity.componentName.className}")
                // restore state
                handler?.removeCallbacks(disableCameraRunnable)
                handler?.removeCallbacks(disableMicRunnable)
                if (cameraStateRestoreValve) {
                    cameraStateRestoreValve = false
                    if (lastCameraDeviceState == Open && lastCameraDeviceInfo != null) {
                        eduContextPool?.mediaContext()?.openLocalDevice(lastCameraDeviceInfo!!)
                    }
                }
                if (micStateRestoreValve) {
                    micStateRestoreValve = false
                    if (lastMicDeviceState == Open && lastMicDeviceInfo != null) {
                        eduContextPool?.mediaContext()?.openLocalDevice(lastMicDeviceInfo!!)
                    }
                }
            }
        }

        override fun onActivityPaused(activity: Activity) {
        }

        override fun onActivityStopped(activity: Activity) {
        }

        override fun onActivityPostStopped(activity: Activity) {
            super.onActivityPostStopped(activity)
            if (activity is BaseClassActivity) {
                AgoraLog.i("$tag->onActivityPostResumed-activity:${activity.componentName.className}")
                // disable device
                lastCameraDeviceInfo?.let {
                    eduContextPool?.mediaContext()?.getLocalDeviceState(it,
                        object : EduContextCallback<AgoraEduContextDeviceState2> {
                            override fun onSuccess(target: AgoraEduContextDeviceState2?) {
                                target?.let { state ->
                                    lastCameraDeviceState = state
                                    handler?.postDelayed(disableCameraRunnable, timeout)
                                }
                            }

                            override fun onFailure(error: EduContextError?) {
                            }
                        })
                }
                lastMicDeviceInfo?.let {
                    eduContextPool?.mediaContext()?.getLocalDeviceState(it,
                        object : EduContextCallback<AgoraEduContextDeviceState2> {
                            override fun onSuccess(target: AgoraEduContextDeviceState2?) {
                                target?.let { state ->
                                    lastMicDeviceState = state
                                    handler?.postDelayed(disableMicRunnable, timeout)
                                }
                            }

                            override fun onFailure(error: EduContextError?) {
                            }
                        })
                }
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        }

        override fun onActivityDestroyed(activity: Activity) {
        }

    }

    init {
        eduContextPool?.mediaContext()?.addHandler(mediaHandler)
        application?.registerActivityLifecycleCallbacks(callback)
    }

    fun dispose() {
        application?.unregisterActivityLifecycleCallbacks(callback)
        application = null
        handler?.removeCallbacks(disableCameraRunnable)
        handler?.removeCallbacks(disableMicRunnable)
        handler = null
    }
}
