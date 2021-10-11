package io.agora.edu.core.internal.report.v2

data class ReportRequest(
        val id: Int,
        val payload: String,
        val qos: Int,
        val sign: String,
        val src: String,
        val ts: Long,
        val vid: Int
)

data class ReportResponse(
        val code: String,
        val msg: String,
        val data: Any?
)

enum class ReportStatus(val code: String, val msg: String) {
    Unknown("", ""),
    Success("0", "Success"),
    PayloadEmpty("10", "Payload empty"),
    QosEmpty("15", "Qos empty"),
    SourceEmpty("20", "Src empty"),
    RequestExceed("100", "Request exceed"),
    SignInvalid("110", "Sign invalid"),
    TsEmpty("120", "Timestamp empty");

    companion object {
        fun fromString(code: String) : ReportStatus {
            return when (code) {
                Success.code -> Success
                PayloadEmpty.code -> PayloadEmpty
                QosEmpty.code -> QosEmpty
                SourceEmpty.code -> SourceEmpty
                RequestExceed.code -> RequestExceed
                SignInvalid.code -> SignInvalid
                TsEmpty.code -> TsEmpty
                else -> Unknown
            }
        }
    }
}