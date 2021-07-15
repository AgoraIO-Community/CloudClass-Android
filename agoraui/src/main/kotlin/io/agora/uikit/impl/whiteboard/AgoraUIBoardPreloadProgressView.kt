package io.agora.uikit.impl.whiteboard

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.core.widget.ContentLoadingProgressBar
import io.agora.uikit.R
import java.lang.ref.WeakReference

class AgoraUIBoardPreloadProgressView : LinearLayout {
    private val tag = "AgoraUIBoardPreloadProgressView"
    private var progressLayout: ConstraintLayout? = null
    private var progressText: AppCompatTextView? = null
    private var downloadProgress: ContentLoadingProgressBar? = null
    private var timeoutLayout: ConstraintLayout? = null
    private var jumpOverText: AppCompatTextView? = null

    private var url: String? = null
    private var listener: WeakReference<BoardPreloadProgressListener>? = null

    companion object {
        fun show(viewGroup: ViewGroup, url: String, listener: BoardPreloadProgressListener?): AgoraUIBoardPreloadProgressView {
            val boardloadProgressView = AgoraUIBoardPreloadProgressView(viewGroup.context)
            boardloadProgressView.url = url
            var layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)
            layoutParams = when (viewGroup) {
                is RelativeLayout -> {
                    RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.MATCH_PARENT)
                }
                is FrameLayout -> {
                    FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT)
                }
                is ConstraintLayout -> {
                    ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT,
                            ConstraintLayout.LayoutParams.MATCH_PARENT)
                }
                is LinearLayout -> {
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT)
                }
                else -> {
                    throw RuntimeException("viewGroup.LayoutParams type is error ")
                }
            }
            (viewGroup.context as Activity).runOnUiThread {
                boardloadProgressView.layoutParams = layoutParams
                viewGroup.addView(boardloadProgressView)
            }
            listener?.let {
                boardloadProgressView.listener = WeakReference(it)
            }
            return boardloadProgressView
        }
    }

    fun show(viewGroup: ViewGroup, url: String) {
        this.url = url
        (viewGroup.context as Activity).runOnUiThread {
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
        val view = inflate(context, R.layout.agora_board_preload_progress_view_layout, this)
        progressLayout = view.findViewById(R.id.progress_Layout)
        progressText = view.findViewById(R.id.progress_Text)
        downloadProgress = view.findViewById(R.id.download_Progress)
    }

    fun updateProgress(progress: Double) {
        val a = progress * 10
        val b: Double = progress * 100
        post {
            this.downloadProgress?.progress = a.toInt()
            this.progressText?.text = "${b.toInt()}%"
        }
    }

    fun timeout() {
        progressText?.let { it ->
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.agora_board_preload_timeout_layout, null)
            timeoutLayout = view.findViewById(R.id.timeout_Layout)
            val layoutParams = ConstraintLayout.LayoutParams(0, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            layoutParams.startToStart = 0
            layoutParams.endToEnd = 0
            layoutParams.topToBottom = it.id
            timeoutLayout?.layoutParams = layoutParams
            this.progressLayout?.addView(view)
            val progressTextParams = it.layoutParams as ConstraintLayout.LayoutParams
            progressTextParams.bottomMargin = 0
            progressTextParams.bottomToBottom = -1
            this.progressText?.layoutParams = progressTextParams
            jumpOverText = view.findViewById(R.id.jumpOver_Text)
            jumpOverText?.setOnClickListener {
                listener?.let {
                    listener!!.get()?.onSkip(this.url)
                    dismiss()
                }
            }
        }
    }

    fun dismiss() {
        post {
            this.downloadProgress?.progress = 0
            this.progressText?.text = "0%"
            parent?.let {
                val viewGroup = parent as ViewGroup
                viewGroup.children.forEach {
                    if (it == this) {
                        viewGroup.removeView(this)
                        listener?.let {
                            listener!!.get()?.onProgressViewDismiss(this.url)
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

interface BoardPreloadProgressListener {
    fun onSkip(url: String?)
    fun onProgressViewDismiss(url: String?)
}