package io.agora.agoraeducore.core.internal.server.proxy

import io.agora.agoraeducore.core.internal.server.struct.request.DeviceStateUpdateReq
import io.agora.agoraeducore.core.internal.server.requests.AgoraRequestClient
import io.agora.agoraeducore.core.internal.server.requests.Request
import io.agora.agoraeducore.core.internal.server.requests.RequestCallback

class MediaServiceProxy : IServiceProxy {
    fun updateDeviceState(appId: String, roomUuid: String, userUuid: String,
                          camera: Int?, facing: Int?, mic: Int?, speaker: Int?,
                          region: String, callback: RequestCallback<Any>) {
        AgoraRequestClient.send(
                request = Request.UpdateDeviceState,
                region = region,
                callback = callback,
                args = *arrayOf(appId, roomUuid, userUuid,
                        DeviceStateUpdateReq(camera, facing, mic, speaker))
        )
    }
}