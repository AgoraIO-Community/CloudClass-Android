package io.agora.agoraeducore.core.internal.server.responses.rtm

import com.google.gson.Gson
import io.agora.agoraeducore.core.internal.education.impl.Constants
import java.lang.Exception

class ServerRtmResp(
        val traceId: String,
        val response: ServerRtmRespBody,
        val status: Int) {

    companion object {
        const val tag = "ServerRtmResp"

        fun parse(text: String) : ServerRtmResp? {
            return try {
                Gson().fromJson(text, ServerRtmResp::class.java)
            } catch (e: Exception) {
                Constants.AgoraLog.w("$tag-> invalid rtm server response format, $text")
                null
            }
        }
    }
}

class ServerRtmRespBody(
        val code: Int,
        val msg: String,
        val data: MutableMap<String, Any>
)