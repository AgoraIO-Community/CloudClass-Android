package io.agora.edu.core.internal.server.proxy

import io.agora.edu.core.internal.server.requests.AgoraRequestClient
import io.agora.edu.core.internal.server.requests.Request
import io.agora.edu.core.internal.server.requests.RequestCallback

class UserServiceProxy : IServiceProxy {
    fun handsUpApply(appId: String, roomUuid: String, region: String, callback: RequestCallback<Any>) {
        AgoraRequestClient.send(
                request = Request.HandsUpApply,
                region = region,
                callback = callback,
                args = *arrayOf(appId, roomUuid))
    }

    fun handsUpCancel(appId: String, roomUuid: String, region: String, callback: RequestCallback<Any>) {
        AgoraRequestClient.send(
                request = Request.HandsUpApply,
                region = region,
                callback = callback,
                args = *arrayOf(appId, roomUuid))
    }

    fun handsUpExit(appId: String, roomUuid: String, region: String, callback: RequestCallback<Any>) {
        AgoraRequestClient.send(
                request = Request.HandsUpApply,
                region = region,
                callback = callback,
                args = *arrayOf(appId, roomUuid))
    }
}