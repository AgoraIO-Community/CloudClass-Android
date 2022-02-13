package io.agora.agoraeduuikit.impl.loading

import android.graphics.Rect
import android.view.ViewGroup
import android.view.ViewTreeObserver
import io.agora.agoraeduuikit.impl.AbsComponent

class AgoraUILoading(parent: ViewGroup,
                     anchorRect: Rect) : AbsComponent() {
    private val tag = "AgoraUILoading"

    private val loadingView = AgoraUILoadingView(parent.context)
    private var width: Int? = null
    private var height: Int? = null

    init {
        parent.addView(loadingView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        loadingView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                loadingView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                width = loadingView.right - loadingView.left
                height = loadingView.bottom - loadingView.top
                val params = loadingView.layoutParams as ViewGroup.MarginLayoutParams
                params.leftMargin = anchorRect.left
                params.topMargin = anchorRect.top
                params.width = anchorRect.right - anchorRect.left
                params.height = anchorRect.bottom - anchorRect.top
                loadingView.layoutParams = params
            }
        })
    }

    fun setVisibility(visibility: Int) {
        loadingView.visibility = visibility
    }

    fun setContent(isLoading: Boolean) {
        loadingView.setContent(isLoading)
    }

    override fun setRect(rect: Rect) {
        loadingView.post {
            val params = loadingView.layoutParams as ViewGroup.MarginLayoutParams
            params.leftMargin = rect.left
            params.topMargin = rect.top
            params.width = rect.right - rect.left
            params.height = rect.bottom - rect.top
            loadingView.layoutParams = params
        }
    }
}