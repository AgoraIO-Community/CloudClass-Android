package io.agora.agoraeduuikit.impl.whiteboard

import android.content.Context
import android.view.ViewGroup

class AgoraUIWhiteBoardBuilder(private val context: Context,
                               private val eduContext: io.agora.agoraeducore.core.context.EduContextPool?,
                               private val parent: ViewGroup) {
    private var left: Float = 0f
    private var top: Float = 0f
    private var width: Int = 0
    private var height: Int = 0
    private var shadowWidth: Float = 0f

    fun width(width: Int): AgoraUIWhiteBoardBuilder {
        this.width = width
        return this
    }

    fun height(height: Int): AgoraUIWhiteBoardBuilder {
        this.height = height
        return this
    }

    fun left(value: Float): AgoraUIWhiteBoardBuilder {
        this.left = value
        return this
    }

    fun top(value: Float): AgoraUIWhiteBoardBuilder {
        this.top = value
        return this
    }

    fun shadowWidth(value: Float): AgoraUIWhiteBoardBuilder {
        this.shadowWidth = value
        return this
    }

    fun build(): AgoraUIWhiteBoard {
        return AgoraUIWhiteBoard(
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