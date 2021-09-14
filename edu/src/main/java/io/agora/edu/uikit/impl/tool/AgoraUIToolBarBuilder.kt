package io.agora.edu.uikit.impl.tool

import android.content.Context
import android.view.ViewGroup

class AgoraUIToolBarBuilder(private val context: Context,
                            private val eduContextPool: io.agora.edu.core.context.EduContextPool?,
                            private val parent: ViewGroup) {
    private var foldWidth = 0
    private var foldHeight = 0
    private var foldLeft = 0
    private var foldTop = 0
    private var unfoldWidth = 0
    private var unfoldHeight = 0
    private var unfoldLeft = 0
    private var unfoldTop = 0
    private var shadowWidth = 0

    /**
     * @param width, a width value smaller than or equal to
     * zero means using default fold button width
     */
    fun foldWidth(width: Int): AgoraUIToolBarBuilder {
        this.foldWidth = width
        return this
    }

    /**
     * @param height, a height value smaller than or equal to
     * zero means using default fold button height
     */
    fun foldHeight(height: Int): AgoraUIToolBarBuilder {
        this.foldHeight = height
        return this
    }

    fun foldLeft(left: Int): AgoraUIToolBarBuilder {
        this.foldLeft = left
        return this
    }

    fun foldTop(top: Int): AgoraUIToolBarBuilder {
        this.foldTop = top
        return this
    }

    /**
     * @param width, a width value smaller than or equal to
     * zero means using default unfold layout width
     */
    fun unfoldWidth(width: Int): AgoraUIToolBarBuilder {
        this.unfoldWidth = width
        return this
    }

    /**
     * @param height, a height value smaller than or equal to
     * zero means using default unfold layout height
     */
    fun unfoldHeight(height: Int): AgoraUIToolBarBuilder {
        this.unfoldHeight = height
        return this
    }

    fun unfoldLeft(left: Int): AgoraUIToolBarBuilder {
        this.unfoldLeft = left
        return this
    }

    fun unfoldTop(top: Int): AgoraUIToolBarBuilder {
        this.unfoldTop = top
        return this
    }

    fun shadowWidth(width: Int): AgoraUIToolBarBuilder {
        this.shadowWidth = width
        return this
    }

    fun build(): AgoraUIToolBar {
        return AgoraUIToolBar(
                context = context,
                eduContext = eduContextPool,
                parent = parent,
                foldTop = foldTop,
                foldLeft = foldLeft,
                foldWidth = foldWidth,
                foldHeight = foldHeight,
                unfoldTop = unfoldTop,
                unfoldLeft = unfoldLeft,
                unfoldWidth = unfoldWidth,
                unfoldHeight = unfoldHeight,
                shadowWidth = shadowWidth)
    }
}