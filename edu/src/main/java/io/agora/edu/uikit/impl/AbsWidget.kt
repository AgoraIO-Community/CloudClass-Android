package io.agora.edu.uikit.impl

import android.graphics.Rect
import io.agora.edu.extensions.widgets.AbsUiWidget
import io.agora.edu.uikit.impl.container.AbsUIContainer

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