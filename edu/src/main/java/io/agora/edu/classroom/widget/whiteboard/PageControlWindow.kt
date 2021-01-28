package io.agora.edu.classroom.widget.whiteboard

import android.content.Context
import android.util.AttributeSet
import io.agora.edu.classroom.widget.window.AbstractWindow

class PageControlWindow : AbstractWindow {
    private val TAG = "PageControlWindow"

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

    }
}