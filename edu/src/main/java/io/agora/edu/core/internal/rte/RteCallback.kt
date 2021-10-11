package io.agora.edu.core.internal.rte

import io.agora.edu.core.internal.rte.data.RteError

interface RteCallback<T> {
    fun onSuccess(res: T?)

    fun onFailure(error: RteError)
}
