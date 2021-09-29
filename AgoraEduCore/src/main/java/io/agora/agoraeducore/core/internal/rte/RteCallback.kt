package io.agora.agoraeducore.core.internal.rte

import io.agora.agoraeducore.core.internal.rte.data.RteError

interface RteCallback<T> {
    fun onSuccess(res: T?)

    fun onFailure(error: RteError)
}
