package io.agora.uikit.impl.whiteboard.paging

import android.content.Context
import android.view.ViewGroup
import io.agora.educontext.EduContextPool

class AgoraUIPagingControlBuilder(private val context: Context,
                                  private val eduContext: EduContextPool?,
                                  private val parent: ViewGroup) {
    private var left: Float = 0f
    private var top: Float = 0f
    private var width: Int = 0
    private var height: Int = 0
    private var shadowWidth: Float = 0f

    fun width(width: Int): AgoraUIPagingControlBuilder {
        this.width = width
        return this
    }

    fun height(height: Int): AgoraUIPagingControlBuilder {
        this.height = height
        return this
    }

    fun left(value: Float): AgoraUIPagingControlBuilder {
        this.left = value
        return this
    }

    fun top(value: Float): AgoraUIPagingControlBuilder {
        this.top = value
        return this
    }

    fun shadowWidth(value: Float): AgoraUIPagingControlBuilder {
        this.shadowWidth = value
        return this
    }

    fun build(): AgoraUIPagingControl {
        return AgoraUIPagingControl(
                context = context,
                eduContext = eduContext,
                parent = parent,
                width = width,
                height = height,
                left = left,
                top = top,
                shadowWidth = shadowWidth)
    }
}