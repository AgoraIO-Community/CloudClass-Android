package com.hyphenate.easeimgroup.modules.manager

import android.os.Process
import java.util.concurrent.ThreadFactory

class BackgroundThreadFactory(private val mThreadPriority: Int): ThreadFactory {
    override fun newThread(r: Runnable?): Thread {
        return Thread {
            Process.setThreadPriority(mThreadPriority)
            r?.run()
        }
    }
}