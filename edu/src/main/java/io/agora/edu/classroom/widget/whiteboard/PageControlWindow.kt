package io.agora.edu.classroom.widget.whiteboard

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import io.agora.edu.R
import io.agora.edu.classroom.widget.window.AbstractWindow

class PageControlWindow : AbstractWindow, View.OnClickListener {
    private val TAG = "PageControlWindow"
    private lateinit var previousIv: AppCompatImageView
    private lateinit var pageNoText: AppCompatTextView
    private lateinit var pageTotalText: AppCompatTextView
    private lateinit var nextIv: AppCompatImageView
    private lateinit var enlargeIv: AppCompatImageView
    private lateinit var narrowIv: AppCompatImageView
    private lateinit var screenIv: AppCompatImageView
    var pageControlListener: PageControlListener? = null

    constructor(context: Context) : super(context) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
            context, attrs, defStyleAttr
    ) {
        initView()
    }

    private fun initView() {
        inflate(context, R.layout.page_control_window_layout, this)
        previousIv = findViewById(R.id.iv_previous)
        pageNoText = findViewById(R.id.pageNo_Text)
        pageTotalText = findViewById(R.id.pageTotal_Text)
        nextIv = findViewById(R.id.iv_next)
        enlargeIv = findViewById(R.id.iv_enlarge)
        narrowIv = findViewById(R.id.iv_narrow)
        screenIv = findViewById(R.id.iv_screen)
        previousIv.setOnClickListener(this)
        nextIv.setOnClickListener(this)
        enlargeIv.setOnClickListener(this)
        narrowIv.setOnClickListener(this)
        screenIv.setOnClickListener(this)
        previousIv.isSelected = false
        nextIv.isSelected = false
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_previous -> {
                pageControlListener?.let {
                    it.onPrevious()
                }
            }
            R.id.iv_next -> {
                pageControlListener?.let {
                    it.onNext()
                }
            }
            R.id.iv_enlarge -> {
                pageControlListener?.let {
                    it.onEnlarge()
                }
            }
            R.id.iv_narrow -> {
                pageControlListener?.let {
                    it.onNarrow()
                }
            }
            R.id.iv_screen -> {
                pageControlListener?.let {
                    if(isSelected) {
                        it.onFitScreen()
                    } else {
                        it.onFullScreen()
                    }
                }
                isSelected = !isSelected
            }
        }
    }





    interface PageControlListener {
        fun onPrevious()

        fun onNext()

        fun onEnlarge()

        fun onNarrow()

        fun onFullScreen()

        fun onFitScreen()
    }
}