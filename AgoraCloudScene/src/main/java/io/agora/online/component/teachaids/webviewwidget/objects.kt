package io.agora.online.component.teachaids.webviewwidget

data class FcrWebViewInteractionPacket(val signal: FcrWebViewInteractionSignal, val body: Any)

enum class FcrWebViewInteractionSignal(val value: Int) {

    //开启大窗
    FcrWebViewShowed(1),

    //关闭大窗
    FcrWebViewClosed(2)
}


