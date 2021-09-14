package io.agora.edu.uikit.util

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
}