package io.agora.agoraeducore.core.internal.server.requests.http.retrofit.dispatch

import io.agora.agoraeducore.core.internal.server.struct.request.DeviceStateUpdateReq
import io.agora.agoraeducore.core.internal.server.requests.Request
import io.agora.agoraeducore.core.internal.server.requests.RequestCallback
import io.agora.agoraeducore.core.internal.server.requests.RequestConfig
import io.agora.agoraeducore.core.internal.server.requests.http.retrofit.services.MediaService

internal class MediaServiceDispatcher(private val mediaService: MediaService) : AbsServiceDispatcher() {
    override fun dispatch(config: RequestConfig, callback: RequestCallback<Any>?, vararg args: Any) {
        if (!Request.isValidArguments(config, args)) {
            return
        }

        when (config.request) {
            Request.UpdateDeviceState -> {
                mediaService.updateDeviceState(args[0] as String, args[1] as String,
                    args[2] as String, args[3] as DeviceStateUpdateReq)
                        .enqueue(ServiceRespCallbackWithDataBody(callback))
            }
            else -> {

            }
        }
    }
}