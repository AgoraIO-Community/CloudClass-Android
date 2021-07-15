package io.agora.report.reporters

import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class HeartbeatReporter(ctype: String, platform: String,
                        version: String, appId: String,
                        private val src: String,
                        private val metric: String) : AbstractReporter(ctype,
        platform, version, appId) {

    private val tag: String = HeartbeatReporter::javaClass.name
    private var beatInterval = 10000

    private var executor: ScheduledExecutorService? = null
    private var started: Boolean = false
    private var roomId: String? = null
    private var uid: String? = null

    private val heartbeatRunnable = Runnable {
        report(getHeartbeatBody(), null)
    }

    private fun getHeartbeatBody(): ReportBody {
        val label = HeartbeatLabel(ctype, platform,
                version, appId)
        val value = ReportValues(1, null)
        return ReportBody.create(label, value, metric, src)
    }

    @Synchronized
    private fun heartbeatStarted(): Boolean {
        return started
    }

    @Synchronized
    private fun setHeartbeatStarted(started: Boolean) {
        this.started = started
    }

    fun startHeartbeat(roomId: String, uid: String) {
        synchronized(this) {
            if (executor == null || executor?.isShutdown == true) {
                executor = Executors.newScheduledThreadPool(1)
            }
        }

        executor?.let {
            if (!heartbeatStarted()) {
                this.roomId = roomId
                this.uid = uid
                it.scheduleAtFixedRate(heartbeatRunnable, 0, beatInterval.toLong(), TimeUnit.MILLISECONDS)
                setHeartbeatStarted(true)
            } else if (heartbeatStarted()) {
                Log.i(tag, "heartbeat has already started")
            }
        }
    }

    fun stopHeartbeat() {
        synchronized(this) {
            if (executor == null) {
                Log.i(tag, "heartbeat has not started yet")
                return
            }

            if (executor?.isShutdown == true) {
                Log.i(tag, "heartbeat has already been stopped")
                return
            }
        }

        executor?.let {
            it.shutdownNow()
            setHeartbeatStarted(false)
            Log.i(tag, "heartbeat is stopped correctly")
        }
    }

    fun setInterval(interval: Int) {
        this.beatInterval = interval
        if (heartbeatStarted()) {
            Log.d(tag, "new interval will not take effect until next time the heartbeat restarts")
        }
    }
}

class HeartbeatLabel(
        ctype: String,
        platform: String,
        version: String,
        appId: String
) : ReportLabel(ctype, platform, version, appId)