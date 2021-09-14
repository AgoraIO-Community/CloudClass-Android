package io.agora.edu.core.internal.server.requests

import com.google.gson.Gson
import io.agora.edu.core.internal.education.impl.Constants
import io.agora.edu.core.internal.server.requests.rtm.RequestBuilder
import io.agora.edu.core.internal.server.responses.rtm.ServerRtmResp
import kotlin.reflect.KClass

enum class RequestChannelPriority {
    /**
     * Use default request channel priority defined in request config,
     * either the rtm or the http channel.
     * This is used as a flag if a certain request does NOT ask for
     * a temporarily different priority from the request default config.
     */
    DEFAULT,

    /**
     * Prior to use RTM request channel, if fails, it's the request
     * client's job to determine whether to request through
     * another channel.
     * By definition, it has a higher priority over HTTP channel, and
     * may drawback to HTTP channel.
     */
    RTM,

    /**
     * Use default HTTP request servers.
     */
    HTTP
}

class RequestConfig(
        val request: Request,
        val priority: RequestChannelPriority,
        val urlFormat: String,
        val httpMethod: String,
        val headerCount: Int = 0,
        val headerKeys: List<String> = mutableListOf(),
        val pathCount: Int = 0,
        val queryCount: Int = 0,
        val hasBody: Boolean = false,
        val bodyType: KClass<out Any>? = null
)

data class RequestParam(
        val region: String,
        val tsInMilliSecond: Long,
        val headers: MutableMap<String, String> = mutableMapOf(),
        val pathValues: MutableList<String> = mutableListOf(),
        val queryValues: MutableList<String> = mutableListOf(),
        var body: Any? = null,
        var callback: RequestCallback<ServerRtmResp>? = null
)

internal data class RequestMessageDataBlock(
        /**
         * Url string with all path & query info replaced by the actual arguments
         */
        val url: String,
        val headers: Map<String, String>,
        val body: Map<String, Any>?,
        val method: String,
        val traceId: String
)

enum class Request {
    // General requests

    // Room Requests
    RoomConfig,
    RoomPreCheck,
    RoomJoin,
    RoomSnapshot,
    RoomSequence,
    RoomSetProperty,
    RoomRemoveProperty,
    RoomSetRoleMuteState,
    RoomSetClassState,

    // User Requests
    HandsUpApply,
    HandsUpCancel,
    HandsUpExit,

    // Message Requests
    SendRoomMessage,
    GetRoomMessage,
    SendRoomCustomMessage,
    SendPeerMessage,
    SendPeerCustomMessage,
    SendConversationMessage,
    GetConversationMessage,
    Translate,
    SetUserChatMuteState,

    // Media Requests
    UpdateDeviceState,

    // Extension App Requests
    SetFlexibleRoomProperty,
    SetFlexibleUserProperty;

    companion object {
        private val requestConfigMap = mutableMapOf<Request, RequestConfig>()

        internal fun addConfig(request: Request,
                               priority: RequestChannelPriority,
                               urlFormat: String,
                               httpMethod: String,
                               headerCount: Int = 0,
                               headerKeys: List<String> = mutableListOf(),
                               pathCount: Int = 0,
                               queryCount: Int = 0,
                               hasBody: Boolean = false,
                               bodyType: KClass<out Any>? = null) {

            if (hasBody && bodyType == null) {
                Constants.AgoraLog.e("$request: request config illegal body params, request " +
                        "has a body but no body type is defined")
                return
            }

            if (!requestConfigMap.containsKey(request)) {
                requestConfigMap[request] = RequestConfig(request,
                        priority, urlFormat, httpMethod,
                        headerCount, headerKeys, pathCount,
                        queryCount, hasBody, bodyType)
            }
        }

        @Synchronized
        fun getRequestConfig(request: Request) : RequestConfig? {
            return requestConfigMap[request]
        }

        fun toRtmMessageString(traceId: String, config: RequestConfig, param: RequestParam) : String {
            val body = if (param.body != null) RequestBuilder.classToMap(param.body!!) else null

            // The url format contains path and queries variables that needs to be
            // replaced by the actual arguments. Here we use the arguments via
            // the passed request param
            val url = RequestBuilder.replaceAllPlaceholders(config.urlFormat, param)

            return Gson().toJson(RequestMessageDataBlock(
                    url, param.headers,
                    body, config.httpMethod, traceId))
        }

        /**
         * Check if the given arguments are consistent with the request config.
         * The arguments passed should be the same length, the same types and also
         * the same order to the request config.
         * But note this check takes strictly a null value as invalid.
         * @param config of a request
         * @param args variable length of array of arguments.
         */
        fun isValidArguments(config: RequestConfig, vararg args: Any) : Boolean {
            val argCount = config.headerCount + config.pathCount +
                    config.queryCount + if (config.hasBody) 1 else 0
            if (argCount != args.size) {
//                Constants.AgoraLog.e("Illegal argument size: request ${config.request}, " +
//                        "size should be ${config.paramCount} but passing ${args.size}")
                return false
            }

            var valid = true
            if (config.hasBody) {
                val body = args[config.headerCount + config.pathCount + config.queryCount]
                if (config.bodyType?.isInstance(body) == false) {
//                Constants.AgoraLog.e("Illegal argument body type: request ${config.request}, " +
//                        "has a body type ${config.bodyType}, but ${body::class} passed")
                    valid  = false
                }
            }

            return valid
        }
    }
}