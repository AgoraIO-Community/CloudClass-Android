package io.agora.educontext

interface EduContextCallback<T> {
    fun onSuccess(target: T?)

    fun onFailure(error: EduContextError?)
}