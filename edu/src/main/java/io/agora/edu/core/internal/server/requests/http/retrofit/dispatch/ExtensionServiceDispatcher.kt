package io.agora.edu.core.internal.server.requests.http.retrofit.dispatch

import io.agora.edu.core.internal.server.struct.request.RoomFlexPropsReq
import io.agora.edu.core.internal.server.struct.request.UserFlexPropsReq
import io.agora.edu.core.internal.server.requests.Request
import io.agora.edu.core.internal.server.requests.RequestCallback
import io.agora.edu.core.internal.server.requests.RequestConfig
import io.agora.edu.core.internal.server.requests.http.retrofit.services.ExtensionService

class ExtensionServiceDispatcher(private val extensionService: ExtensionService) : AbsServiceDispatcher() {
    override fun dispatch(config: RequestConfig, callback: RequestCallback<Any>?, vararg args: Any) {
        if (!Request.isValidArguments(config, args)) {
            return
        }

        when (config.request) {
            Request.SetFlexibleRoomProperty -> {
                extensionService.updateFlexRoomProps(args[0] as String, args[1] as String,
                    args[2] as RoomFlexPropsReq).enqueue(ServiceRespCallbackWithBaseBody(callback))
            }

            Request.SetFlexibleUserProperty -> {
                extensionService.updateFlexUserProps(args[0] as String, args[1] as String,
                        args[3] as String, args[4] as UserFlexPropsReq)
                        .enqueue(ServiceRespCallbackWithBaseBody(callback))
            }
            else -> {

            }
        }
    }
}