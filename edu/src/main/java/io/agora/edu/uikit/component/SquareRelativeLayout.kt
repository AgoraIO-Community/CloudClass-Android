package io.agora.edu.uikit.component

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout

class SquareRelativeLayout(context: Context, attrRes: AttributeSet?, defStyleAttr: Int)
    : RelativeLayout(context, attrRes, defStyleAttr) {

    constructor(context: Context, attrRes: AttributeSet?): this(context, attrRes, 0)

    constructor(context: Context): this(context, null)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = MeasureSpec.getSize(widthMeasureSpec)
        val widthSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(size,MeasureSpec.EXACTLY)
        super.onMeasure(widthSpec, heightSpec)
        setMeasuredDimension(size, size)
    }
}