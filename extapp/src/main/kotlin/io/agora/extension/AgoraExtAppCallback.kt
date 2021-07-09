package io.agora.extension

interface AgoraExtAppCallback<T> {
    fun onSuccess(t: T)

    fun onFail(error: Throwable?)
}