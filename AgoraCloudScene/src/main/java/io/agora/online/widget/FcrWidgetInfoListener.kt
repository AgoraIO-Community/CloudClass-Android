package io.agora.online.widget


/**
 * author : felix
 * date : 2023/6/16
 * description :
 */
interface FcrWidgetInfoListener {
    fun onWidgetUpdate(isShow: Boolean, widgetId: String, count: Int)

    /**
     * 激活的widget
     */
    fun onActiveWidget(widgetId: String)
}