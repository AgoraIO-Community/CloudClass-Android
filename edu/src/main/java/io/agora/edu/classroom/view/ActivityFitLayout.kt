package io.agora.edu.classroom.view

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout

class ActivityFitLayout : RelativeLayout {
    constructor(context: Context) : super(context) {

    }

    constructor(context: Context, attr: AttributeSet) : super(context, attr) {

    }

    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr) {

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val modeWidth = MeasureSpec.getMode(widthMeasureSpec)
        val modeHeight = MeasureSpec.getMode(heightMeasureSpec)

        if (modeWidth != MeasureSpec.EXACTLY || modeHeight != MeasureSpec.EXACTLY) {
            return
        }

        var sizeWidth = MeasureSpec.getSize(widthMeasureSpec)
        var sizeHeight = MeasureSpec.getSize(heightMeasureSpec)

        val ratio = 16 / 9f
        val ratioView = sizeWidth / sizeHeight.toFloat()
        if (ratioView.compareTo(ratio) >= 0) {
            // The original view size is wider than or equal to the standard w/h ratio
            sizeWidth = (sizeHeight * ratio).toInt()
        } else {
            // The original view size is higher than the standard w/h ratio
            sizeHeight = (sizeWidth /ratio ).toInt()
        }

        setMeasuredDimension(sizeWidth, sizeHeight)
        super.onMeasure(MeasureSpec.makeMeasureSpec(sizeWidth, modeWidth),
            MeasureSpec.makeMeasureSpec(sizeHeight, modeHeight))
    }
}