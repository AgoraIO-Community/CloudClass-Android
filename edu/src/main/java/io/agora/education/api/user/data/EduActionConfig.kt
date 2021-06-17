package io.agora.education.api.user.data

import io.agora.education.api.message.AgoraActionType

class EduActionConfig(
        val processUuid: String,
        val action: AgoraActionType,
        val toUserUuid: String,
        val fromUserUuid: String?,
        /*最多允许接受多少人同时申请;以第一次的数据为准*/
        val limit: Int,
        val payload: Map<String, Any>?
) {
    /*申请超时时间*/
    var timeout: Long = 60

    constructor(processUuid: String, action: AgoraActionType, toUserUuid: String,
                fromUserUuid: String?, timeout: Long, limit: Int, payload: Map<String, Any>?)
            : this(processUuid, action, toUserUuid, fromUserUuid, limit, payload) {
        this.timeout = timeout
    }
}

class EduStopActionConfig(
        val processUuid: String,
        val action: AgoraActionType,
        var payload: Map<String, Any>?) {
}