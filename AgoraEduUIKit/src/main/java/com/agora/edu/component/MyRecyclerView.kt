package com.agora.edu.component

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * author : wf
 * date : 2022/9/6 9:17 下午
 * description :
 */
class MyRecyclerView : RecyclerView {
    /**
     * 记录按下位置
     */
    private var interceptX = 0
    private var interceptY = 0
    private var childView: View? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    override fun onInterceptTouchEvent(e: MotionEvent?): Boolean {
        return true
    }

    override fun onTouchEvent(e: MotionEvent?): Boolean {
        e?.let {
            if (e.action == MotionEvent.ACTION_DOWN) {
                childView = findChildViewUnder(e.x, e.y)
            }

            if (childView is AgoraEduVideoComponent) {
                (childView as? AgoraEduVideoComponent)?.onMyTouchEvent(e)
            }
        }
        return super.onTouchEvent(e)
    }

    /**
     * 解决点击与拖动冲突
     *
     * @param ev
     * @return
     */
//    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
//        var isIntercept = true // 返回true则表示拦截TouchEvent由自己处理
//
//        when (ev.action) {
//            MotionEvent.ACTION_DOWN -> {
//                interceptX = ev.x.toInt()
//                interceptY = ev.y.toInt()
//                isIntercept = false
//            }
//
//            MotionEvent.ACTION_MOVE -> { // 左右滑动，拦截
//                LogX.i("MyRecyclerView", "x:${Math.abs(ev.x - interceptX)} y:${Math.abs(ev.y - interceptY)}")
//                isIntercept = Math.abs(ev.x - interceptX) > Math.abs(ev.y - interceptY)
//            }
//
//            MotionEvent.ACTION_UP -> {
//
//            }
//            else -> {}
//        }
//
//        LogX.i("MyRecyclerView", "上台列表，是否拦截：$isIntercept ，action = ${ev.action}")
//        return isIntercept
//    }

}