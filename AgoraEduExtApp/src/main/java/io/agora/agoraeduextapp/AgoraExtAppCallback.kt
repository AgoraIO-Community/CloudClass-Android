package io.agora.agoraeduextapp

interface AgoraExtAppCallback<T> {
    fun onSuccess(t: T)

    fun onFail(error: Throwable?)
}