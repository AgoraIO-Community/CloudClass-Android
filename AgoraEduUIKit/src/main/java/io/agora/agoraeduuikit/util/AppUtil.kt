package io.agora.agoraeduuikit.util

import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup

object AppUtil {
    /**
     * 防止按钮连续点击
     */
    private var lastClickTime: Long = 0

    @Synchronized
    fun isFastClick(): Boolean {
        val time = System.currentTimeMillis()
        if (time - lastClickTime < 700) {
            return true
        }
        lastClickTime = time
        return false
    }

    @Synchronized
    fun isFastClick(interval: Long): Boolean {
        val time = System.currentTimeMillis()
        if (time - lastClickTime < interval) {
            return true
        }
        lastClickTime = time
        return false
    }

    fun isVisibleToUser(view: View, rootParentId: Int): Boolean {
        var visible = view.visibility
        while (true) {
            val parent = view.parent
            if (parent !is ViewGroup) {
                return false
            }
            if (parent.id != rootParentId) {
                visible = parent.visibility
            } else {
                break
            }
        }
        return visible == VISIBLE
    }
}