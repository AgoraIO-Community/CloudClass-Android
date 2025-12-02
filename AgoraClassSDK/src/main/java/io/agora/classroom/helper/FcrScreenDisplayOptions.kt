package io.agora.classroom.helper

import android.content.Context
import android.os.Looper

/**
 * 功能作用：屏幕操作函数定义
 * 创建人：王亮（Loren）
 * 思路：
 * 方法：
 * 注意：
 * 修改人：
 * 修改时间：
 * 备注：
 * 使用流程：
 *
 * @author 王亮（Loren）
 */
interface FcrScreenDisplayOptions {
    /**
     * 更新多屏显示
     *
     * @param showMore 是否显示副屏
     */
    fun updateMoreScreenShow(showMore: Boolean? = null)


    /**
     * 判断并切换主线程
     */
    fun runOnUiThread(runnable: Runnable)
}
