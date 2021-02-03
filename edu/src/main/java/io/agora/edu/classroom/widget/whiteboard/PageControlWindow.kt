package io.agora.edu.classroom.widget.whiteboard

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import io.agora.edu.R
import io.agora.edu.classroom.widget.window.AbstractWindow

class PageControlWindow : AbstractWindow, View.OnClickListener {
    private val TAG = "PageControlWindow"

    companion object {
        const val defaultWidth = 906
        const val maxWidth = 906
        const val minWidth = 708
    }

    private lateinit var previousIv: AppCompatImageView
    private lateinit var pageNoText: AppCompatTextView
    private lateinit var slashImg: AppCompatImageView
    private lateinit var pageTotalText: AppCompatTextView
    private lateinit var nextIv: AppCompatImageView
    private lateinit var enlargeIv: AppCompatImageView
    private lateinit var narrowIv: AppCompatImageView
    private lateinit var screenIv: AppCompatImageView
    var pageControlListener: PageControlListener? = null

    fun resize(width: Int) {
        if (width in minWidth..maxWidth) {
            val ratio = width / defaultWidth
            val previousParams = previousIv.layoutParams as LinearLayout.LayoutParams
            previousParams.width *= ratio
            previousIv.setPadding(previousIv.paddingStart * ratio, previousIv.paddingTop,
                    previousIv.paddingEnd * ratio, previousIv.paddingBottom)
            val pageNoParams = pageNoText.layoutParams as LinearLayout.LayoutParams
            pageNoParams.width *= ratio
            pageNoParams.marginStart *= ratio
            pageNoText.setPadding(pageNoText.paddingStart * ratio, pageNoText.paddingTop,
                    pageNoText.paddingEnd * ratio, pageNoText.paddingBottom)
            val slashParams = slashImg.layoutParams as LinearLayout.LayoutParams
            slashParams.width *= ratio
            slashParams.marginStart *= ratio
            val pageTotalParams = pageTotalText.layoutParams as LinearLayout.LayoutParams
            pageTotalParams.width *= ratio
            pageTotalParams.marginStart *= ratio
            val nextParams = nextIv.layoutParams as LinearLayout.LayoutParams
            nextParams.width *= ratio
            nextIv.setPadding(nextIv.paddingStart * ratio, nextIv.paddingTop,
                    nextIv.paddingEnd * ratio, nextIv.paddingBottom)
            val enlargeParams = enlargeIv.layoutParams as LinearLayout.LayoutParams
            enlargeParams.width *= ratio
            enlargeIv.setPadding(enlargeIv.paddingStart * ratio, enlargeIv.paddingTop,
                    enlargeIv.paddingEnd * ratio, enlargeIv.paddingBottom)
            val narrowParams = narrowIv.layoutParams as LinearLayout.LayoutParams
            narrowParams.width *= ratio
            narrowIv.setPadding(narrowIv.paddingStart * ratio, narrowIv.paddingTop,
                    narrowIv.paddingEnd * ratio, narrowIv.paddingBottom)
            val screenParams = screenIv.layoutParams as LinearLayout.LayoutParams
            screenParams.width *= ratio
            screenIv.setPadding(screenIv.paddingStart * ratio, screenIv.paddingTop,
                    screenIv.paddingEnd * ratio, screenIv.paddingBottom)
        }
    }

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
        slashImg = findViewById(R.id.slash_Img)
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
                pageControlListener?.onPrevious()
            }
            R.id.iv_next -> {
                pageControlListener?.onNext()
            }
            R.id.iv_enlarge -> {
                pageControlListener?.onEnlarge()
            }
            R.id.iv_narrow -> {
                pageControlListener?.onNarrow()
            }
            R.id.iv_screen -> {
                pageControlListener?.let {
                    if (screenIv.isSelected) {
                        it.onFitScreen()
                    } else {
                        it.onFullScreen()
                    }
                }
                screenIv.isSelected = !screenIv.isSelected
            }
        }
    }

    fun setPageIndex(index: Int, pages: Int) {
        previousIv.isSelected = index != 0
        nextIv.isSelected = index != pages
        pageNoText.text = (index + 1).toString()
        pageTotalText.text = pages.toString()
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