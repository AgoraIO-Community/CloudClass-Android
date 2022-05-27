package io.agora.classroom.common

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.agora.agoraeducore.core.internal.education.impl.Constants
import java.lang.ref.WeakReference

class ScreenOffBroadcastReceiver(activity: Activity) : BroadcastReceiver() {
    private val TAG = "ScreenOffBroadcastReceiver"
    private val weakContext = WeakReference(activity)

    companion object {
        var autoHandleScreenOffEvent = true
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_SCREEN_OFF && autoHandleScreenOffEvent) {
            Constants.AgoraLog?.d("$TAG->onReceive:auto handle ACTION_SCREEN_OFF event")
            weakContext.get()?.let {
                if (!it.isFinishing && !it.isDestroyed) {
                    weakContext.clear()
                    it.finish()
                }
            }
        }
    }
}