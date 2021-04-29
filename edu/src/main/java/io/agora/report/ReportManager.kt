package io.agora.report

import android.util.Log
import androidx.annotation.NonNull
import io.agora.edu.BuildConfig
import io.agora.report.reporters.*

object ReportManager {
    private val tag = ReportManager::javaClass.name

    private var ctype: String? = null
    private var platform: String? = null
    private var appId: String? = null
    private var init: Boolean = false

    private val reporterMap = mutableMapOf<String, AbstractReporter>()
    private var heartbeat: HeartbeatReporter? = null

    var joinInfo: RoomJoinInfo? = null

    fun init(@NonNull ctype: String, @NonNull platform: String, @NonNull appId: String) {
        synchronized(this) {
            if (!init) {
                this.ctype = ctype
                this.platform = platform
                this.appId = appId

                heartbeat = HeartbeatReporter(ctype, platform,
                        BuildConfig.SDK_VERSION, appId,
                        ReportSource.Rte.toString(),
                        ReportMetric.OnlineUser.toString())
                init = true
            } else {
                Log.i(tag, "report manager has been initialized")
            }
        }
    }

    private fun checkInit(): Boolean {
        return ctype != null && platform != null && appId != null && init
    }

    fun setJoinRoomInfo(rid: String, uid:String, sid: String) {
        joinInfo = RoomJoinInfo(rid, uid, sid)
    }

    fun clearJoinRoomInfo() {
        joinInfo = null
    }

    private fun getReporter(src: String): AbstractReporter? {
        if (!checkInit()) {
            Log.w(tag, "Do you forget to initialize report manager?")
            return null
        }

        return if (reporterMap.containsKey(src) && reporterMap[src] != null) {
            reporterMap[src]
        } else {
            val reporter = when (src) {
                ReportSource.Rte.toString() -> {
                    RteReporter(ctype!!, platform!!, BuildConfig.SDK_VERSION, appId!!, src)
                }
                ReportSource.Apaas.toString() -> {
                    APaasReporter(ctype!!, platform!!, BuildConfig.APAAS_VERSION, appId!!, src)
                }
                else -> null
            }

            reporter?.let { reporterMap[src] = reporter }
            reporter
        }
    }

    fun getAPaasReporter(): APaasReporter {
        return getReporter(ReportSource.Apaas.toString()) as APaasReporter
    }

    fun getRteReporter(): RteReporter {
        return getReporter(ReportSource.Rte.toString()) as RteReporter
    }

    fun getReporterBySource(src: String): AbstractReporter? {
        return reporterMap[src]
    }

    fun removeReporter(src: String) {
        reporterMap.remove(src)
    }

    fun getHeartbeat(): HeartbeatReporter? {
        return heartbeat
    }
}

data class RoomJoinInfo(
        val rid: String,
        val uid: String,
        val sid: String)