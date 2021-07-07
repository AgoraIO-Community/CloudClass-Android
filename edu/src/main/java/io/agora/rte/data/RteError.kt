package io.agora.rte.data

import io.agora.rtc.RtcEngine
import io.agora.rtm.ErrorInfo

class RteError(
        val type: ErrorType,
        val errorCode: Int,
        val errorDesc: String) {

    companion object {
        fun rtcError(errorCode: Int): RteError {
            return RteError(ErrorType.RTC, errorCode, RtcEngine.getErrorDescription(errorCode))
        }

        fun rtmError(errorInfo: ErrorInfo): RteError {
            return RteError(ErrorType.RTM, errorInfo.errorCode, errorInfo.errorDescription)
        }
    }

}

enum class ErrorType(val value: Int) {
    RTC(0),
    RTM(1)
}