package io.agora.educontext.eventHandler

interface IHandlerPool<T> {
    fun addHandler(handler: T?)

    fun removeHandler(handler: T?)

    fun getHandlers(): List<T>?
}