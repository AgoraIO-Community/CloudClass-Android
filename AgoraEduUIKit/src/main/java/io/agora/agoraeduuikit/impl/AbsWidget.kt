package io.agora.agoraeduuikit.impl

import android.graphics.Rect
import io.agora.agoraeduuikit.impl.container.AbsUIContainer
import io.agora.agoraeduwidget.AbsUiWidget

abstract class AgoraAbsWidget : AbsUiWidget() {
    private var container: AbsUIContainer? = null

    fun setContainer(container: AbsUIContainer) {
        this.container = container
    }

    fun getContainer(): AbsUIContainer? {
        return this.container
    }

    abstract fun setRect(rect: Rect)
}