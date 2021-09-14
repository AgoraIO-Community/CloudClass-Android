package io.agora.edu.core.internal.server.requests.http.retrofit.dispatch

import io.agora.edu.core.internal.server.requests.Request
import io.agora.edu.core.internal.server.requests.RequestCallback
import io.agora.edu.core.internal.server.requests.RequestConfig
import io.agora.edu.core.internal.server.requests.http.retrofit.services.UserService

internal class UserServiceDispatcher(private val userService: UserService) : AbsServiceDispatcher() {
    override fun dispatch(config: RequestConfig, callback: RequestCallback<Any>?, vararg args: Any) {
        if (!Request.isValidArguments(config, args)) {
            return
        }

        when (config.request) {
            Request.HandsUpApply -> {
                userService.applyHandsUp(args[0] as String, args[1] as String)
                        .enqueue(ServiceRespCallbackWithDataBody(callback))
            }
            Request.HandsUpCancel -> {
                userService.cancelApplyHandsUp(args[0] as String, args[1] as String)
                        .enqueue(ServiceRespCallbackWithDataBody(callback))
            }
            Request.HandsUpExit -> {
                userService.exitHandsUp(args[0] as String, args[1] as String)
                        .enqueue(ServiceRespCallbackWithDataBody(callback))
            }
            else -> {

            }
        }
    }
}