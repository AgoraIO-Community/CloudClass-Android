package io.agora.rte

import io.agora.rte.data.RteError

interface RteCallback<T> {
    fun onSuccess(res: T?)

    fun onFailure(error: RteError)
}
