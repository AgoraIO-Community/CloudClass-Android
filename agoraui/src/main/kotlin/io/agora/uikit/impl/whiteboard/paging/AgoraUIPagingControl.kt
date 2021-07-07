package io.agora.uikit.impl.whiteboard.paging

import android.content.Context
import android.graphics.Rect
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import io.agora.educontext.EduContextPool
import io.agora.educontext.context.WhiteboardContext
import io.agora.uikit.impl.AbsComponent
import io.agora.uikit.R
import io.agora.uikit.educontext.handlers.WhiteboardHandler
import java.io.File

class AgoraUIPagingControl(
        context: Context,
        private val eduContext: EduContextPool?,
        parent: ViewGroup,
        width: Int = 0,
        height: Int = 0,
        left: Float,
        top: Float,
        shadowWidth: Float) : AbsComponent(), View.OnClickListener {

    private val tag = "AgoraUIPaging"

    val view: View = LayoutInflater.from(context).inflate(R.layout.agora_paging_layout, parent, false)
    private val cardView: CardView = view.findViewById(R.id.cardView)
    private val cover: View
    private val fullScreenBtn: AppCompatImageView
    private val zoomOutBtn: AppCompatImageView
    private val zoomInBtn: AppCompatImageView
    private val previousBtn: AppCompatImageView
    private val pageNoTv: AppCompatTextView
    private val nextBtn: AppCompatImageView
    private var fullScreen = false

    private val whiteboardHandler = object : WhiteboardHandler() {
        override fun onPageNo(no: Int, count: Int) {
            super.onPageNo(no, count)
            this@AgoraUIPagingControl.setPageNo(no, count)
        }

        override fun onPagingEnabled(enabled: Boolean) {
            super.onPagingEnabled(enabled)
            this@AgoraUIPagingControl.setPagingEnable(enabled)
        }

        override fun onZoomEnabled(zoomOutEnabled: Boolean?, zoomInEnabled: Boolean?) {
            super.onZoomEnabled(zoomOutEnabled, zoomInEnabled)
            this@AgoraUIPagingControl.setZoomEnable(zoomOutEnabled, zoomInEnabled)
        }

        override fun onFullScreenEnabled(enabled: Boolean) {
            super.onFullScreenEnabled(enabled)
            this@AgoraUIPagingControl.setResizeScreenEnable(enabled)
        }

        override fun onFullScreenChanged(isFullScreen: Boolean) {
            super.onFullScreenChanged(isFullScreen)
            this@AgoraUIPagingControl.setFullScreen(isFullScreen)
        }

        override fun onInteractionEnabled(enabled: Boolean) {
            super.onInteractionEnabled(enabled)
            this@AgoraUIPagingControl.setEnabled(enabled)
        }
    }

    init {
        val desiredHeight: Int = if (height <= 0) context.resources
                .getDimensionPixelSize(R.dimen.agora_paging_control_height) else height
        val margin = shadowWidth.toInt()

        var params = cardView.layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(margin, margin, margin, margin)
        cardView.layoutParams = params
        cardView.cardElevation = shadowWidth
        cardView.radius = height / 2f

        parent.addView(view, ViewGroup.LayoutParams.WRAP_CONTENT, desiredHeight)
        params = view.layoutParams as ViewGroup.MarginLayoutParams
        params.topMargin = top.toInt()
        params.leftMargin = left.toInt()
        view.layoutParams = params

        cover = view.findViewById(R.id.cover)
        fullScreenBtn = view.findViewById(R.id.agora_paging_full_screen_btn)
        fullScreenBtn.setOnClickListener(this)
        zoomOutBtn = view.findViewById(R.id.agora_paging_zoom_out_btn)
        zoomOutBtn.setOnClickListener(this)
        zoomInBtn = view.findViewById(R.id.agora_paging_zoom_in_btn)
        zoomInBtn.setOnClickListener(this)
        previousBtn = view.findViewById(R.id.agora_paging_previous_btn)
        previousBtn.setOnClickListener(this)
        pageNoTv = view.findViewById(R.id.agora_paging_pageNo_tv)
        nextBtn = view.findViewById(R.id.agora_paging_next_btn)
        nextBtn.setOnClickListener(this)
        previousBtn.isEnabled = false
        nextBtn.isEnabled = false
        previousBtn.tag = true
        nextBtn.tag = true
        previousBtn.isSelected = true
        nextBtn.isSelected = true

        eduContext?.whiteboardContext()?.addHandler(whiteboardHandler)
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.agora_paging_full_screen_btn -> {
                val full = this.fullScreen
//                setFullScreen(!full)
                eduContext?.whiteboardContext()?.setFullScreen(!full)
            }
            R.id.agora_paging_zoom_out_btn -> {
                eduContext?.whiteboardContext()?.setZoomOut()
            }
            R.id.agora_paging_zoom_in_btn -> {
                eduContext?.whiteboardContext()?.setZoomIn()
            }
            R.id.agora_paging_previous_btn -> {
                eduContext?.whiteboardContext()?.setPrevPage()
            }
            R.id.agora_paging_next_btn -> {
                eduContext?.whiteboardContext()?.setNextPage()
            }
        }
    }

    fun setPageNo(no: Int, count: Int) {
        previousBtn.post {
            previousBtn.tag = no != 0
            previousBtn.isEnabled = previousBtn.isSelected and (previousBtn.tag as Boolean)
            nextBtn.tag = no != count - 1
            nextBtn.isEnabled = nextBtn.isSelected and (nextBtn.tag as Boolean)
            pageNoTv.text = (no + 1).toString().plus(File.separator).plus(count.toString())
        }
    }

    fun setPagingEnable(enabled: Boolean) {
        previousBtn.post {
            if (enabled) {
                previousBtn.isSelected = true
                previousBtn.isEnabled = previousBtn.isSelected and (previousBtn.tag as Boolean)
                nextBtn.isSelected = true
                nextBtn.isEnabled = nextBtn.isSelected and (nextBtn.tag as Boolean)
            } else {
                previousBtn.isSelected = false
                previousBtn.isEnabled = false
                nextBtn.isEnabled = false
                nextBtn.isSelected = false
            }
        }
    }

    fun setZoomEnable(zoomOutEnable: Boolean?, zoomInEnable: Boolean?) {
        zoomOutBtn.post {
            zoomOutEnable?.let {
                zoomOutBtn.isEnabled = it
            }
            zoomInEnable?.let {
                zoomInBtn.isEnabled = it
            }
        }
    }

    fun setResizeScreenEnable(enabled: Boolean) {
        fullScreenBtn.post { fullScreenBtn.isEnabled = enabled }
    }

    fun setFullScreen(fullScreen: Boolean) {
        fullScreenBtn.post {
            this.fullScreen = fullScreen
            fullScreenBtn.post {
                fullScreenBtn.setImageResource(
                        if (fullScreen) R.drawable.agora_paging_icon_fit_screen
                        else R.drawable.agora_paging_icon_full_screen)
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        cover.post {
            if (!enabled) {
                cover.setOnClickListener { }
            } else {
                cover.setOnClickListener(null)
            }
            cover.visibility = if (enabled) GONE else VISIBLE
        }
    }

    override fun setRect(rect: Rect) {
        view.post {
            val params = view.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = rect.top
            params.leftMargin = rect.left
            params.height = rect.bottom - rect.top
            view.layoutParams = params
        }
    }
}