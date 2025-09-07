package io.agora.online.widget

/**
 * author : felix
 * date : 2023/6/14
 * description :
 */
object FcrWidgetManager {
    val WIDGET_ID_DASH = "-"
    val WIDGET_Z_INDEX = "zIndex"
    val WIDGET_WEBVIEW = "webView"
    val WIDGET_WEBVIEW_RUL = "webViewUrl"
    val WIDGET_WEBVIEW_TITLE = "webviewTitle"
    val WIDGET_WEBVIEW_MEDIAPLAYER = "mediaPlayer"
    val WIDGETS_RTT_ID = "rtt"

    fun isWebViewWidget(widgetId: String): Boolean {
        return widgetId.contains(WIDGET_WEBVIEW) || widgetId.contains(WIDGET_WEBVIEW_MEDIAPLAYER)
    }

}