package io.agora.agoraeduuikit.impl.screenshare

import android.content.Context
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.impl.AbsComponent

class AgoraUIFullScreenBtn(
        context: Context,
        private val eduContext: io.agora.agoraeducore.core.context.EduContextPool?,
        parent: ViewGroup,
        width: Int,
        height: Int,
        left: Int,
        top: Int) : AbsComponent(), View.OnClickListener {

    private val contentView = LayoutInflater.from(context).inflate(R.layout.agora_fullscreen_btn_layout,
            parent, false)
    private val btn: AppCompatImageView
//    private val handler = object : WhiteboardHandler() {
//        override fun onFullScreenChanged(isFullScreen: Boolean) {
//            super.onFullScreenChanged(isFullScreen)
//            setFullScreen(isFullScreen)
//        }
//    }

    init {
        parent.addView(contentView, width, height)
        val params = contentView.layoutParams as ViewGroup.MarginLayoutParams
        params.topMargin = top
        params.leftMargin = left
        contentView.layoutParams = params

        btn = contentView.findViewById(R.id.btn)
        btn.setOnClickListener(this)
//        eduContext?.whiteboardContext()?.addHandler(handler)
    }

    override fun onClick(v: View?) {
//        eduContext?.whiteboardContext()?.setFullScreen(btn.isSelected)
    }

    fun setFullScreen(fullScreen: Boolean) {
        btn.post {
            btn.isSelected = !fullScreen
        }
    }

    fun setVisibility(visibility: Int) {
        contentView.post {
            contentView.visibility = visibility
        }
    }

    override fun setRect(rect: Rect) {
        contentView.post {
            val params = contentView.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = rect.top
            params.leftMargin = rect.left
            params.width = rect.right - rect.left
            params.height = rect.bottom - rect.top
            contentView.layoutParams = params
        }
    }
}