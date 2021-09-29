package io.agora.agoraeducore.core.internal.server.struct.response

class BaseResponseBody(
        val code: Int,
        val msg: String?,
        val ts: Long?)

class DataResponseBody<T>(
        code: Int,
        msg: String,
        ts: Long,
        var data: T
) : io.agora.agoraeducore.core.internal.base.network.ResponseBody<String>()