package io.agora.online.sdk.presenter

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.framework.impl.handler.MediaHandler
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.online.sdk.common.AgoraEduClassActivity

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
        override fun onLocalDeviceStateUpdated(
            deviceInfo: AgoraEduContextDeviceInfo,
            state: AgoraEduContextDeviceState2
        ) {
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
            if (activity is AgoraEduClassActivity) {
                LogX.d(tag, "onActivityPostResumed-activity:${activity.componentName.className}")
                // restore state
                handler?.removeCallbacks(disableCameraRunnable)
                handler?.removeCallbacks(disableMicRunnable)
                if (cameraStateRestoreValve) {
                    cameraStateRestoreValve = false
                    if (lastCameraDeviceState == AgoraEduContextDeviceState2.Open && lastCameraDeviceInfo != null) {
                        eduContextPool?.mediaContext()?.openLocalDevice(lastCameraDeviceInfo!!)
                    }
                }
                if (micStateRestoreValve) {
                    micStateRestoreValve = false
                    if (lastMicDeviceState == AgoraEduContextDeviceState2.Open && lastMicDeviceInfo != null) {
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
            if (activity is AgoraEduClassActivity) {
                LogX.d(tag, "onActivityPostResumed-activity:${activity.componentName.className}")
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