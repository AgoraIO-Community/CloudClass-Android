package com.hyphenate.easeim.modules.utils

import android.content.Context
import android.util.TypedValue
import kotlin.properties.Delegates

class ScreenUtil {

    companion object{
        const val TAG = "ScreenUtil"
        val instance by lazy(LazyThreadSafetyMode.NONE){
            ScreenUtil()
        }
    }
    var screenWidth by Delegates.notNull<Int>()
    var screenHeight by Delegates.notNull<Int>()
    lateinit var context: Context

    fun init(context: Context){
        this.context = context
    }


    fun dip2px(dpValue: Float): Int{
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    fun px2dip(pxValue: Float): Int{
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    fun sp2px(spValue: Float): Float{
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue, context.resources.displayMetrics)
    }

}