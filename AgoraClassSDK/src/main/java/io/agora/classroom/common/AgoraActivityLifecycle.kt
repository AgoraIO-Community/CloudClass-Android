package io.agora.classroom.common

import android.app.Activity
import android.app.Application
import android.os.Bundle
import io.agora.agoraeducore.core.internal.education.impl.Constants

/**
 * author : hefeng
 * date : 2022/4/17
 * description :
 */
class AgoraActivityLifecycle : Application.ActivityLifecycleCallbacks {
    val TAG = "BaseActivity"

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Constants.AgoraLog?.e("${TAG} -> onCreate:$activity")
    }

    override fun onActivityStarted(activity: Activity) {
        Constants.AgoraLog?.e("${TAG} -> onStart:$activity")
    }

    override fun onActivityResumed(activity: Activity) {
        Constants.AgoraLog?.e("${TAG} -> onResume:$activity")
    }

    override fun onActivityPaused(activity: Activity) {
        Constants.AgoraLog?.e("${TAG} -> onPause:$activity")
    }

    override fun onActivityStopped(activity: Activity) {
        Constants.AgoraLog?.e("${TAG} -> onStop:$activity")
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        Constants.AgoraLog?.e("${TAG} -> onSaveInstanceState:$activity")
    }

    override fun onActivityDestroyed(activity: Activity) {
        Constants.AgoraLog?.e("${TAG} -> onDestroy:$activity")
    }
}