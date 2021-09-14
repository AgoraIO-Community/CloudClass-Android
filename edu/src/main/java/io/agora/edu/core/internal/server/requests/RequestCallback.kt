package io.agora.edu.core.internal.server.requests

interface RequestCallback<in T> {
    fun onSuccess(t: T?)

    fun onMayRetry(t: T?)

    fun onFailure(error: RequestError)
}

data class RequestError(
        val code: Int,
        val msg: String) {

    companion object {
        const val noError = 0
        const val requestMaxTry = -1
        const val requestIllegalArgument = -2
        const val requestServerError = -3

        val MaxTryCount = RequestError(requestMaxTry, "Request max count exceeds")
    }
}