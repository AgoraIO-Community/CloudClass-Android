package io.agora.agoraeducore.core.internal.education.impl.network

import io.agora.agoraeducore.core.internal.base.network.ResponseBody

class ResponseBody<T> constructor() : ResponseBody<String?>() {
    var data: T? = null

    constructor(code: Int, msg: String, data: T?) : this() {
        this.code = code
        this.msg = msg
        this.data = data
    }
}
