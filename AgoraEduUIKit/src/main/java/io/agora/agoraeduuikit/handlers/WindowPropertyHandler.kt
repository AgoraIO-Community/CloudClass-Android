package io.agora.agoraeduuikit.handlers

import io.agora.agoraeducore.core.context.IWindowPropertyHandler
import io.agora.agoraeducore.core.internal.edu.common.bean.handsup.WindowPropertyBody

open class WindowPropertyHandler : IWindowPropertyHandler {
    override fun onWindowPropertyChanged(windowPropertyBody: WindowPropertyBody?) {
    }

    override fun onWindowPropertyDeleted() {
    }

    override fun onWindowPropertyUpdated(streamUuid: String) {
    }
}