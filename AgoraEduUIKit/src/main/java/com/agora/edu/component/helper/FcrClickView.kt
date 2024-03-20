package com.agora.edu.component.helper

import android.os.Handler
import android.view.MotionEvent
import android.view.View
import com.agora.edu.component.view.FcrDragTouchGroupView
import io.agora.agoraeducore.core.internal.log.LogX

/**
 * author : felix
 * date : 2023/10/9
 * description :
 */
class FcrClickView {
    private var downX = 0f
    private var downY = 0f
    private val isEnableDrag = true
    private val touchLimit = 0.1f
    private val handler = Handler()
    private var count = 0 //点击次数
    private var firstClick: Long = 0 //第一次点击时间
    private var secondClick: Long = 0 //第二次点击时间
    private var lastDoubleClick: Long = 0 //上次响应成功时间戳
    private var isDoubleClicked = false // 当前view是否被双击

    /**
     * 两次点击时间间隔，单位毫秒
     */
    private val totalTimeOne = 200
    private val totalTimeTwo = 500

    /**
     * 双击 响应 间隔
     */
    private val timeComplete = 1000

    var listener: FcrDragTouchGroupView.OnDoubleClickListener? = null

    fun onTouchEvent(view: View, event: MotionEvent) {
        //super.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                downForClick()
            }

            MotionEvent.ACTION_MOVE -> {
            }

            MotionEvent.ACTION_UP -> {
                val xDistance = event.x - downX
                val yDistance = event.y - downY

                LogX.e("FcrClickView", "x=" + Math.abs(xDistance) + "||y=" + Math.abs(yDistance))
                if (Math.abs(xDistance) < touchLimit && Math.abs(yDistance) < touchLimit) {
                    // 避免拖动的时候，触发单击事件
                    upForClick()
                } else {
                    // 避免拖动的时候，触发双击事件
                    handler.removeCallbacksAndMessages(null)
                    count = 0
                    firstClick = 0
                    secondClick = 0
                }
                view.setPressed(false)
            }

            MotionEvent.ACTION_CANCEL -> view.setPressed(false)
            else -> {}
        }
    }

    fun downForClick() {
        count++
        if (1 == count) {
            firstClick = System.currentTimeMillis() //记录第一次点击时间
            isDoubleClicked = false
        } else if (2 == count) {
            handler.removeCallbacksAndMessages(null)
            secondClick = System.currentTimeMillis() //记录第二次点击时间
            if (secondClick - firstClick < totalTimeTwo) { //判断二次点击时间间隔是否在设定的间隔时间之内
                /*判断 上次按钮生效间隔 */
                if (secondClick - lastDoubleClick > timeComplete) {
                    lastDoubleClick = secondClick
                    if (listener != null) {
                        listener?.onDoubleClick()
                        isDoubleClicked = true
                    }
                }
                count = 0
                firstClick = 0
                secondClick = 0
            } else {
                firstClick = secondClick
                count = 1
            }
        }
    }

    fun upForClick() {
        if (1 == count) {
            val firstUpClick = System.currentTimeMillis() //记录第一次点击时间
            val deltaClick: Long = firstUpClick - firstClick
            if (deltaClick < totalTimeOne / 2) {
                handler.postDelayed(Runnable {
                    listener?.onClick()
                    count = 0
                    firstClick = 0
                    secondClick = 0
                }, totalTimeOne.toLong())
            }
        }
    }
}