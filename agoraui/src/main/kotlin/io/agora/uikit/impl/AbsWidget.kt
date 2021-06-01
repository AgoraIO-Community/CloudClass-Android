package io.agora.uikit.impl

import android.graphics.Rect
import io.agora.uicomponent.AbsUiWidget
import io.agora.uikit.impl.container.AbsUIContainer

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