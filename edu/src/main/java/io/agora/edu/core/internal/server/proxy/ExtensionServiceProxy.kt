package io.agora.edu.core.internal.server.proxy

import io.agora.edu.core.internal.server.struct.request.RoomFlexPropsReq
import io.agora.edu.core.internal.server.struct.request.UserFlexPropsReq
import io.agora.edu.core.internal.server.requests.AgoraRequestClient
import io.agora.edu.core.internal.server.requests.Request
import io.agora.edu.core.internal.server.requests.RequestCallback

class ExtensionServiceProxy : IServiceProxy {
    fun setFlexibleRoomProperty(appId: String, roomUuid: String,
                                properties: MutableMap<String, String>,
                                cause: MutableMap<String, String>?,
                                region: String, callback: RequestCallback<Any>) {
        AgoraRequestClient.send(
                request = Request.SetFlexibleRoomProperty,
                region = region,
                callback = callback,
                args = *arrayOf(appId, roomUuid,
                        RoomFlexPropsReq(properties, cause))
        )
    }

    fun setFlexibleUserProperty(appId: String, roomUuid: String, userUuid: String,
                                properties: MutableMap<String, String>,
                                cause: MutableMap<String, String>?,
                                region: String, callback: RequestCallback<Any>) {
        AgoraRequestClient.send(
                request = Request.SetFlexibleUserProperty,
                region = region,
                callback = callback,
                args = *arrayOf(appId, roomUuid, userUuid,
                        UserFlexPropsReq(properties, cause))
        )
    }
}