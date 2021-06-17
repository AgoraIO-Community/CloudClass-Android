package io.agora.report.reporters

import android.util.Log
import io.agora.education.api.EduCallback
import io.agora.report.ReportManager

class RteReporter(ctype: String, platform: String,
                  version: String, appId: String,
                  private val src: String) : AbstractReporter(
        ctype, platform, version, appId) {
    private val tag: String = RteReporter::javaClass.name

    private var rtmLoginStartTime: Long = 0
    private var joinStartTimestamp: Long = 0
    private var entryApiCallTimestamp: Long = 0
    private var rtmJoinStart: Long = 0
    private var rtcJoinStart: Long = 0

    fun reportRtmLoginStart() {
        rtmLoginStartTime = System.currentTimeMillis()
    }

    fun reportRtmLoginResult(result: String, errCode: String?, callback: EduCallback<Void>?) {
        ReportManager.joinInfo?.let {
            reportRtmLoginResult(it.uid, it.sid, result, errCode, callback)
            return
        }

        Log.i(tag, "Do you forget to reset the join room info?")
    }

    private fun reportRtmLoginResult(uid: String, sid: String, result: String, errCode: String?,
                       callback: EduCallback<Void>?) {
        val label = RoomReportLabel(ctype, platform, version, appId, ReportEvent.Init.toString(),
                ReportCategory.Rtm.toString(), "login", result, errCode, null)
        val timestamp = System.currentTimeMillis()
        val value = ReportValues(null,
                (timestamp - rtmLoginStartTime).toInt())
        val body = ReportBody.create(label, value,
                ReportMetric.Event.toString(), src, timestamp)
        report(body, callback)
    }

    fun reportJoinRoomStart() {
        ReportManager.joinInfo?.let {
            joinStartTimestamp = System.currentTimeMillis()
            reportJoinRoomStart(it.rid, it.uid, it.sid, null)
            return
        }
        Log.i(tag, "Do you forget to reset the join room info?")
    }

    private fun reportJoinRoomStart(rid: String, uid: String, sid: String, callback: EduCallback<Void>?) {
        val label = RoomReportLabel(ctype, platform, version, appId,ReportEvent.Join.toString(),
                ReportCategory.Start.toString(), null, null, null, null)
        val values = ReportValues(1, null)
        val body = ReportBody.create(label, values,
                ReportMetric.Event.toString(), src)
        report(body, callback)
    }

    fun reportRoomEntryApiStart() {
        entryApiCallTimestamp = System.currentTimeMillis()
    }

    fun reportRoomEntryApiResult(result: String, errCode: String?,
                                 httpCode: String?, callback: EduCallback<Void>?) {
        ReportManager.joinInfo?.let {
            reportRoomEntryEnd(it.rid, it.uid, it.sid,
                    result, errCode, httpCode, callback)
            return
        }
        Log.i(tag, "Do you forget to reset the join room info?")
    }

    private fun reportRoomEntryEnd(rid: String, uid: String, sid: String,
                           result: String, errCode: String?, httpCode: String?,
                           callback: EduCallback<Void>?) {
        val label = RoomReportLabel(ctype, platform, version, appId,ReportEvent.Join.toString(),
                ReportCategory.Http.toString(), "entry", result, errCode, httpCode)
        val timestamp = System.currentTimeMillis()
        val entryValue = ReportValues(null,
                (timestamp - entryApiCallTimestamp).toInt())
        val body = ReportBody.create(label, entryValue,
                ReportMetric.Event.toString(), src, timestamp)
        report(body, callback)
    }

    fun reportRtmJoinStart() {
        rtmJoinStart = System.currentTimeMillis()
    }

    fun reportRtmJoinResult(result: String, errCode: String?, callback: EduCallback<Void>?) {
        ReportManager.joinInfo?.let {
            reportRtmJoin(it.rid, it.uid, it.sid, result, errCode, callback)
            return
        }
        Log.i(tag, "Do you forget to reset the join room info?")
    }

    private fun reportRtmJoin(rid: String, uid: String, sid: String,
                      result: String, errCode: String?, callback: EduCallback<Void>?) {
        val label = RoomReportLabel(ctype, platform, version, appId,ReportEvent.Join.toString(),
                ReportCategory.Rtm.toString(), "joinChannel", result, errCode, null)
        val timestamp = System.currentTimeMillis()
        val values = ReportValues(null, (timestamp - rtmJoinStart).toInt())
        val body = ReportBody.create(label, values,
                ReportMetric.Event.toString(), src, timestamp)
        report(body, callback)
    }

    fun reportRtcJoinStart() {
        rtcJoinStart = System.currentTimeMillis()
    }

    fun reportRtcJoinResult(result: String, errCode: String?, callback: EduCallback<Void>?) {
        ReportManager.joinInfo?.let {
            reportRtcJoin(it.rid, it.uid, it.sid, result, errCode, callback)
            return
        }

        Log.i(tag, "Do you forget to reset the join room info?")
    }

    private fun reportRtcJoin(rid: String, uid: String, sid: String,
                      result: String, errCode: String?, callback: EduCallback<Void>?) {
        val label = RoomReportLabel(ctype, platform, version, appId,ReportEvent.Join.toString(),
                ReportCategory.Rtc.toString(), "joinChannel", result, errCode, null)
        val timestamp = System.currentTimeMillis()
        val values = ReportValues(null, (timestamp - rtcJoinStart).toInt())
        val body = ReportBody.create(label, values,
                ReportMetric.Event.toString(), src, timestamp)
        report(body, callback)
    }

    fun reportJoinRoomEnd(result: String, callback: EduCallback<Void>?) {
        ReportManager.joinInfo?.let {
            reportJoinRoomEnd(it.rid, it.uid, it.sid, result, callback)
            return
        }

        Log.i(tag, "Do you forget to reset the join room info?")
    }

    private fun reportJoinRoomEnd(rid: String, uid: String, sid: String,
                          result: String, callback: EduCallback<Void>?) {
        val label = RoomReportLabel(ctype, platform, version, appId,ReportEvent.Join.toString(),
                ReportCategory.End.toString(), null, result, null, null)
        val timestamp = System.currentTimeMillis()
        val values = ReportValues(null,
                (timestamp - joinStartTimestamp).toInt())
        val body = ReportBody.create(label, values,
                ReportMetric.Event.toString(), src, timestamp)
        report(body, callback)
    }
}