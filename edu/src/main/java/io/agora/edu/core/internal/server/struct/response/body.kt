package io.agora.edu.core.internal.server.struct.response

class BaseResponseBody(
        val code: Int,
        val msg: String?,
        val ts: Long?)

class DataResponseBody<T>(
        code: Int,
        msg: String,
        ts: Long,
        var data: T
) : io.agora.edu.core.internal.base.network.ResponseBody<String>()