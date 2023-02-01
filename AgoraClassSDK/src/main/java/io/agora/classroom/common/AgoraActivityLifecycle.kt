package io.agora.classroom.common

import android.app.Activity
import android.app.Application
import android.os.Bundle
import io.agora.agoraeducore.core.internal.log.LogX

/**
 * author : hefeng
 * date : 2022/4/17
 * description :
 */
class AgoraActivityLifecycle : Application.ActivityLifecycleCallbacks {
    val TAG = "AgoraActivityLifecycle"

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        LogX.e(TAG, "-> onCreate:$activity")
    }

    override fun onActivityStarted(activity: Activity) {
        LogX.e(TAG, "-> onStart:$activity")
    }

    override fun onActivityResumed(activity: Activity) {
        LogX.e(TAG, "-> onResume:$activity")
    }

    override fun onActivityPaused(activity: Activity) {
        LogX.e(TAG, "-> onPause:$activity")
    }

    override fun onActivityStopped(activity: Activity) {
        LogX.e(TAG, "-> onStop:$activity")
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        LogX.e(TAG, "-> onSaveInstanceState:$activity")
    }

    override fun onActivityDestroyed(activity: Activity) {
        LogX.e(TAG, "-> onDestroy:$activity")
    }
}