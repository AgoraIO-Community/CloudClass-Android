package io.agora.edu.extensions.extapp

interface AgoraExtAppCallback<T> {
    fun onSuccess(t: T)

    fun onFail(error: Throwable?)
}