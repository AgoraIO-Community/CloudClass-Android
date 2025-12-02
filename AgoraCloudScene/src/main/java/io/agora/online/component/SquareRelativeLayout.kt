package io.agora.online.component

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout

class SquareRelativeLayout(context: Context, attrRes: AttributeSet?, defStyleAttr: Int)
    : RelativeLayout(context, attrRes, defStyleAttr) {

    constructor(context: Context, attrRes: AttributeSet?): this(context, attrRes, 0)

    constructor(context: Context): this(context, null)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val size = width.coerceAtMost(height)
        setMeasuredDimension(size, size)
        val wMeasure = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
        val hMeasure = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
        super.onMeasure(wMeasure, hMeasure)
    }
}