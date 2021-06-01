package io.agora.report.v2.reporter

import io.agora.base.network.RetrofitManager
import io.agora.report.v2.ReportServiceV2
import io.agora.edu.launch.AgoraEduSDK.reportUrlV2
import io.agora.report.Md5Util
import io.agora.report.v2.ReportRequest
import io.agora.report.v2.ReportResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

abstract class AbsReporterV2(
        protected val vendorId: Int
) {
    protected val tag = "APaaSReporterV2"

    private val source = "apaas"

    // report rest environment 101
    // production environment 1
    private val qosLevel = 101

    private val service: ReportServiceV2 = RetrofitManager.instance()
            .getService(reportUrlV2(), ReportServiceV2::class.java)

    companion object {
        const val format = "payload=%s&src=%s&ts=%d"
        fun getSign(payload: String, src: String, timestamp: Long): String {
            return Md5Util.toMD5String(String.format(format, payload, src, timestamp))
        }
    }

    protected fun report(protocolId: Int, payload: String, timestamp: Long) {
        service.report(ReportRequest(
                protocolId,
                payload,
                qosLevel,
                getSign(payload, source, timestamp),
                source,
                timestamp,
                vendorId)).enqueue(object : Callback<ReportResponse> {
            override fun onResponse(call: Call<ReportResponse>, response: Response<ReportResponse>) {

            }

            override fun onFailure(call: Call<ReportResponse>, t: Throwable) {

            }
        })
    }
}

enum class ProtocolIds(val value: Int) {
    UserJoin(9012),
    UserQuit(9013),
    UserReconnect(9014)
}