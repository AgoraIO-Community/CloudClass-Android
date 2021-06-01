package io.agora.report.v2.reporter

import android.util.Base64
import android.util.SparseArray
import androidx.core.util.contains
import io.agora.report.v2.protobuf.ApaasUserJoin

class ReporterV2(vendorId: Int) : AbsReporterV2(vendorId) {
    private val scenario = "education"
    private val codeNoError = 0

    private var apaasVer = ""
    private var roomId = ""
    private var uid = ""
    private var userName = ""
    private var streamUuid = 0L
    private var streamSuid = ""
    private var role = ""
    private var streamSid = ""
    private var rtmSid = ""

    fun setRoomReportInfo(apaasVer: String, roomId: String, userId: String,
                          userName: String, streamUuid: Long, streamSuid: String,
                          role: String, streamSid: String, rtmSid: String) {
        this.apaasVer = apaasVer
        this.roomId = roomId
        this.uid = userId
        this.userName = userName
        this.streamUuid = streamUuid
        this.streamSuid = streamSuid
        this.role = role
        this.streamSid = streamSid
        this.rtmSid = rtmSid
    }

    fun reportAPaaSUserJoined(code: Int = codeNoError, timestamp: Long) {
        val payload = createReportPayload(
                protocol = ProtocolIds.UserJoin.value,
                code = code,
                timestamp = timestamp)
        report(ProtocolIds.UserJoin.value, payload, timestamp)
    }

    fun reportAPaaSUserQuit(code: Int = codeNoError, timestamp: Long) {
        val payload = createReportPayload(
                protocol = ProtocolIds.UserQuit.value,
                code = code,
                timestamp = timestamp)
        report(ProtocolIds.UserQuit.value, payload, timestamp)
    }

    fun reportAPpaSUserReconnect(code: Int = codeNoError, timestamp: Long) {
        val payload = createReportPayload(
                protocol = ProtocolIds.UserReconnect.value,
                code = code,
                timestamp = timestamp
        )
        report(ProtocolIds.UserReconnect.value, payload, timestamp)
    }

    private fun createReportPayload(protocol: Int, code: Int = codeNoError, timestamp: Long) : String {
         val array = ApaasUserJoin.newBuilder()
                .setLts(timestamp)
                .setVid(vendorId)
                .setVer(apaasVer)
                .setScenario(scenario)
                .setErrorCode(code)
                .setUid(uid)
                .setUserName(userName)
                .setStreamUid(streamUuid)
                .setStreamSuid(streamSuid)
                .setRole(role)
                .setStreamSid(streamSid)
                .setRtmSid(rtmSid)
                .setRoomId(roomId)
                .build().toByteArray()
        return Base64.encodeToString(array, Base64.DEFAULT)
    }

    companion object {
        private val reporters = SparseArray<ReporterV2>()

        fun getReporterV2(vendorId: Int) : ReporterV2 {
            if (!reporters.contains(vendorId)) {
                reporters.put(vendorId, ReporterV2(vendorId))
            }

            return reporters.get(vendorId)
        }

        fun deleteReporterV2(vendorId: Int) {
            reporters.remove(vendorId)
        }
    }
}