package io.agora.report.reporters

import android.util.Log
import io.agora.education.api.EduCallback
import io.agora.report.ReportManager

class APaasReporter(ctype: String, platform: String,
                    version: String, appId: String,
                    private val src: String) : AbstractReporter(ctype, platform, version, appId) {
    private val tag: String = APaasReporter::javaClass.name

    private var joinStartTimestamp: Long = 0
    private var preCheckStartTimestamp: Long = 0
    private var whiteBoardStartTimestamp: Long = 0

    private fun resetJoinRoomReport() {
        joinStartTimestamp = System.currentTimeMillis()
    }

    fun reportRoomEntryStart(callback: EduCallback<Void>?) {
        resetJoinRoomReport()
        ReportManager.joinInfo?.let {
            reportRoomEntryStart(it.rid, it.uid, it.sid, callback)
            return
        }

        Log.i(tag, "Do you forget to reset the join room info?")
    }

    private fun reportRoomEntryStart(rid: String, uid: String, sid: String, callback: EduCallback<Void>?) {
        val label = RoomReportLabel(ctype, platform, version, appId, ReportEvent.Join.toString(),
                ReportCategory.Start.toString(),
                null, null, null, null)
        val values = ReportValues(1, null)
        val body = ReportBody.create(label, values, ReportMetric.Event.toString(), src)
        report(body, callback)
    }

    fun reportPreCheckStart() {
        preCheckStartTimestamp = System.currentTimeMillis()
    }

    fun reportPreCheckResult(result: String, errCode: String?, httpCode: String?, callback: EduCallback<Void>?) {
        ReportManager.joinInfo?.let {
            reportPreCheckResult(it.rid, it.uid, it.sid, result, errCode, httpCode,
                    (System.currentTimeMillis() - preCheckStartTimestamp).toInt(), callback)
        }
    }

    private fun reportPreCheckResult(rid: String, uid: String, sid: String, result: String,
                             errCode: String?, httpCode: String?, elapse: Int, callback: EduCallback<Void>?) {
        val label = RoomReportLabel(ctype, platform, version, appId, ReportEvent.Join.toString(),
                ReportCategory.Http.toString(),
                "preflight", result, errCode, httpCode)
        val values = ReportValues(null, elapse)
        val body = ReportBody.create(label, values, ReportMetric.Event.toString(), src)
        report(body, callback)
    }

    fun reportWhiteBoardStart() {
        whiteBoardStartTimestamp = System.currentTimeMillis()
    }

    fun reportWhiteBoardResult(result: String, errCode: String?, callback: EduCallback<Void>?) {
        ReportManager.joinInfo?.let {
            reportWhiteBoardResult(it.rid, it.uid, it.sid, result, errCode, callback)
            return
        }
        Log.i(tag, "Do you forget to reset the join room info?")
    }

    private fun reportWhiteBoardResult(rid: String, uid: String, sid: String, result: String,
                               errCode: String?, callback: EduCallback<Void>?) {
        val label = RoomReportLabel(ctype, platform, version, appId, ReportEvent.Join.toString(),
                ReportCategory.Board.toString(),
                "join", result, errCode, null)
        val timestamp = System.currentTimeMillis()
        val values = ReportValues(null,
                (timestamp - whiteBoardStartTimestamp).toInt())
        val body = ReportBody.create(label, values,
                ReportMetric.Event.toString(), src, timestamp)
        report(body, callback)
    }

    fun reportRoomEntryEnd(result: String, errCode: String?, httpCode: String?, callback: EduCallback<Void>?) {
        ReportManager.joinInfo?.let {
            reportRoomEntryEnd(it.rid, it.uid, it.sid, result, errCode, httpCode, callback)
            return
        }
        Log.i(tag, "Do you forget to reset the join room info?")
    }

    private fun reportRoomEntryEnd(rid: String, uid: String, sid: String, result: String,
                           errCode: String?, httpCode: String?, callback: EduCallback<Void>?) {
        val label = RoomReportLabel(ctype, platform, version, appId, ReportEvent.Join.toString(),
                ReportCategory.End.toString(),
                null, result, errCode, null)
        val timestamp = System.currentTimeMillis()
        val values = ReportValues(null, (timestamp - joinStartTimestamp).toInt())
        val body = ReportBody.create(label, values, ReportMetric.Event.toString(), src, timestamp)
        report(body, callback)
    }
}