package io.agora.agoraeducore.core.internal.report.reporters

import android.util.Log
import com.google.gson.Gson
import io.agora.agoraeducore.core.internal.base.network.RetrofitManager
import io.agora.agoraeducore.core.internal.framework.data.EduCallback
import io.agora.agoraeducore.core.internal.framework.data.EduError
import io.agora.agoraeducore.core.internal.launch.AgoraEduSDK
import io.agora.agoraeducore.core.internal.report.Md5Util
import retrofit2.Call
import retrofit2.Response

abstract class AbstractReporter(
        val ctype: String,
        val platform: String,
        var version: String,
        val appId: String) {

    private val tag = "report"

    companion object {
        const val format = "src=%s&ts=%d"
        fun getSign(src: String, timestamp: Long): String {
            return Md5Util.toMD5String(String.format(format, src, timestamp))
        }
    }

    fun report(body: Any, callback: EduCallback<Void>?) {
        Log.d(tag, Gson().toJson(body))
        RetrofitManager.instance().getService(AgoraEduSDK.reportUrl(), ReportService::class.java).report(appId, body).enqueue(object : retrofit2.Callback<ReportResp> {
            override fun onResponse(call: Call<ReportResp>, response: Response<ReportResp>) {
                response.body()?.let {
                    callback?.onSuccess(null)
                }
            }

            override fun onFailure(call: Call<ReportResp>, t: Throwable) {
                callback?.onFailure(EduError(0, t.message?: ""))
            }
        })
    }
}

class ReportBody(
        val pts: List<ReportPoint>,
        val sign: String,
        val src: String,
        val ts: Long) {

    companion object {
        fun create(label: ReportLabel, values: ReportValues,
                 metric: String, src: String): ReportBody {
            val point = ReportPoint(metric, label, values)
            val timestamp = System.currentTimeMillis()
            val sign = AbstractReporter.getSign(src, timestamp)
            return ReportBody(mutableListOf(point), sign, src, timestamp)
        }

        fun create(label: ReportLabel, values: ReportValues,
                   metric: String, src: String, timestamp: Long): ReportBody {
            val point = ReportPoint(metric, label, values)
            val sign = AbstractReporter.getSign(src, timestamp)
            return ReportBody(mutableListOf(point), sign, src, timestamp)
        }
    }
}

class ReportPoint(
        val m: String,
        val ls: ReportLabel,
        val vs: ReportValues)

open class ReportLabel(
        val ctype: String,
        val platform: String,
        val version: String,
        val appId: String)

class RoomReportLabel(
        ctype: String,
        platform: String,
        version: String,
        appId: String,
        val event: String,
        val category: String,
        val api: String?,
        val result: String?,
        val errCode: String?,
        val httpCode: String?
) : ReportLabel(ctype, platform, version, appId)

class ReportValues(
        val count: Int?,
        val elapse: Int?)

class ReportResp()

enum class ReportMetric {
    OnlineUser, Event;

    override fun toString(): String {
        return when (this) {
            OnlineUser -> "online_user"
            Event -> "event"
        }
    }
}

enum class ReportSource {
    Apaas, Rte;

    override fun toString(): String {
        return when (this) {
            Apaas -> "apaas"
            Rte -> "rte"
        }
    }
}

enum class ReportCategory {
    Start, End, Rtc, Rtm, Board, Http;

    override fun toString(): String {
        return when (this) {
            Start -> "start"
            End -> "end"
            Rtc -> "rtc"
            Rtm -> "rtm"
            Board -> "board"
            Http -> "http"
        }
    }
}

enum class ReportEvent {
    Init, Join;

    override fun toString(): String {
        return when (this) {
            Init -> "init"
            Join -> "joinRoom"
        }
    }
}