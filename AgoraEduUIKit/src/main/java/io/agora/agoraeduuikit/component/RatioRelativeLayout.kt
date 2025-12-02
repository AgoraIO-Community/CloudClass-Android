package io.agora.agoraeduuikit.component

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout

class RatioRelativeLayout(context: Context, attrRes: AttributeSet?, defStyleAttr: Int)
    : RelativeLayout(context, attrRes, defStyleAttr) {
    private var ratio: Float = 1f

    constructor(context: Context, attrRes: AttributeSet?): this(context, attrRes, 0)

    constructor(context: Context): this(context, null)

    fun setRatio(ratio: Float) {
        this.ratio = ratio
        invalidate()
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = MeasureSpec.getSize(widthMeasureSpec)
        val width = (height * ratio).toInt()
        val widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(height,MeasureSpec.EXACTLY)
        super.onMeasure(widthSpec, heightSpec)
        setMeasuredDimension(width, height)
    }
}