package io.agora.online.sdk.common

import android.app.Activity
import android.app.Application
import android.os.Bundle
import io.agora.agoraeducore.core.internal.log.LogX

/**
 * author : felix
 * date : 2022/4/17
 * description :
 */
class AgoraActivityLifecycle : Application.ActivityLifecycleCallbacks {
    val TAG = "AgoraActivityLifecycle"

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        LogX.d(TAG, "-> onCreate:$activity")
    }

    override fun onActivityStarted(activity: Activity) {
        LogX.d(TAG, "-> onStart:$activity")
    }

    override fun onActivityResumed(activity: Activity) {
        LogX.d(TAG, "-> onResume:$activity")
    }

    override fun onActivityPaused(activity: Activity) {
        LogX.d(TAG, "-> onPause:$activity")
    }

    override fun onActivityStopped(activity: Activity) {
        LogX.d(TAG, "-> onStop:$activity")
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        LogX.d(TAG, "-> onSaveInstanceState:$activity")
    }

    override fun onActivityDestroyed(activity: Activity) {
        LogX.d(TAG, "-> onDestroy:$activity")
    }
}