package com.agora.edu.component.teachaids.webviewwidget

/**
 * author : cjw
 * date : 2022/2/16
 * description : 视频大窗
 */
data class FcrWebViewWidgetExtra(
    // 当前视频widget是否全屏(仅限于限制区域内)
    val webViewUrl: String?,
    // 当前视频widget的垂直层级
    val zIndex: Float?
)
