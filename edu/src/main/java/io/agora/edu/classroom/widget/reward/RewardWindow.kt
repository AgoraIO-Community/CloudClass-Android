package io.agora.edu.classroom.widget.reward

import android.content.Context
import android.util.AttributeSet
import io.agora.edu.classroom.widget.window.AbstractWindow

class RewardWindow : AbstractWindow {

    constructor(context: Context) : super(context) {
        view()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        view()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
            context, attrs, defStyleAttr
    ) {
        view()
    }

    private fun view() {
    }
}