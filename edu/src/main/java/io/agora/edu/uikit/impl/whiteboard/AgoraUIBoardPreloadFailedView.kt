package io.agora.edu.uikit.impl.whiteboard

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import io.agora.edu.R
import java.lang.ref.WeakReference

class AgoraUIBoardPreloadFailedView : LinearLayout {
    private val tag = "AgoraUIBoardPreloadFailedView"

    private var url: String? = null
    private var listener: WeakReference<BoardPreloadFailedListener>? = null

    companion object {
        fun show(viewGroup: ViewGroup, url: String, listener: BoardPreloadFailedListener?): AgoraUIBoardPreloadFailedView {
            val boardloadFailedView = AgoraUIBoardPreloadFailedView(viewGroup.context)
            boardloadFailedView.url = url
            val layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.MATCH_PARENT)
            layoutParams.startToStart = 0
            layoutParams.topToTop = 0
            layoutParams.endToEnd = 0
            layoutParams.bottomToBottom = 0
            boardloadFailedView.layoutParams = layoutParams
            viewGroup.post {
                viewGroup.addView(boardloadFailedView)
            }
            listener?.let {
                boardloadFailedView.listener = WeakReference(it)
            }
            return boardloadFailedView
        }
    }

    fun show(viewGroup: ViewGroup, url: String) {
        this.url = url
        viewGroup.post {
            viewGroup.addView(this)
        }
    }

    constructor(context: Context?) : super(context) {
        view(context)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        view(context)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        view(context)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        view(context)
    }

    private fun view(context: Context?) {
        val view = inflate(context, R.layout.agora_board_preload_failed_view_layout, this)
        view.findViewById<AppCompatTextView>(R.id.close_Text).setOnClickListener { view ->
            listener?.let {
                it.get()?.onClose(this.url)
            }
            dismiss()
        }
        view.findViewById<AppCompatTextView>(R.id.retry_Text).setOnClickListener { view ->
            listener?.let {
                it.get()?.onRetry(this.url)
            }
            dismiss()
        }
    }

    fun dismiss() {
        post {
            parent?.let {
                val viewGroup = parent as ViewGroup
                viewGroup.children.forEach { it ->
                    if (it == this) {
                        viewGroup.removeView(this)
                        listener?.let {
                            listener!!.get()?.onFailedViewDismiss()
                        }
                    }
                }
            }
        }
    }

    fun isShowing(): Boolean {
        return parent != null && visibility == VISIBLE
    }
}

interface BoardPreloadFailedListener {
    fun onClose(url: String?)
    fun onRetry(url: String?)
    fun onFailedViewDismiss()
}
