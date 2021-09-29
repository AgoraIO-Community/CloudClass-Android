package io.agora.agoraeducore.core.internal.server.requests.rtm

import io.agora.agoraeducore.core.internal.server.requests.RequestConfig

/**
 * Identify a runtime rtm server request instance
 */
class RtmServerTask(
        val traceId: String,
        val config: RequestConfig,
        val ts: Long,
        val timeout: Int,
        val body: MutableMap<String, Any>? = null,
        val headers: MutableMap<String, String>? = null
)