package com.hyphenate.easeimgroup.modules.utils

import android.content.Context
import android.util.TypedValue
import kotlin.properties.Delegates

class ScreenUtil {

    companion object {
        const val TAG = "ScreenUtil"
        val instance by lazy(LazyThreadSafetyMode.NONE) {
            ScreenUtil()
        }
    }

    var screenWidth by Delegates.notNull<Int>()
    var screenHeight by Delegates.notNull<Int>()

    fun dip2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    //像素转dp px2dp
    fun px2dip(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    fun sp2px(context: Context, spValue: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue, context.resources.displayMetrics)
    }

}