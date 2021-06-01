package io.agora.educontext

import io.agora.educontext.eventHandler.IHandlerPool

abstract class AbsHandlerPool<T> : IHandlerPool<T> {
    private val handlers: MutableList<T> = mutableListOf()

    override fun addHandler(handler: T?) {
        handler?.let { h ->
            synchronized(this) {
                if (!handlers.contains(h)) {
                    handlers.add(h)
                }
            }
        }
    }

    override fun removeHandler(handler: T?) {
        handler?.let { h ->
            synchronized(this) {
                if (handlers.contains(h)) {
                    handlers.remove(h)
                }
            }
        }
    }

    override fun getHandlers(): List<T>? {
        return handlers.toList()
    }
}